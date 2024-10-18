/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
final def ossrhCredentials = usernamePassword(
    credentialsId: 'ossrh-credentials',
    usernameVariable: 'OSSRH_USER',
    passwordVariable: 'OSSRH_PASS')
final def nexusCredentials = usernamePassword(
    credentialsId: 'nexus-artifact-zl-credentials',
    usernameVariable: 'NEXUS_USER', passwordVariable: 'NEXUS_PASS')
final def jetbrainsCredentials = usernamePassword(
    credentialsId: 'jetbrains-credentials',
    usernameVariable: 'JETBRAINS_USER',
    passwordVariable: 'JETBRAINS_PASS')
final def jiraCredentials = usernamePassword(
    credentialsId: 'jira-credentials',
    usernameVariable: 'JIRA_USER',
    passwordVariable: 'JIRA_PASS')
final def gitCredentials = usernamePassword(
    credentialsId: 'github-credentials',
    usernameVariable: 'GITHUB_USER',
    passwordVariable: 'GITHUB_PASS')
final def dockerCredentials = usernamePassword(
    credentialsId: 'artifactory-datapwn-credentials',
    usernameVariable: 'DOCKER_USER',
    passwordVariable: 'DOCKER_PASS')
final def sonarCredentials = usernamePassword(
    credentialsId: 'sonar-credentials',
    usernameVariable: 'SONAR_LOGIN',
    passwordVariable: 'SONAR_PASSWORD')
final def keyImportCredentials = usernamePassword(
    credentialsId: 'component-runtime-import-key-credentials',
    usernameVariable: 'KEY_USER',
    passwordVariable: 'KEY_PASS')
final def gpgCredentials = usernamePassword(
    credentialsId: 'component-runtime-gpg-credentials',
    usernameVariable: 'GPG_KEYNAME',
    passwordVariable: 'GPG_PASSPHRASE')

// In PR environment, the branch name is not valid and should be swap with pr name.
final String pull_request_id = env.CHANGE_ID
final String branch_name = pull_request_id != null ? env.CHANGE_BRANCH : env.BRANCH_NAME

// Job config
final Boolean isMasterBranch = branch_name == "master"
final Boolean isStdBranch = (branch_name == "master" || branch_name.startsWith("maintenance/"))
final Boolean hasPostLoginScript = params.POST_LOGIN_SCRIPT != ""
final String extraBuildParams = ""
final String buildTimestamp = String.format('-%tY%<tm%<td%<tH%<tM%<tS', LocalDateTime.now())
final String artifactoryAddr = "https://artifactory.datapwn.com"
final String artifactoryPath = "tlnd-docker-dev/talend/common/tacokit"

// Job variables declaration
String branch_user
String branch_ticket
String branch_description
String pomVersion
String finalVersion
Boolean needQualify
Boolean stdBranch_buildOnly = false
Boolean devBranch_mavenDeploy = false
Boolean devBranch_dockerPush = false


String skipOptions = "-Dspotless.apply.skip=true -Dcheckstyle.skip=true -Drat.skip=true -DskipTests -Dinvoker.skip=true"
String deployOptions = "$skipOptions -Possrh -Prelease -Pgpg2 -Denforcer.skip=true"


pipeline {
  libraries {
    lib("connectors-lib@main")  // https://github.com/Talend/tdi-jenkins-shared-libraries
  }
  agent {
    kubernetes {
      yamlFile '.jenkins/jenkins_pod.yml'
      defaultContainer 'main'
    }
  }

  environment {
    MAVEN_OPTS = "-Dformatter.skip=true -Dmaven.artifact.threads=256"
    BUILD_ARGS = "-Dgpg.skip=true -Denforcer.skip=true"
    ARTIFACTORY_REGISTRY = "artifactory.datapwn.com"
    VERACODE_APP_NAME = 'Talend Component Kit'
    VERACODE_SANDBOX = 'component-runtime'
    APP_ID = '579232'
  }

  options {
    buildDiscarder(logRotator(artifactNumToKeepStr: '10', numToKeepStr: branch_name == 'master' ? '15' : '10'))
    timeout(time: 180, unit: 'MINUTES')
    skipStagesAfterUnstable()
  }

  triggers {
    cron(branch_name == "master" ? "0 0 * * *" : "")
  }

  parameters {
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    separator(name: "BASIC_CONFIG",
              sectionHeader: "Basic configuration",
              sectionHeaderStyle: """ background-color: #ABEBC6;
                text-align: center; font-size: 35px !important; font-weight : bold;
			          """)
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    choice(
        name: 'Action',
        choices: ['STANDARD', 'RELEASE'],
        description: 'Kind of running:\nSTANDARD: (default) classical CI\nRELEASE: Build release')

    booleanParam(
        name: 'FORCE_DOC',
        defaultValue: false,
        description: 'Force documentation stage for development branches. No effect on master and maintenance.')

    string(name: 'JAVA_VERSION',
           defaultValue: 'adoptopenjdk-17.0.5+8',
           description: """Provided java version will be installed with asdf  
                        Examples: adoptopenjdk-11.0.22+7, adoptopenjdk-17.0.11+9  
                        """)

    string(name: 'MAVEN_VERSION',
           defaultValue: '3.8.8',
           description: """Provided maven version will be installed with asdf  
                        Examples: 3.8.8, 4.0.0-beta-4  
                        """)

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    separator(name: "DEPLOY_CONFIG",
              sectionHeader: "Deployment configuration",
              sectionHeaderStyle: """ background-color: #F9E79F;
                text-align: center; font-size: 35px !important; font-weight : bold;
			          """)
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
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
    choice(
        name: 'DOCKER_CHOICE',
        choices: ['component-server',
                  'component-starter-server',
                  'remote-engine-customizer',
                  'All'],
        description: 'Choose which docker image you want to build and push. Only available if DOCKER_PUSH == True.')

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    separator(name: "QUALIFIER_CONFIG",
              sectionHeader: "Qualifier configuration",
              sectionHeaderStyle: """ background-color: #AED6F1;
                text-align: center; font-size: 35px !important; font-weight : bold;
			          """)
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    string(
        name: 'VERSION_QUALIFIER',
        defaultValue: 'DEFAULT',
        description: '''
            Deploy jars with the given version qualifier. No effect on master and maintenance.
             - DEFAULT means the qualifier will be the Jira id extracted from the branch name.
            From "user/JIRA-12345_some_information" the qualifier will be JIRA-12345.''')

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    separator(name: "ADVANCED_CONFIG",
              sectionHeader: "Advanced configuration",
              sectionHeaderStyle: """ background-color: #F8C471;
                text-align: center; font-size: 35px !important; font-weight : bold;
			          """)
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    string(
        name: 'EXTRA_BUILD_PARAMS',
        defaultValue: '',
        description: 'Add some extra parameters to maven commands. Applies to all maven calls.')
    string(
        name: 'POST_LOGIN_SCRIPT',
        defaultValue: '',
        description: 'Execute a shell command after login. Useful for maintenance.')
    booleanParam(
        name: 'DISABLE_SONAR',
        defaultValue: false,
        description: 'Cancel the Sonar analysis stage execution')
    booleanParam(
        name: 'FORCE_SECURITY_ANALYSIS',
        defaultValue: false,
        description: 'Force OSS security analysis stage for branches.')
    booleanParam(
        name: 'FORCE_DEPS_REPORT',
        defaultValue: false,
        description: 'Force dependencies report stage for branches.')

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    separator(name: "EXPERT_CONFIG",
              sectionHeader: "Expert configuration",
              sectionHeaderStyle: """ background-color: #A9A9A9;
                text-align: center; font-size: 35px !important; font-weight : bold;
			          """)
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    separator(name: "DEBUG_CONFIG",
              sectionHeader: "Jenkins job debug configuration ",
              sectionHeaderStyle: """ background-color: #FF0000;
                text-align: center; font-size: 35px !important; font-weight : bold;
			          """)
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    booleanParam(
        name: 'JENKINS_DEBUG',
        defaultValue: false,
        description: 'Add an extra step to the pipeline allowing to keep the pod alive for debug purposes.')
  }

  stages {
    stage('Preliminary steps') {
      steps {

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

        ///////////////////////////////////////////
        // edit java version
        ///////////////////////////////////////////
        script {
          echo "edit asdf tool version with version from jenkins param"

          asdfTools.edit_version_in_file("$env.WORKSPACE/.tool-versions", 'java', params.JAVA_VERSION)
          jenkinsJobTools.job_description_append("Use java version:  $params.JAVA_VERSION  ")
          asdfTools.edit_version_in_file("$env.WORKSPACE/.tool-versions", 'maven', params.MAVEN_VERSION)
          jenkinsJobTools.job_description_append("Use maven version:  $params.MAVEN_VERSION  ")

        }

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

          needQualify = devBranch_mavenDeploy || devBranch_dockerPush

          if (needQualify) {
            // Qualified version have to be released on talend_repository
            // Overwrite the deployOptions
            deployOptions = "$skipOptions --activate-profiles private_repository -Denforcer.skip=true"
          }

          // By default the doc is skipped for standards branches
          Boolean skip_documentation = !(params.FORCE_DOC || isStdBranch)
          extraBuildParams = assemblyExtraBuildParams(skip_documentation)

        }
        ///////////////////////////////////////////
        // Pom version and Qualifier management
        ///////////////////////////////////////////
        script {
          final def pom = readMavenPom file: 'pom.xml'
          pomVersion = pom.version

          echo 'Manage the version qualifier'
          if (!needQualify) {
            println """
              No need to add qualifier in followings cases:
              - We are on Master or Maintenance branch
              - We do not want to deploy on dev branch
              """.stripIndent()
            finalVersion = pomVersion
          }
          else {
            branch_user = ""
            branch_ticket = ""
            branch_description = ""
            if (params.VERSION_QUALIFIER != ("DEFAULT")) {
              // If the qualifier is given, use it
              println """No need to add qualifier, use the given one: "$params.VERSION_QUALIFIER" """
            }
            else {
              println "Validate the branch name"

              (branch_user,
              branch_ticket,
              branch_description) = extract_branch_info(branch_name)

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
            finalVersion = add_qualifier_to_version(
                pomVersion,
                branch_ticket,
                "$params.VERSION_QUALIFIER" as String)

            echo """
                          Configure the version qualifier for the curent branche: $branch_name  
                          requested qualifier: $params.VERSION_QUALIFIER  
                          with User = $branch_user, Ticket = $branch_ticket, Description = $branch_description  
                          Qualified Version = $finalVersion  """

            // On development branches the connectors version shall be edited for deployment
            // Maven documentation about maven_version:
            // https://docs.oracle.com/middleware/1212/core/MAVEN/maven_version.htm
            println "Edit version on dev branches, new version is ${finalVersion}"
            sh """\
                        #!/usr/bin/env bash
                        mvn versions:set --define newVersion=${finalVersion}
                        mvn versions:set --file bom/pom.xml --define newVersion=${finalVersion}
                        """.stripIndent()
          }

        }
        ///////////////////////////////////////////
        // Updating build displayName and description
        ///////////////////////////////////////////
        script {
          String user_name = currentBuild.getBuildCauses('hudson.model.Cause$UserIdCause').userId[0]
          if (user_name == null) {
            user_name = "auto"
          }

          String deploy_info = ''
          if (stdBranch_buildOnly || devBranch_mavenDeploy) {
            deploy_info = deploy_info + '+DEPLOY'
          }
          if (devBranch_dockerPush) {
            deploy_info = deploy_info + '+DOCKER'
          }

          jenkinsJobTools.job_name_creation("$params.Action" + deploy_info)

          // updating build description
          String description = """
                      Version = $finalVersion - $params.Action Build  
                      Disable Sonar: $params.DISABLE_SONAR - Script: $hasPostLoginScript  
                      Debug: $params.JENKINS_DEBUG  
                      Extra build args: $extraBuildParams  """.stripIndent()
          jenkinsJobTools.job_description_append(description)
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
    stage('Maven validate to install') {
      when { expression { params.Action != 'RELEASE' } }
      steps {
        withCredentials([ossrhCredentials,
                         nexusCredentials]) {
          sh """\
                    #!/usr/bin/env bash
                    set -xe
                    mvn clean install --file bom/pom.xml
                    mvn clean install $BUILD_ARGS \
                                      $extraBuildParams \
                                      --settings .jenkins/settings.xml
                    """.stripIndent()
        }
      }
      post {
        always {
          recordIssues(
              enabledForFailure: false,
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
                        bash mvn deploy $deployOptions \
                                        $extraBuildParams \
                                        --settings .jenkins/settings.xml
                        """.stripIndent()
          }
        }
        // Add description to job
        script {
          def repo
          if (devBranch_mavenDeploy) {
            repo = ['artifacts-zl.talend.com',
                    'https://artifacts-zl.talend.com/nexus/content/repositories/snapshots/org/talend/sdk/component']
          }
          else {
            repo = ['oss.sonatype.org',
                    'https://oss.sonatype.org/content/repositories/snapshots/org/talend/sdk/component/']
          }

          jenkinsJobTools.job_description_append("Maven artefact deployed as ${finalVersion} on [${repo[0]}](${repo[1]})  ")
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

            String images_options = ''
            if (isStdBranch) {
              // Build and push all images
              jenkinsJobTools.job_description_append("Docker images deployed: component-server, component-starter-server and remote-engine-customizer  ")
            }
            else {
              String image_list
              if (params.DOCKER_CHOICE == 'All') {
                images_options = 'false'
              }
              else {
                images_options = 'false ' + params.DOCKER_CHOICE
              }

              if (params.DOCKER_CHOICE == 'All') {
                jenkinsJobTools.job_description_append("All docker images deployed  ")

                jenkinsJobTools.job_description_append("As ${finalVersion}${buildTimestamp} on " +
                                                       "[artifactory.datapwn.com]" +
                                                       "($artifactoryAddr/$artifactoryPath)  ")
                jenkinsJobTools.job_description_append("docker pull $artifactoryAddr/$artifactoryPath" +
                                                       "/component-server:${finalVersion}${buildTimestamp}  ")
                jenkinsJobTools.job_description_append("docker pull $artifactoryAddr/$artifactoryPath" +
                                                       "/component-starter-server:${finalVersion}${buildTimestamp}  ")
                jenkinsJobTools.job_description_append("docker pull $artifactoryAddr/$artifactoryPath" +
                                                       "/remote-engine-customize:${finalVersion}${buildTimestamp}  ")

              }
              else {
                jenkinsJobTools.job_description_append("Docker images deployed: $params.DOCKER_CHOICE  ")
                jenkinsJobTools.job_description_append("As ${finalVersion}${buildTimestamp} on " +
                                                       "[artifactory.datapwn.com]($artifactoryAddr/$artifactoryPath)  ")
                jenkinsJobTools.job_description_append("docker pull $artifactoryAddr/$artifactoryPath/$params.DOCKER_CHOICE:" +
                                                       "${finalVersion}${buildTimestamp}  ")

              }

            }

            // Build and push specific image
            sh """
                              bash .jenkins/scripts/docker_build.sh \
                                ${finalVersion}${buildTimestamp} \
                                ${images_options}
                            """
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
                    mvn verify pre-site --file documentation/pom.xml \
                                        --settings .jenkins/settings.xml \
                                        --activate-profiles gh-pages \
                                        --define gpg.skip=true \
                                        $skipOptions \
                                        $extraBuildParams 
                    """.stripIndent()
        }
      }
    }
    stage('OSS security analysis') {
      when {
        anyOf {
          expression { params.Action != 'RELEASE' && branch_name == 'master' }
          expression { params.FORCE_SECURITY_ANALYSIS == true }
        }
      }
      steps {
        withCredentials([ossrhCredentials]) {
          catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
            sh """
                            bash .jenkins/scripts/mvn-ossindex-audit.sh
                        """
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
        anyOf {
          expression { params.Action != 'RELEASE' && branch_name == 'master' }
          expression { params.FORCE_DEPS_REPORT == true }
        }
      }
      steps {
        withCredentials([ossrhCredentials]) {
          catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
            sh """
                           bash .jenkins/scripts/mvn_dependency_updates_report.sh
                        """
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
        expression { (params.Action != 'RELEASE') && !params.DISABLE_SONAR }
      }
      steps {
        script {
          withCredentials([nexusCredentials,
                           sonarCredentials,
                           gitCredentials]) {

            if (pull_request_id != null) {

              println 'Run analysis for PR'
              sh """
                            bash .jenkins/scripts/mvn_sonar_pr.sh \
                                '${branch_name}' \
                                '${env.CHANGE_TARGET}' \
                                '${pull_request_id}' \
                                ${extraBuildParams}
                            """
            }
            else {
              echo 'Run analysis for branch'
              sh """
                            bash .jenkins/scripts/mvn_sonar_branch.sh \
                                '${branch_name}' \
                                ${extraBuildParams}
                            """
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
        script {
          withCredentials([gitCredentials, dockerCredentials, ossrhCredentials, jetbrainsCredentials, jiraCredentials, gpgCredentials, nexusCredentials]) {
            configFileProvider([configFile(fileId: 'maven-settings-nexus-zl', variable: 'MAVEN_SETTINGS')]) {
              sh """
                            bash .jenkins/scripts/release_legacy.sh $branch_name $finalVersion $extraBuildParams
                            """
            }
          }
        }
      }
    }
  }
  post {
    success {
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
    always {
      script {
        alertingTools.slack_result(
            env.SLACK_CI_CHANNEL,
            currentBuild.result,
            currentBuild.previousBuild.result,
            true, // Post for success
            true, // Post for failure
            "Failure of $pomVersion $params.ACTION release.")
      }
      recordIssues(
          enabledForFailure: false,
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
          jenkinsJobTools.jenkinsBreakpoint()
        }
      }
    }
  }
}

/**
 * Assembly all needed items to put inside extraBuildParams
 *
 * @param Boolean skip_doc, if set to true documentation build will be skipped
 *
 * @return extraBuildParams as a string ready for mvn cmd
 */
private String assemblyExtraBuildParams(Boolean skip_doc) {
  String extraBuildParams

  println 'Processing extraBuildParams'
  final List<String> buildParamsAsArray = []

  println 'Manage user params'
  if (params.EXTRA_BUILD_PARAMS) {
    buildParamsAsArray.add(params.EXTRA_BUILD_PARAMS as String)
  }

  println 'Manage the skip_doc option'
  if (skip_doc) {
    buildParamsAsArray.add('--projects !documentation')
    buildParamsAsArray.add('--define documentation.skip=true')
  }

  println 'Construct final params content'
  extraBuildParams = buildParamsAsArray.join(' ')
  println "extraBuildParams: $extraBuildParams"

  return extraBuildParams
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
    }
    else {
      new_version = "$version-$ticket".toString()
    }
  }
  else {
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
private static ArrayList<String> extract_branch_info(String branch_name) {

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
