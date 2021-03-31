# Template-Based Question Answering
[![Build Status](https://travis-ci.org/dice-group/TeBaQA.svg?branch=master)](https://travis-ci.org/dice-group/TeBaQA)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/d0441bf0c82e47d6a3f2b23f11b223e6)](https://www.codacy.com/app/pnancke/TeBaQA?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=pnancke/TeBaQA&amp;utm_campaign=Badge_Grade)
## Preview
- A running example of this application is at https://tebaqa.demos.dice-research.org/
## Execution
- Checkout the project
- Execute the following command in the root directory to start the server: `mvn spring-boot:run`

### Docker
First install docker in your system. For ubuntu you may refer to below link. [https://www.digitalocean.com/community/tutorials/how-to-install-and-use-docker-on-ubuntu-16-04]

For windows users [https://docs.docker.com/toolbox/toolbox_install_windows/#step-1-check-your-version]

Move to the parent directory of project and execute the below commands
```
mvn clean
mvn package
```
Now to build your image, type the below command.
```
sudo docker build -f Dockerfile -t tebaqa .
```
To run your image, type the below command.
```
sudo docker run -d -p 8187:8080 -t tebaqa --restart always
```
It will be available under localhost:8187

## Citation

Vollmers, D., Jalota, R., Moussallem, D., Topiwala, H., Ngomo, A. C. N., & Usbeck, R. (2021). Knowledge Graph Question Answering using Graph-Pattern Isomorphism. arXiv preprint arXiv:2103.06752. https://arxiv.org/abs/2103.06752

## Question Answering
- To answer a question simply execute a HTTP POST request to
  - ```http://localhost:8181/qa``` for the answer which follows the W3C Query Results JSON Format (see https://www.w3.org/TR/sparql11-results-json/).
  - ```http://localhost:8181/qa-simple``` for a simple JSON with only the answer.
- Parameters:
  - `query`: A string which contains a question (required).
  - `lang`: The language of the question (default:`en`) *Note: Other languages than English haven't been implemented yet.*
- An example request could look like this: 
  - `http://localhost:8181/qa?query=What is the original title of the interpretation of dreams?&lang=en`
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
