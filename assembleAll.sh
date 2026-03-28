#!/bin/bash

#this file is used to verify that all MC versions compile correctly,
#without starting a client/server pair for each one.

export JAVA_HOME=/home/builderb0y/java/jdk-25+36/

./gradlew "Switch to 1.21.11" && ./gradlew checkIfMyCodeCompiles && \
./gradlew --stop;