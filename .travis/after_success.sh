#! /usr/bin/env bash


echo TRAVIS_PULL_REQUEST=$TRAVIS_PULL_REQUEST
echo TRAVIS_TAG=$TRAVIS_TAG

if [ "$TRAVIS_PULL_REQUEST" = "true" ] || [ "x$encrypted_03e441b90173_key" = "x" ]; then
    exit 0
fi

export MAVEN_OPTS="-DTLND_BLACKDUCK_PASS=${TLND_BLACKDUCK_PASS:-skip} -DTLND_BLACKDUCK_USER=${TLND_BLACKDUCK_USER:-skip}"

OPTS="--batch-mode --settings $PWD/.travis/settings.xml"

mvn clean deploy verify -DskipTests -Dinvoker.skip=true -Possrh -Prelease $OPTS

cd documentation
    mvn -Dgithub.site.profile=latest clean package pre-site -Pgh-pages $OPTS
cd -
