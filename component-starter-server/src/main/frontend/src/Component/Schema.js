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
import { Icon } from '@talend/react-components';
import keycode from 'keycode';
import Input from './Input';
import Help from './Help';

import theme from './Schema.scss';

class Node extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      opened: !!this.props.parent || !!this.props.readOnly,
      type: this.props.node.type || 'object',
      edited: false,
      entries: (this.props.node.model || this.props.node).entries
    };

    this.nodeTypes = ['object', 'boolean', 'double', 'integer', 'uri', 'url', 'string'] // don't support file yet, this is not big data friendly
      .map(i => {return {value: i, label: i.substring(0, 1).toUpperCase() + i.substring(1)}});

    ['addChild', 'onTypeChange', 'onEdit', 'deleteNode', 'onDeleteChild', 'validEdition']
      .forEach(i => this[i] = this[i].bind(this));
  }

  componentWillReceiveProps(nextProps) {
    if (this.props !== nextProps) {
      this.setState({
        opened: !!nextProps.parent || !!nextProps.readOnly,
        type: nextProps.node.type || 'object',
        edited: false,
        entries: (nextProps.node.model || nextProps.node).entries
      });
    }
  }

  onTypeChange(event) {
    const type = event.target.value;
    switch (type) {
      case 'object':
        this.props.node.model = this.props.node.model || this.props.node._model || { entries: [] };
        delete this.props.node.type;
        break;
      default:
        this.props.node.type = type;
        this.props.node._model = this.props.node.model || this.props.node._model;
        delete this.props.node.model;
    }
    this.setState({type, entries: !!this.props.node.model ? this.props.node.model.entries : undefined});
  }

  addChild() {
    this.setState(state => state.entries.push({
      name: 'configuration' + (state.entries.length + 1),
      type: 'string'
    }));
  }

  onEdit() {
    if (!!this.props.readOnly) {
      return;
    }
    this.setState({edited: true})
  }

  deleteNode() {
    !!this.props.onDeleteChild && this.props.onDeleteChild(this.props.node);
  }

  onDeleteChild(node) {
    const idx = this.state.entries.indexOf(node);
    if (idx >= 0) {
      this.setState(state => {
        state.entries.splice(idx, 1);
        (this.props.node.model || this.props.node).entries = state.entries;
      });
    }
  }

  validEdition(event) {
    switch (event.which) {
      case keycode.codes.enter:
        this.setState({edited: false});
        break;
      default:
        break;
    }
  }

  render() {
    const nodeIcon = !!this.state.opened ? 'talend-caret-down': 'talend-chevron-left';
    let nodeView;
    if (!!this.state.edited) {
      nodeView = (
        <span>
          <Input type="text" placeholder="Enter the configuration name..." required="required" minLength="1"
                 aggregate={this.props.node} accessor="name" initialValue={this.props.node.name || this.props.name} onChange={() => this.setState({})}
                 onKeyDown={this.validEdition} />
          ({
            <select value={this.state.type} onChange={this.onTypeChange}>
              {this.nodeTypes.map(t => <option selected={this.state.value === t.value} value={t.value}>{t.label}</option>)}
            </select>
          })
          <Icon name="talend-check" onClick={() => this.setState({edited: false})} />
        </span>
      );
    } else {
      nodeView = <span>
        <span onClick={this.onEdit}>{this.props.node.name || this.props.name} ({this.props.node.type || 'object'})</span>
        {!this.props.readOnly && <Icon className={theme.nodeIcon} name="talend-trash" onClick={this.deleteNode} />}
        {
          !this.props.parent && (
            <Help title="Designing your model" i18nKey="schema_designing_model" content={
              <span>
                <p>This view allows you to modelize your model.</p>
                <p>You can click on the <Icon name="talend-plus-circle" /> to add an object field, on the <Icon name="talend-trash" /> to delete one and on any field to edit its name and type.</p>
                <p>To add a nested object just add a field, click on it, customize its name and select the type object into the select box then valid your updates.</p>
                <p>
                  <Icon name="talend-info-circle"/> The field names must be a valid java name (no space, special characters, ...). If the field name contains
                  some specific keywords it will be considered as a credential and marked as such (for instance <code>password</code>, <code>token</code>, ...);
                </p>
              </span>
            } />
          )
        }
      </span>;
    }
    return (
      <div>
        <span>
          {
            !this.props.node.type && <Icon className={theme.nodeIcon} name={nodeIcon} onClick={() => this.setState({opened: !this.state.opened})} />
          }
          <nodeLabel>
            {nodeView}
          </nodeLabel>
        </span>
        {
          (!this.props.parent || !!this.state.entries) && (
              <ul className={theme.vLine}>
                {!!this.state.opened && this.state.entries.map((node, index) => {
                  return (
                    <li key={index}>
                      <Node node={node} parent={this.props.node} onDeleteChild={this.onDeleteChild} />
                    </li>
                  );
                })}
                {!!this.state.opened && !!this.state.entries && this.state.entries.length === 0 && <li>No configuration yet, click on the plus bellow</li>}
              </ul>
          )
        }
        {
          (!this.props.node.type || this.props.node.type === 'object') &&
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
      schema: props.schema
    };
  }

  componentWillReceiveProps(nextProps) {
    if (this.props !== nextProps) {
      this.setState({
        schema: nextProps.schema
      });
      !!this.props.onReload && this.props.onReload();
    }
  }

  render() {
    return (
      <div className={theme.Schema}>
        <Node node={this.state.schema} readOnly={this.props.readOnly || !!this.props.parent} name={this.props.name} />
      </div>
    );
  }
}
