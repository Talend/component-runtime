#! /usr/bin/env bash

echo TRAVIS_PULL_REQUEST=$TRAVIS_PULL_REQUEST
echo TRAVIS_TAG=$TRAVIS_TAG

if [ "$TRAVIS_PULL_REQUEST" = "true" ] || [ "x$encrypted_03e441b90173_key" = "x" ]; then
    exit 0
fi

OPTS="--batch-mode --settings $PWD/.travis/settings.xml"

# deploy artifact
node .travis/heartbeat.js mvn deploy:deploy -Possrh -Prelease $OPTS

# launch blackdoc analysis
node .travis/heartbeat.js mvn org.talend.tools:talend-tools-maven-plugin:hub-detect -Possrh -Prelease $OPTS

# deploy documentation
cd documentation
    node ../.travis/heartbeat.js mvn org.codehaus.gmavenplus:gmavenplus-plugin:execute -Pgh-pages -Dgithub.site.profile=latest $OPTS
cd -
