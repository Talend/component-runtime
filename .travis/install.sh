#!/usr/bin/env bash

date

mkdir -p $HOME/.m2/

function installJava {
  local name="$1-linux_x64.tar.gz"
  cd "/tmp"
  wget "https://cdn.azul.com/zulu/bin/$name"
  tar xvf "zulu8.36.0.1-ca-fx-jdk8.0.202-linux_x64.tar.gz"
  mv "zulu8.36.0.1-ca-fx-jdk8.0.202-linux_x64" "$HOME/.cache/talend/build/java-$2"
  cd -
}

function installMaven {
  cd "/tmp"
  wget "http://apache.mirrors.ovh.net/ftp.apache.org/dist/maven/maven-3/$1/binaries/apache-maven-$1-bin.tar.gz"
  tar xvf "apache-maven-$1-bin.tar.gz"
  mv "apache-maven-$1" "$HOME/.cache/talend/build/maven-$1"
  cd -
}
# ensure env is set up
BUILD_MAVEN_VERSION=${BUILD_MAVEN_VERSION:-3.6.1}
[ "x$BUILD_ENV_RESET" == "x" ] || rm -Rf "$HOME/.cache/talend/build"
if [ ! -d "$HOME/.cache/talend/build" ]; then
  mkdir -p "$HOME/.cache/talend/build"
  installJava "zulu8.36.0.1-ca-fx-jdk8.0.202" "8"
  installJava "zulu11.29.3-ca-fx-jdk11.0.2-linux_x64.tar.gz" "11"
  installMaven "$BUILD_MAVEN_VERSION"
fi
export JAVA_HOME="$HOME/.cache/talend/build/java-${BUILD_JAVA_VERSION:-8}"
export PATH="$JAVA_HOME/bin:$HOME/.cache/talend/build/maven-$BUILD_MAVEN_VERSION/bin:$PATH"

# log mvn+java versions
mvn -version

# travis helper
mkdir -p /tmp/dep && cd /tmp/dep &&
  travis_wait 50 mvn -Ptravis dependency:copy -Dartifact=com.github.rmannibucau:maven-travis-output:1.0.0 -DoutputDirectory=/tmp &&
cd -

# ensure default settings.xml works contextually without specifying it
cp -v $HOME/build/Talend/component-runtime/.travis/settings.xml $HOME/.m2/settings.xml
