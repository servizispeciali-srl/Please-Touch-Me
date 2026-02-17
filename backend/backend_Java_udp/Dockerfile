# Immagine base compatibile: OpenJDK 8 JRE su Alpine (leggera e stabile per JAR Java 8 con Log4j)
FROM openjdk:8-jre-alpine

# Imposta la directory di lavoro semplice
WORKDIR /app

# Crea le directory necessarie (logs e data)
RUN mkdir -p /app/logs /app/data

# Copia solo i file richiesti: JAR, config e data
COPY BeaconDL.jar /app/BeaconDL.jar
COPY config /app/config
COPY data /app/data

# Specifica il file di configurazione Log4j per caricare DOMConfigurator
#ENV LOG4J_CONFIG=/app/config/log4j.xml

# Comando di avvio: Esegue direttamente il JAR con configurazione Log4j (processo in foreground)
CMD ["java", "-jar", "BeaconDL.jar"]