# Web Api Test

## Description
TO BE WRITEN

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
- Go into the mock connector folder [/talend-component-maven-plugin/src/it/web/mock/connector-mock](/talend-component-maven-plugin/src/it/web/mock/connector-mock)
- Build the connector
- Start the web tester
  - Use Dtalend.web.batch, Dtalend.web.batch.timeout, Dtalend.web.port options depending on your needs (default test environment is [https://localhost:8081](https://localhost:8081))
- Go to the test folder [/talend-component-maven-plugin/src/it/web/test](/talend-component-maven-plugin/src/it/web/test)
- Run the tests with your Talend instance and account ID


```bash
cd "/talend-component-maven-plugin/src/it/web/mock/connector-mock"
mvn clean install
mvn talend-component:web -Dtalend.web.batch=true -Dtalend.web.batch.timeout=30 -Dtalend.web.port=8081
cd "/talend-component-maven-plugin/src/it/web/mock/connector-mock"
mvn clean test --define selectedEnvironment='localhost' --define accountId='YOUR_ID_ON_TENANT' 
mvn clean test --define instance='YOUR_TENANT_INSTANCE'\
               --define accountId='YOUR_ID_ON_TENANT'\
               --define selectedEnvironment='localhost'\
               --define file='component_runtime_prod.json'
```

## To edit the tests
### Requirements
If you want to edit the test or simply play them in API Tester environment, you will need the followings:
- A Chromium based web brother
- The [API Tester plugin](https://chrome.google.com/webstore/detail/talend-api-tester-free-ed/aejoelaoggembcahagimdiliamlcdmfm) installed web brother

## How To
If you are part of Talend
- Request to be part of the XXX group in XXX Tenant

If you are not part of Talend
- Import the test project form [/talend-component-maven-plugin/src/it/web/test](/talend-component-maven-plugin/src/it/web/test) in your API Tester DRIVE



## REMARK
If your server is running in a wsl container, but your API Tester is under windows, you wou have to replace localhost by your IP over the wsl network (ifconfig)