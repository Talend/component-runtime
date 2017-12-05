#! /usr/bin/env bash


echo TRAVIS_PULL_REQUEST=$TRAVIS_PULL_REQUEST
echo TRAVIS_TAG=$TRAVIS_TAG

if [ "$TRAVIS_PULL_REQUEST" = "true" ] || [ "x$encrypted_03e441b90173_key" = "x" ]; then
    exit 0
fi

OPTS="--batch-mode --settings $PWD/.travis/settings.xml"

mvn clean deploy verify -Dhub-detect.skip=false -DskipTests -Dinvoker.skip=true -Possrh -Prelease $OPTS

cd documentation
    mvn clean package gplus:execute@deploy-site -Pgh-pages -Dgithub.site.profile=latest $OPTS
cd -
