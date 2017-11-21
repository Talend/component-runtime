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
import { Toggle, Drawer } from '@talend/react-components';
import Help from '../Component/Help';
import AppButton from '../Component/AppButton';
import Schema from '../Component/Schema';

export default class Mapper extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      configurationStructure: props.component.source.configurationStructure,
      outputStructure: props.component.source.outputStructure
    };

    this.drawerActions = {
      actions: {
        right: [
          {
            label: 'Close',
            bsStyle: 'primary',
            onClick: () => this.props.onUpdateDrawers([])
          }
        ]
      }
    };

    ['onStreamChange', 'onConfigurationButtonClick', 'onRecordTypeChange', 'onRecordButtonClick']
      .forEach(i => this[i] = this[i].bind(this));
  }

  componentWillReceiveProps(nextProps) {
    if (this.props !== nextProps) {
      this.setState({
        configurationStructure: nextProps.component.source.configurationStructure,
        outputStructure: nextProps.component.source.outputStructure
      });
    }
  }

  onStreamChange() {
    this.props.component.source.stream = !this.props.component.source.stream;
    this.setState({});
  }

  onConfigurationButtonClick() {
    this.props.onUpdateDrawers([
      <Drawer title={`${this.props.component.configuration.name} Configuration Model`} footerActions={this.drawerActions}>
        <Schema schema={this.state.configurationStructure} readOnly={true} name="configuration" />
      </Drawer>
    ]);
  }

  onRecordButtonClick() {
    this.props.onUpdateDrawers([
      <Drawer title={`${this.props.component.configuration.name} Record Model`} footerActions={this.drawerActions}>
        <Schema schema={this.state.outputStructure} readOnly={true} name="root" />
      </Drawer>
    ]);
  }

  onRecordTypeChange(event) {
    this.props.component.source.genericOutput = event.target.value === 'generic';
    if (this.props.component.source.genericOutput) {
      delete this.props.component.source.outputStructure;
    } else {
      this.props.component.source.outputStructure = {
        entries: []
      };
    }
    this.setState({recordType: event.target.value, outputStructure: this.props.component.source.outputStructure});
  }

  render() {
    return (
      <mapper className={this.props.theme.Mapper}>
        <AppButton text="Configuration Model" onClick={this.onConfigurationButtonClick} help={(
          <Help title="Configuration" content={
            <span>
              <p>The component configuration schema design can be configured by clicking on this button.</p>
              <p>It allows you to define the data you need to be able to execute the component logic.</p>
            </span>
          } />
        )} />
        <div className={this.props.theme['form-row']}>
          <p className={this.props.theme.title}>
            Stream
            <Help title="Stream" content={
              <span>
                <p>Activate this toggle if your input will issue a stream.</p>
                <p>It means that there is no real "end" of the data and that the job/pipeline using this component
                is not intended to be terminated.</p>
              </span>
            } />
          </p>
          <Toggle checked={this.props.component.source.stream} onChange={() => this.onStreamChange()} />
        </div>
        <div className={this.props.theme['form-row']}>
          <p className={this.props.theme.title}>
            Record Type
            <Help title="Record" content={
              <span>
                <p>The input will issue some records.</p>
                <p>This configuration allows you to either define the schema of the records
                or to use a generic record type you will implement if the data structure can vary depending the component configuration.</p>
              </span>
            } />
          </p>
          <select className={this.props.theme.recordSelector} value={this.state.recordType} onChange={this.onRecordTypeChange}>
            <option selected={!!this.props.component.source.genericOutput} value="generic">Generic</option>
            <option selected={!this.props.component.source.genericOutput} value="custom">Custom</option>
          </select>
          {
            !this.props.component.source.genericOutput && <AppButton text="Record Model" onClick={this.onRecordButtonClick} help={(
              <Help title="Record Model" content={
                <span>
                  <p>In custom mode you can define the schema of the record you want to design by clicking on this button.</p>
                  <p>The schema will be used to create POJO model you will use to instantiate the records sent by your input to the job.</p>
                </span>
              } />
            )} />
          }
        </div>
      </mapper>
    );
  }
}
