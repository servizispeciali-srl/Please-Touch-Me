# HOWTO: Come eseguire l'applicazione `videoprocessing.py`

Questo documento spiega passo passo come configurare ed eseguire l'applicazione di **hand tracking** e invio dati via **UDP/TCP**.

---

## 1. Prerequisiti

- **Python 3.10+**
- **pip** (gestore pacchetti Python)
- Accesso a una webcam funzionante
- Connessione di rete verso il server UDP/TCP, se necessario

---

## 2. Clonare il repository

Aprire il terminale e clonare il progetto:

```bash
git clone <URL_del_repository>
cd <nome_repository>
```

### 3. Creare l'ambiente virtuale

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
## 4. Installare le dipendenze

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

## 5. Configurare l'applicazione

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

## 6. Eseguire l'applicazione

Da terminale:
```bash
python videoprocessing.py
```

La finestra video mostrerà la mano rilevata e messaggi di stato.

Premere Esc per chiudere la finestra.

Per interrompere l'applicazione dal terminale: Ctrl+C

## 7. Cosa aspettarsi

Invio dei flag UDP quando la mano entra o tocca un poligono POI.

Invio dei frame TCP al server video configurato.

Visualizzazione della mano e dei poligoni sul frame.

Log dettagliati in app.log.

## 8. Note aggiuntive

L'applicazione supporta solo una mano alla volta (max_num_hands=1).

I log vengono automaticamente ripuliti se la dimensione totale supera 1 GB.

I poligoni POI possono essere modificati senza cambiare il codice, usando coordPoiList.txt.

## 9. Risoluzione dei problemi

Errore connessione TCP/UDP: verificare IP e porta configurati, e la disponibilità della rete.

Camera non rilevata: verificare che src=0 sia corretto o sostituirlo con il numero della camera corretta.

Dipendenze mancanti: verificare di aver attivato l'ambiente virtuale e installato requirements.txt.

## 10. Chiusura

Chiudere sempre l'applicazione correttamente per rilasciare la camera e le risorse:

Esc -> chiusura finestra
Ctrl+C -> interrompe il loop principale
