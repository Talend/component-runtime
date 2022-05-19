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
import classnames from 'classnames';
import { withRouter, Link } from 'react-router-dom';

import theme from '../SideMenu/SideMenu.scss';

function StepByStep(props) {
	return (
		<nav className={theme.menu}>
			<ol>
				<li className={classnames({ [theme.active]: props.location.pathname === '/apitester/project' })}>
					<Link to="/apitester/project" id="step-start">
						Start
					</Link>
				</li>
				<li id="step-apitester">
					<Link to="/apitester/design">
						API Tester
					</Link>
				</li>
				<li id="step-finish" className={classnames({ [theme.active]: props.location.pathname === '/apitester/export' })}>
					<Link to="/apitester/export" id="go-to-finish-button">
						Finish
					</Link>
				</li>
			</ol>
		</nav>
	);
}

export default withRouter(StepByStep);
