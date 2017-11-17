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

import theme from './Input.scss';

export default class Input extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      value: this.props.initialValue
    };
    this.onChange = this.onChange.bind(this);
  }

  onChange(evt) {
    let diff = {};
    if (!!this.props.aggregate) {
      this.props.aggregate[this.props.accessor] = evt.target.value;
    } else {
      diff.value = evt.target.value;
    }
    diff.message = evt.target.checkValidity() ? undefined: evt.target.validationMessage;
    if (!diff.message) {
      this.props.onChange && this.props.onChange(evt.target.value);
    }
    this.setState(diff);
  }

  render() {
    return (
      <span className={theme.Input}>
        <input {...this.props} onChange={e => this.onChange(e)} value={!!this.props.aggregate ?  this.props.aggregate[this.props.accessor] : this.state.value}/>
        {
          !!this.state.message && <span className={theme.error}>{this.state.message}</span>
        }
      </span>
    );
  }
}
