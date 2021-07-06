FROM maven:3.8.1-jdk-8 AS MAVEN_BUILD

WORKDIR /build
COPY tebaqa-commons/pom.xml tebaqa-commons/pom.xml
COPY tebaqa-commons/src tebaqa-commons/src
COPY tebaqa-controller/pom.xml tebaqa-controller/pom.xml
COPY tebaqa-controller/src tebaqa-controller/src

WORKDIR /build/tebaqa-commons
RUN mvn clean install package -Dmaven.test.skip=true
WORKDIR /build/tebaqa-controller
RUN mvn clean package -Dmaven.test.skip=true

EXPOSE 8085
ENTRYPOINT ["java","-jar","target/tebaqa-controller-1.0.jar"]
