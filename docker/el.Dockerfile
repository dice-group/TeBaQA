FROM maven:3.8.1-jdk-8 AS MAVEN_BUILD

WORKDIR /build
COPY tebaqa-commons/pom.xml tebaqa-commons/pom.xml
COPY tebaqa-commons/src tebaqa-commons/src
COPY entity-linking/pom.xml entity-linking/pom.xml
COPY entity-linking/src entity-linking/src

WORKDIR /build/tebaqa-commons
RUN mvn clean install package -Dmaven.test.skip=true
WORKDIR /build/entity-linking
RUN mvn clean package -Dmaven.test.skip=true

EXPOSE 8082
ENTRYPOINT ["java","-jar","target/entity-linking-1.0.jar"]