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
import { IconsProvider, Icon } from '@talend/react-components';

import theme from './Schema.scss';

class Node extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      opened: this.props.onlyChildren,
      entries: (this.props.node.model || this.props.node).entries
    };
    ['addChild'].forEach(i => this[i] = this[i].bind(this));
  }

  addChild() {
    this.setState(state => state.entries.push({
      name: 'newAttribute',
      type: 'string'
    }));
  }

  render() {
    const nodeIcon = !!this.state.opened ? 'talend-caret-down': 'talend-chevron-left';
    return (
      <div>
        <IconsProvider icons={['talend-chevron-left', 'talend-caret-down', 'talend-plus-circle']}/>

        {
          !this.props.onlyChildren && (
            <span>
            {
              !this.props.node.type && <Icon className={theme.nodeIcon} name={nodeIcon} onClick={() => this.setState({opened: !this.state.opened})} />
            }
            {this.props.node.name} ({this.props.node.type || 'object'})
            </span>
          )
        }
        {
          (!!this.props.onlyChildren || (!!this.state.entries && !!this.state.opened)) &&
            <ul>
              {this.state.entries.map((node, index) => <li key={index}><Node node={node} /></li>)}
            </ul>
        }
        {
          !!this.props.node.model &&
            <Icon name="talend-plus-circle" onClick={() => {this.addChild(); !this.state.opened && this.setState({opened: true});}} />
        }
      </div>
    );
  }
}

export default class Schema extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
    };

    // [].forEach(i => this[i] = this[i].bind(this));
  }

  render() {
    return (
      <div className={theme.Schema}>
        <Node node={this.props.schema} onlyChildren={this.props.onlyChildren} />
      </div>
    );
  }
}
