/**
 *  Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
'use strict';

import React from 'react';
import ReactDOM from 'react-dom';
import { BrowserRouter, Route } from 'react-router-dom';
import IconsProvider from '@talend/react-components/lib/IconsProvider';
import HeaderBar from '@talend/react-components/lib/HeaderBar';
import 'whatwg-fetch'
import '@talend/bootstrap-theme/dist/bootstrap.css';
import Home from './Home';

class Root extends React.Component {
  render() {
    return (
      <BrowserRouter>
        <div className="App">
          <IconsProvider/>

          <div className="header">
            <HeaderBar logo={{ isFull: true }} brand={{ name: 'Talend Component Kit Tester' }} />
          </div>
          <div className="content">
            <Route exact path="/" component={Home}/>
          </div>
        </div>
      </BrowserRouter>
    );
  }
}

ReactDOM.render(<Root />, document.getElementById('component-kit-tools-webapp'));
