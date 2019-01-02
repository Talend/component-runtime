/**
 * Copyright (C) 2006-2019 Talend Inc. - www.talend.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
def slackChannel = 'tacokit_ci'
def kafkaVersion = '1.1.0'
def version = 'will be replaced'
def image = 'will be replaced'

pipeline {
  agent {
    kubernetes {
      label 'talend-component-kit'
      yaml """
apiVersion: v1
kind: Pod
spec:
  containers:
    - name: maven
      image: jenkinsxio/builder-maven:0.0.319
      command:
      - cat
      tty: true
      volumeMounts:
      - name: docker
        mountPath: /var/run/docker.sock
      - name: m2
        mountPath: /root/.m2/repository

  volumes:
  - name: docker
    hostPath:
      path: /var/run/docker.sock
  - name: m2
    hostPath:
      path: /tmp/jenkins/component-kit/m2
"""
    }
  }

  environment {
    MAVEN_OPTS = '-Dmaven.artifact.threads=128 -Dorg.slf4j.simpleLogger.showThreadName=true -Dorg.slf4j.simpleLogger.showDateTime=true -Dorg.slf4j.simpleLogger.dateTimeFormat=HH:mm:ss'
    TALEND_REGISTRY="registry.datapwn.com"
  }

  options {
    buildDiscarder(logRotator(artifactNumToKeepStr: '5', numToKeepStr: env.BRANCH_NAME == 'master' ? '10' : '2'))
    timeout(time: 60, unit: 'MINUTES')
    skipStagesAfterUnstable()
  }

  triggers {
    cron(env.BRANCH_NAME == "master" ? "@daily" : "")
  }

  stages {
    stage('Run maven') {
      steps {
        container('maven') {
          sh 'mvn clean install'
        }
      }
    }
    stage('Build Docker Components Image') {
      when {
        expression { sh(returnStdout: true, script: 'git rev-parse --abbrev-ref HEAD').trim() == 'master' }
      }
      steps {
        container('maven') {
          withCredentials([
            usernamePassword(
              credentialsId: 'docker-registry-credentials',
              passwordVariable: 'DOCKER_PASSWORD',
              usernameVariable: 'DOCKER_LOGIN')
          ]) {
            script {
              version = sh(returnStdout: true, script: 'grep "<version>" pom.xml  | head -n 1 | sed "s/.*>\\(.*\\)<.*/\\1/"').trim()
              image = sh(returnStdout: true, script: 'echo "talend/component-kit:$(echo "' + version + '" | sed "s/SNAPSHOT/dev/")"').trim()
            }

            sh "mvn dependency:copy -Dartifact=org.apache.kafka:kafka-clients:${kafkaVersion} -DoutputDirectory=."

            sh """
# drop already existing snapshot image if any
if [[ "${version}" = *"SNAPSHOT" ]]; then
  docker rmi "${image}" "$TALEND_REGISTRY/${image}" || :
fi

# build and push current image
docker build --tag "${image}" --build-arg SERVER_VERSION=${version} --build-arg KAFKA_CLIENT_VERSION=${kafkaVersion} . && \
docker tag "${image}" "$TALEND_REGISTRY/${image}" || exit 1
"""
            retry(5) {
            sh '''#! /bin/bash
set +x
echo $DOCKER_PASSWORD | docker login $TALEND_REGISTRY -u $DOCKER_LOGIN --password-stdin
'''
              sh "docker push ${env.TALEND_REGISTRY}/${image}"
            }
          }
        }
      }
    }
    stage('Publish Site') {
      steps {
        container('maven') {
          sh 'mvn clean site:site site:stage -T1C -Dmaven.test.failure.ignore=true'
        }
      }
    }
  }
  post {
    always {
      junit testResults: '*/target/surefire-reports/*.xml', allowEmptyResults: true
      publishHTML (target: [
        allowMissing: true,
        alwaysLinkToLastBuild: false,
        keepAll: true,
        reportDir: 'target/staging',
        reportFiles: 'index.html',
        reportName: "Maven Site"
      ])
    }
    success {
      slackSend (color: '#00FF00', message: "[CI_KUBE][SUCCESSFUL] Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})", channel: "${slackChannel}")
    }
    failure {
      slackSend (color: '#FF0000', message: "[CI_KUBE][FAILED] Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})", channel: "${slackChannel}")
    }
  }
}
