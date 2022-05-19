/**
 *  Copyright (C) 2006-2022 Talend Inc. - www.talend.com
 *
 *  Licensed under the Apache License, Version 2.0 (the 'License');
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an 'AS IS' BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
import React from 'react';
import { Route, Switch } from 'react-router-dom';
import AceEditor from 'react-ace';
import 'brace/mode/javascript';
import 'brace/theme/chrome';
import 'brace/ext/language_tools';
import { withTranslation } from 'react-i18next';
import { GENERATOR_APITESTER_ZIP_URL } from '../../constants';
import Help from '../Help';
import ProjectContext from '../../ProjectContext';
import ProjectMetadata from '../ProjectMetadata';
import Finish from '../Finish';
import StepStep from './StepByStep';
import theme from './TalendAPITester.scss';
import generatorTheme from '../Generator/Generator.scss';

const defaultApiTesterJson = JSON.stringify({
  "version": 6,
  "entities": [
    {
      "entity": {
        "type": "Project",
        "id": "37b206f6-0e61-4309-8a79-f3c0826eafe3",
        "name": "DEMO"
      },
      "children": [
        {
          "entity": {
            "type": "Scenario",
            "id": "0ba61444-7d6e-40b9-9eab-7df49e050288",
            "name": "DEMO2"
          },
          "children": [
            {
              "entity": {
                "type": "Request",
                "method": {
                  "link": "http://tools.ietf.org/html/rfc7231#section-4.3.1",
                  "name": "GET"
                },
                "body": {
                  "formBody": {
                    "overrideContentType": true,
                    "encoding": "application/x-www-form-urlencoded",
                    "items": []
                  },
                  "bodyType": "Text"
                },
                "uri": {
                  "query": {
                    "delimiter": "&",
                    "items": [
                      {
                        "enabled": true,
                        "name": "abc",
                        "value": "${\"name_input\"}"
                      },
                      {
                        "enabled": true,
                        "name": "log",
                        "value": "${\"log_level\"}"
                      }
                    ]
                  },
                  "scheme": {
                    "secure": true,
                    "name": "https",
                    "version": "V11"
                  },
                  "host": "httpbin.org",
                  "path": "/get"
                },
                "id": "387ca33e-76e0-4811-bdd5-07ba6d85902d",
                "name": "HTTPBIN1",
                "headers": []
              }
            },
            {
              "entity": {
                "type": "Request",
                "method": {
                  "requestBody": true,
                  "link": "http://tools.ietf.org/html/rfc7231#section-4.3.4",
                  "name": "PUT"
                },
                "body": {
                  "formBody": {
                    "overrideContentType": true,
                    "encoding": "application/x-www-form-urlencoded",
                    "items": []
                  },
                  "bodyType": "Text",
                  "textBody": "{\n  \"name\": \"${getEntityById(\"387ca33e-76e0-4811-bdd5-07ba6d85902d\").\"response\".\"body\".\"args\".\"abc\"}\"\n}"
                },
                "uri": {
                  "query": {
                    "delimiter": "&",
                    "items": []
                  },
                  "scheme": {
                    "secure": true,
                    "name": "https",
                    "version": "V11"
                  },
                  "host": "httpbin.org",
                  "path": "/put"
                },
                "id": "05fbd2e7-1df6-4d26-a7ac-06ed63ce0e12",
                "name": "HTTPBIN2",
                "headers": [
                  {
                    "enabled": true,
                    "name": "Content-Type",
                    "value": "application/json"
                  }
                ]
              }
            }
          ]
        }
      ]
    }
  ],
  "environments": [
    {
      "id": "b70b6814-bded-45e9-a3e0-dd8ab64fcc72",
      "name": "DEMO2",
      "variables": {
        "11b2708d-a811-4d68-8570-30bdee5d0d53": {
          "name": "name_input",
          "value": "my name",
          "enabled": true,
          "createdAt": "2022-05-16T19:56:57.071Z",
          "private": false
        },
        "4a377094-4c9e-47d9-a89f-3da7770bffc5": {
          "name": "log_level",
          "value": "INFO",
          "enabled": true,
          "createdAt": "2022-05-16T19:57:18.306Z",
          "private": false
        }
      }
    }
  ]
}, ' ', 4);

function LeftPane(props) {
    return (
        <form className={theme.formPadding} noValidate onSubmit={() => false}>
            <h2>
                {props.t('apitester_list_title', { defaultValue: 'Talend API Tester scenario:' })}
                <Help
                    title="Talend API Tester Design"
                    i18nKey="apitester_overview"
                    content={
                        <div>
                            <p>The right editor enables you to edit the Talend API Tester document.</p>
                            <p></p>
                        </div>
                    }
                />
            </h2>
        </form>
    );
}

const LeftPaneTrans = withTranslation('Help')(LeftPane);

class TalendAPITester extends React.Component {
    constructor(props) {
        super(props);
        this.onSpecChange = this.onSpecChange.bind(this);
        this.state = { endpoints: [] }; // mainly to manage the refresh only
    }

    onSpecChange(spec, project) {
        project.mode = 'apitester';
        project.$$apitester = {
            ...project.$$apitester,
        };
    }

    render() {
        return (
            <ProjectContext.Consumer>
				{ project => {
				    project.mode = 'apitester';
                    if (!project.$$apitester) {
                        project.$$apitester = {
                            apitester: defaultApiTesterJson,
                        };
                    }

                    return (
                        <div className={`${theme.TalendAPITester} row`}>
                            <div className={theme.selector}>
                                <LeftPaneTrans
                                    spec={project.$$apitester.apitester}
                                    project={project}
                                />
                            </div>
                            <div className={theme.editor}>
                                <AceEditor
                                    mode='javascript'
                                    theme='chrome'
                                    name='apitester-ace-editor'
                                    value={project.$$apitester.apitester}
                                    width='auto'
                                    height='100%'
                                    setOptions={{
                                        enableBasicAutocompletion: true,
                                        enableLiveAutocompletion: true,
                                    }}
                                    onChange={value => this.onSpecChange(value, project)}
                                />
                            </div>
                        </div>);
                    }
                }
            </ProjectContext.Consumer>
        );
    }
}

const TalendAPITesterTrans = withTranslation('Help')(TalendAPITester);

export default class TalendAPITesterWizard extends React.Component {
    render() {
        return (
            <div className={generatorTheme.Generator}>
                <div className={generatorTheme.container}>
                    <div className={generatorTheme.wizard}>
                        <StepStep />
                    </div>
                    <div className={generatorTheme.content}>
                        <main>
                            <Switch>
                                <Route exact path="/apitester/project" component={(props) => <ProjectMetadata hideFacets={true} hideCategory={true} />} />
                                <Route exact path="/apitester/design"  component={TalendAPITesterTrans} />
                                <Route exact path="/apitester/export"  render={(props) => <Finish {...props} apitester={true} actionUrl={GENERATOR_APITESTER_ZIP_URL} />} />
                            </Switch>
                        </main>
                    </div>
                </div>
            </div>);
    }
}
