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
import Project from './Project';

export default class Generator extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
         project: {
           buildType: 'Maven',
           version: '0.0.1-SNAPSHOT',
           group: 'com.company',
           artifact: 'company-component',
           name: 'A Component',
           description: 'A generated component project',
           packageBase: 'com.company.talend.components',
           facets: []
         }
    };
  }

  render() {
    return (
      <div className="Generator">
         <Project project={this.state.project} />
      </div>
    );
  }
}
