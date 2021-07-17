#!/usr/bin/env bash

docker build . -f docker/nlp.Dockerfile -t nlp
docker build . -f docker/tc.Dockerfile -t template-classification
docker build . -f docker/el.Dockerfile -t entity-linking
docker build . -f docker/qr.Dockerfile -t query-ranking
docker build . -f docker/controller.Dockerfile -t tebaqa-controller
