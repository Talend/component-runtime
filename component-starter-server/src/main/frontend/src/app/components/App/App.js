/**
 *  Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
import { BrowserRouter as Router, Route, useLocation, useNavigate, Routes } from 'react-router-dom';
import IconsProvider from '@talend/react-components/lib/IconsProvider';
import Icon from '@talend/react-components/lib/Icon';
import Toggle from '@talend/react-components/lib/Toggle';
import HeaderBar from '@talend/react-components/lib/HeaderBar';
import Generator from '../Generator';
import OpenAPIWizard from '../OpenAPI';
import DatastoreContext from '../../DatastoreContext';
import DatasetContext from '../../DatasetContext';
import ComponentsContext from '../../ComponentsContext';
import ProjectContext from '../../ProjectContext';

import theme from './App.module.scss';

function ModeSwitcher() {
	const navigate = useNavigate();
	const openapi = (useLocation().pathname || '/').startsWith('/openapi');

	return (
		<>
			<Icon name="talend-component-kit-negative" />
			<label>Standard</label>
			<Toggle
				id="starter-mode-switcher"
				onChange={() => {
					if (openapi) {
						navigate('/');
					} else {
						navigate('/openapi/project');
					}
				}}
				checked={openapi}
			/>
			<label>OpenAPI</label>
			<Icon name="talend-rest" />
		</>
	);
}

function App() {
	return (
		<Router>
			<div className={theme.App}>
				<IconsProvider />

				<div className={theme.header}>
					<HeaderBar
						id="header-bar"
						logo={{ isFull: true }}
						brand={{
							label: 'Starter Toolkit',
						}}
						user={{
							className: theme.switcher,
						}}
						getComponent={(component) => {
							if (component == 'User') {
								return ModeSwitcher;
							}
							throw new Error('get the default');
						}}
						app="Starter Toolkit"
					/>
				</div>

				<div className={theme.content}>
					<ProjectContext.Provider>
						<DatastoreContext.Provider>
							<DatasetContext.Provider>
								<ComponentsContext.Provider>
									<Routes>
										<Route path="/" element={<Generator />} />
										<Route path="/openapi" element={<OpenAPIWizard />} />
									</Routes>
								</ComponentsContext.Provider>
							</DatasetContext.Provider>
						</DatastoreContext.Provider>
					</ProjectContext.Provider>
				</div>
			</div>
		</Router>
	);
}

export default App;
