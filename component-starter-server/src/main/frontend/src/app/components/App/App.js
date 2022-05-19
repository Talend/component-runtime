/**
 *  Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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
import PropTypes from 'prop-types';
import { BrowserRouter as Router, Route, Switch, withRouter } from 'react-router-dom';
import { withTranslation } from 'react-i18next';
import IconsProvider from '@talend/react-components/lib/IconsProvider';
import Icon from '@talend/react-components/lib/Icon';
import Toggle from '@talend/react-components/lib/Toggle';
import LabelToggle from '@talend/react-components/lib/Toggle/LabelToggle';
import HeaderBar from '@talend/react-components/lib/HeaderBar';
import Generator from '../Generator';
import OpenAPIWizard from '../OpenAPI';
import TalendAPITesterWizard from '../TalendAPITester';
import DatastoreContext from '../../DatastoreContext';
import DatasetContext from '../../DatasetContext';
import ComponentsContext from '../../ComponentsContext';
import ProjectContext from '../../ProjectContext';

import theme from './App.scss';

function ModeSwitcher (props) {
	const currentPath = (props.history.location.pathname || '/');
	if (currentPath.startsWith('/openapi')) {
        props.mode = 'openapi';
    } else if (currentPath.startsWith('/apitester')) {
        props.mode ='apitester';
    } else {
        props.mode = 'generic';
    }
	return (
		<React.Fragment>
			<Toggle.Label
                id="starter-mode-switcher"
                autoFocus
            	values={[
            		{ value: 'generic',   label: 'Standard', dataFeature: 'generic' },
            		{ value: 'openapi',   label: 'OpenAPI', dataFeature: 'openapi' },
            		{ value: 'apitester', label: 'Talend API Tester', dataFeature: 'apitest' },
            	]}
            	value={props.mode}
            	onChange={(val) => {
                    if (val == "openapi") {
                        props.history.push('/openapi/project');
                    } else if (val == "apitester") {
                        props.history.push('/apitester/project');
                    } else {
                      	props.history.push('/');
                    }
                    props.mode = val;
                }}
            />
		</React.Fragment>
	);
}
const ModeSwitcherRouterAware = withRouter(ModeSwitcher);

function App (props) {
	return (
		<Router>
			<div className={theme.App}>
				<IconsProvider />

				<div className={theme.header}>
					<HeaderBar
						id='header-bar'
						logo={{ isFull: true }}
						brand={{
							label: 'Starter Toolkit',
						}}
						user={{
							className: theme.switcher,
						}}
						getComponent={component => {
							if (component == 'User') {
								return ModeSwitcherRouterAware;
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
									<Switch>
										<Route path="/openapi" component={OpenAPIWizard} />
										<Route path="/apitester" component={TalendAPITesterWizard} />
										<Route component={Generator} />
									</Switch>
								</ComponentsContext.Provider>
							</DatasetContext.Provider>
						</DatastoreContext.Provider>
					</ProjectContext.Provider>
				</div>
			</div>
		</Router>
	);
}

export default withTranslation('Help')(App);

App.propTypes = {
	t: PropTypes.func,
};
