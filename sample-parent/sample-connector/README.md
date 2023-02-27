# Sample Connector

## Description
This module contains a sample connector developed as a QA tool.
It is also a good code sample for tck beginners.

## Goal
This sample connector has been firstly built to cover endpoint tests with API Tester.
You can see actual tests in [API test project](../../talend-component-maven-plugin/src/it/web/README.md)
It should have enough content to be able to validate all tck api endpoints with all possible parameters.
The connectors should have everything translated in at least 2 languages, no need for real translation

# Translation
In order to have an easy testing of language mechanic, a simple python script has been provided in the project: [i18n_messages_properties_generator.py](src/main/resources/org/talend/sdk/component/test/connectors/i18n_messages_properties_generator.py).
Simply execute it to generate a copy of every `messages.properties` file in multiples languages.
The translation will add "-xx" at the end to easily test translations.
