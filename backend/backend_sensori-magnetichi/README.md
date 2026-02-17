📄 Documentazione del codice ESP32 (WiFi + UDP + Web Server)
Introduzione

Questo programma è progettato per un ESP32.
Le sue funzionalità principali sono:

Connessione alla rete WiFi.

Lettura dello stato di 4 sensori digitali collegati a determinati pin.

Invio periodico dello stato dei sensori tramite UDP a un server remoto.

Pubblicazione di una pagina web accessibile dal browser, che mostra in tempo reale lo stato dei sensori.

⚙️ Configurazione hardware

I sensori sono collegati ai seguenti pin GPIO dell’ESP32:

SA0 → GPIO 0

SA1 → GPIO 4

SA2 → GPIO 26

SA3 → GPIO 25

Ognuno è configurato come input con resistenza di pull-up interna (INPUT_PULLUP).

🔧 Costanti principali

DEVICE_ID: Identificativo del dispositivo (qui "RTNode6").

ssid e pass: Nome e password della rete WiFi a cui connettersi.

localPort: Porta locale UDP usata dall’ESP32 (1702).

receiverPort: Porta remota del server UDP (8085).

dataLoggerAddress: Indirizzo IP del server UDP che riceve i dati.

MAX_REPEAT: Numero massimo di ripetizioni dell’invio del pacchetto UDP per evitare perdite.

📡 Connessione WiFi

Nel setup(), l’ESP32:

Avvia la comunicazione seriale (Serial.begin).

Si connette alla rete WiFi usando WiFi.begin().

Attende fino a quando la connessione non è stabilita.

Stampa via seriale l’IP assegnato e l’indirizzo a cui è disponibile il web server.

🌐 Web Server

Il programma crea un WebServer sulla porta 80.

Quando un client (es. browser) accede alla root /, viene generata e inviata una pagina HTML tramite la funzione makeHTML().

La pagina mostra:

Il nome del dispositivo (DEVICE_ID).

Una tabella con lo stato dei 4 sensori (verde = HIGH, rosso = LOW).

L’indirizzo IP locale dell’ESP32.

La pagina si aggiorna automaticamente ogni 2 secondi (<meta http-equiv='refresh' content='2'>).

📤 Comunicazione UDP

L’ESP32 prepara un messaggio nel formato:

#RTNode6=1010


dove 1010 rappresenta lo stato dei 4 sensori (HIGH o LOW).

Il messaggio viene inviato tramite UDP al server remoto (dataLoggerAddress, porta receiverPort).

L’invio avviene:

Ogni secondo.

Oppure subito se cambia lo stato di un sensore.

La funzione sendUDPMessage() si occupa di inviare il pacchetto e stampa il risultato nel monitor seriale.

🔄 Ciclo principale (loop)

Legge continuamente lo stato dei sensori.

Se uno di essi cambia, viene impostata la variabile sendMsg=1 per inviare i dati.

Se è passato più di 1 secondo dall’ultimo invio, prepara un nuovo messaggio UDP.

Gestisce le richieste dei client HTTP con server.handleClient().

Attende 100 ms prima di ripetere il ciclo.

📑 Funzioni principali

makeHTML(): Genera la pagina HTML con la tabella degli stati dei sensori.

sendUDPMessage(): Invia un pacchetto UDP al server remoto.

✅ Riassunto

Questo programma trasforma l’ESP32 in un nodo IoT che:

Legge 4 ingressi digitali.

Invia il loro stato a un server centrale tramite UDP.

Offre un’interfaccia web semplice e aggiornata automaticamente per monitorare i valori in tempo reale