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
def nexusCredentials = usernamePassword(
	credentialsId: 'nexus-artifact-zl-credentials',
    usernameVariable: 'NEXUS_USER',
    passwordVariable: 'NEXUS_PASS')
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

def PRODUCTION_DEPLOYMENT_REPOSITORY = "snapshots"

def branchName = env.BRANCH_NAME
if (BRANCH_NAME.startsWith("PR-")) {
    branchName = env.CHANGE_BRANCH
}

def escapedBranch = branchName.toLowerCase().replaceAll("/", "_")
def deploymentSuffix = (env.BRANCH_NAME == "master" || env.BRANCH_NAME.startsWith("maintenance/")) ? "${PRODUCTION_DEPLOYMENT_REPOSITORY}" : "dev_branch_snapshots/branch_${escapedBranch}"
def deploymentRepository = "https://artifacts-zl.talend.com/nexus/content/repositories/${deploymentSuffix}"
def m2 = "/tmp/jenkins/tdi/m2/${deploymentSuffix}"
def talendRepositoryArg = (env.BRANCH_NAME == "master" || env.BRANCH_NAME.startsWith("maintenance/")) ? "" : "-Dtalend_oss_snapshots=https://nexus-smart-branch.datapwn.com/nexus/content/repositories/${deploymentSuffix} -Dtalend_snapshots=https://nexus-smart-branch.datapwn.com/nexus/content/repositories/${deploymentSuffix}"

def calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

def commandFindModule = $/"grep '<module>' pom.xml | cut -d '>' -f 2 | cut -d '<' -f 1 | tr '\n' ','"/$
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
            resources: {requests: {memory: 3G, cpu: '2.5'}, limits: {memory: 3G, cpu: '2.5'}}
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
        DECRYPTER_ARG = "-Dtalend.maven.decrypter.m2.location=${env.WORKSPACE}/.jenkins/"
        MAVEN_OPTS = "-Dtalend-image.layersCacheDirectory=/root/.dockercache -Dmaven.artifact.threads=128 -Dorg.slf4j.simpleLogger.showThreadName=true -Dorg.slf4j.simpleLogger.showDateTime=true -Dorg.slf4j.simpleLogger.dateTimeFormat=HH:mm:ss ${DECRYPTER_ARG}"
        TALEND_REGISTRY = "registry.datapwn.com"
        VERACODE_APP_NAME = 'Talend Component Kit'
        VERACODE_SANDBOX = 'component-runtime'
        APP_ID = '579232'
        ARTIFACTORY_REGISTRY = "artifactory.datapwn.com"
    }

    options {
        buildDiscarder(logRotator(artifactNumToKeepStr: '5', numToKeepStr: env.BRANCH_NAME == 'master' ? '10' : '2'))
        timeout(time: 60, unit: 'MINUTES')
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
        stage('Docker login') {
            steps {
                container('main') {
                    withCredentials([dockerCredentials]) {
                        sh '''#!/bin/bash
                        env|sort
                        docker version
                        echo $ARTIFACTORY_PASSWORD | docker login $ARTIFACTORY_REGISTRY -u $ARTIFACTORY_LOGIN --password-stdin
                        '''
                    }
                }
            }
        }
        stage('Run maven') {
            when {
                expression { params.Action == 'STANDARD' }
            }
            steps {
                container('main') {
                    withCredentials([nexusCredentials
                        , tdsCredentials
                        , netsuiteCredentials
                        , netsuiteConsumerCredentials
                        , netsuiteTokenCredentials]) {
                        // for next concurrent builds
                        sh 'for i in ci_nexus; do rm -Rf $i; rsync -av . $i; done'
                        // real task
                        sh "mvn -B -s .jenkins/settings.xml clean install ${DECRYPTER_ARG} -DskipDocker=false ${talendRepositoryArg}"
                        sh "mvn compile com.github.spotbugs:spotbugs-maven-plugin:3.1.12:spotbugs -Dspotbugs.effort=Max"
                        sh """
                                |cd .jenkins
                                |javac -classpath . org/talend/view/Main.java
                                |java -cp . org.talend.view.Main ..
                        """.stripMargin()
                    }
                }
            }
            post {
                always {
                    junit testResults: '*/target/surefire-reports/*.xml', allowEmptyResults: true
                    publishHTML(target: [
                            allowMissing: false, alwaysLinkToLastBuild: false, keepAll: true,
                            reportDir   : 'target/talend-component-kit', reportFiles: 'icon-report.html',
                            reportName  : "Icon Report"
                    ])
                    publishHTML(target: [
                            allowMissing: false, alwaysLinkToLastBuild: false, keepAll: true,
                            reportDir   : 'target/talend-component-kit', reportFiles: 'repository-dependency-report.html',
                            reportName  : "Dependencies Report"
                    ])
                    publishHTML(target: [
                            allowMissing: false, alwaysLinkToLastBuild: false, keepAll: true,
                            reportDir   : 'target',
                            reportFiles : "indexSpotBug.html",
                            reportName  : "Bugs Report"
                    ])
                }
            }
        }
        stage('Post Build Tasks') {
            when {
                expression { params.Action == 'STANDARD' }
            }
            parallel {
                stage('Nexus Deployment') {
                    when {
                        anyOf {
                            branch 'master'
                            expression { env.BRANCH_NAME.startsWith('maintenance/') }
                        }
                    }
                    steps {
                        container('main') {
                            withCredentials([nexusCredentials]) {
                                sh "cd ci_nexus && mvn -B -s .jenkins/settings.xml clean deploy -DskipTests -DskipITs ${DECRYPTER_ARG} ${talendRepositoryArg}"
                            }
                        }
                    }
                }
                stage('Sonar') {
                    when {
                        anyOf {
                            branch 'master'
                            expression { env.BRANCH_NAME.startsWith('maintenance/') }
                            expression { params.FORCE_SONAR == true }
                        }
                    }
                    steps {
                        container('main') {
                            withCredentials([sonarCredentials]) {
                                sh "mvn -Dsonar.host.url=https://sonar-eks.datapwn.com -Dsonar.login='$SONAR_LOGIN' -Dsonar.password='$SONAR_PASSWORD' -Dsonar.branch.name=${env.BRANCH_NAME} sonar:sonar"
                            }
                        }
                    }
                }
            }
        }
        stage('Release') {
            when {
                expression { params.Action == 'RELEASE' }
                anyOf {
                    branch 'master'
                    expression { BRANCH_NAME.startsWith('maintenance/') }
                }
            }
            steps {
                withCredentials([gitCredentials, nexusCredentials, dockerCredentials, tdsCredentials, netsuiteCredentials, netsuiteConsumerCredentials, netsuiteTokenCredentials]) {
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
