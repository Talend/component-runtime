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
import { Action } from '@talend/react-components';
import Summary from './Summary';

import { GENERATOR_URL } from '../constants';

import theme from './Finish.scss';

export default class Finish extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      project: this.createModel()
    };

    ['notifyProgressDone', 'createModel', 'onSave', 'onDownload'].forEach(i => this[i] = this[i].bind(this));
  }

  componentWillReceiveProps(nextProps) {
    this.setState({project: this.createModel()});
  }

  createModel() {
    // we copy the model to compute sources and processors attributes
    let lightCopyModel = Object.assign({}, this.props.project);
    lightCopyModel.sources = this.props.components.filter(c => c.type === 'Input').map(c => {
      let source = Object.assign({}, c.source);
      source.name = c.name;
      return source;
    });
    lightCopyModel.processors = this.props.components.filter(c => c.type === 'Processor').map(c => {
      let processor = Object.assign({}, c.processor);
      processor.name = c.name;
      return processor;
    });
    return lightCopyModel;
  }

  listenForDone(promise) {
    promise.then(this.notifyProgressDone, this.notifyProgressDone);
  }

  notifyProgressDone() {
    this.setState({progress: undefined});
  }

  onDownload() {
    this.setState({progress: 'zip'});
    this.listenForDone(this.doDownload());
  }

  doDownload(model) {
    return fetch(`${GENERATOR_URL}`, {
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
            a.download = this.state.value.project.artifact + '.zip';
            a.click();
          } finally {
            window.URL.revokeObjectURL(url);
            document.body.removeChild(a);
          }
      });
  }

  onSave() {
    this.setState({progress: 'json'});
  }

  render() {
    return (
      <div className={theme.Finish}>
        <h2>Project Summary</h2>
        <Summary project={this.state.project} />
        <div className={theme.bigButton}>
          <Action label="Download as ZIP" bsStyle="primary" onClick={this.onDownload}
                  inProgress={this.state.progress === 'zip'} disabled={!!this.state.progress && this.state.progress !== 'zip'}
                  className="btn btn-lg" />
        </div>
        {/*
        <Action label="Save Project" onClick={this.onSave}
                inProgress={this.state.progress === 'json'} disabled={!!this.state.progress && this.state.progress !== 'json'}
                className={`btn btn-lg ${theme.bigButton}`}/>

        {
          this.state.progress === 'json' && (
            <Dialog header="Project Model" bsDialogProps={{show: true, size: "small", onHide: this.notifyProgressDone }} action={{label: "OK", onClick: this.notifyProgressDone }}>
              <div>
                <pre>
                  {JSON.stringify(this.state.project, ' ', 2)}
                </pre>
              </div>
            </Dialog>
          )
        }
        */}
      </div>
    );
  }
}
