FROM maven:3.8.1-jdk-8 AS MAVEN_BUILD

WORKDIR /build
COPY tebaqa-commons/pom.xml tebaqa-commons/pom.xml
COPY tebaqa-commons/src tebaqa-commons/src
COPY query-ranking/pom.xml query-ranking/pom.xml
COPY query-ranking/src query-ranking/src

WORKDIR /build/tebaqa-commons
RUN mvn clean install package -Dmaven.test.skip=true
WORKDIR /build/query-ranking
RUN mvn clean package -Dmaven.test.skip=true

EXPOSE 8085
ENTRYPOINT ["java","-jar","target/query-ranking-1.0.jar"]
