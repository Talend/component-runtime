#! /usr/bin/env bash

echo TRAVIS_PULL_REQUEST=$TRAVIS_PULL_REQUEST
echo TRAVIS_TAG=$TRAVIS_TAG

if [ "$TRAVIS_PULL_REQUEST" = "true" ] || [ "x$encrypted_03e441b90173_key" = "x" ]; then
    exit 0
fi

OPTS="--batch-mode --settings $PWD/.travis/settings.xml"

node .travis/heartbeat.js mvn clean deploy -Dhub-detect.skip=true -DskipTests -Dinvoker.skip=true -Possrh -Prelease $OPTS
node .travis/heartbeat.js mvn clean verify -Dhub-detect.skip=false -DskipTests -Dinvoker.skip=true -Dskip.yarn=true -Possrh -Prelease $OPTS

cd documentation
    node ../.travis/heartbeat.js mvn clean verify -Pgh-pages -Dgithub.site.profile=latest -Dhub-detect.skip=true -Dinvoker.skip=true -Dskip.yarn=true $OPTS
cd -
