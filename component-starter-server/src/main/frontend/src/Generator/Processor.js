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
import { Drawer, Toggle, Icon } from '@talend/react-components';
import AppButton from '../Component/AppButton';
import Input from '../Component/Input';
import Schema from '../Component/Schema';

const drawerActions = props => {
  return {
    actions: {
      right: [
        {
          label: 'Close',
          bsStyle: 'primary',
          onClick: () => props.onUpdateDrawers([])
        }
      ]
    }
  };
};

class EmbeddableToggle extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      checked: !!props.connection.generic,
      structure: props.connection.structure
    };
    this.onChange = this.onChange.bind(this);
  }

  componentWillReceiveProps(nextProps) {
    if (this.props !== nextProps) {
      this.setState({
        checked: !!nextProps.connection.generic,
        structure: nextProps.connection.structure
      });
    }
  }

  onChange() {
    this.setState(({checked}) => {
      this.props.connection.generic = !this.props.connection.generic;
      return {checked: this.props.connection.generic};
    });
  }

  render() {
    return (
      <schemaConfiguration>
        <div className={this.props.theme['form-row']}>
          <p className={this.props.theme.title}>Generic</p>
          <Toggle checked={this.state.checked} onChange={this.onChange} />
        </div>
        {
          !this.state.checked &&
            <div className={this.props.theme['form-row']}>
              <p className={this.props.theme.title}>Structure</p>
              <Schema schema={this.state.structure} readOnly={true} name="root" />
            </div>
        }
      </schemaConfiguration>
    );
  }
}

class Connection extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      custom: !this.props.connection.generic
    };
    ['switchStructureType', 'showDrawer']
      .forEach(i => this[i] = this[i].bind(this));
  }

  componentWillReceiveProps(nextProps) {
    if (this.props !== nextProps) {
      this.setState({
        custom: !nextProps.connection.generic
      });
    }
  }

  switchStructureType() {
    this.props.connection.generic = !this.props.connection.generic;
    this.setState({
      custom: !this.props.connection.generic
    });
  }

  showDrawer() {
    this.props.onUpdateDrawers([
      <Drawer title={`${this.props.connection.name} Record Model (${this.props.type})`} footerActions={drawerActions(this.props)}>
        <div className="field">
          <label>Name</label>
          <Input className="form-control" type="text" placeholder="Enter the connection name..."
                 aggregate={this.props.connection} accessor="name" />
        </div>
        <EmbeddableToggle connection={this.props.connection} theme={this.props.theme} onChange={this.switchStructureType} />
      </Drawer>
    ]);
  }

  render() {
    return (
      <li className={this.props.theme.Connection}>
        <div>
          <span onClick={this.showDrawer}>
            {this.props.connection.name}&nbsp;
            <span className={this.props.theme.recordType}>({!this.props.connection.generic ? 'custom' : 'generic'})</span>&nbsp;
          </span>
          {!this.props.readOnly && (<span>(<Icon name="talend-trash" onClick={e => {e.preventDefault(); this.props.removeConnection(this.props.connection);}} />)</span>)}
        </div>
      </li>
    );
  }
}

class Connections extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      connections: props.connections
    };
    this.removeConnection = this.removeConnection.bind(this);
  }

  removeConnection(connection) {
    this.props.connections.splice(this.props.connections.indexOf(connection), 1);
    this.setState({
      connections: this.props.connections.map(i => i)
    })
  }

  componentWillReceiveProps(nextProps) {
    if (this.props !== nextProps) {
      this.setState({
        connections: nextProps.connections
      });
    }
  }

  render (props) {
    return (
      <ul className={this.props.theme.Connections}>
        {
          this.props.connections.map(connection =>
            <Connection connection={connection} theme={this.props.theme}
                        readOnly={this.props.type === 'Input' && connection.name === 'MAIN'}
                        type={this.props.type} onUpdateDrawers={this.props.onUpdateDrawers}
                        removeConnection={this.removeConnection}  />)
        }
        {
          (!this.props.connections || this.props.connections.length === 0) &&
            <li className={this.props.theme.ConnectionEnd}></li>
        }
      </ul>
    );
  }
}

export default class Processor extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      inputs: this.props.component.processor.inputStructures,
      outputs: this.props.component.processor.outputStructures
    };

    ['onConfigurationButtonClick']
      .forEach(i => {
        this[i] = this[i].bind(this);
      });
  }

  componentWillReceiveProps(nextProps) {
    if (this.props !== nextProps) {
      this.setState({
        inputs: nextProps.component.processor.inputStructures,
        outputs: nextProps.component.processor.outputStructures
      });
    }
  }

  onConfigurationButtonClick() {
    this.props.onUpdateDrawers([
      <Drawer title={`${this.props.component.configuration.name} Configuration Model`} footerActions={drawerActions(this.props)}>
        <Schema schema={this.props.component.processor.configurationStructure} readOnly={true} name="configuration" />
      </Drawer>
    ]);
  }

  newStructure(prefix, array) {
    array.push({
      name: array.length === 0 ? 'MAIN' : `${prefix}_${array.length + 1}`,
      generic: false,
      structure: {
        entries: []
      }
    });
    return array;
  }

  render() {
    return (
      <processor className={this.props.theme.Processor}>
        <AppButton text="Configuration Model" onClick={this.onConfigurationButtonClick} />

        <div className={this.props.theme['form-row']}>
          <p className={this.props.theme.title}>Input(s) / Ouput(s)</p>
          <div className={[this.props.theme.ComponentButtons, 'col-sm-12'].join(' ')}>
            <div className="col-sm-6">
              <AppButton text="Add Input" icon="talend-plus-circle" onClick={() => this.setState(s => s.inputs = this.newStructure('input', this.state.inputs))} />
            </div>
            <div className="col-sm-6">
              <AppButton text="Add Ouput" icon="talend-plus-circle" className="pull-right" iconPosition="left"
                         onClick={() => this.setState(s => s.outputs = this.newStructure('output', this.state.outputs))} />
            </div>
          </div>
          <div className={[this.props.theme.ComponentWrapper, 'col-sm-12'].join(' ')}>
            <div className={[this.props.theme.Inputs, 'col-sm-5'].join(' ')}>
              <Connections connections={this.props.component.processor.inputStructures} theme={this.props.theme}
                           onUpdateDrawers={this.props.onUpdateDrawers} type="Input" />
            </div>
            <div className={[this.props.theme.ComponentBox, 'col-sm-2'].join(' ')}>
              <componentBox>T</componentBox>
            </div>
            <div className={[this.props.theme.Outputs, 'col-sm-5'].join(' ')}>
              <Connections connections={this.props.component.processor.outputStructures} theme={this.props.theme}
                           onUpdateDrawers={this.props.onUpdateDrawers} type="Output" />
            </div>
          </div>
        </div>
      </processor>
    );
  }
}
