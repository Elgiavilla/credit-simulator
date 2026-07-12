FROM maven:3.9-eclipse-temurin-11 AS build

WORKDIR /build

COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B clean package

# --------------------------------------------------------------------------------------------------

FROM eclipse-temurin:11-jre

RUN useradd --create-home --shell /bin/bash simulator
USER simulator

WORKDIR /home/simulator

COPY --from=build --chown=simulator:simulator /build/target/credit-simulator.jar ./credit-simulator.jar
COPY --chown=simulator:simulator file_inputs.txt ./file_inputs.txt

ENTRYPOINT ["java", "-jar", "/home/simulator/credit-simulator.jar"]