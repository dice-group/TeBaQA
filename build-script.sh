#!/bin/bash

# Notes:
# 1. Pre-requisites: java8, maven
# 2. Run this script from the root folder

# Build each module and save the build output to build.log
cd tebaqa-commons/
mvn clean install -Dmaven.test.skip=true > build.log
cd ..

cd nlp/
mvn clean install -Dmaven.test.skip=true > build.log
cd ..

cd template-classification/
mvn clean install -Dmaven.test.skip=true > build.log
cd ..

cd entity-linking/
mvn clean install -Dmaven.test.skip=true > build.log
cd ..

cd query-ranking/
mvn clean install -Dmaven.test.skip=true > build.log
cd ..

cd tebaqa-controller/
mvn clean install -Dmaven.test.skip=true > build.log
cd ..
