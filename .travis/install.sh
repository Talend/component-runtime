#!/usr/bin/env bash

date

mkdir -p $HOME/.m2/

# travis helper
mvn -Ptravis dependency:copy -Dartifact=com.github.rmannibucau:maven-travis-output:1.0.0 -DoutputDirectory=/tmp

# ensure default settings.xml works contextually without specifying it
cp -v $HOME/build/Talend/component-runtime/.travis/settings.xml $HOME/.m2/settings.xml
