🚀 Guida: Caricare il codice su ESP32

1️⃣ Installare l’IDE Arduino

Scaricare ed installare l’Arduino IDE (ultima versione) dal sito ufficiale:
👉 https://www.arduino.cc/en/software

Avviare l’IDE dopo l’installazione.

2️⃣ Aggiungere il supporto per ESP32

In Arduino IDE, aprire:
File → Preferenze.

Nella sezione URL aggiuntive per il Gestore schede, inserire:

https://raw.githubusercontent.com/espressif/arduino-esp32/gh-pages/package_esp32_index.json


(se ci sono altre URL, separarle con una virgola).

Confermare con OK.

Andare su Strumenti → Scheda → Gestore schede….

Cercare ESP32 e installare la libreria ufficiale esp32 by Espressif Systems.

3️⃣ Collegare l’ESP32 al PC

Usare un cavo micro-USB o USB-C (a seconda del modello ESP32).

Verificare che il PC riconosca la porta COM (Windows) o /dev/ttyUSB0 (Linux).

In caso di problemi, potrebbe essere necessario installare i driver CP210x o CH340 (dipende dal chip USB della tua scheda).

4️⃣ Selezionare la scheda e la porta

In Arduino IDE:
Strumenti → Scheda → ESP32 Arduino → ESP32 Dev Module (o il modello corretto della tua scheda).

In Strumenti → Porta, selezionare la porta COM corretta.

5️⃣ Caricare il codice

Copiare tutto il codice nell’editor di Arduino IDE.

Premere Verifica (✔) per compilare.

Se non ci sono errori, passare al passo successivo.

Premere Carica (→).

Il codice verrà compilato e inviato all’ESP32.

Se l’upload non parte, premere e tenere premuto il pulsante BOOT sull’ESP32, rilasciandolo quando compaiono i puntini ..... nel log di caricamento.

6️⃣ Monitorare l’output

Aprire il Monitor Seriale: Strumenti → Monitor Seriale.

Impostare la velocità a 115200 baud.

Se il caricamento è andato a buon fine, dovresti vedere:

Connecting to GPV
Connected! IP address: 192.168.x.x
Web server available at: http://192.168.x.x


Aprendo quell’indirizzo in un browser (es. Chrome), comparirà la pagina web con lo stato dei sensori.