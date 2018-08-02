#!/usr/bin/env bash

date

# travis helper
mvn -Ptravis dependency:copy -Dartifact=com.github.rmannibucau:maven-travis-output:1.0.0 -DoutputDirectory=/tmp
