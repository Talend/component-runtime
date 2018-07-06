#!/usr/bin/env bash

date

rm -Rf $HOME/.m2/repository/org/apache/beam

# travis helper
mvn dependency:copy -Dartifact=com.github.rmannibucau:maven-travis-output:1.0.0 -DoutputDirectory=/tmp

# maven
#echo "Resolving maven dependencies and plugins"
#MAVEN_OPTS="-Dmaven.ext.class.path=/tmp/maven-travis-output-1.0.0.jar -Dorg.slf4j.simpleLogger.defaultLogLevel=warn $MAVEN_OPTS" mvn dependency:resolve dependency:resolve-plugins --batch-mode -e -q
#
## front
#for i in component-tools-webapp component-starter-server component-tools; do
#    cd $i
#        mvn frontend:install-node-and-yarn@install-node-and-yarn frontend:yarn@yarn-install --batch-mode -e -q
#    cd -
#done
#
## documentation - we got some issues on travis so this is a sanity check
#cd documentation
#    mvn dependency:unpack@unpack-api-javadoc --batch-mode -e -q
#cd -

