#!/usr/bin/env bash

date

# travis helper
mvn -Ptravis dependency:copy -Dartifact=com.github.rmannibucau:maven-travis-output:1.0.0 -DoutputDirectory=/tmp

# openjdk requires this to get javafx (intellij plugin) on travis
sudo add-apt-repository ppa:tj/java-for-14.04 && \
sudo apt-get update && \
sudo apt-get install openjfx
