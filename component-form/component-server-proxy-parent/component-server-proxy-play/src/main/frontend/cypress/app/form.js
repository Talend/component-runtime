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
import React from 'react'
import { UIForm } from '@talend/react-forms/lib/UIForm'
import { CircularProgress } from '@talend/react-components';
import kit from 'component-kit.js'
import '@talend/bootstrap-theme/src/theme/theme.scss'

// todo: replace by the lib once ready
export class ComponentForm extends React.Component {
  constructor(props) {
    super(props);

    this.state = {};
    this.type = 'datastore';
    this.trigger = kit.createTriggers({
      url: '/componentproxy/api/v1/actions/execute',
      customRegistry: {
        reloadForm: ({ body }) => {
          const { _datasetMetadata } = this.state.uiSpec.properties;
          return {
            ...body,
            // reset the dynamic part
            properties: { _datasetMetadata },
          };
        }
      }
    });


    ['onSubmit', 'onTrigger', 'onChange'].forEach(it => this[it] = this[it].bind(this));
  }

  componentWillMount() {
    fetch(
      `/componentproxy/api/v1/configurations/form/initial/${this.type}`,
      { method: 'GET', headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' } })
    .then(resp => resp.json())
    .then(body => this.setState({ uiSpec: body.ui, metadata: body.metadata }));
  }

  onSubmit() {
    fetch(
      `/componentproxy/api/v1/configurations/persistence/save-from-type/${this.type}`,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
        body: JSON.stringify(this.state.uiSpec.properties)
      });
  }

  onChange(event, { properties }) {
    this.setState({ uiSpec: { ...this.state.uiSpec, properties } });
  }

  onTrigger(event, payload) {
    return this.trigger(event, payload)
      .then(result => {
        if (result.properties || result.errors || result.uiSchema || result.jsonSchema) {
          this.setState({
            uiSpec: {
              ...this.state.uiSpec,
              ...result,
            }
          });
        }
      });
  }

  render() {
    if (!this.state.uiSpec) {
      return (<CircularProgress />);
    }
    return (
      <div className='content'>
        <UIForm
          data={this.state.uiSpec}
          onChange={this.onChange}
          onTrigger={this.onTrigger}
          onSubmit={this.onSubmit}
          actions={[{ bsStyle: 'primary', label: 'Save', type: 'submit', widget: 'button' }]}
        />
      </div>);
  }
}
