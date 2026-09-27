# ---- build stage: Maven wrapper pins the Maven version, same as laptop/CI ----
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /build

# Dependencies first so this layer is reused when only src/ changes.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline

COPY src/ src/
RUN ./mvnw -q -B package -DskipTests

# ---- runtime stage: JRE only, non-root ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=builder /build/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
