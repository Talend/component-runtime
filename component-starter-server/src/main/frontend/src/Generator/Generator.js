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

import theme from './Generator.scss';

import ProjectMetadata from './ProjectMetadata';

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
      facets: []
    };

    this.state = {
        currentStep: 0,
        project: project,
        configuration: {
         buildTypes: []
       },
       steps: [
       ]
    };
    this.state.steps.push({name: 'Start', component: <ProjectMetadata project={this.state.project} buildTypes={this.state.configuration.buildTypes} />});
    this.state.steps.push({name: 'Finish', component: <div>Hello</div>});
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
                    return (
                      <li className={classes.join(' ')} onClick={e => onClick(e, i)} key={i}>
                        <span className={theme.sectionLabel}>{this.state.steps[i].name}</span>
                      </li>
                    );
                  })
                }
              </ol>
            </nav>
          </div>
          <div className={theme.content}>
            {this.state.steps[this.state.currentStep].component}
          </div>
        </div>
      </div>
    );
  }
}
