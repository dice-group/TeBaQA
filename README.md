# Template-Based Question Answering (TeBaQA)
[![Build Status](https://travis-ci.org/dice-group/TeBaQA.svg?branch=master)](https://travis-ci.org/dice-group/TeBaQA)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/d0441bf0c82e47d6a3f2b23f11b223e6)](https://www.codacy.com/app/pnancke/TeBaQA?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=pnancke/TeBaQA&amp;utm_campaign=Badge_Grade)

TeBaQA is available at https://tebaqa.demos.dice-research.org/
## Execution

TeBaQA implements microservices architecture. The application comprises following 5 modules:

- Template Classification :- to classify query templates (localhost:8081)
- Entity Linking :- finding and linking entities and relations (localhost:8082)
- Query Ranking :- candidate query execution, ranking (localhost:8083)
- TeBaQA Controller :- central controller, frontend application (localhost:8080)
- NLP Server :- CoreNLP Server endpoint (localhost:8085)

Additionally, Entity Linking requires Elasticsearch indices for data and ontology of the knowledge base. We provide dumps of DBPedia ([2016-10 release](https://downloads.dbpedia.org/2016-10/core/)). Find the dump files along with the instructions at [Hobbit data](https://hobbitdata.informatik.uni-leipzig.de/TeBaQA/). 
The indices were generated on Elastiscearch 6.6.1
However, TeBaQA can also be run on your own knowledge base. Check the instruction in [this file](https://github.com/dice-group/TeBaQA/blob/development-modular/tebaqa-commons/src/main/resources/indexing.properties) for more information on creating your own Elasticsearch indices.


#### There are two ways to run TeBaQA
#### 1. Run locally
- Checkout the project
- Build all modules
  
  `./build-script.sh`
- Run all modules
  
  `./run-script.sh`

#### 2. Run as Docker ([installation guide](https://www.digitalocean.com/community/tutorials/how-to-install-and-use-docker-on-ubuntu-20-04), Ubuntu 20.04) containers
- Checkout the project
- Build docker images for each module
  
  `./docker/build-images.sh`
- To run all containers

  `./docker/run-containers.sh`
- To stop all containers
  
  `./docker/stop-containers.sh`


## Citation

Vollmers, D., Jalota, R., Moussallem, D., Topiwala, H., Ngomo, A. C. N., & Usbeck, R. (2021). Knowledge Graph Question Answering using Graph-Pattern Isomorphism. arXiv preprint arXiv:2103.06752. https://arxiv.org/abs/2103.06752

## Question Answering
- To answer a question, simply execute an HTTP POST request to
  - ```http://localhost:8080/qa``` for the answer which follows the W3C Query Results JSON Format (see https://www.w3.org/TR/sparql11-results-json/).
  - ```http://localhost:8080/qa-simple``` for a simple JSON with only the answer.
- Parameters:
  - `query`: A string which contains a question (required).
  - `lang`: The language of the question (default:`en`) *Note: Other languages than English haven't been implemented yet.*
- An example request could look like this: 
  - `http://localhost:8080/qa?query=Where is the birthplace of Angela Merkel?&lang=en`

## Evaluation
- QALD-8: http://gerbil-qa.cs.upb.de:8080/gerbil/experiment?id=202012090005
- QALD-9: http://gerbil-qa.cs.upb.de:8080/gerbil/experiment?id=202012050000
- LC-QUAD v1: http://gerbil-qa.cs.upb.de:8080/gerbil/experiment?id=202012160002
- LC-QUAD v2: http://gerbil-qa.cs.upb.de:8080/gerbil/experiment?id=202103220000
### Ablation study
- perfect Classification: http://gerbil-qa.cs.upb.de:8080/gerbil/experiment?id=202012070002
- perfect Classification and Entity Linking: http://gerbil-qa.cs.upb.de:8080/gerbil/experiment?id=202012090000
- perfect Classification and Ranking: http://gerbil-qa.cs.upb.de:8080/gerbil/experiment?id=202012090001
- perfect Entity Linking: http://gerbil-qa.cs.upb.de:8080/gerbil/experiment?id=202012070004
- perfect Entity Linking and Ranking: http://gerbil-qa.cs.upb.de:8080/gerbil/experiment?id=202012080001
- perfect Ranking: http://gerbil-qa.cs.upb.de:8080/gerbil/experiment?id=202012080000
- perfect Entity Classification,Entity Linking and Ranking: http://gerbil-qa.cs.upb.de:8080/gerbil/experiment?id=202012080002

## Credit
- [DBpedia Chatbot](https://github.com/dbpedia/chatbot): Styling of the result cards.
