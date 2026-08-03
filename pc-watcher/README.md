# 게임 화면 숫자 감시기

게임 화면의 `3/200` 같은 숫자를 2초마다 확인하고, 분자가 바뀌면 운좋은 백엔드에 전송합니다. 백엔드는 DB에 기록한 뒤 Discord Webhook으로 알립니다.

## 설치

Python 3.11을 권장합니다. 첫 설치와 EasyOCR 모델 다운로드에는 인터넷 연결이 필요합니다.

```powershell
cd pc-watcher
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
Copy-Item config.example.json config.json
```

## 좌표 설정

1. 게임을 실제 감시할 위치와 해상도로 실행합니다.
2. `python coordinate_picker.py`를 실행합니다.
3. 숫자 영역의 왼쪽 위와 오른쪽 아래 좌표를 확인합니다.
4. `config.json`의 `left`, `top`, `width`, `height`를 수정합니다.

예시 좌표는 제공된 708×322 이미지에서 오른쪽 아래 `3/200`을 기준으로 한 값이므로 실제 화면에서는 반드시 다시 잡아야 합니다. 좌표는 선택한 모니터의 왼쪽 위를 기준으로 합니다.

## OCR 시험 및 디버그

```powershell
python watcher.py --config config.json --debug --once
```

`debug-captures`에 원본 영역과 threshold 적용 이미지가 저장됩니다. 숫자가 끊기면 영역을 넓히거나 `threshold`를 120~200 사이에서 조정합니다.

## 계속 감시

```powershell
python watcher.py --config config.json
```

첫 정상 인식값은 기준값으로만 저장합니다. 이후 프레임 변화가 있고 `숫자/숫자` 형식이 정상 인식되며 분자가 바뀐 경우에만 서버로 보냅니다. 여러 영역은 `targets` 배열에 추가할 수 있습니다.

## Railway 환경변수

- `WATCH_API_KEY`: 충분히 긴 임의 문자열. PC의 `config.json` `apiKey`와 동일해야 합니다.
- `DISCORD_WEBHOOK_URL`: Discord 채널 설정에서 만든 Webhook URL.

Webhook URL과 API 키는 GitHub에 커밋하지 마세요. 게임 화면 자동 인식이 게임 이용약관에 허용되는지는 별도로 확인해야 합니다.
