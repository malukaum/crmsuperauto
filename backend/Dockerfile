# --- build ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
# Os testes rodam no "mvn verify" local/CI; aqui só empacota.
RUN mvn -q -B package -DskipTests

# --- runtime ---
FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache tzdata && addgroup -S crm && adduser -S crm -G crm
ENV TZ=America/Sao_Paulo \
    JAVA_TOOL_OPTIONS="-Duser.timezone=America/Sao_Paulo -XX:MaxRAMPercentage=75"
WORKDIR /app
COPY --from=build /app/target/crm-backend-*.jar app.jar
USER crm
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
