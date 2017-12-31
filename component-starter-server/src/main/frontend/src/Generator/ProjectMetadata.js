/**
 *  Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
import { Actions, Icon } from '@talend/react-components';
import { CONFIGURATION_URL } from '../constants';
import Help from '../Component/Help';
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

  componentWillReceiveProps(nextProps) {
    this.setState({ project: nextProps.project });
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
                const mvnIdx = current.buildToolActions.indexOf(maven[0]);
                const saved = current.buildToolActions[0];
                current.buildToolActions[0] = maven[0];
                current.buildToolActions[mvnIdx] = saved;
              } else {
                current.project.buildType = current.buildToolActions[0].label;
              }

              const currentBuildTool = current.project.buildType || 'Maven';
              const selected = current.buildToolActions.filter(i => i.label === currentBuildTool);
              if (selected.length > 0) {
                selected[0].className = theme.selected;
              } else {
                current.buildToolActions[0].className = theme.selected;
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

        <div className={theme.main}>
          <div className={theme['form-row']}>
            <p className={theme.title}>Create a Talend Component Family Project</p>
            <div>
              <Actions actions={this.state.buildToolActions} />
              <Help title="Build Tool" content={
                <span>
                  <p>Maven is the most commonly used build tool and Talend Component Kit integrates with it smoothly.</p>
                  <p>Gradle is less used but get more and more attraction because it is communicated as being faster than Maven.</p>
                  <p>
                    <Icon name="talend-warning" /> Talend Component Kit does not provide as much features with Gradle than with Maven. The
                    components validation is not yet supported for instance.
                  </p>
                </span>
              } />
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
                <Help title="Family" content={
                  <span>
                    <p>The family groups multiple components altogether.</p>
                    <p>
                    <Icon name="talend-info-circle"/> It is recommanded to use a single family name per component module. The name must be a valid
                                     java name (no space, special characters, ...).
                    </p>
                  </span>
                } />
                <Input className="form-control" id="projectFamily" type="text" placeholder="Enter the component family..."
                       required="required" aggregate={this.state.project} accessor="family"/>
              </div>
              <div className="field">
                <label forHtml="projectCategory">Category</label>
                <Help title="Category" content={
                  <span>
                    <p>The category is a group used by the Studio to organize components of different families in the same bucket into the <code>Palette</code>.</p>
                    <p>It is recommanded to use a two level category. The first level is generally very general and the second one is close to the family name.</p>
                    <Icon name="talend-info-circle"/> The names must be valid java names (no space, special characters, ...).
                  </span>
                } />
                <CategorySelector initialValue={this.state.project.category} onChange={(value) => this.onCategoryUpdate(value)} />
              </div>
            </form>
          </div>

          <p className={[theme.title, theme['form-row']].join(' ')}>Project Metadata</p>
          <form novalidate submit={e => e.preventDefault()}>
            <div className="field">
              <label forHtml="projectGroup">Group</label>
              <Help title="Project Group" content={
                <span>
                  <p>The project group used when deployed on a repository (like a Nexus or central).</p>
                  <p>The best practice recommands to use the reversed commpany hostname suffixed with something specific to the project.</p>
                  <p>
                  Example: <code>company.com</code> would lead to <code>com.company</code> package and for a component the used package would be,
                  for instance, <code>com.company.talend.component</code>.
                  </p>
                </span>
              } />
              <Input className="form-control" id="projectGroup" type="text" placeholder="Enter the project group..."
                     required="required" aggregate={this.state.project} accessor="group"/>
            </div>
            <div className="field">
              <label forHtml="projectArtifact">Artifact</label>
              <Help title="Project Artifact" content={
                <span>
                  <p>The project artifact used when deployed on a repository (like a Nexus or central).</p>
                  <p>It must be a unique identifier in the group namespace.</p>
                  <p>Talend recommandation is to follow the pattern <code>${'{'}component{'}'}-component</code> but you can use whatever you want.</p>
                </span>
              } />
              <Input className="form-control" id="projectArtifact" type="text" placeholder="Enter the project artifact..."
                     required="required" aggregate={this.state.project} accessor="artifact"/>
            </div>
            <div className="field">
              <label forHtml="projectPackage">Package</label>
              <Help title="Project Root package" content={
                <span>
                  <p>The root package represents a unique namespace in term of code.</p>
                  <p>Talend recommandation is to align it on the selected group.</p>
                </span>
              } />
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
                    <Help title="Project Version" content={
                      <span>
                        <p>The version to use when deploying the artifact.</p>
                        <p>Generally this generator is used for a first version so the default should fit without modification.</p>
                      </span>
                    } />
                    <Input className="form-control" id="projectVersion" type="text" placeholder="Enter the project group..."
                           aggregate={this.state.project} accessor="version"/>
                  </div>,
                  <div className="field">
                    <label forHtml="projectName">Project Name</label>
                    <Help title="Project Name" content={
                      <span>
                        <p>Giving a human readable name to the project is more friendly in an IDE or continuous integration platform.</p>
                      </span>
                    } />
                    <Input className="form-control" id="projectName" type="text" placeholder="Enter the project name..."
                           aggregate={this.state.project} accessor="name"/>
                  </div>,
                  <div className="field">
                    <label forHtml="projectDescription">Project Description</label>
                    <Help title="Project Description" content={
                      <span>
                        <p>Giving a human readable description to the project allows to share some goals of the project with other developers in a standard fashion.</p>
                      </span>
                    } />
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
