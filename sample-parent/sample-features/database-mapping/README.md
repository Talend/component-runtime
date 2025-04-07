This is a TCK connector to validate the integration of Database Mapping property in `@Component` annotation.

# How to build & deploy in a studio
From the root of `component-runtime` workspace, run:
```shell
mvn clean install -pl sample-parent/sample-features/database-mapping/ -am
```
Then deploy it into a studio:
```shell
java -jar sample-parent/sample-features/database-mapping/target/database-mapping-<version>.car studio-deploy --location <path to a studio> -f
```