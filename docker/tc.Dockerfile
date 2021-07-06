FROM maven:3.8.1-jdk-8 AS MAVEN_BUILD

WORKDIR /build
COPY tebaqa-commons/pom.xml tebaqa-commons/pom.xml
COPY tebaqa-commons/src tebaqa-commons/src

COPY template-classification/pom.xml template-classification/pom.xml
COPY template-classification/src template-classification/src
COPY template-classification/graphs_QALD9_Train_Multilingual.txt  template-classification/graphs_QALD9_Train_Multilingual.txt
COPY template-classification/mappings_QALD9_Train_Multilingual.json template-classification/mappings_QALD9_Train_Multilingual.json
COPY template-classification/qald-9-train-multilingual.json template-classification/qald-9-train-multilingual.json
COPY template-classification/question_classification_QALD9_Train_Multilingual.model template-classification/question_classification_QALD9_Train_Multilingual.model
COPY template-classification/Train_QALD9_Train_Multilingual.arff template-classification/Train_QALD9_Train_Multilingual.arff

WORKDIR /build/tebaqa-commons
RUN mvn clean install package -Dmaven.test.skip=true
WORKDIR /build/template-classification
RUN mvn clean package -Dmaven.test.skip=true

EXPOSE 8085
ENTRYPOINT ["java","-jar","target/template-classification-1.0.jar"]
