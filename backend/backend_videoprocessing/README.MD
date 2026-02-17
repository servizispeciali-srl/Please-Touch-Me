# Sistema di Hand Tracking UDP/TCP

![Python](https://img.shields.io/badge/python-3.10+-blue) ![OpenCV](https://img.shields.io/badge/OpenCV-4.x+-green) ![MediaPipe](https://img.shields.io/badge/MediaPipe-latest-orange)

Sistema di tracciamento della mano utilizzando **MediaPipe** e **OpenCV**, con invio dei dati tramite **UDP** e **TCP**, interazione con un server video e gestione di punti di interesse (POI) sullo schermo.

---

## 📋 Descrizione

Il programma:

- Acquisisce video dalla camera (fonte configurabile).
- Rileva mani e coordinate delle dita con **MediaPipe Hands**.
- Verifica se le dita sono all'interno di poligoni definiti (POI).
- Invia lo stato della mano e flag tramite **UDP**.
- Invia i frame tramite **TCP** al server video.
- Gestisce i log rotativi con pulizia automatica dei file vecchi.
- Mostra messaggi di stato temporanei sul frame.

---

## ⚙️ Funzionalità

1. **Classe CameraThread**
   - Lettura video in un thread separato.
   - Risoluzione, FPS e formato MJPG configurabili.

2. **Tracciamento della mano**
   - Utilizzo di **MediaPipe Hands**.
   - Rilevamento posizione delle dita e controllo dei poligoni POI.

3. **Sistema di logging**
   - RotatingFileHandler con limite massimo dei log.
   - Output dei log in console se `debug` abilitato.

4. **Comunicazione di rete**
   - **UDP**: invio dei flag dello stato della mano.
   - **TCP**: invio asincrono dei frame con riconnessione automatica.

5. **Gestione dei POI**
   - Lettura delle coordinate da `coordPoiList.txt` o fallback predefinito.
   - Controllo se il dito indice si trova all'interno dei poligoni.

6. **Messaggi di stato**
   - Notifiche temporanee sul video per indicare lo stato corrente della mano.

---

## 🛠 Installazione
<br>

1. Clonare il repository:
<br>

```bash
git clone <URL del repository>
cd <nome repository>
```
<br>

2. Creare l'ambiente virtuale
<br>
È consigliato creare un ambiente virtuale per isolare le dipendenze:

```bash
python -m venv venv
```

Attivare l'ambiente:

Su Linux/macOS:
```bash
source venv/bin/activate
```

Su Windows:
```bash
venv\Scripts\activate
```
<br>

3. Installare le dipendenze
<br>
All'interno dell'ambiente virtuale, installare le librerie necessarie:
```bash
pip install -r requirements.txt
```

requirements.txt dovrebbe contenere almeno:

```bash
opencv-python
mediapipe
numpy
```

<br>

4. Configurare l'applicazione
<br>
Prima di eseguire l'applicazione, è possibile modificare alcune impostazioni nel file videoprocessing.py:

Server UDP:

```bash
UDP_IP = "172.16.16.2"
UDP_PORT = 3478
```

Server video TCP:
```bash
VIDEOSERVER_IP = "172.16.16.2"
VIDEOSERVER_PORT = 9999
```

Poligoni POI:
Se vuoi utilizzare un file esterno, crea coordPoiList.txt con coordinate dei poligoni.
Formato esempio:

```bash
[
    [(415, 412, 470, 515, 535), (216, 183, 163, 190, 240)],
    [(359, 353, 319, 460, 481, 417), (140, 106, 57, 51, 89, 150)]
]
```

Se il file non esiste, il programma userà valori di default integrati.

Logging:

```bash
LOG_FILE = "app.log"
LOG_MAX_TOTAL_SIZE = 1 * 1024 * 1024 * 1024  # 1 GB
debug = True  # True per visualizzare log anche in console
```

<br>

5. Eseguire l'applicazione
<br>
Da terminale:

```bash
python videoprocessing.py
```

La finestra video mostrerà la mano rilevata e messaggi di stato.

Premere Esc per chiudere la finestra.

Per interrompere l'applicazione dal terminale: Ctrl+C

<br>

6. Cosa aspettarsi
<br>
Invio dei flag UDP quando la mano entra o tocca un poligono POI.

Invio dei frame TCP al server video configurato.

Visualizzazione della mano e dei poligoni sul frame.

Log dettagliati in app.log.

<br>

7. Note aggiuntive
<br>
L'applicazione supporta solo una mano alla volta (max_num_hands=1).

I log vengono automaticamente ripuliti se la dimensione totale supera 1 GB.

I poligoni POI possono essere modificati senza cambiare il codice, usando coordPoiList.txt.

<br>

8. Risoluzione dei problemi
<br>
Errore connessione TCP/UDP: verificare IP e porta configurati, e la disponibilità della rete.

Camera non rilevata: verificare che src=0 sia corretto o sostituirlo con il numero della camera corretta.

Dipendenze mancanti: verificare di aver attivato l'ambiente virtuale e installato requirements.txt.

<br>

9. Chiusura
<br>
Chiudere sempre l'applicazione correttamente per rilasciare la camera e le risorse:

Esc -> chiusura finestra
Ctrl+C -> interrompe il loop principale

