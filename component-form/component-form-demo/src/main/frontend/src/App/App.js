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
import { BrowserRouter as Router, Route } from 'react-router-dom';
import IconsProvider from '@talend/react-components/lib/IconsProvider';
import HeaderBar from '@talend/react-components/lib/HeaderBar';
import BackToList from '../BackToList';
import ComponentsList from '../ComponentsList';
import ComponentForm from '../ComponentForm';

import './App.css';
import './favicon.ico';

export default function App() {
	return (
		<Router>
			<div className="App">
				<IconsProvider/>

				<div className="header">
					<HeaderBar logo={{ isFull: true }} brand={{ name: 'TCOMP DEMO' }} />
				</div>

				<BackToList/>

				<div className="content">
					<Route exact path="/" component={ComponentsList}/>
					<Route path="/detail/:componentId" component={ComponentForm}/>
				</div>
			</div>
		</Router>
	);
}