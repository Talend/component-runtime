/**
 *  Copyright (C) 2006-2023 Talend Inc. - www.talend.com
 *
 *  Licensed under the Apache License, Version 2.0 (the 'License');
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an 'AS IS' BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import React from 'react';

import { withTranslation, I18nextProvider } from 'react-i18next';
import { IconsProvider, HeaderBar, Layout, Notification, CircularProgress } from '@talend/react-components';

import Menu from '../Menu';
import Detail from '../Detail';

import i18n from '../../i18n';

import theme from './App.scss';

class App extends React.Component {
  constructor(props) {
    super(props);
    this.notificationLeaveFn = this.notificationLeaveFn.bind(this);
  }

  notificationLeaveFn(notification) {
    this.props.removeNotification(notification);
  }

  render() {
    const header = (
      <HeaderBar logo={{ isFull: true }} brand={{
        id: 'header-brand',
        label: 'Talend Component Kit Web Tester'
      }}/>
    );
    const menu = (<Menu />);

    return (
      <div className={theme.App}>
        <I18nextProvider i18n={i18n}>
          <IconsProvider/>
          <Layout mode={'TwoColumns'} header={header} one={menu}>
            <Detail saga="Detail::start" />
          </Layout>
          <Notification notifications={this.props.notifications} leaveFn={this.notificationLeaveFn} />
        </I18nextProvider>
      </div>
    );
  }
}

export default withTranslation('app')(App);
