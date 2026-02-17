import cv2
import mediapipe as mp
import socket
import asyncio
from time import time
import threading
import numpy as np
import struct
import json
import logging
from logging.handlers import RotatingFileHandler
import os
import glob

# === Configurazione del logging ===
debug = True
LOG_FILE = "app.log"
LOG_MAX_TOTAL_SIZE = 1 * 1024 * 1024 * 1024  # 1 GB

def check_log_size():
    """Elimina i log più vecchi se la dimensione totale supera il limite."""
    log_pattern = LOG_FILE + "*"
    log_files = sorted(glob.glob(log_pattern), key=os.path.getmtime)
    total_size = sum(os.path.getsize(f) for f in log_files if os.path.exists(f))
    if total_size > LOG_MAX_TOTAL_SIZE:
        print(f"[LOG CLEANUP] Total log size {total_size / (1024**2):.2f} MB > 1 GB, cleaning old logs...")
        while total_size > LOG_MAX_TOTAL_SIZE and log_files:
            oldest = log_files.pop(0)
            try:
                os.remove(oldest)
                print(f"[LOG CLEANUP] Removed old log: {oldest}")
            except Exception as e:
                print(f"[LOG CLEANUP] Error removing {oldest}: {e}")
            total_size = sum(os.path.getsize(f) for f in log_files if os.path.exists(f))

check_log_size()

logger = logging.getLogger("HandDetector")
logger.setLevel(logging.DEBUG if debug else logging.INFO)

file_handler = RotatingFileHandler(LOG_FILE, maxBytes=5*1024*1024, backupCount=5)
file_handler.setFormatter(logging.Formatter(
    "%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S"
))
logger.addHandler(file_handler)

if debug:
    console_handler = logging.StreamHandler()
    console_handler.setFormatter(logging.Formatter(
        "%(asctime)s [%(levelname)s] %(message)s",
        datefmt="%H:%M:%S"
    ))
    logger.addHandler(console_handler)

def log_info(msg): logger.info(msg)
def log_error(msg): logger.error(msg)
def log_debug(msg): logger.debug(msg)

# === Impostazioni ===
node = "#RTNode6="
lastMessageTS = 0
lastFlags = "11110"
handFound = 0
lastFoundTS = 0
status_msg = ""
status_expire_ts = 0.0
STATUS_DURATION = 1.5  # sec

UDP_IP = "172.16.16.2"
UDP_PORT = 3478
VIDEOSERVER_IP = UDP_IP
VIDEOSERVER_PORT = 9999

polygon = np.array(
    [(639, 258), (638, 480), (0, 480), (0, 235), (180, 0), (480, 0)],
    dtype=np.int32
)

# === Importazione di coordPoiList da un file separato (.txt) con fallback ===
import os
import ast  # metodo sicuro per convertire una stringa in un oggetto Python

coordPoiList_file = "coordPoiList.txt"

# Opzione di backup se il file non viene trovato
coordPoiList_default = [
    [(415, 412, 470, 515, 535), (216, 183, 163, 190, 240)],
    [(359, 353, 319, 460, 481, 417), (140, 106, 57, 51, 89, 150)],
    [(192, 317, 358, 334, 120), (172, 172, 237, 280, 292)],
    [(258, 302, 340, 292, 247), (118, 69, 102, 161, 142)]
]

try:
    if os.path.exists(coordPoiList_file):
        with open(coordPoiList_file, "r", encoding="utf-8") as f:
            content = f.read()
            coordPoiList = ast.literal_eval(content)
    else:
        print(f"File {coordPoiList_file} non trovato, usando il fallback.")
        coordPoiList = coordPoiList_default
except Exception as e:
    print(f"Errore leggendo {coordPoiList_file}: {e}, uso fallback.")
    coordPoiList = coordPoiList_default

# === Classe della fotocamera con thread separato ===
class CameraThread:
    def __init__(self, src=0):
        self.cap = cv2.VideoCapture(src)
        self.cap.set(cv2.CAP_PROP_FRAME_WIDTH, 480)
        self.cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 360)
        self.cap.set(cv2.CAP_PROP_FPS, 24)
        self.cap.set(cv2.CAP_PROP_EXPOSURE, 0)
        self.cap.set(cv2.CAP_PROP_FOURCC, cv2.VideoWriter_fourcc(*"MJPG"))
        self.ret, self.frame = self.cap.read()
        self.lock = threading.Lock()
        self.running = True
        threading.Thread(target=self.update, daemon=True).start()
        log_info("Camera thread started.")

    def update(self):
        while self.running:
            ret, frame = self.cap.read()
            with self.lock:
                self.ret = ret
                self.frame = frame

    def read(self):
        with self.lock:
            return self.ret, self.frame.copy()

    def release(self):
        self.running = False
        self.cap.release()
        log_info("Camera released.")

def is_inside(x, y, polygon):
    n = len(polygon)
    inside = False
    p1x, p1y = polygon[0]
    for i in range(n + 1):
        p2x, p2y = polygon[i % n]
        if y > min(p1y, p2y):
            if y <= max(p1y, p2y):
                if x <= max(p1x, p2x):
                    if p1y != p2y:
                        xints = (y - p1y) * (p2x - p1x) / (p2y - p1y) + p1x
                    if p1x == p2x or x <= xints:
                        inside = not inside
        p1x, p1y = p2x, p2y
    return inside

def _set_status(msg):
    global status_msg, status_expire_ts
    status_msg = msg
    status_expire_ts = time() + STATUS_DURATION
    log_info(msg)

def inPolygon(x, y, coordinates):
    polygon = list(zip(coordinates[0], coordinates[1]))
    n = len(polygon)
    inside = False
    px, py = polygon[0]
    for i in range(n + 1):
        px2, py2 = polygon[i % n]
        if y > min(py, py2):
            if y <= max(py, py2):
                if x <= max(px, px2):
                    if py != py2:
                        x_intersection = (y - py) * (px2 - px) / (py2 - py) + px
                    if px == px2 or x <= x_intersection:
                        inside = not inside
        px, py = px2, py2
    return inside

# === Invio UDP ===
async def sendUDP(message):
    loop = asyncio.get_running_loop()
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    await loop.run_in_executor(None, sock.sendto, message.encode(), (UDP_IP, UDP_PORT))
    sock.close()
    log_debug(f"[UDP] Sent: {message}")

# === Funzione di elaborazione della mano ===
def findPosition(results, img):
    global lastMessageTS, lastFlags, handFound
    flags = ""
    if results.multi_hand_landmarks:
        for hand_landmarks in results.multi_hand_landmarks:
            h, w, _ = img.shape
            cx = [int(lm.x * w) for lm in hand_landmarks.landmark]
            cy = [int(lm.y * h) for lm in hand_landmarks.landmark]

            cv2.circle(img, (cx[8], cy[8]), 5, (255, 0, 0), cv2.FILLED)

            if not is_inside(cx[8], cy[8], polygon):
                return

            if handFound == 0:
                handFound = 1
                asyncio.create_task(sendUDP(node + "11110"))
                lastFlags = "11110"
                _set_status("Hand entered frame")
                return

            touched_indices = []
            flags = ""
            for idx, poi_coords in enumerate(coordPoiList):
                if inPolygon(cx[8], cy[8], poi_coords):
                    touched_indices.append(f"poly_{idx}")
                    _set_status(f"Finger inside polygon {idx}")
                    flags += "0"
                    cv2.circle(img, (cx[8], cy[8]), 7, (0, 255, 255), cv2.FILLED)
                else:
                    flags += "1"
            flags += "0"

            if touched_indices:
                _set_status("POI touched: " + ",".join(str(i) for i in touched_indices))

            if flags and flags != lastFlags and time() - lastMessageTS > 0.5:
                asyncio.create_task(sendUDP(node + flags))
                lastFlags = flags
                lastMessageTS = time()
                _set_status("Flags sent: " + flags)

# === Sender TCP asincrono con riconnessione ===
async def tcp_sender(host, port, get_packet):
    writer = None
    while True:
        try:
            if writer is None:
                reader, writer = await asyncio.open_connection(host, port)
                log_info(f"Connected to TCP server {host}:{port}")

            packet = get_packet()
            if packet:
                writer.write(packet)
                await writer.drain()

        except (ConnectionResetError, ConnectionError, OSError) as e:
            log_error(f"TCP connection error: {e}, reconnecting...")
            if writer:
                try:
                    writer.close()
                    await writer.wait_closed()
                except Exception:
                    pass
            writer = None
            await asyncio.sleep(2)

        await asyncio.sleep(0.01)  # пауза между отправками

# === Ciclo principale ===
async def main_loop():
    global handFound, lastFlags, lastFoundTS, status_msg, status_expire_ts
    camera = CameraThread(0)
    mpHands = mp.solutions.hands
    hands = mpHands.Hands(
        static_image_mode=False,
        max_num_hands=1,
        min_detection_confidence=0.5,
        min_tracking_confidence=0.5
    )

    frame_queue = []

    def get_frame_packet():
        if not frame_queue:
            return None
        return frame_queue.pop(0)

    asyncio.create_task(tcp_sender(VIDEOSERVER_IP, VIDEOSERVER_PORT, get_frame_packet))

    try:
        while True:
            await asyncio.sleep(0.03)
            success, frame = camera.read()
            if not success or frame is None:
                continue

            imgRGB = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            results = hands.process(imgRGB)

            if results.multi_hand_landmarks:
                lastFoundTS = time()
                for handLms in results.multi_hand_landmarks:
                    mp.solutions.drawing_utils.draw_landmarks(frame, handLms, mpHands.HAND_CONNECTIONS)
                findPosition(results, frame)
            else:
                if time() - lastFoundTS > 2 and lastFlags != "11111":
                    handFound = 0
                    asyncio.create_task(sendUDP(node + "11111"))
                    lastFlags = "11111"
                    _set_status("Hand lost")

            if status_msg and time() < status_expire_ts:
                cv2.putText(frame, status_msg, (10,30), cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0,255,0),2,cv2.LINE_AA)

            cv2.imshow("Hand Tracking", frame)
            if cv2.waitKey(1) & 0xFF == 27:
                break

            try:
                ok, jpeg = cv2.imencode(".jpg", frame, [int(cv2.IMWRITE_JPEG_QUALITY), 80])
                if ok:
                    frame_bytes = jpeg.tobytes()
                    meta = {"user": node, "frame_len": len(frame_bytes)}
                    meta_json = json.dumps(meta).encode("utf-8")
                    header = struct.pack("<Q", len(meta_json))
                    packet = header + meta_json + frame_bytes
                    frame_queue.append(packet)
            except Exception as e:
                log_error(f"Frame encode failed: {e}")

    finally:
        camera.release()
        cv2.destroyAllWindows()
        log_info("Application closed.")

if __name__ == "__main__":
    try:
        asyncio.run(main_loop())
    except KeyboardInterrupt:
        log_info("Interrupted by user")
