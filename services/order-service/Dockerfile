# ── Stage 1: Build ────────────────────────────────────────────────────── 
FROM eclipse-temurin:21 AS builder 
WORKDIR /build 

# Copy Maven wrapper & dependency descriptor FIRST — Docker caches this layer 
COPY .mvn .mvn 
COPY mvnw pom.xml ./ 
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B 

# Source changes more often than pom.xml — put it AFTER deps 
COPY src ./src 
RUN ./mvnw package -DskipTests -B 

# ── Stage 2: Runtime ────────────────────────────────────────────── 
FROM eclipse-temurin:21-jre-jammy 
WORKDIR /app 

# Security: never run as root in production 
RUN addgroup --system spring && adduser --system --group spring 
USER spring 

# Copy ONLY the compiled JAR from the builder stage 
COPY --from=builder /build/target/*.jar app.jar 
EXPOSE 8082 
ENTRYPOINT ["java", "-jar", "app.jar"] 
