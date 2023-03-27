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
final String extraBuildParams = ""
final String buildTimestamp = String.format('-%tY%<tm%<td%<tH%<tM%<tS', java.time.LocalDateTime.now())

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
          name: 'FORCE_DOC',
          defaultValue: false,
          description: 'Force documentation stage for development branches. No action for Master/Maintenance.')
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
                        sh """ bash .jenkins/scripts/git_login.sh "\${GITHUB_USER}" "\${GITHUB_PASS}" """
                    }
                    withCredentials([dockerCredentials]) {
                        sh """ bash .jenkins/scripts/docker_login.sh "${ARTIFACTORY_REGISTRY}" "\${DOCKER_USER}" "\${DOCKER_PASS}" """
                    }
                    withCredentials([keyImportCredentials]) {
                        sh """ bash .jenkins/scripts/setup_gpg.sh"""
                    }

                    def pom = readMavenPom file: 'pom.xml'
                    env.PROJECT_VERSION = pom.version

                    Boolean documentation_requested = params.FORCE_DOC || isStdBranch

                    extraBuildParams = extraBuildParams_assembly(documentation_requested)

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
                       Debug: $params.DEBUG_BEFORE_EXITING
                       Extra build args: $extraBuildParams""".stripIndent()
                    )
                }
                ///////////////////////////////////////////
                // asdf install
                ///////////////////////////////////////////
                script {
                    println "asdf install the content of repository .tool-versions'\n"
                    sh 'bash .jenkins/scripts/asdf_install.sh'
                }
            }
        }
        stage('Post login') {
            steps {
                withCredentials([gitCredentials, dockerCredentials, ossrhCredentials, jetbrainsCredentials, jiraCredentials, gpgCredentials]) {
                    script {
                        try {
                            sh """\
                                #!/usr/bin/env bash
                                bash "${params.POST_LOGIN_SCRIPT}"
                                bash .jenkins/scripts/npm_fix.sh
                                """.stripIndent()
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
                    sh """\
                        #!/usr/bin/env bash
                        set -xe
                        mvn clean install $BUILD_ARGS \
                                          $extraBuildParams \
                                          --settings .jenkins/settings.xml
                        """.stripIndent()
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
                    sh """\
                        #!/usr/bin/env bash
                        set -xe
                        bash mvn deploy $DEPLOY_OPTS \
                                        $extraBuildParams \
                                        --settings .jenkins/settings.xml
                    """.stripIndent()
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
                        sh """\
                            #!/usr/bin/env bash 
                            bash .jenkins/scripts/docker_build.sh ${env.PROJECT_VERSION}${buildTimestamp}
                            """.stripIndent()
                    }
                }
            }
        }
        stage('Documentation') {
            when {
                expression {
                  params.FORCE_DOC || (params.Action != 'RELEASE' && isMasterBranch)
                }
            }
            steps {
                withCredentials([ossrhCredentials, gitCredentials]) {
                    sh """\
                        #!/usr/bin/env bash 
                        set -xe                       
                        mvn verify pre-site --file documentation/pom.xml \\
                                            --settings .jenkins/settings.xml \\
                                            -Pgh-pages \\
                                            --define gpg.skip=true \\
                                            $SKIP_OPTS \\
                                            $extraBuildParams 
                        mvn verify pre-site --file documentation/pom.xml \\
                                            --settings .jenkins/settings.xml \\
                                            -Pgh-pages \\
                                            --define gpg.skip=true \\
                                            $SKIP_OPTS \\
                                            $extraBuildParams 

                    """.stripIndent()
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
                        sh """\
                            #!/usr/bin/env bash 
                            set -xe
                            mvn ossindex:audit-aggregate -pl '!bom' \
                                                         --define ossindex.fail=false \
                                                         --define ossindex.reportFile=target/audit.txt \
                                                         --settings .jenkins/settings.xml
                            mvn versions:dependency-updates-report versions:plugin-updates-report versions:property-updates-report -pl '!bom'
                           """.stripIndent()
                    }
                }
                withCredentials([sonarCredentials]) {
                    catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                        // TODO https://jira.talendforge.org/browse/TDI-48980 (CI: Reactivate Sonar cache)
                        sh """\
                            #!/usr/bin/env bash 
                            set -xe
                            _JAVA_OPTIONS='--add-opens=java.base/java.lang=ALL-UNNAMED'
                            mvn sonar:sonar
                                --define sonar.host.url=https://sonar-eks.datapwn.com \
                                --define sonar.login='$SONAR_USER' \
                                --define sonar.password='$SONAR_PASS' \
                                --define sonar.branch.name=${env.BRANCH_NAME} \
                                --define sonar.analysisCache.enabled=false
                                
                        """.stripIndent()
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
                            sh """\
                               #!/usr/bin/env bash
                               bash .jenkins/scripts/release.sh ${env.BRANCH_NAME} ${env.PROJECT_VERSION} 
                               """.stripIndent()
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
                    if ("SUCCESS" == (currentBuild.previousBuild.result)) {
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
                println '====== Archive jacoco reports artifacts'
                archiveArtifacts artifacts: "${'**/jacoco-aggregate/**/*.*'}", allowEmptyArchive: true, onlyIfSuccessful: false
            }
        }
    }
}

/**
 * Assembly all needed items to put inside extraBuildParams
 *
 * @param Boolean use_antora, if set to true, ---define skipAntora=true will be added
 *
 * @return extraBuildParams as a string ready for mvn cmd
 */
private String extraBuildParams_assembly(Boolean use_antora) {
    String extraBuildParams

    println 'Processing extraBuildParams'
    println 'Manage the EXTRA_BUILD_PARAMS'
    final List<String> buildParamsAsArray = []

    if ( params.EXTRA_BUILD_PARAMS )
        buildParamsAsArray.add( params.EXTRA_BUILD_PARAMS as String )

    println 'Manage the use_antora option'
    if (! use_antora) {
        buildParamsAsArray.add('--define skipAntora=true')
        buildParamsAsArray.add('--define antora.skip=true')
        buildParamsAsArray.add('--define component.front.build.skip=true')
        buildParamsAsArray.add('--define talend.documentation.pdf.skip=true')
        buildParamsAsArray.add('--projects \\!org.talend.sdk.component:documentation')
        buildParamsAsArray.add('--projects \\!documentation')
    }

    println 'Construct extraBuildParams'

    extraBuildParams = buildParamsAsArray.join(' ')
    println "extraBuildParams: $extraBuildParams"

    return extraBuildParams
}
