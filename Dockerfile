# =====================================================================
#  HEALTH+ · Cómo se construye y arranca la app en el servidor (Render)
#  Paso 1: compila el proyecto con Maven y Java 21 (como hace IntelliJ).
#  Paso 2: copia solo el .jar resultante a una imagen pequeña y lo ejecuta.
#  En tu computador NO se usa: allá sigues dándole Run en IntelliJ.
# =====================================================================
FROM maven:3.9-eclipse-temurin-21 AS compilar
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=compilar /app/target/*.jar app.jar
# Hora de Colombia: si no, el servidor usa la hora universal (UTC) y "hoy", la racha
# y el saludo cambiarían de día a las 7 p. m. de Colombia.
ENV TZ=America/Bogota
# Límite de memoria pensado para el plan gratis de Render (512 MB)
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -Duser.timezone=America/Bogota"
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
