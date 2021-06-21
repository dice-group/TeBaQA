#!/bin/bash

# Notes:
# 1. Pre-requisites: java8, maven
# 2. Run this script from the root folder

# Build each module and save the build output to build.log
cd tebaqa-commons/
rm build.log
mvn clean install -Dmaven.test.skip=true > build.log
rm nohup.out
cd ..

cd nlp/
rm build.log
mvn clean install -Dmaven.test.skip=true > build.log
rm nohup.out
cd ..

cd template-classification/
rm build.log
mvn clean install -Dmaven.test.skip=true > build.log
rm nohup.out
cd ..

cd entity-linking/
rm build.log
mvn clean install -Dmaven.test.skip=true > build.log
rm nohup.out
cd ..

cd query-ranking/
rm build.log
mvn clean install -Dmaven.test.skip=true > build.log
rm nohup.out
cd ..

cd tebaqa-controller/
rm build.log
mvn clean install -Dmaven.test.skip=true > build.log
rm nohup.out
cd ..
