#!/bin/bash

# Notes:
# 1. Pre-requisites: sudo privilege, docker
# 2. Run this script from the root folder

# Install and run ES
wget https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-6.6.1.tar.gz
tar -xf elasticsearch-6.6.1.tar.gz
cd elasticsearch-6.6.1/bin
nohup ./elasticsearch &

# Get codebase
wget https://github.com/dice-group/TeBaQA/archive/speaker-integration.zip
unzip speaker-integration.zip

# Build and run docker containers
cd TeBaQA-speaker-integration
sudo ./docker/build-images.sh
sudo ./docker/run-containers.sh