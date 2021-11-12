

# Notes:
# 1. TeBaQA application uses elasticsearch indices. To know more about indexing, check
# tebaqa-commons/src/main/resources/indexing.properties.
# 2. Run this script from the root folder.
# 3. Default settings assume that ES and all the individual services are running on the localhost.
# If that is not the case, don't forget to change the hostnames/ports in properties file of relevant modules.


# Start each service in background, save output to nohop.out.
cd nlp/
nohup mvn exec:java -Dexec.mainClass="de.uni.leipzig.tebaqa.nlp.NlpApplication" &
cd ..

cd template-classification/
nohup mvn exec:java -Dexec.mainClass="de.uni.leipzig.tebaqa.template.ClassificationApplication" &
cd ..

cd entity-linking/
nohup mvn exec:java -Dexec.mainClass="de.uni.leipzig.tebaqa.entitylinking.EntityLinkingApplication" &
cd ..

cd query-ranking/
nohup mvn exec:java -Dexec.mainClass="de.uni.leipzig.tebaqa.queryranking.QueryRankingApplication" &
cd ..

cd tebaqa-controller/
nohup mvn exec:java -Dexec.mainClass="de.uni.leipzig.tebaqa.tebaqacontroller.TebaqaControllerApplication" &
cd ..
