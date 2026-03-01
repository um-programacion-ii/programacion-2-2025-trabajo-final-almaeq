# ETAPA 1: Construcción (Build)
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copiamos archivos de configuración de Maven
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Damos permisos de ejecución al wrapper y descargamos dependencias (para aprovechar caché)
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline

# Copiamos el código fuente
COPY src ./src

# Compilamos el proyecto (saltando tests para ir más rápido en este paso)
RUN ./mvnw clean package -DskipTests

# ETAPA 2: Ejecución (Run)
FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache curl

WORKDIR /app

# Copiamos el JAR generado en la etapa anterior
COPY --from=builder /app/target/*.jar app.jar

# Exponemos el puerto
EXPOSE 8080

# Comando de inicio
ENTRYPOINT ["java", "-jar", "app.jar"]