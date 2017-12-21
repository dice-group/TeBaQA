# Template Based Question Answering
[![Build Status](https://travis-ci.org/dice-group/TeBaQA.svg?branch=master)](https://travis-ci.org/dice-group/TeBaQA)
## Execution
- Execute the following command in the root directory to start the server: `mvn spring-boot:run`
## Question Answering
- To answer a question simply execute a HTTP POST request to
  - ```http://localhost:8181/qa``` for the answer which follows the W3C Query Results JSON Format (see https://www.w3.org/TR/sparql11-results-json/).
  - ```http://localhost:8181/qa-simple``` for a simple JSON with only the answer.
- Parameters:
  - `query`: A string which contains a question (required).
  - `lang`: The language of the question (default:`en`) *Note: Other languages than English haven't been implemented yet.*
- An example request could look like this: 
  - `http://localhost:8181/qa?query=What is the original title of the interpretation of dreams?&lang=en`