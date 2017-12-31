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
import { Action, Dialog, Toggle } from '@talend/react-components';
import Help from '../Component/Help';
import Input from '../Component/Input';
import Summary from './Summary';

import { GENERATOR_GITHUB_URL, GENERATOR_ZIP_URL } from '../constants';

import theme from './Finish.scss';

export default class Finish extends React.Component {
  constructor(props) {
    super(props);

    const model = this.createModel(this.props);
    this.state = {
      project: model,
      github: {
        username: '',
        password: '',
        organization: '',
        repository: model.artifact,
        useOrganization: true
      }
    };

    ['closeModal', 'notifyProgressDone', 'createModel', 'onDownload', 'onGithub', 'showGithub'].forEach(i => this[i] = this[i].bind(this));
  }

  componentWillReceiveProps(nextProps) {
    this.setState(state => {
      state.project = this.createModel(nextProps);
      state.github.repository = state.project.artifact
    });
  }

  onClearGithubModal() {
    this.setState({ githubError: undefined, current: undefined });
  }

  createModel(props) {
    // we copy the model to compute sources and processors attributes
    let lightCopyModel = Object.assign({}, props.project);
    const components = props.components();
    lightCopyModel.sources = components.filter(c => c.type === 'Input').map(c => {
      let source = Object.assign({}, c.source);
      source.name = c.configuration.name;
      return source;
    });
    lightCopyModel.processors = components.filter(c => c.type === 'Processor').map(c => {
      let processor = Object.assign({}, c.processor);
      processor.name = c.configuration.name;
      return processor;
    });
    return lightCopyModel;
  }

  listenForDone(promise) {
    return promise.then(this.notifyProgressDone, this.notifyProgressDone);
  }

  closeModal() {
    this.setState({current: undefined});
  }

  notifyProgressDone() {
    this.setState({progress: undefined});
  }

  showGithub() {
    this.setState({current: 'github'});
  }

  onDownload() {
    this.listenForDone(this.doDownload());
  }

  onGithub() {
    if (this.isEmpty(this.state.github.username) || this.isEmpty(this.state.github.password) ||
        (this.state.github.useOrganization && this.isEmpty(this.state.github.organization)) || this.isEmpty(this.state.github.repository)) {
      this.setState({githubError: 'Please fill the form properly before launching the project creation.'});
      return;
    }
    this.listenForDone(this.doGithub());
  }

  doGithub(model) {
    this.setState({progress: 'github'});
    return fetch(`${GENERATOR_GITHUB_URL}`, {
        method: 'POST',
        body: JSON.stringify({ model: this.state.project, repository: this.state.github }),
        headers: new Headers({'Accept': 'application/json', 'Content-Type': 'application/json'})
      })
      .then(d => {
        if (d.status > 299) {
          d.json().then(json => {
            this.setState({
              current: 'message',
              modalMessage: (
                <div className={theme.error}>
                  <p>{json.message || JSON.stringify(json)}</p>
                </div>
              )
            });
          });
        } else {
          const link = `https://github.com/${this.state.github.useOrganization ? this.state.github.organization : this.state.github.username}/${this.state.github.repository}`;
          this.setState({
            current: 'message',
            modalMessage: (
              <div>
                Project <a target="_blank" href={link}>{this.state.github.repository}</a> created with success!
              </div>
            )
          });
        }
      });
  }

  doDownload(model) {
    this.setState({progress: 'zip'});
    return fetch(`${GENERATOR_ZIP_URL}`, {
        method: 'POST',
        body: JSON.stringify(this.state.project),
        headers: new Headers({'Accept': 'application/zip', 'Content-Type': 'application/json'})
      })
      .then(response => response.blob())
      .then(blob => {
          let a = document.createElement("a");
          const url = window.URL.createObjectURL(blob);
          try {
            document.body.appendChild(a);
            a.style = "display: none";
            a.href = url;
            a.download = this.state.project.artifact + '.zip';
            a.click();
          } finally {
            window.URL.revokeObjectURL(url);
            document.body.removeChild(a);
          }
      });
  }

  isEmpty(str) {
    return !str || str.trim().length === 0;
  }

  render() {
    const fieldClasses = ['field', theme.field].join(' ');
    return (
      <form className={theme.Finish} novalidate submit={e => e.preventDefault()}>
        <h2>Project Summary</h2>
        <Summary project={this.state.project} />
        <div className={theme.bigButton}>
          <Action label="Download as ZIP" bsStyle="info" onClick={this.onDownload} icon="fa-file-archive-o"
                  inProgress={this.state.progress === 'zip'} disabled={!!this.state.progress && this.state.progress !== 'zip'}
                  className="btn btn-lg" />
          <Action label="Create on Github" bsStyle="primary" onClick={this.showGithub} icon="fa-github"
                  inProgress={this.state.progress === 'github'} disabled={!!this.state.progress && this.state.progress !== 'github'}
                  className="btn btn-lg" />
        </div>
        {
          this.state.current === 'github' && (
            <Dialog header="Github Configuration"
                    bsDialogProps={{show: true, size: "small", onHide: () => {this.notifyProgressDone(); this.onClearGithubModal(); } }}
                    action={{label: "Create on Github", onClick: this.onGithub }}>
              <form novalidate submit={e => e.preventDefault()} className={theme.modal}>
                {!!this.state.githubError && <p className={theme.error}>{this.state.githubError}</p>}
                <div className={fieldClasses}>
                  <label forHtml="githubUser">User</label>
                  <Help title="Github User" content={
                    <span>
                      <p>The Github username to use to create the project.</p>
                    </span>
                  } />
                  <Input className="form-control" id="githubUser" type="text" placeholder="Enter your Github username..." required
                         aggregate={this.state.github} accessor="username"/>
                </div>
                <div className={fieldClasses}>
                  <label forHtml="githubPassword">Password</label>
                  <Help title="Github Password" content={
                    <span>
                      <p>The Github password to use to create the project.</p>
                    </span>
                  } />
                  <Input className="form-control" id="githubPassword" type="password" placeholder="Enter your Github password..." required
                         aggregate={this.state.github} accessor="password"/>
                </div>

                {
                  !!this.state.github.useOrganization && (
                    <div className={fieldClasses}>
                      <label forHtml="githubOrganization">Organization</label>
                      <Help title="Github Organization" content={
                        <span>
                          <p>The Github organization to use to create the project.</p>
                        </span>
                      } />
                      <Input className="form-control" id="githubOrganization" type="text" placeholder="Enter your Github organization..." required
                             aggregate={this.state.github} accessor="organization"/>
                    </div>)
                }
                <div className={fieldClasses}>
                  <label forHtml="githubRepository">Repository</label>
                  <Help title="Github Repository" content={
                    <span>
                      <p>The Github repository to create for the project.</p>
                    </span>
                  } />
                  <Input className="form-control" id="githubRepository" type="text" placeholder="Enter the Github repository to create..." required
                         aggregate={this.state.github} accessor="repository"/>
                </div>

                <div className={fieldClasses}>
                  <label forHtml="githubUseOrganization">Create the repository for an organization</label>
                  <Help title="Github Use Organization" content={
                    <span>
                      <p>If checked an organization project will be created instead of a user project.</p>
                    </span>
                  } />
                  <Toggle id="githubUseOrganization" checked={this.state.github.useOrganization}
                          onChange={() => this.setState(state => state.github.useOrganization = !state.github.useOrganization)} />
                </div>
              </form>
            </Dialog>
          )
        }
        {
          this.state.current === 'message' && (
            <Dialog header="Result"
                    bsDialogProps={{show: true, size: "small", onHide: () => {this.notifyProgressDone();this.closeModal();} }}
                    action={{label: "Close", onClick: () => {this.notifyProgressDone();this.closeModal();} }}>
              <p>{this.state.modalMessage}</p>
            </Dialog>
          )
        }
      </form>
    );
  }
}
