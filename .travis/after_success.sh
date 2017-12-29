#! /usr/bin/env bash

echo TRAVIS_PULL_REQUEST=$TRAVIS_PULL_REQUEST
echo TRAVIS_TAG=$TRAVIS_TAG

if [ "$TRAVIS_PULL_REQUEST" = "true" ] || [ "x$encrypted_03e441b90173_key" = "x" ]; then
    exit 0
fi

OPTS="--batch-mode --settings $PWD/.travis/settings.xml"

# clean the currently built data to be able to copy the project N times and build it faster
work_dir=../after_success_workdir
mvn clean

for i in deploy blackduck documentation; do
    rm -Rf $work_dir/$i
    mkdir -p $work_dir/$i
    time cp -r . $work_dir/$i && echo "Copied the source repository for $i"
done


# run in parallel the OSS snapshot deployment, the blackduck scanning and the doc deployment
cd $work_dir/deploy
    mvn clean deploy -Dhub-detect.skip=true -DskipTests -Dinvoker.skip=true -Possrh -Prelease $OPTS &
cd -

cd $work_dir/blackduck
    mvn clean verify -Dhub-detect.skip=false -DskipTests -Dinvoker.skip=true -Dskip.yarn=true -Possrh -Prelease $OPTS &
cd -

cd $work_dir/documentation/documentation
    mvn clean verify -Pgh-pages -Dgithub.site.profile=latest $OPTS &
cd -

# wait they are all done
wait
