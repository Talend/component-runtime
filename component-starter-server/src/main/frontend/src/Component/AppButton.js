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
import { Action } from '@talend/react-components';

import theme from './AppButton.scss';

export default class AppButton extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
    };
  }

  render() {
    const classes = [theme.AppButton];
    if ('right' === this.props.side) {
      classes.push(theme.right);
    }
    return (
      <div className={classes.join(' ')}>
        <Action onClick={e => e.preventDefault() || (!!this.props.onClick && this.props.onClick())}
                icon={this.props.icon} iconPosition={this.props.iconPosition || 'right'}
                className={this.props.className}
                label={this.props.text || 'Schema'} />
        {!!this.props.help && this.props.help}
      </div>
    );
  }
}
