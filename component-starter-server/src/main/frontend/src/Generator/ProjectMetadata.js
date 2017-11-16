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
import { Actions, Icon, IconsProvider } from '@talend/react-components';
import { CONFIGURATION_URL } from '../constants';
import FacetSelector from './FacetSelector';
import CategorySelector from './CategorySelector';
import Input from '../Component/Input';

import theme from './ProjectMetadata.scss';

export default class ProjectMetadata extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      project: props.project,
      buildToolActions: [],
      facets: {},
      view: {
        light: true
      }
    };
  }

  componentWillMount() {
    fetch(`${CONFIGURATION_URL}`)
      .then(resp => resp.json())
      .then(payload => {
          this.setState(current => {
              current.configuration = payload;

              payload.buildTypes.forEach(item => current.buildToolActions.push(
                {label: item, onClick: () => this.setState(state => {
                    state.project.buildType = item;
                    state.buildToolActions.forEach(entry => {
                      if (entry.label === item) {
                        entry.className = theme.selected;
                      } else {
                        delete entry.className;
                      }
                  })
                })
              }));

              // select one build tool, preferrably maven since it is the standard
              const maven = current.buildToolActions.filter(i => i.label === 'Maven');
              if (maven.length > 0) {
                maven[0].className = theme.selected;

                const mvnIdx = current.buildToolActions.indexOf(maven[0]);
                const saved = current.buildToolActions[0];
                current.buildToolActions[0] = maven[0];
                current.buildToolActions[mvnIdx] = saved;
              } else {
                current.buildToolActions[0].className = theme.selected;
                current.project.buildType = current.buildToolActions[0].label;
              }
          });
      });
  }

  onCategoryUpdate(value) {
    this.setState(current => current.project.category = value.value )
  }

  showAll(event) {
      this.setState(current => current.view.light = false);
      event.preventDefault();
  }

  showLight(event) {
      this.setState(current => current.view.light = true);
      event.preventDefault();
  }

  render() {
    if(! this.state.configuration) {
      return (<div>Loading ...</div>);
    }

    return (
      <div className={theme.ProjectMetadata}>
        <IconsProvider />

        <div className={theme.main}>
          <div className={theme['form-row']}>
            <p className={theme.title}>Create a Talend Component Kit Project</p>
            <div>
              <Actions actions={this.state.buildToolActions} />
            </div>
          </div>

          <div className={theme['form-row']}>
            {
              (!!this.state.configuration && <FacetSelector facets={this.state.configuration.facets} selected={this.state.project.facets} />)
            }
          </div>

          <div className={theme['form-row']}>
            <p className={theme.title}>Component Metadata</p>
            <form novalidate submit={e => e.preventDefault()}>
              <div className="field">
                <label forHtml="projectFamily">Component Family</label>
                <Input className="form-control" id="projectFamily" type="text" placeholder="Enter the component family..."
                       required="required" aggregate={this.state.project} accessor="family"/>
              </div>
              <div className="field">
                <label forHtml="projectCategory">Category</label>
                <CategorySelector initialValue={this.state.project.category} onChange={(value) => this.onCategoryUpdate(value)} />
              </div>
            </form>
          </div>

          <p className={[theme.title, theme['form-row']].join(' ')}>Project Metadata</p>
          <form novalidate submit={e => e.preventDefault()}>
            <div className="field">
              <label forHtml="projectGroup">Group</label>
              <Input className="form-control" id="projectGroup" type="text" placeholder="Enter the project group..."
                     required="required" aggregate={this.state.project} accessor="group"/>
            </div>
            <div className="field">
              <label forHtml="projectArtifact">Artifact</label>
              <Input className="form-control" id="projectArtifact" type="text" placeholder="Enter the project artifact..."
                     required="required" aggregate={this.state.project} accessor="artifact"/>
            </div>
            <div className="field">
              <label forHtml="projectPackage">Package</label>
              <Input className="form-control" id="projectPackage" type="text" placeholder="Enter the project base package..."
                     required="required" aggregate={this.state.project} accessor="packageBase"/>
            </div>

            {
              this.state.view.light && (
                <div className="field" onClick={e => this.showAll(e)}>
                  <Icon name="talend-plus-circle" />
                  <span>See more options</span>
                </div>
              )
            }

            {
              !this.state.view.light &&
                [
                  <div className="field">
                    <label forHtml="projectVersion">Version</label>
                    <Input className="form-control" id="projectVersion" type="text" placeholder="Enter the project group..."
                           aggregate={this.state.project} accessor="version"/>
                  </div>,
                  <div className="field">
                    <label forHtml="projectName">Name</label>
                    <Input className="form-control" id="projectName" type="text" placeholder="Enter the project name..."
                           aggregate={this.state.project} accessor="name"/>
                  </div>,
                  <div className="field">
                    <label forHtml="projectDescription">Description</label>
                    <Input className="form-control" id="projectDescription" type="text" placeholder="Enter the project description..."
                           aggregate={this.state.project} accessor="description"/>
                  </div>,
                  <div className="field" onClick={e => this.showLight(e)}>
                    <Icon name="talend-zoomout" />
                    <span>See less options</span>
                  </div>
                ]
            }
          </form>
        </div>
      </div>
    );
  }
}
