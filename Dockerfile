# ---------------------------------------------------------------------------
# Stage 1 – Build
# ---------------------------------------------------------------------------
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper & POM first so dependency layer caches
COPY .mvn/           .mvn/
COPY mvnw            mvnw
COPY pom.xml         pom.xml

# Download dependencies (cached layer – invalidated only when pom.xml changes)
RUN ./mvnw dependency:go-offline -q

# Copy the rest of the source
COPY src/            src/

# Build the fat JAR (-DskipTests because integration tests need a live DB)
RUN ./mvnw package -Dmaven.test.skip=true -q

# ---------------------------------------------------------------------------
# Stage 2 – Runtime
# ---------------------------------------------------------------------------
FROM eclipse-temurin:17-jre-alpine AS runtime

WORKDIR /app

# Non-root user --------------------------------------------------------
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy only the fat JAR
COPY --from=builder /app/target/*.jar app.jar

# Render sets PORT automatically; default to 8080 for local testing
ENV PORT=8080

# Expose the port the app listens on
EXPOSE 8080

# JVM flags tuned for small containers (Render free-tier = 512 MB)
#   -XX:+UseContainerSupport          – honour container memory limits
#   -XX:MaxRAMPercentage=75.0         – use ≤ 75 % of container RAM for heap
#   -XX:+UseSerialGC                  – lowest overhead GC for small heaps
#   -Djava.security.manager=disallow  – explicit opt-out (avoids warning spam in JDK 17+)
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+UseSerialGC", \
  "-Djava.security.manager=disallow", \
  "-jar", "app.jar"]
