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
import { ThemeProvider } from '@talend/design-system';
import IconsProvider from '@talend/react-components/lib/IconsProvider';
import HeaderBar from '@talend/react-components/lib/HeaderBar';
import Generator from '../Generator';
import OpenAPIWizard from '../OpenAPI';
import DatastoreContext from '../../DatastoreContext';
import DatasetContext from '../../DatasetContext';
import ComponentsContext from '../../ComponentsContext';
import ProjectContext from '../../ProjectContext';
import Component from '../Component';

import theme from './App.module.scss';
import ProjectMetadata from '../ProjectMetadata';
import DatastoreList from '../DatastoreList';
import DatasetList from '../DatasetList';
import ComponentAddForm from '../ComponentAddForm';
import Finish from '../Finish';
import { GENERATOR_OPENAPI_ZIP_URL, GENERATOR_ZIP_URL } from '../../constants';
import { OpenAPITrans } from '../OpenAPI/OpenAPI.component';

const FinishZip = () => <Finish actionUrl={GENERATOR_ZIP_URL} openapi={false} />;

function ModeSwitcher() {
	const navigate = useNavigate();
	const openapi = (useLocation().pathname || '/').startsWith('/openapi');

	return (
		<>
			<button
				id="starter-mode-switcher"
				class="btn btn-link"
				onClick={() => {
					if (openapi) {
						navigate('/');
					} else {
						navigate('/openapi/project');
					}
				}}
				checked={openapi}
			>
				{openapi ? 'Switch to Starter' : 'Switch to OpenAPI'}
			</button>
		</>
	);
}

function App() {
	return (
		<ThemeProvider>
			<Router>
				<div className={theme.App}>
					<IconsProvider />

					<div className={theme.header}>
						<HeaderBar
							id="header-bar"
							logo={{ isFull: false }}
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
											<Route path="/" element={<Generator />}>
												<Route exact path="/" element={<ProjectMetadata />} />
												<Route exact path="/project" element={<ProjectMetadata />} />
												<Route exact path="/datastore" element={<DatastoreList />} />
												<Route exact path="/dataset" element={<DatasetList />} />
												<Route path="/component/:componentId" element={<Component />} />
												<Route path="/add-component" element={<ComponentAddForm />} />
												<Route path="/export" element={<FinishZip />} />
											</Route>

											<Route path="/openapi" element={<OpenAPIWizard />}>
												<Route
													exact
													path="/openapi/project"
													element={<ProjectMetadata hideFacets={true} hideCategory={true} />}
												/>
												<Route exact path="/openapi/design" element={<OpenAPITrans />} />
												<Route
													exact
													path="/openapi/export"
													element={<Finish openapi={true} actionUrl={GENERATOR_OPENAPI_ZIP_URL} />}
												/>
											</Route>
										</Routes>
									</ComponentsContext.Provider>
								</DatasetContext.Provider>
							</DatastoreContext.Provider>
						</ProjectContext.Provider>
					</div>
				</div>
			</Router>
		</ThemeProvider>
	);
}

export default App;
