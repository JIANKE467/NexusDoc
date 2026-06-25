# NexusDoc 后端构建镜像
FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY pom.xml .
COPY src ./src

RUN mvn -B package -DskipTests

# 运行镜像
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app
COPY --from=build /workspace/target/nexusdoc-0.0.1-SNAPSHOT.jar ./app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]