# Web Api Test

## Description
This folder contains API test for TCOMP to be run with Talend API Tester Maven plug-in.  
You will find:
 - The "test" folder containing test projects and scenarios pushed by our API tester instance in eu.cloud.talend.com (tenant: rd.aws.eu.talend.com)
 - The ".jenkins" folder with jenkins files and scripts

## To run the tests
### Requirements
If you want to run the test with maven, you will need the followings:

- API Tester Maven Plugin
  - Setup it in your maven m2 or get it from an external artifact repository 
  - [Help page](https://help.talend.com/r/en-US/Cloud/api-tester-user-guide/installing-maven-plugin)
- Your Talend account ID.
  - Get it in your account information in Talend Management Console Subscription page.
  - Or from API Tester generated pom.xml file

## Execution
The execution with maven plugin is done in several steps as follows:
- Go into the tested connector folder (TODO: use the new demo component)
- Build the connector
- Start the tcomp server using the component-tool-webapp 
  - Use Dtalend.web.batch, Dtalend.web.batch.timeout, Dtalend.web.port options depending on your needs (default test environment is (https://localhost:8080))
- Go to the test location folder (/talend-component-maven-plugin/src/it/web/test)
- Run the tests from chosen file with your Talend instance and account ID

sample:
```bash
cd "/connector"
mvn clean install
mvn talend-component:web -Dtalend.web.batch=true -Dtalend.web.batch.timeout=30 -Dtalend.web.port=8081
cd "talend-component-maven-plugin/src/it/web/test"
mvn clean test --define instance='YOUR_TENANT_INSTANCE'\
               --define accountId='YOUR_ID'\
               --define selectedEnvironment='localhost'\
               --define file='tcomp-approved.json'
```

## To edit the tests
### Requirements
If you want to edit the test or simply play them in API Tester environment, you will need the followings:
- A Chromium based web brother
- The [API Tester plugin](https://chrome.google.com/webstore/detail/talend-api-tester-free-ed/aejoelaoggembcahagimdiliamlcdmfm) installed web brother

### How To
If you are part of Talend
- Request to be part of the connectors group in rd.aws.eu.talend.com Tenant

If you are not part of Talend
- Import the test project form (/talend-component-maven-plugin/src/it/web/test) in your API Tester DRIVE


### REMARK
If your server is running in a wsl container, but your API Tester is under windows, you have to replace localhost by your IP over the wsl network (ifconfig is your friend)