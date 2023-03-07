/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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
// Credentials
final def ossrhCredentials = usernamePassword(credentialsId: 'ossrh-credentials', usernameVariable: 'OSSRH_USER', passwordVariable: 'OSSRH_PASS')
final def jetbrainsCredentials = usernamePassword(credentialsId: 'jetbrains-credentials', usernameVariable: 'JETBRAINS_USER', passwordVariable: 'JETBRAINS_PASS')
final def jiraCredentials = usernamePassword(credentialsId: 'jira-credentials', usernameVariable: 'JIRA_USER', passwordVariable: 'JIRA_PASS')
final def gitCredentials = usernamePassword(credentialsId: 'github-credentials', usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_PASS')
final def dockerCredentials = usernamePassword(credentialsId: 'artifactory-datapwn-credentials', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')
final def sonarCredentials = usernamePassword( credentialsId: 'sonar-credentials', usernameVariable: 'SONAR_USER', passwordVariable: 'SONAR_PASS')
final def keyImportCredentials = usernamePassword(credentialsId: 'component-runtime-import-key-credentials', usernameVariable: 'KEY_USER', passwordVariable: 'KEY_PASS')
final def gpgCredentials = usernamePassword(credentialsId: 'component-runtime-gpg-credentials', usernameVariable: 'GPG_KEYNAME', passwordVariable: 'GPG_PASSPHRASE')

// Job config
final String slackChannel = 'components-ci'
final Boolean isMasterBranch = env.BRANCH_NAME == "master"
final Boolean isStdBranch = (env.BRANCH_NAME == "master" || env.BRANCH_NAME.startsWith("maintenance/"))
final Boolean hasPostLoginScript = params.POST_LOGIN_SCRIPT != ""
final Boolean hasExtraBuildArgs = params.EXTRA_BUILD_ARGS != ""
final String buildTimestamp = String.format('-%tY%<tm%<td%<tH%<tM%<tS', java.time.LocalDateTime.now())

// Files and folder definition
final String _COVERAGE_REPORT_PATH = '**/jacoco-aggregate/jacoco.xml'

// Artifacts paths
final String _ARTIFACT_COVERAGE = '**/jacoco-aggregate/**/*.*'

pipeline {
    agent {
        kubernetes {
            yamlFile '.jenkins/jenkins_pod.yml'
            defaultContainer 'main'
        }
    }

    environment {
        MAVEN_OPTS="-Dformatter.skip=true -Dmaven.artifact.threads=256"
        BUILD_ARGS="-Dgpg.skip=true -Denforcer.skip=true"
        SKIP_OPTS="-Dspotless.apply.skip=true -Dcheckstyle.skip=true -Drat.skip=true -DskipTests -Dinvoker.skip=true"
        DEPLOY_OPTS="$SKIP_OPTS -Possrh -Prelease -Pgpg2 -Denforcer.skip=true"
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
        choice(
          name: 'Action',
          choices: ['STANDARD', 'RELEASE'],
          description: 'Kind of running:\nSTANDARD: (default) classical CI\nRELEASE: Build release')
        booleanParam(
          name: 'FORCE_SONAR',
          defaultValue: false,
          description: 'Force Sonar analysis')
        string(
          name: 'EXTRA_BUILD_ARGS',
          defaultValue: '',
          description: 'Add some extra parameters to maven commands. Applies to all maven calls.')
        string(
          name: 'POST_LOGIN_SCRIPT',
          defaultValue: '',
          description: 'Execute a shell command after login. Useful for maintenance.')
        booleanParam(
          name: 'DEBUG_BEFORE_EXITING',
          defaultValue: false,
          description: 'Add an extra step to the pipeline allowing to keep the pod alive for debug purposes.')
    }

    stages {
        stage('Preliminary steps') {
            steps {
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

                    def pom = readMavenPom file: 'pom.xml'
                    env.PROJECT_VERSION = pom.version
                    try {
                        EXTRA_BUILD_ARGS = params.EXTRA_BUILD_ARGS
                    } catch (ignored) {
                        EXTRA_BUILD_ARGS = ""
                    }
                }
                ///////////////////////////////////////////
                // Updating build displayName and description
                ///////////////////////////////////////////
                script {
                    String user_name = currentBuild.getBuildCauses('hudson.model.Cause$UserIdCause').userId[0]
                    if ( user_name == null) { user_name = "auto" }

                    currentBuild.displayName = (
                      "#$currentBuild.number-$params.Action: $user_name"
                    )

                    // updating build description
                    currentBuild.description = ("""
                       User: $user_name - $params.Action Build
                       Sonar: $params.FORCE_SONAR - Script: $hasPostLoginScript
                       Extra args: $hasExtraBuildArgs - Debug: $params.DEBUG_BEFORE_EXITING""".stripIndent()
                    )
                }
                ///////////////////////////////////////////
                // asdf install
                ///////////////////////////////////////////
                script {
                    println "asdf install the content of repository .tool-versions'\n"
                    sh """
                        bash .jenkins/scripts/asdf_install.sh
                    """
                }
            }
        }
        stage('Post login') {
            steps {
                withCredentials([gitCredentials, dockerCredentials, ossrhCredentials, jetbrainsCredentials, jiraCredentials, gpgCredentials]) {
                    script {
                        try {
                            sh "${params.POST_LOGIN_SCRIPT}"
                            sh """
                               bash .jenkins/scripts/npm_fix.sh
                               """
                        } catch (ignored) {
                            //
                        }
                    }
                }
            }
        }
        stage('Standard maven build') {
            when { expression { params.Action != 'RELEASE' } }
            steps {
                withCredentials([ossrhCredentials]) {
                    sh "mvn clean install $BUILD_ARGS $EXTRA_BUILD_ARGS -s .jenkins/settings.xml"
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
                withCredentials([ossrhCredentials, gpgCredentials]) {
                    sh "mvn deploy $DEPLOY_OPTS $EXTRA_BUILD_ARGS -s .jenkins/settings.xml"
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
                script {
                    configFileProvider([configFile(fileId: 'maven-settings-nexus-zl', variable: 'MAVEN_SETTINGS')]) {
                        sh """
                           bash .jenkins/scripts/docker_build.sh ${env.PROJECT_VERSION}${buildTimestamp}
                           """
                    }
                }
            }
        }
        stage('Documentation') {
            when {
                allOf {
                    expression { params.Action != 'RELEASE' }
                    expression { isMasterBranch }
                }
            }
            steps {
                withCredentials([ossrhCredentials, gitCredentials]) {
                    sh "cd documentation && mvn verify pre-site -Pgh-pages -Dgpg.skip=true $SKIP_OPTS $EXTRA_BUILD_ARGS -s ../.jenkins/settings.xml && cd -"
                }
            }
        }
        stage('Master Post Build Tasks') {
            when {
                expression { params.Action != 'RELEASE' }
                branch 'master'
            }
            steps {
                withCredentials([ossrhCredentials]) {
                    catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                        sh """
                            mvn ossindex:audit-aggregate -pl '!bom' -Dossindex.fail=false -Dossindex.reportFile=target/audit.txt -s .jenkins/settings.xml
                            mvn versions:dependency-updates-report versions:plugin-updates-report versions:property-updates-report -pl '!bom'
                           """
                    }
                }
                withCredentials([sonarCredentials]) {
                    catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                        // TODO https://jira.talendforge.org/browse/TDI-48980 (CI: Reactivate Sonar cache)
                        sh "_JAVA_OPTIONS='--add-opens=java.base/java.lang=ALL-UNNAMED' mvn -Dsonar.host.url=https://sonar-eks.datapwn.com -Dsonar.login='$SONAR_USER' -Dsonar.password='$SONAR_PASS' -Dsonar.branch.name=${env.BRANCH_NAME} -Dsonar.analysisCache.enabled=false sonar:sonar"
                    }
                }
            }
            post {
                always {
                    publishHTML(
                            target: [
                                    allowMissing         : true,
                                    alwaysLinkToLastBuild: false,
                                    keepAll              : true,
                                    reportDir            : 'target/',
                                    reportFiles          : 'audit.txt',
                                    reportName           : "security::audit"
                            ])
                    publishHTML(
                            target: [
                                    allowMissing         : true,
                                    alwaysLinkToLastBuild: false,
                                    keepAll              : true,
                                    reportDir            : 'target/site/',
                                    reportFiles          : 'property-updates-report.html',
                                    reportName           : "outdated::property"
                            ])
                    publishHTML(
                            target: [
                                    allowMissing         : true,
                                    alwaysLinkToLastBuild: false,
                                    keepAll              : true,
                                    reportDir            : 'target/site/',
                                    reportFiles          : 'dependency-updates-report.html',
                                    reportName           : "outdated::dependency"
                            ])
                    publishHTML(
                            target: [
                                    allowMissing         : true,
                                    alwaysLinkToLastBuild: false,
                                    keepAll              : true,
                                    reportDir            : 'target/site/',
                                    reportFiles          : 'plugin-updates-report.html',
                                    reportName           : "outdated::plugins"
                            ])
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
        stage('Debug') {
            when { expression { return params.DEBUG_BEFORE_EXITING } }
            steps { script { input message: 'Finish the job?', ok: 'Yes' } }
        }
    }
    post {
        success {
            script {
                //Only post results to Slack for Master and Maintenance branches
                if (isStdBranch) {
                    slackSend(
                        color: '#00FF00',
                        message: "SUCCESSFUL: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})",
                        channel: "${slackChannel}"
                    )
                }
            }
            script {
                println "====== Publish Coverage"
                publishCoverage adapters: [jacocoAdapter('**/jacoco-aggregate/*.xml')]
                publishCoverage adapters: [jacocoAdapter('**/jacoco-it/*.xml')]
                publishCoverage adapters: [jacocoAdapter('**/jacoco-ut/*.xml')]
                println "====== Publish HTML API Coverage"
                publishHTML([
                  allowMissing         : false,
                  alwaysLinkToLastBuild: false,
                  keepAll              : true,
                  reportDir            : 'reporting/target/site/jacoco-aggregate',
                  reportFiles          : 'index.html',
                  reportName           : 'Coverage',
                  reportTitles         : 'Coverage'
                ])
            }
        }
        failure {
            script {
                //Only post results to Slack for Master and Maintenance branches
                if (isStdBranch) {
                    //if previous build was a success, ping channel in the Slack message
                    if ("SUCCESS".equals(currentBuild.previousBuild.result)) {
                        slackSend(
                            color: '#FF0000',
                            message: "@here : NEW FAILURE: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})",
                            channel: "${slackChannel}"
                        )
                    } else {
                        //else send notification without pinging channel
                        slackSend(
                            color: '#FF0000',
                            message: "FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})",
                            channel: "${slackChannel}"
                        )
                    }
                }
            }
        }
        always {
            recordIssues(
                enabledForFailure: true,
                tools: [
                    junitParser(
                      id: 'unit-test',
                      name: 'Unit Test',
                      pattern: '**/target/surefire-reports/*.xml'
                    ),
                    taskScanner(
                        id: 'disabled',
                        name: '@Disabled',
                        includePattern: '**/src/**/*.java',
                        ignoreCase: true,
                        normalTags: '@Disabled'
                    ),
                    taskScanner(
                        id: 'todo',
                        name: 'Todo(low)/Fixme(high)',
                        includePattern: '**/src/**/*.java',
                        ignoreCase: true,
                        highTags: 'FIX_ME, FIXME',
                        lowTags: 'TO_DO, TODO'
                    )
                ]
            )
            script {
                println '====== Archive artifacts'
                println "Artifact 1: ${_ARTIFACT_COVERAGE}"
                archiveArtifacts artifacts: "${_ARTIFACT_COVERAGE}", allowEmptyArchive: true, onlyIfSuccessful: false
            }
        }
    }
}
