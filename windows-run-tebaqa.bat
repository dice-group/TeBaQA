cd TeBaQA-speaker-integration

cd nlp/
javaw -jar target/nlp-1.0.jar
cd ..

cd template-classification/
javaw -jar target/template-classification-1.0.jar
cd ..

cd entity-linking/
javaw -jar target/entity-linking-1.0.jar
cd ..

cd query-ranking/
javaw -jar target/query-ranking-1.0.jar
cd ..

cd tebaqa-controller/
javaw -jar target/tebaqa-controller-1.0.jar ../../knowledge_base_storage/
cd ..

cd ..