#!/usr/bin/env bash

docker run -itd --rm --name nlp --network="host" nlp:latest
docker run -itd --rm --name template-classification --network="host" template-classification:latest
docker run -itd --rm --name entity-linking --network="host" entity-linking:latest
docker run -itd --rm --name query-ranking --network="host" query-ranking:latest
docker run -itd --rm --name tebaqa-controller --network="host" tebaqa-controller:latest
