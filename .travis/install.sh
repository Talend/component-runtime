#!/usr/bin/env bash

date

# travis helper
mvn -Ptravis dependency:copy -Dartifact=com.github.rmannibucau:maven-travis-output:1.0.0 -DoutputDirectory=/tmp

# workaround to ensure the resolution
for art in beam-runners-flink_2.11 beam-runners-spark beam-runners-direct-java; do
  mvn -Ptravis dependency:get -Dartifact=org.apache.beam:$art:2.6.0
done

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
