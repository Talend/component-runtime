#! /usr/bin/env bash

echo TRAVIS_PULL_REQUEST=$TRAVIS_PULL_REQUEST
echo TRAVIS_TAG=$TRAVIS_TAG

if [ "$TRAVIS_PULL_REQUEST" = "true" ] || [ "x$encrypted_03e441b90173_key" = "x" ]; then
    exit 0
fi

OPTS="--batch-mode --settings $PWD/.travis/settings.xml"

travis_wait 60 mvn clean deploy -Dhub-detect.skip=true -DskipTests -Dinvoker.skip=true -Possrh -Prelease $OPTS
travis_wait 60 mvn clean verify -Dhub-detect.skip=false -DskipTests -Dinvoker.skip=true -Dskip.yarn=true -Possrh -Prelease $OPTS

cd documentation
    travis_wait 60 mvn clean verify -Pgh-pages -Dgithub.site.profile=latest $OPTS
cd -
