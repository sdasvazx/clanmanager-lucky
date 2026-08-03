from __future__ import annotations

import argparse
import json
import re
import time
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import Any

import cv2
import easyocr
import mss
import numpy as np
import requests


VALUE_PATTERN = re.compile(r"^(\d+)/(\d+)$")


@dataclass(frozen=True)
class Target:
    target_id: str
    monitor: int
    left: int
    top: int
    width: int
    height: int
    threshold: int
    diff_threshold: float

    @classmethod
    def from_dict(cls, data: dict[str, Any], defaults: dict[str, Any]) -> "Target":
        region = data["region"]
        return cls(
            target_id=str(data["targetId"]),
            monitor=int(data.get("monitor", 1)),
            left=int(region["left"]),
            top=int(region["top"]),
            width=int(region["width"]),
            height=int(region["height"]),
            threshold=int(data.get("threshold", defaults.get("threshold", 150))),
            diff_threshold=float(data.get("diffThreshold", defaults.get("diffThreshold", 2.0))),
        )

    def capture_region(self, monitors: list[dict[str, int]]) -> dict[str, int]:
        if self.monitor < 1 or self.monitor >= len(monitors):
            raise ValueError(f"{self.target_id}: monitor {self.monitor} does not exist")
        display = monitors[self.monitor]
        return {
            "left": display["left"] + self.left,
            "top": display["top"] + self.top,
            "width": self.width,
            "height": self.height,
        }


def load_json(path: Path) -> dict[str, Any]:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def save_json(path: Path, data: dict[str, Any]) -> None:
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")


def preprocess(frame: np.ndarray, threshold: int) -> np.ndarray:
    gray = cv2.cvtColor(frame, cv2.COLOR_BGRA2GRAY)
    enlarged = cv2.resize(gray, None, fx=3, fy=3, interpolation=cv2.INTER_CUBIC)
    _, binary = cv2.threshold(enlarged, threshold, 255, cv2.THRESH_BINARY)
    return binary


def recognize(reader: easyocr.Reader, binary: np.ndarray) -> str | None:
    results = reader.readtext(binary, detail=0, paragraph=False, allowlist="0123456789/")
    compact = "".join(str(value) for value in results).replace(" ", "")
    match = VALUE_PATTERN.fullmatch(compact)
    return match.group(0) if match else None


def numerator(value: str) -> int:
    match = VALUE_PATTERN.fullmatch(value)
    if not match:
        raise ValueError(f"invalid recognized value: {value}")
    return int(match.group(1))


def report_change(config: dict[str, Any], target_id: str, old_value: str, new_value: str) -> None:
    api = config["backend"]
    response = requests.post(
        f"{str(api['baseUrl']).rstrip('/')}/api/watch/report",
        headers={"Authorization": f"Bearer {api['apiKey']}"},
        json={
            "targetId": target_id,
            "oldValue": old_value,
            "newValue": new_value,
            # Spring LocalDateTime expects an offset-free ISO value.
            "changedAt": datetime.now().astimezone().replace(tzinfo=None).isoformat(timespec="seconds"),
        },
        timeout=10,
    )
    response.raise_for_status()


def main() -> None:
    parser = argparse.ArgumentParser(description="Watch game number regions and report numerator changes.")
    parser.add_argument("--config", default="config.json", help="Path to JSON configuration")
    parser.add_argument("--debug", action="store_true", help="Save captured and threshold images")
    parser.add_argument("--once", action="store_true", help="Capture each target once and exit")
    args = parser.parse_args()

    config_path = Path(args.config).resolve()
    config = load_json(config_path)
    targets = [Target.from_dict(row, config.get("defaults", {})) for row in config["targets"]]
    interval = float(config.get("intervalSeconds", 2))
    state_path = config_path.parent / str(config.get("stateFile", "watch-state.json"))
    debug_dir = config_path.parent / str(config.get("debugDirectory", "debug-captures"))
    state = load_json(state_path) if state_path.exists() else {}
    previous_frames: dict[str, np.ndarray] = {}
    reader = easyocr.Reader(["en"], gpu=bool(config.get("easyocrGpu", False)))

    if args.debug:
        debug_dir.mkdir(parents=True, exist_ok=True)

    with mss.mss() as screen:
        print(f"Watching {len(targets)} target(s) every {interval:g}s. Press Ctrl+C to stop.")
        while True:
            for target in targets:
                frame = np.asarray(screen.grab(target.capture_region(screen.monitors)))
                gray = cv2.cvtColor(frame, cv2.COLOR_BGRA2GRAY)
                previous = previous_frames.get(target.target_id)
                diff_mean = float(cv2.absdiff(gray, previous).mean()) if previous is not None else float("inf")
                previous_frames[target.target_id] = gray

                if previous is not None and diff_mean < target.diff_threshold:
                    continue

                binary = preprocess(frame, target.threshold)
                if args.debug:
                    stamp = datetime.now().strftime("%Y%m%d-%H%M%S-%f")
                    cv2.imwrite(str(debug_dir / f"{target.target_id}-{stamp}-raw.png"), frame)
                    cv2.imwrite(str(debug_dir / f"{target.target_id}-{stamp}-binary.png"), binary)

                value = recognize(reader, binary)
                if value is None:
                    print(f"[{target.target_id}] OCR skipped (diff={diff_mean:.2f}, invalid pattern)")
                    continue

                old_value = state.get(target.target_id)
                if old_value is None:
                    state[target.target_id] = value
                    save_json(state_path, state)
                    print(f"[{target.target_id}] initial value: {value}")
                    continue

                # The denominator is informational; alerts are emitted only when the numerator changes.
                if numerator(old_value) == numerator(value):
                    if old_value != value:
                        state[target.target_id] = value
                        save_json(state_path, state)
                    continue

                try:
                    report_change(config, target.target_id, old_value, value)
                except requests.RequestException as error:
                    # Do not advance persisted state when delivery fails; the next changed frame retries it.
                    print(f"[{target.target_id}] report failed: {error}")
                    continue

                state[target.target_id] = value
                save_json(state_path, state)
                print(f"[{target.target_id}] changed: {old_value} -> {value}")

            if args.once:
                break
            time.sleep(interval)


if __name__ == "__main__":
    main()
