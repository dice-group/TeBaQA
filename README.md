# Template-Based Question Answering (TeBaQA)
[![Build Status](https://travis-ci.org/dice-group/TeBaQA.svg?branch=master)](https://travis-ci.org/dice-group/TeBaQA)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/d0441bf0c82e47d6a3f2b23f11b223e6)](https://www.codacy.com/app/pnancke/TeBaQA?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=pnancke/TeBaQA&amp;utm_campaign=Badge_Grade)

TeBaQA is available at https://tebaqa.demos.dice-research.org/
## Execution

TeBaQA-Offline implements microservices architecture. The application comprises following 5 modules:

- Template Classification :- to classify query templates (localhost:8081)
- Entity Linking :- finding and linking entities and relations (localhost:8082)
- Query Ranking :- candidate query execution, ranking (localhost:8083)
- TeBaQA Controller :- central controller, frontend application (localhost:8080)
- NLP Server :- CoreNLP Server endpoint (localhost:8085)

In addition to these, TeBaQA-Offline requires Elasticsearch and Apache Fuseki. To install TeBaQA-Offline, download the [installation script](https://github.com/dice-group/TeBaQA/raw/speaker-integration/windows-run-tebaqa.bat). This will install all the required softwares and dependencies in the current directory.

To run TeBaQA-Offline, download the [run](https://github.com/dice-group/TeBaQA/raw/speaker-integration/windows-run-tebaqa.bat) and [stop](https://github.com/dice-group/TeBaQA/raw/speaker-integration/windows-stop-tebaqa.bat) scripts. 

### Memory requirements:
Softwares:
- Elasticsearch: 1GB
- Apache Fuseki: 1GB

TeBaQA-Offline services:
- NLP Server: 4GB
- Template Classification: 4GB
- Entity Linking: 1GB
- Query Ranking: 1GB
- TeBaQA Controller: 1GB



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
