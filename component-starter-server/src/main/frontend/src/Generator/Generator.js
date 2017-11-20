/**
 *  Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
import React from 'react';
import { Action, Icon } from '@talend/react-components';

import theme from './Generator.scss';

import ProjectMetadata from './ProjectMetadata';
import Component from './Component';
import Finish from './Finish';

export default class Generator extends React.Component {
  constructor(props) {
    super(props);

    let project = {
      buildType: 'Maven',
      version: '0.0.1-SNAPSHOT',
      group: 'com.company',
      artifact: 'company-component',
      name: 'A Component',
      description: 'A generated component project',
      packageBase: 'com.company.talend.components',
      family: 'CompanyFamily',
      category: 'Misc',
      facets: []
    };

    this.state = {
        currentStep: 0,
        project: project,
        configuration: {
         buildTypes: []
       },
       components: [],
       steps: [
       ]
    };
    this.state.steps.push({name: 'Start', component: <ProjectMetadata project={this.state.project} buildTypes={this.state.configuration.buildTypes} />});
    this.state.steps.push({name: 'Finish', component: <Finish project={this.state.project} components={this.state.components} />});

    ['onAddComponent', 'onGoToFinishPage'].forEach(action => this[action] = this[action].bind(this));
  }

  onAddComponent() {
    this.setState(state => {
      let component = {
        configuration: {
          name: `New Component #${state.steps.length}`
        },
        source: {
          genericOutput: true,
          stream: false,
          configurationStructure: {
            entries: []
          },
          outputStructure: {
            entries: []
          }
        },
        processor: {
          configurationStructure: {
            entries: []
          },
          inputStructures: [
            {
              name: 'MAIN',
              generic: false,
              structure: {
                entries: []
              }
            }
          ],
          outputStructures: []
        }
      };
      state.components.push(component);
      state.steps.splice(state.steps.length - 1, 0, ({component: <Component component={component} onChange={() => this.updateComponent(component)} />}));
      state.currentStep = state.steps.length - 2;
    });
  }

  updateComponent(component) {
    this.setState({components: [].concat(this.state.components)});
  }

  deleteComponent(index) {
    this.setState(state => state.steps.splice(index, 1));
  }

  onGoToFinishPage() {
    this.setState({currentStep: this.state.steps.length - 1});
  }

  // <Project project={this.state.project} />
  render() {
    const onClick = (evt, index) => {
      evt.preventDefault();
      this.setState({currentStep: index});
    };
    return (
      <div className={theme.Generator}>
        <div className={theme.container}>
          <div className={theme.wizard}>
            <nav>
              <ol>
                {
                  this.state.steps.map((step, i) => {
                    const classes = [];
                    if (this.state.currentStep === i) {
                      classes.push(theme.active);
                    }
                    const fixedItem = i === 0 || (i + 1) === this.state.steps.length;
                    return (
                      <li className={classes.join(' ')} onClick={e => onClick(e, i)} key={i}>
                        <sectionLabel>
                        {
                          (fixedItem && step.name) ||
                          (!fixedItem && step.component.props.component.configuration.name)
                        }
                        </sectionLabel>
                        {
                          !fixedItem && (
                            <trashIcon onClick={() => this.deleteComponent(i)}>
                              <Icon name="talend-trash" />
                            </trashIcon>
                          )
                        }
                      </li>
                    );
                  })
                }
              </ol>
            </nav>
          </div>
          <div className={theme.content}>
            <main>
              {this.state.steps[this.state.currentStep].component}
            </main>
            {
              (this.state.currentStep + 1) !== this.state.steps.length && <footer>
                <Action id="add-component-button" label="Add A Component" bsStyle="info" onClick={() => this.onAddComponent()} />
                <Action id="go-to-finish-button" label="Go to Finish" bsStyle="primary" onClick={() => this.onGoToFinishPage()} />
              </footer>
            }
          </div>
        </div>
      </div>
    );
  }
}
