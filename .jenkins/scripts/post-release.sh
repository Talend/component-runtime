#!/usr/bin/env bash

#
#  Copyright (C) 2006-2026 Talend Inc. - www.talend.com
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#

#
# IMPORTANT: there is no error handling on this script so ensure the docker builds/pushes worked.
#

#
# This script assumes you are in the root directory of the project,
# you have git and maven, you set the environment varaible `release` and
# that you already ran the following command:
# $ mvn release:prepare release:perform
#
# To launch a full release in one line, use (don't forget to set the release you are doingg):
# $ mvn release:prepare release:perform && release=1.0.3 ./post-release.sh
#

# ensure it is on the remote repo if needed - /!\ assumes the default remote is right
echo "Pushing tags"
git reset --hard
git push --follow-tags

if [ "x${release}" == "x" ]; then
    echo "No \$release, set it before running this script please"
    exit 1
fi

# Get the right tag
echo "Getting tag $release"
git checkout -b component-runtime-$release component-runtime-$release

# push docker images
echo "Building and pushing docker images $release"
cd images
    for i in component-server-image component-starter-server-image; do
        cd $i
        mvn -DskipTests -Dinvoker.skip=true -T1C -Prelease \
            clean install \
            jib:build@build -Dimage.currentVersion=$release -Dtalend.server.image.registry=registry.hub.docker.com/
        cd -
    done
cd -

echo "Rebuilding master and updating it (doc) for next iteration"
git reset --hard
git checkout master
mvn clean install -DskipTests -Dinvoker.skip=true -T1C && \
git commit -a -m "Updating doc for next iteration" && \
git push

# we don't update the doc here since travis will do it with previous push
#echo "Updating the documentation for next iteration"
#cd documentation
#    mvn clean install pre-site -Pgh-pages
#cd -
