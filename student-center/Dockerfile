# Stage 1: Build ứng dụng
FROM maven:3.9.9-eclipse-temurin-17-alpine AS build

# Đặt thư mục làm việc
WORKDIR /app

# Copy file pom.xml từ thư mục student-center
COPY student-center/pom.xml .

# Download dependencies
RUN mvn dependency:go-offline

# Copy toàn bộ source code từ thư mục student-center
COPY student-center/src ./src

# Build ứng dụng (bỏ qua test để nhanh)
RUN mvn clean package -DskipTests

# Stage 2: Chạy ứng dụng
FROM eclipse-temurin:17-jre-alpine

# Tạo thư mục làm việc
WORKDIR /app

# Copy file JAR từ stage build
COPY --from=build /app/target/student-center-0.0.1-SNAPSHOT.jar app.jar

# Mở port 8088
EXPOSE 8088

# Chạy ứng dụng
ENTRYPOINT ["java", "-jar", "app.jar"]
