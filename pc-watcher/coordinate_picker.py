import time

import pyautogui


print("Move the mouse to a corner of the number region. Press Ctrl+C to stop.")
try:
    while True:
        x, y = pyautogui.position()
        print(f"x={x:5d}, y={y:5d}", end="\r", flush=True)
        time.sleep(0.1)
except KeyboardInterrupt:
    print("\nStopped.")
