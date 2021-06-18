/**
 * Copyright (C) 2006-2021 Talend Inc. - www.talend.com
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

def slackChannel = 'components-ci'

def ossrhCredentials = usernamePassword(
	credentialsId: 'ossrh-credentials',
    usernameVariable: 'OSSRH_USER',
    passwordVariable: 'OSSRH_PASS')
def jetbrainsCredentials = usernamePassword(
	credentialsId: 'jetbrains-credentials',
    usernameVariable: 'JETBRAINS_USER',
    passwordVariable: 'JETBRAINS_PASS')
def jiraCredentials = usernamePassword(
	credentialsId: 'jira-credentials',
    usernameVariable: 'JIRA_USER',
    passwordVariable: 'JIRA_PASS')
def gitCredentials = usernamePassword(
	credentialsId: 'github-credentials',
    usernameVariable: 'GITHUB_USER',
    passwordVariable: 'GITHUB_PASS')
def dockerCredentials = usernamePassword(
	credentialsId: 'artifactory-datapwn-credentials',
    passwordVariable: 'DOCKER_PASS',
    usernameVariable: 'DOCKER_USER')
def sonarCredentials = usernamePassword(
    credentialsId: 'sonar-credentials',
    passwordVariable: 'SONAR_PASS',
    usernameVariable: 'SONAR_USER')

def branchName = env.BRANCH_NAME
if (BRANCH_NAME.startsWith("PR-")) {
    branchName = env.CHANGE_BRANCH
}
def escapedBranch = branchName.toLowerCase().replaceAll("/", "_")
def deploymentSuffix = (env.BRANCH_NAME == "master" || env.BRANCH_NAME.startsWith("maintenance/")) ? "snapshots" : "dev_branch_snapshots/branch_${escapedBranch}"
def deploymentRepository = "https://artifacts-zl.talend.com/nexus/content/repositories/${deploymentSuffix}"
def m2 = "/tmp/jenkins/tdi/m2/${deploymentSuffix}"
def isStdBranch = (env.BRANCH_NAME == "master" || env.BRANCH_NAME.startsWith("maintenance/"))
def talendRepositoryArg = isStdBranch ? "" : "-Dtalend_oss_snapshots=https://nexus-smart-branch.datapwn.com/nexus/content/repositories/${deploymentSuffix} -Dtalend_snapshots=https://nexus-smart-branch.datapwn.com/nexus/content/repositories/${deploymentSuffix}"
def podLabel = "component-runtime-${UUID.randomUUID().toString()}".take(53)

pipeline {
    agent {
        kubernetes {
            label podLabel
            yaml """
apiVersion: v1
kind: Pod
spec:
    containers:
        -
            name: main
            image: '${env.TSBI_IMAGE}'
            command: [cat]
            tty: true
            volumeMounts: [{name: docker, mountPath: /var/run/docker.sock}, {name: m2main, mountPath: /root/.m2/repository}, {name: dockercache, mountPath: /root/.dockercache}]
            resources: {requests: {memory: 4G, cpu: '2.5'}, limits: {memory: 8G, cpu: '3.5'}}
    volumes:
        -
            name: docker
            hostPath: {path: /var/run/docker.sock}
        -
            name: m2main
            hostPath: {path: ${m2} }
        -
            name: dockercache
            hostPath: {path: /tmp/jenkins/tdi/docker}
    imagePullSecrets:
        - name: talend-registry
"""
        }
    }

    environment {
        MAVEN_OPTS="-Dformatter.skip=true -Dmaven.artifact.threads=256"
        BUILD_ARGS="-Possrh -Prelease -Dgpg.skip=true"
        SKIP_OPTS="-Dspotless.apply.skip=true -Dcheckstyle.skip=true -Drat.skip=true -DskipTests -Dinvoker.skip=true"
        DEPLOY_OPTS="$SKIP_OPTS -Possrh -Prelease"
        GPG_DIR="$HOME/.gpg"
        ARTIFACTORY_REGISTRY = "artifactory.datapwn.com"
        VERACODE_APP_NAME = 'Talend Component Kit'
        VERACODE_SANDBOX = 'component-runtime'
        APP_ID = '579232'
    }

    options {
        buildDiscarder(logRotator(artifactNumToKeepStr: '5', numToKeepStr: env.BRANCH_NAME == 'master' ? '10' : '2'))
        timeout(time: 80, unit: 'MINUTES')
        skipStagesAfterUnstable()
    }

    triggers {
        cron(env.BRANCH_NAME == "master" ? "@daily" : "")
    }

    parameters {
        choice(name: 'Action',
                choices: ['STANDARD', 'RELEASE'],
                description: 'Kind of running : \nSTANDARD : (default) classical CI\nRELEASE : Build release')
        booleanParam(name: 'FORCE_SONAR', defaultValue: false, description: 'Force Sonar analysis')
    }

    stages {
        stage('Standard maven build') {
            when { expression { params.Action != 'RELEASE' } }
            steps {
                container('main') {
                    withCredentials([ossrhCredentials]) {
                        sh "mvn clean install $BUILD_ARGS -s .jenkins/settings.xml"
                    }
                }
            }
        }
        stage('Master/Maintenance Build Tasks') {
            when {
                allOf {
                    expression { params.Action != 'RELEASE' }
                    expression { isStdBranch }
                }
            }
            steps {
                container('main') {
                    withCredentials([ossrhCredentials]) {
                        sh "mvn deploy $DEPLOY_OPTS -s .jenkins/settings.xml"
                    }
                }
            }
//            steps {
//                container('main') {
//                    withCredentials([dockerCredentials]) {
//                        sh '''#!/bin/bash
//                              env|sort
//                              docker version
//                              echo $DOCKER_PASS | docker login $ARTIFACTORY_REGISTRY -u $DOCKER_USER --password-stdin
//                              env.PROJECT_VERSION = sh(returnStdout: true, script: "mvn org.apache.maven.plugins:maven-help-plugin:3.2.0:evaluate -Dexpression=project.version -q -DforceStdout").trim()
//                              echo ">> Building and pushing TSBI images ${PROJECT_VERSION}"
//                              cd images/component-server-image
//                              mvn clean verify dockerfile:build -P ci-tsbi
//                              docker tag talend/common/tacokit/component-server:${PROJECT_VERSION} artifactory.datapwn.com/tlnd-docker-dev/talend/common/tacokit/component-server:${PROJECT_VERSION}
//                              docker push artifactory.datapwn.com/tlnd-docker-dev/talend/common/tacokit/component-server:${PROJECT_VERSION}
//                              cd ../component-server-vault-proxy-image
//                              mvn clean verify dockerfile:build -P ci-tsbi
//                              docker tag talend/common/tacokit/component-server-vault-proxy:${PROJECT_VERSION} artifactory.datapwn.com/tlnd-docker-dev/talend/common/tacokit/component-server-vault-proxy:${PROJECT_VERSION}
//                              docker push artifactory.datapwn.com/tlnd-docker-dev/talend/common/tacokit/component-server-vault-proxy:${PROJECT_VERSION}
//                              cd ../..
//                              #TODO starter and remote-engine-customizer
//                           '''
//                    }
//                }
//            }
        }
        stage('Master Post Build Tasks') {
            when {
                    expression { params.Action != 'RELEASE' }
                    branch 'master'
            }
            steps {
                container('main') {
                    withCredentials([ossrhCredentials, gitCredentials]) {
                        sh "cd documentation && mvn verify pre-site -Pgh-pages -Dgpg.skip=true $SKIP_OPTS -s .jenkins/settings.xml && cd -"
                    }
                }
                container('main') {
                    withCredentials([ossrhCredentials]) {
                        sh "mvn ossindex:audit -s .jenkins/settings.xml"
                    }
                }
                container('main') {
                    withCredentials([sonarCredentials]) {
                        sh "mvn -Dsonar.host.url=https://sonar-eks.datapwn.com -Dsonar.login='$SONAR_LOGIN' -Dsonar.password='$SONAR_PASSWORD' -Dsonar.branch.name=${env.BRANCH_NAME} sonar:sonar"
                    }
                }
            }
        }
        stage('Release') {
                when {
                    allOf {
                        expression { params.Action == 'RELEASE' }
                        expression { isStdBranch }
                    }
                }
            steps {
                withCredentials([gitCredentials, dockerCredentials, ossrhCredentials ]) {
                    container('main') {
                        script {
                            env.RELEASE_VERSION = sh(returnStdout: true, script: "mvn org.apache.maven.plugins:maven-help-plugin:3.2.0:evaluate -Dexpression=project.version -q -DforceStdout|cut -d- -f1").trim()
                            sh "sh .jenkins/release.sh"
                        }
                    }
                }
            }
        }
    }
    post {
        success {
            slackSend(color: '#00FF00', message: "SUCCESSFUL: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})", channel: "${slackChannel}")
        }
        failure {
            slackSend(color: '#FF0000', message: "FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})", channel: "${slackChannel}")
        }
    }
}
