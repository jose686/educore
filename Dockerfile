# Etapa 1: Compilación
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Descarga previa de dependencias para aprovechar caché de Docker
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar código fuente y compilar
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Ejecución
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Instalar FFmpeg para transcodificación de vídeo HLS y crear directorio de subidas
RUN apk add --no-cache ffmpeg && mkdir -p uploads

# Copiar el empaquetado final
COPY --from=build /app/target/*.jar app.jar

# Puerto interno estándar
EXPOSE 8080

# Ejecución
ENTRYPOINT ["java", "-jar", "app.jar"]
