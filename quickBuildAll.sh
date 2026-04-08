#!/bin/bash

#this file is used to verify that all MC versions compile correctly,
#without starting a client/server pair for each one.

export JAVA_HOME=/home/builderb0y/java/jdk-25+36/

./gradlew "Switch to 1.20.1" && ./gradlew -DquickBuild=true build && \
./gradlew "Switch to 1.20.2" && ./gradlew -DquickBuild=true build && \
./gradlew "Switch to 1.20.4" && ./gradlew -DquickBuild=true build && \
./gradlew "Switch to 1.20.6" && ./gradlew -DquickBuild=true build && \
./gradlew "Switch to 1.21.1" && ./gradlew -DquickBuild=true build && \
./gradlew "Switch to 1.21.3" && ./gradlew -DquickBuild=true build && \
./gradlew "Switch to 1.21.4" && ./gradlew -DquickBuild=true build && \
./gradlew "Switch to 1.21.5" && ./gradlew -DquickBuild=true build && \
./gradlew "Switch to 1.21.8" && ./gradlew -DquickBuild=true build && \
./gradlew "Switch to 1.21.10" && ./gradlew -DquickBuild=true build && \
./gradlew "Switch to 1.21.11" && ./gradlew -DquickBuild=true build && \
./gradlew --stop;