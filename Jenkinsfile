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
final def slackChannel = 'components-ci'
final def ossrhCredentials = usernamePassword(credentialsId: 'ossrh-credentials', usernameVariable: 'OSSRH_USER', passwordVariable: 'OSSRH_PASS')
final def jetbrainsCredentials = usernamePassword(credentialsId: 'jetbrains-credentials', usernameVariable: 'JETBRAINS_USER', passwordVariable: 'JETBRAINS_PASS')
final def jiraCredentials = usernamePassword(credentialsId: 'jira-credentials', usernameVariable: 'JIRA_USER', passwordVariable: 'JIRA_PASS')
final def gitCredentials = usernamePassword(credentialsId: 'github-credentials', usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_PASS')
final def dockerCredentials = usernamePassword(credentialsId: 'artifactory-datapwn-credentials', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')
final def sonarCredentials = usernamePassword( credentialsId: 'sonar-credentials', usernameVariable: 'SONAR_USER', passwordVariable: 'SONAR_PASS')
final def keyImportCredentials = usernamePassword(credentialsId: 'component-runtime-import-key-credentials', usernameVariable: 'KEY_USER', passwordVariable: 'KEY_PASS')
final def gpgCredentials = usernamePassword(credentialsId: 'component-runtime-gpg-credentials', usernameVariable: 'GPG_KEYNAME', passwordVariable: 'GPG_PASSPHRASE')
final def isStdBranch = (env.BRANCH_NAME == "master" || env.BRANCH_NAME.startsWith("maintenance/"))
final def tsbiImage = "artifactory.datapwn.com/tlnd-docker-dev/talend/common/tsbi/jdk11-svc-springboot-builder:2.7.2-2.3-20210616074048"
final def podLabel = "component-runtime-${UUID.randomUUID().toString()}".take(53)

def EXTRA_BUILD_ARGS = ""

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
            image: '${tsbiImage}'
            command: [cat]
            tty: true
            volumeMounts: [
                { name: docker, mountPath: /var/run/docker.sock }, 
                { name: efs-jenkins-component-runtime-m2, mountPath: /root/.m2/repository}, 
                { name: dockercache, mountPath: /root/.dockercache}
            ]
            resources: {requests: {memory: 8G, cpu: '6.0'}, limits: {memory: 12G, cpu: '6.5'}}
    volumes:
        -
            name: docker
            hostPath: {path: /var/run/docker.sock}
        -
            name: efs-jenkins-component-runtime-m2
            persistentVolumeClaim: 
                claimName: efs-jenkins-component-runtime-m2
        -
            name: dockercache
            hostPath: {path: /tmp/jenkins/component-runtime/docker}
    imagePullSecrets:
        - name: talend-registry
"""
        }
    }

    environment {
        MAVEN_OPTS="-Dformatter.skip=true -Dmaven.artifact.threads=256"
        BUILD_ARGS="-Dgpg.skip=true"
        SKIP_OPTS="-Dspotless.apply.skip=true -Dcheckstyle.skip=true -Drat.skip=true -DskipTests -Dinvoker.skip=true"
        DEPLOY_OPTS="$SKIP_OPTS -Possrh -Prelease -Pgpg2"
        ARTIFACTORY_REGISTRY = "artifactory.datapwn.com"
        VERACODE_APP_NAME = 'Talend Component Kit'
        VERACODE_SANDBOX = 'component-runtime'
        APP_ID = '579232'
    }

    options {
        buildDiscarder(logRotator(artifactNumToKeepStr: '10', numToKeepStr: env.BRANCH_NAME == 'master' ? '15' : '10'))
        timeout(time: 180, unit: 'MINUTES')
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
        string(name: 'EXTRA_BUILD_ARGS', defaultValue: "", description: 'Add some extra parameters to maven commands. Applies to all maven calls.')
        string(name: 'POST_LOGIN_SCRIPT', defaultValue: "", description: 'Execute a shell command after login. Useful for maintenance.')
    }

    stages {
        stage('Preliminary steps') {
            steps {
                container('main') {
                    script {
                        withCredentials([gitCredentials]) {
                            sh """
                               bash .jenkins/scripts/git_login.sh "\${GITHUB_USER}" "\${GITHUB_PASS}"
                               """
                        }
                        withCredentials([dockerCredentials]) {
                            sh """
                               bash .jenkins/scripts/docker_login.sh "${ARTIFACTORY_REGISTRY}" "\${DOCKER_USER}" "\${DOCKER_PASS}"
                               """
                        }
                        withCredentials([keyImportCredentials]) {
                            sh """
                               bash .jenkins/scripts/setup_gpg.sh
                               """
                        }
                        env.PROJECT_VERSION = sh(returnStdout: true, script: "mvn org.apache.maven.plugins:maven-help-plugin:3.2.0:evaluate -Dexpression=project.version -q -DforceStdout").trim()
                        try {
                            EXTRA_BUILD_ARGS = params.EXTRA_BUILD_ARGS
                        } catch (error) {
                            EXTRA_BUILD_ARGS = ""
                        }
                    }
                }
            }
        }
        stage('Post login') {
            steps {
                container('main') {
                    script {
                        try {
                            sh "${params.POST_LOGIN_SCRIPT}"
                        } catch (error) {
                            //
                        }
                    }
                }
            }
        }
        stage('Standard maven build') {
            when { expression { params.Action != 'RELEASE' } }
            steps {
                container('main') {
                    withCredentials([ossrhCredentials]) {
                        sh "mvn clean install $BUILD_ARGS $EXTRA_BUILD_ARGS -s .jenkins/settings.xml"
                    }
                }
            }
        }
        stage('Deploy artifacts') {
            when {
                allOf {
                    expression { params.Action != 'RELEASE' }
                    expression { isStdBranch }
                }
            }
            steps {
                container('main') {
                    withCredentials([ossrhCredentials, gpgCredentials]) {
                        sh "mvn deploy $DEPLOY_OPTS $EXTRA_BUILD_ARGS -s .jenkins/settings.xml"
                    }
                }
            }
        }
        stage('Docker images') {
            when {
                allOf {
                    expression { params.Action != 'RELEASE' }
                    expression { isStdBranch }
                }
            }
            steps {
                container('main') {
                    script {
                        configFileProvider([configFile(fileId: 'maven-settings-nexus-zl', variable: 'MAVEN_SETTINGS')]) {
                            sh """
                               bash .jenkins/scripts/docker_build.sh ${env.PROJECT_VERSION}
                               """
                        }
                    }
                }
            }
        }
        stage('Documentation') {
            when {
                expression { params.Action != 'RELEASE' }
                branch 'master'
            }
            steps {
                container('main') {
                    withCredentials([ossrhCredentials, gitCredentials]) {
                        sh "cd documentation && mvn verify pre-site -Pgh-pages -Dgpg.skip=true $SKIP_OPTS $EXTRA_BUILD_ARGS -s ../.jenkins/settings.xml && cd -"
                    }
                }
            }
        }
        stage('Master Post Build Tasks') {
            when {
                expression { params.Action != 'RELEASE' }
                branch 'master'
            }
            steps {
                container('main') {
                    withCredentials([ossrhCredentials]) {
                        catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                            sh "mvn ossindex:audit -s .jenkins/settings.xml"
                        }
                    }
                    withCredentials([sonarCredentials]) {
                        catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                            sh "mvn -Dsonar.host.url=https://sonar-eks.datapwn.com -Dsonar.login='$SONAR_USER' -Dsonar.password='$SONAR_PASS' -Dsonar.branch.name=${env.BRANCH_NAME} sonar:sonar"
                        }
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
                container('main') {
                    script {
                        withCredentials([gitCredentials, dockerCredentials, ossrhCredentials, jetbrainsCredentials, jiraCredentials, gpgCredentials]) {
                            configFileProvider([configFile(fileId: 'maven-settings-nexus-zl', variable: 'MAVEN_SETTINGS')]) {
                                sh """
                                   bash .jenkins/scripts/release.sh ${env.BRANCH_NAME} ${env.PROJECT_VERSION} 
                                   """
                            }
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
