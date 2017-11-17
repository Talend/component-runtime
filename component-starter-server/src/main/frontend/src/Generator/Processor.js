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
import { Drawer, Action, Toggle } from '@talend/react-components';
import SchemaButton from '../Component/SchemaButton';
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
      return {checked: !checked};
    });
    !!this.props.onChange && this.props.onChange();
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
      <li className={this.props.theme.Connection} onClick={this.showDrawer}>
        {this.props.connection.name} (todo: trash)
      </li>
    );
  }
}

function Connections (props) {
  return (
    <ul className={props.theme.Connections}>
      {props.connections.map(connection =>
        <Connection connection={connection} theme={props.theme}
                    type={props.type} onUpdateDrawers={props.onUpdateDrawers}
                    onChange={() => !!props.onChange && props.onChange()} />)}
    </ul>
  );
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
        <SchemaButton text="Configuration Model" onClick={this.onConfigurationButtonClick} />

        <div className={this.props.theme['form-row']}>
          <p className={this.props.theme.title}>Connectivity</p>
          <div className={this.props.theme.InlineDocumentation}>
            <p>Click on plus buttons to add input(s)/output(s) and on a branch to edit it.</p>
          </div>
          <div className={[this.props.theme.ComponentWrapper, 'col-sm-12'].join(' ')}>
            <div className={[this.props.theme.Inputs, 'col-sm-5'].join(' ')}>
              <Connections connections={this.props.component.processor.inputStructures} theme={this.props.theme}
                           onUpdateDrawers={this.props.onUpdateDrawers} type="Input" onChange={() => !!this.props.onChange && this.props.onChange()} />
            </div>
            <div className={[this.props.theme.ComponentBox, 'col-sm-1'].join(' ')}>
              <Action icon="talend-plus-circle" onClick={() => this.setState(s => s.inputs = this.newStructure('input', this.state.inputs))} />
              <Action icon="talend-plus-circle" onClick={() => this.setState(s => s.outputs = this.newStructure('output', this.state.outputs))} />
            </div>
            <div className={[this.props.theme.Outputs, 'col-sm-5'].join(' ')}>
              <Connections connections={this.props.component.processor.outputStructures} theme={this.props.theme}
                           onUpdateDrawers={this.props.onUpdateDrawers} type="Output" onChange={() => !!this.props.onChange && this.props.onChange()} />
            </div>
          </div>
        </div>
      </processor>
    );
  }
}
