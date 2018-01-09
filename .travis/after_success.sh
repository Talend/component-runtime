#! /usr/bin/env bash

echo TRAVIS_PULL_REQUEST=$TRAVIS_PULL_REQUEST
echo TRAVIS_TAG=$TRAVIS_TAG

if [ "$TRAVIS_PULL_REQUEST" = "true" ] || [ "x$encrypted_03e441b90173_key" = "x" ]; then
    exit 0
fi

OPTS="--batch-mode --settings $PWD/.travis/settings.xml"

node .travis/heartbeat.js mvn clean deploy  -Pquick -Possrh -Prelease  -Dmaven.javadoc.skip=false $OPTS
node .travis/heartbeat.js mvn clean verify  -Pquick -Possrh -Prelease  -Dhub-detect.skip=false $OPTS

cd documentation
    node ../.travis/heartbeat.js mvn clean verify -Pquick -Pgh-pages  -Dgithub.site.profile=latest $OPTS
cd -
