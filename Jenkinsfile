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
// Imports
import java.time.LocalDateTime
import java.util.regex.Matcher

// Credentials
final def ossrhCredentials = usernamePassword(credentialsId: 'ossrh-credentials', usernameVariable: 'OSSRH_USER', passwordVariable: 'OSSRH_PASS')
final def nexusCredentials = usernamePassword(credentialsId: 'nexus-artifact-zl-credentials', usernameVariable: 'NEXUS_USER', passwordVariable: 'NEXUS_PASS')
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
final String buildTimestamp = String.format('-%tY%<tm%<td%<tH%<tM%<tS', LocalDateTime.now())

// Job variables declaration
String branch_user
String branch_ticket
String branch_description
String pomVersion
String qualifiedVersion
Boolean stdBranch_buildOnly = false
Boolean devBranch_mavenDeploy = false
Boolean devBranch_dockerPush = false

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
          name: 'MAVEN_DEPLOY',
          defaultValue: false,
          description: '''
            Force MAVEN deploy stage for development branches. No effect on master and maintenance.
            INFO: master/maintenance branch are deploying on <oss.sonatype.org>
                  dev branches are deploying on <artifacts-zl.talend.com>''')
        booleanParam(
          name: 'DOCKER_PUSH',
          defaultValue: false,
          description: '''
            Force DOCKER push stage for development branches. No effect on master and maintenance.
            INFO: master/maintenance and dev branches are deploying on <artifactory.datapwn.com>''')
        string(
          name: 'VERSION_QUALIFIER',
          defaultValue: 'DEFAULT',
          description: '''
            Deploy jars with the given version qualifier. No effect on master and maintenance.
             - DEFAULT means the qualifier will be the Jira id extracted from the branch name.
            From "user/JIRA-12345_some_information" the qualifier will be JIRA-12345.''')
        string(
          name: 'EXTRA_BUILD_PARAMS',
          defaultValue: '',
          description: 'Add some extra parameters to maven commands. Applies to all maven calls.')
        string(
          name: 'POST_LOGIN_SCRIPT',
          defaultValue: '',
          description: 'Execute a shell command after login. Useful for maintenance.')
        booleanParam(
          name: 'FORCE_SONAR',
          defaultValue: false,
          description: 'Force Sonar analysis')
        booleanParam(
          name: 'FORCE_DOC',
          defaultValue: false,
          description: 'Force documentation stage for development branches. No effect on master and maintenance.')
        booleanParam(
          name: 'JENKINS_DEBUG',
          defaultValue: false,
          description: 'Add an extra step to the pipeline allowing to keep the pod alive for debug purposes.')
    }

    stages {
        stage('Preliminary steps') {
            steps {

                ///////////////////////////////////////////
                // asdf install
                ///////////////////////////////////////////
                script {
                    println "asdf install the content of repository .tool-versions'\n"
                    sh 'bash .jenkins/scripts/asdf_install.sh'
                }

                ///////////////////////////////////////////
                // Variables init
                ///////////////////////////////////////////
                script {
                    stdBranch_buildOnly = isStdBranch && params.Action != 'RELEASE'
                    devBranch_mavenDeploy = !isStdBranch && params.MAVEN_DEPLOY
                    devBranch_dockerPush = !isStdBranch && params.DOCKER_PUSH

                    // By default the doc is skipped for standards branches
                    Boolean skip_documentation = !( params.FORCE_DOC || isStdBranch )
                    extraBuildParams = assemblyExtraBuildParams(skip_documentation, devBranch_mavenDeploy || devBranch_dockerPush)

                }

                ///////////////////////////////////////////
                // Pom version and Qualifier management
                ///////////////////////////////////////////
                script{
                    final def pom = readMavenPom file: 'pom.xml'
                    pomVersion = pom.version

                    echo 'Manage the version qualifier'
                    if (isStdBranch || (!params.MAVEN_DEPLOY && !isStdBranch)) {
                        println """
                             No need to add qualifier in followings cases:' +
                             - We are on Master or Maintenance branch
                             - We do not want to deploy on dev branch
                             """.stripIndent()
                        qualifiedVersion = pomVersion
                    }
                    else {
                        branch_user = ""
                        branch_ticket = ""
                        branch_description = ""
                        if (params.VERSION_QUALIFIER != ("DEFAULT")) {
                            // If the qualifier is given, use it
                            println """
                             No need to add qualifier, use the given one: "$params.VERSION_QUALIFIER"
                             """.stripIndent()
                        }
                        else {
                            echo "Validate the branch name"
                            (branch_user,
                            branch_ticket,
                            branch_description) = extract_branch_info("$env.BRANCH_NAME")

                            // Check only branch_user, because if there is an error all three params are empty.
                            if (branch_user == ("")) {
                                println """
                                ERROR: The branch name doesn't comply with the format: user/JIRA-1234-Description
                                It is MANDATORY for artifact management.
                                You have few options:
                                - You do not need to deploy, uncheck MAVEN_DEPLOY checkbox
                                - Change the VERSION_QUALIFIER text box to a personal qualifier, BUT you need to do it on ALL se/ee and cloud-components build
                                - Rename your branch
                                """.stripIndent()
                                currentBuild.description = ("ERROR: The branch name is not correct")
                                sh """exit 1"""
                            }
                        }

                        echo "Insert a qualifier in pom version..."
                        qualifiedVersion = add_qualifier_to_version(
                          pomVersion,
                          branch_ticket,
                          "$params.VERSION_QUALIFIER" as String)

                        echo """
                          Configure the version qualifier for the curent branche: $env.BRANCH_NAME
                          requested qualifier: $params.VERSION_QUALIFIER
                          with User = $branch_user, Ticket = $branch_ticket, Description = $branch_description
                          Qualified Version = $qualifiedVersion"""

                        // On development branches the connectors version shall be edited for deployment
                        // Maven documentation about maven_version:
                        // https://docs.oracle.com/middleware/1212/core/MAVEN/maven_version.htm
                        println "Edit version on dev branches, new version is ${qualifiedVersion}"
                        sh """\
                          #!/usr/bin/env bash
                          mvn versions:set --define newVersion=${qualifiedVersion}
                        """.stripIndent()
                    }

                }
                ///////////////////////////////////////////
                // Updating build displayName and description
                ///////////////////////////////////////////
                script {
                    String user_name = currentBuild.getBuildCauses('hudson.model.Cause$UserIdCause').userId[0]
                    if ( user_name == null) { user_name = "auto" }

                    String deploy_info = ''
                    if (stdBranch_buildOnly || devBranch_mavenDeploy){
                        deploy_info = deploy_info + '+DEPLOY'
                    }
                    if (devBranch_dockerPush){
                        deploy_info = deploy_info + '+PUSH'
                    }

                    currentBuild.displayName = (
                      "#$currentBuild.number-$params.Action" + deploy_info + ": $user_name"
                    )

                    // updating build description
                    String description = """
                      Version = $qualifiedVersion - $params.Action Build
                      Sonar forced: $params.FORCE_SONAR - Script: $hasPostLoginScript
                      Debug: $params.JENKINS_DEBUG
                      Extra build args: $extraBuildParams""".stripIndent()
                    job_description_append(description)
                }
                ///////////////////////////////////////////
                // Login tasks
                ///////////////////////////////////////////
                script {
                    withCredentials([gitCredentials]) {
                        sh """ bash .jenkins/scripts/git_login.sh "\${GITHUB_USER}" "\${GITHUB_PASS}" """
                    }
                    withCredentials([dockerCredentials]) {
                        sh """ bash .jenkins/scripts/docker_login.sh "${ARTIFACTORY_REGISTRY}" "\${DOCKER_USER}" "\${DOCKER_PASS}" """
                    }
                    withCredentials([keyImportCredentials]) {
                        sh """ bash .jenkins/scripts/setup_gpg.sh """
                    }
                }
            }
            post {
                always {
                    println "Artifact Poms files for analysis if needed"
                    archiveArtifacts artifacts: '**/*pom.*', allowEmptyArchive: false, onlyIfSuccessful: false
                }
            }
        }
        stage('Post login') {
            steps {
                withCredentials([gitCredentials,
                                 dockerCredentials,
                                 ossrhCredentials,
                                 jetbrainsCredentials,
                                 jiraCredentials,
                                 gpgCredentials]) {
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
        stage('Maven build') {
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
            post {
                always {
                    recordIssues(
                      enabledForFailure: true,
                      tools: [
                        junitParser(
                          id: 'unit-test',
                          name: 'Unit Test',
                          pattern: '**/target/surefire-reports/*.xml'
                        )
                      ]
                    )
                }
            }
        }
        stage('Maven deploy') {
            when {
                anyOf {
                    expression { stdBranch_buildOnly }
                    expression { devBranch_mavenDeploy }
                }
            }
            steps {
                script {
                    withCredentials([ossrhCredentials,
                                     gpgCredentials,
                                     nexusCredentials]) {
                        sh """\
                        #!/usr/bin/env bash
                        set -xe
                        bash mvn deploy $DEPLOY_OPTS \
                                        $extraBuildParams \
                                        --settings .jenkins/settings.xml \
                                        $extraArgs
                    """.stripIndent()
                    }
                }
                // Add description to job
                script {
                    def repo
                    if (devBranch_mavenDeploy) {
                        repo = ['artifacts-zl.talend.com',
                                'https://artifacts-zl.talend.com/nexus/content/repositories/snapshots/org/talend/sdk/component']
                    } else {
                        repo = ['oss.sonatype.org',
                                'https://oss.sonatype.org/content/repositories/snapshots/org/talend/sdk/component/']
                    }

                    job_description_append("Maven artefact deployed as ${qualifiedVersion} on [${repo[0]}](${repo[1]})")
                }
            }
        }
        stage('Docker build/push') {
            when {
                anyOf {
                  expression { stdBranch_buildOnly }
                  expression { devBranch_dockerPush }
                }
            }
            steps {
                script {
                    configFileProvider([configFile(fileId: 'maven-settings-nexus-zl', variable: 'MAVEN_SETTINGS')]) {

                        if (isStdBranch){
                            // Build and push all images
                            sh """
                              bash .jenkins/scripts/docker_build.sh \
                                ${qualifiedVersion}${buildTimestamp}
                            """
                            job_description_append("Docker images deployed: component-server, component-starter-server and remote-engine-customizer")

                        }
                        else{
                            // Build and push specific image
                            sh """
                              bash .jenkins/scripts/docker_build.sh \
                                ${qualifiedVersion}${buildTimestamp} \
                                'false' \
                                'component-server'
                            """
                            job_description_append("Only component-server docker images deployed:")
                        }
                        job_description_append("As ${qualifiedVersion}${buildTimestamp} on [artifactory.datapwn.com](https://artifactory.datapwn.com/tlnd-docker-dev/talend/common/tacokit)" as String)
                    }

                }
            }
        }
        stage('Generate Doc') {
            when {
                expression {
                  params.FORCE_DOC || (params.Action != 'RELEASE' && isMasterBranch)
                }
            }
            steps {
                withCredentials([ossrhCredentials,
                                 gitCredentials]) {
                    sh """\
                        #!/usr/bin/env bash 
                        set -xe                       
                        mvn verify pre-site --file documentation/pom.xml \
                                            --settings .jenkins/settings.xml \
                                            --activate-profiles gh-pages \
                                            --define gpg.skip=true \
                                            $SKIP_OPTS \
                                            $extraBuildParams 

                    """.stripIndent()
                }
            }
        }
        stage('OSS security analysis') {
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
                }
            }
        }
        stage('Deps report') {
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
                            mvn versions:dependency-updates-report versions:plugin-updates-report \
                                                                   versions:property-updates-report \
                                                                   -pl '!bom'
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
        stage('Sonar') {
            when {
                expression { params.Action != 'RELEASE' }
                branch 'master'
            }
            steps {
                withCredentials([sonarCredentials]) {
                    catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                        // TODO https://jira.talendforge.org/browse/TDI-48980 (CI: Reactivate Sonar cache)
                        sh """\
                            #!/usr/bin/env bash 
                            set -xe
                            _JAVA_OPTIONS='--add-opens=java.base/java.lang=ALL-UNNAMED'
                            mvn sonar:sonar \
                                --define sonar.host.url=https://sonar-eks.datapwn.com \
                                --define sonar.login='$SONAR_USER' \
                                --define sonar.password='$SONAR_PASS' \
                                --define sonar.branch.name=${env.BRANCH_NAME} \
                                --define sonar.analysisCache.enabled=false
                        """.stripIndent()
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
                script {
                    withCredentials([gitCredentials, dockerCredentials, ossrhCredentials, jetbrainsCredentials, jiraCredentials, gpgCredentials]) {
                        configFileProvider([configFile(fileId: 'maven-settings-nexus-zl', variable: 'MAVEN_SETTINGS')]) {
                            sh """\
                               #!/usr/bin/env bash
                               bash .jenkins/scripts/release.sh ${env.BRANCH_NAME} ${qualifiedVersion} 
                               """.stripIndent()
                        }
                    }
                }
            }
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
                    if ("SUCCESS" == currentBuild.previousBuild.result) {
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

            script {
                if (params.JENKINS_DEBUG) {
                    jenkinsBreakpoint()
                }
            }
        }
    }
}


/**
 * Append a new line to job description
 * This is MARKDOWN, do not forget double space at the end of line
 *
 * @param new line
 * @return void
 */
private void job_description_append(String new_line) {
    if (currentBuild.description == null) {
        println "Create the job description with: \n$new_line"
        currentBuild.description = new_line
    } else {
        println "Edit the job description adding: $new_line"
        currentBuild.description = currentBuild.description + '\n' + new_line
    }
}

/**
 * Assembly all needed items to put inside extraBuildParams
 *
 * @param Boolean skip_doc, if set to true documentation build will be skipped
 *
 * @return extraBuildParams as a string ready for mvn cmd
 */
private String assemblyExtraBuildParams(Boolean skip_doc, Boolean use_dev_profile) {
    String extraBuildParams

    println 'Processing extraBuildParams'
    final List<String> buildParamsAsArray = []

    println 'Manage user params'
    if ( params.EXTRA_BUILD_PARAMS ) {
        buildParamsAsArray.add(params.EXTRA_BUILD_PARAMS as String)
    }

    println 'Manage the skip_doc option'
    if (skip_doc) {
        buildParamsAsArray.add('--projects !documentation')
        buildParamsAsArray.add('--define documentation.skip=true')
    }

    println 'Manage profile option'
    if (use_dev_profile) {
        buildParamsAsArray.add('--activate-profiles dev_branch')
    }

    println 'Construct final params content'
    extraBuildParams = buildParamsAsArray.join(' ')
    println "extraBuildParams: $extraBuildParams"

    return extraBuildParams
}

/**
 * Implement a simple breakpoint to stop actual job
 * Use the method anywhere you need to stop
 * The first usage is to keep the pod alive on post stage.
 * Change and restore the job description to be more visible
 *
 * @param none
 * @return void
 */
private void jenkinsBreakpoint() {
    // Backup the description
    String job_description_backup = currentBuild.description
    // updating build description
    currentBuild.description = "ACTION NEEDED TO CONTINUE \n ${job_description_backup}"
    // Request user action
    input message: 'Finish the job?', ok: 'Yes'
    // updating build description
    currentBuild.description = "$job_description_backup"
}

/**
 * create a new version from actual one and given jira ticket or user qualifier
 * Priority to user qualifier
 *
 * The branch name has comply with the format: user/JIRA-1234-Description
 * It is MANDATORY for artifact management.
 *
 * @param String version actual version to edit
 * @param GString ticket
 * @param GString user_qualifier to be checked as priority qualifier
 *
 * @return String new_version with added qualifier
 */
private static String add_qualifier_to_version(String version, String ticket, String user_qualifier) {
    String new_version

    if (user_qualifier.contains("DEFAULT")) {
        if (version.contains("-SNAPSHOT")) {
            new_version = version.replace("-SNAPSHOT", "-$ticket-SNAPSHOT" as String)
        } else {
            new_version = "$version-$ticket".toString()
        }
    } else {
        new_version = version.replace("-SNAPSHOT", "-$user_qualifier-SNAPSHOT" as String)
    }
    return new_version
}

/**
 * extract given branch information
 *
 * The branch name has comply with the format: user/JIRA-1234-Description
 * It is MANDATORY for artifact management.
 *
 * @param branch_name row name of the branch
 *
 * @return A list containing the extracted: [user, ticket, description]
 * The method also raise an assert exception in case of wrong branch name
 */
private static ArrayList<String> extract_branch_info(GString branch_name) {

    String branchRegex = /^(?<user>.*)\/(?<ticket>[A-Z]{2,8}-\d{1,6})[_-](?<description>.*)/
    Matcher branchMatcher = branch_name =~ branchRegex

    try {
        assert branchMatcher.matches()
    }
    catch (AssertionError ignored) {
        return ["", "", ""]
    }

    String user = branchMatcher.group("user")
    String ticket = branchMatcher.group("ticket")
    String description = branchMatcher.group("description")

    return [user, ticket, description]
}