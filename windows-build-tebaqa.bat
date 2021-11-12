@ECHO OFF

:: Notes:
:: 1. Pre-requisites: java8, maven
:: 2. Run this script from the root folder

ECHO ==============================================
ECHO Installing tebaqa-commons ...
ECHO ==============================================
cd tebaqa-commons/
CALL mvn -DskipTests clean install
cd ..
ECHO ==============================================
ECHO Finished installing tebaqa-commons.

ECHO ==============================================
ECHO Installing nlp ...
ECHO ==============================================
cd nlp/
CALL mvn -DskipTests clean install
cd ..
ECHO ==============================================
ECHO Finished installing nlp.

ECHO ==============================================
ECHO Installing template-classification ...
ECHO ==============================================
cd template-classification/
CALL mvn -DskipTests clean install
cd ..
ECHO ==============================================
ECHO Finished installing template-classification.

ECHO ==============================================
ECHO Installing entity-linking ...
ECHO ==============================================
cd entity-linking/
CALL mvn -DskipTests clean install
cd ..
ECHO ==============================================
ECHO Finished installing entity-linking.

ECHO ==============================================
ECHO Installing query-ranking ...
ECHO ==============================================
cd query-ranking/
CALL mvn -DskipTests clean install
cd ..
ECHO ==============================================
ECHO Finished installing query-ranking.

ECHO ==============================================
ECHO Installing tebaqa-controller ...
ECHO ==============================================
cd tebaqa-controller/
CALL mvn -DskipTests clean install
cd ..
ECHO ==============================================
ECHO Finished installing tebaqa-controller.
ECHO ==============================================
