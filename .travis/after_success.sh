#! /usr/bin/env bash

echo TRAVIS_PULL_REQUEST=$TRAVIS_PULL_REQUEST
echo TRAVIS_TAG=$TRAVIS_TAG

if [ "$TRAVIS_PULL_REQUEST" = "true" ] || [ "x$encrypted_03e441b90173_key" = "x" ]; then
    exit 0
fi

OPTS="-Dcheckstyle.skip=true -Drat.skip=true -DskipTests -Dinvoker.skip=true -Dskip.yarn=true --batch-mode --settings $PWD/.travis/settings.xml  -Possrh -Prelease"

node .travis/heartbeat.js mvn clean deploy -Dhub-detect.skip=true $OPTS
node .travis/heartbeat.js mvn clean verify -Dhub-detect.skip=false $OPTS

cd documentation
    node ../.travis/heartbeat.js mvn verify -Pgh-pages -Dgithub.site.profile=latest $OPTS
cd -
