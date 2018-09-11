#!/usr/bin/env bash

date

# travis helper
mvn -Ptravis dependency:copy -Dartifact=com.github.rmannibucau:maven-travis-output:1.0.0 -DoutputDirectory=/tmp

# openjdk requires this to get javafx (intellij plugin) on travis
add-apt-repository ppa:tj/java-for-14.04 && \
apt-get update && \
apt-get install openjfx
