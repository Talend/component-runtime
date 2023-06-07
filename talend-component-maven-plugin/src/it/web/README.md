# Web Api Test

## Description
This folder contains API test for TCOMP to be run with Talend API Tester Maven plug-in.  
You will find:
 - The "test" folder containing test projects and scenarios pushed by our API tester instance in eu.cloud.talend.com (tenant: rd.aws.eu.talend.com)
 - The ".jenkins" folder with jenkins files and scripts

## To run the tests
### Requirements
If you want to run the test with maven, you will need the followings:

- API Tester Maven Plugin.
  - Setup it in your maven m2 or get it from an external artifact repository .
  - [Help page](https://help.talend.com/r/en-US/Cloud/api-tester-user-guide/installing-maven-plugin)
- Your Talend account ID.
  - Get it in your account information in Talend Management Console Subscription page.
  - Or from API Tester generated pom.xml file.
- A running server with correct connectors loaded inside as explained [here](#Start-local-server).

### Start local server
The execution with maven plugin is done in several steps as follows:
- Build with Maven a first time the whole project
- Do not forget to build the bom
- Build demo connector with Maven.
  - No need to rebuild component-runtime if you work only on the connector.
- Start the component server using the provided script `\talend-component-maven-plugin\src\it\web\.jenkins\scripts\server-registry-manual_start.sh`
  - Default test environment is (https://localhost:8081).
- Run the tests from with Maven  with your Talend instance and account ID and test name.
  - Your Account ID here is in Talend Management Console Subscription page
  - Tests are in `/talend-component-maven-plugin/src/it/web/test`.
  - We provide a pre-filled `apitester-manual-run.sh` script to start the test campaign
  - The script will load needed components for tests as configured in: `server-registry-init.sh`

Example:
```bash
# From component-runtime root folder

# Quick component runtime build for api test purpose.
bash talend-component-maven-plugin/src/it/web/.jenkins/scripts/tcomp-build-fast.sh pom.xml
mvn instal --file "bom/pom.xml"

# Sample connector build, if you edit is, do it every time.
mvn install --file "sample-parent/sample-connector/pom.xml"

# Component server manual start (defining server and connectors version
bash talend-component-maven-plugin/src/it/web/.jenkins/scripts/server-registry-manual_start.sh \
            "1.58.0-SNAPSHOT" \
            "1.41.0"

# You can now test with api tester on your local machine
# Or execute tests with maven (You need your ACCOUNT_ID)
bash talend-component-maven-plugin/src/it/web/.jenkins/scripts/apitester-manual-run.sh ACCOUNT_ID
# Or only one test (You need your ACCOUNT_ID and test name)
bash talend-component-maven-plugin/src/it/web/.jenkins/scripts/apitester-manual-single-run.sh ACCOUNT_ID tck-bulk-api-test
```

### Debugging server during test
In `server-registry-manual_start.sh` you can uncomment `# _JAVA_DEBUG_PORT="5005"` to enable java remote debugging on port 5005

## To edit the tests
### Requirements
If you want to edit the test or simply play them in API Tester environment, you will need the followings:
- A Chromium based web browser
- The [API Tester plugin](https://chrome.google.com/webstore/detail/talend-api-tester-free-ed/aejoelaoggembcahagimdiliamlcdmfm) installed web browser

### How To
- Import the test project that you want to work on, in API Tester
  - from: `/talend-component-maven-plugin/src/it/web/test`.
- It will be loaded on your personnal API Tester DRIVE.
- You can then edit and test your modifications directly on API Tester while running [local server](#Start-local-server)
- When the edition is finish, you can push it back to GitHub branch and make a Pull Request.
  - You can use built-in API Tester GitHub integration on doing it manually by exporting the project file.

### REMARK
1. If your server is running in a wsl container, but your API Tester is under windows, depending on your configuration, you may have to replace localhost by your IP over the wsl network (ifconfig is your friend).
2. All mvn builds are quicker over linux than windows, Windows user should better use wsl.
