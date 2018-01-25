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
import { connect } from 'react-redux';
import UIForm from '@talend/react-forms';
import TalendComponentKitTrigger from 'component-kit.js';

import Api from './Api';

import theme from './Detail.scss';

class Detail extends React.Component {
  constructor(props) {
    super(props);
    this.trigger = new TalendComponentKitTrigger({ url: 'api/v1/application/action' });
    this.state = {};
    this.classes = [theme.Detail, this.props.className].join(' ');
    ['onTrigger', 'onSubmit', 'changeForm'].forEach(a => this[a] = this[a].bind(this));
  }

  loadState(props) {
    if (!props.component) {
      this.setState({ uiSpec: undefined });
      return;
    }
    Api.get(props.component.$$detail)
      .then(payload => this.setState({ uiSpec: payload }));
  }

  componentWillMount() {
    this.loadState(this.props);
  }

  componentWillReceiveProps(nextProps) {
    if (this.props !== nextProps && (this.props.component || {}).$$detail !== (nextProps.component || {}).$$detail) {
      this.loadState(nextProps);
    }
  }

  onTrigger(event, payload) {
    return this.trigger.onDefaultTrigger(event, payload);
  }

  onSubmit(event, payload) {
    this.setState({ submitted: true });
  }

  changeForm() {
    this.setState({ submitted: false });
  }

  render() {
    return (
      <div className={this.classes}>
        { !this.state.uiSpec &&
          <div>
            <h1>No component selected</h1>
            <p>Click on a component to see its form</p>
          </div>
        }
        { this.state.uiSpec &&
          <UIForm
            data={this.state.uiSpec}
            onTrigger={this.onTrigger}
            onSubmit={this.onSubmit}
          />
        }
      </div>
    );
  }
}

export default connect(state => {
  return {
    ...state,
    component: state.component
  };
})(Detail);
