@ECHO OFF
TITLE TeBaQA

:: run ES
cd elasticsearch-6.6.1/bin
CALL elasticsearch-service.bat start
cd ..
cd ..

:: run fuseki
cd apache-jena-fuseki-3.9.0/
START javaw -jar fuseki-server.jar
timeout 5 > NUL
ECHO Started Fuseki
cd ..


:: run TeBaQA services
cd TeBaQA-speaker-integration

cd nlp/
START javaw -jar target/nlp-1.0.jar
timeout 25 > NUL
ECHO Started tebaqa/nlp
cd ..

cd template-classification/
START javaw -jar target/template-classification-1.0.jar
timeout 25 > NUL
ECHO Started tebaqa/template-classification
cd ..

cd entity-linking/
START javaw -jar target/entity-linking-1.0.jar
timeout 15 > NUL
ECHO Started tebaqa/entity-linking
cd ..

cd query-ranking/
START javaw -jar target/query-ranking-1.0.jar
timeout 15 > NUL
ECHO Started tebaqa/query-ranking
cd ..

cd tebaqa-controller/
START javaw -jar target/tebaqa-controller-1.0.jar ../../knowledge_base_storage/
timeout 20 > NUL
ECHO Started tebaqa/tebaqa-controller
cd ..

cd ..
start http://localhost:8080
