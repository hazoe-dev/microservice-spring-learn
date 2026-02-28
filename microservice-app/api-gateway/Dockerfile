# -------- Stage 1: Build --------
FROM eclipse-temurin:25-jdk AS builder

WORKDIR /app

# Copy wrapper + build files trước để tối ưu cache layer
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# Cấp quyền execute cho gradlew
RUN chmod +x gradlew

# Download dependencies trước (giúp cache tốt hơn)
RUN ./gradlew dependencies --no-daemon

# Copy source sau cùng (nếu src thay đổi thì chỉ rebuild layer này)
COPY src src

# Build bootJar
RUN ./gradlew bootJar -x test --no-daemon


# -------- Stage 2: Runtime --------
FROM eclipse-temurin:25-jdk

WORKDIR /app

# Copy jar từ stage build
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8090

ENTRYPOINT ["java","-XX:+UseContainerSupport","-XX:MaxRAMPercentage=75.0","-jar","app.jar"]