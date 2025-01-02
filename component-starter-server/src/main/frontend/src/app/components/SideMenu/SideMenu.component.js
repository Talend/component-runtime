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
import PropTypes from 'prop-types';
import classnames from 'classnames';
import { Link, useLocation } from 'react-router-dom';
import { ActionButton } from '@talend/react-components/lib/Actions';

import ComponentsContext from '../../ComponentsContext';
import DatastoreContext from '../../DatastoreContext';
import DatasetContext from '../../DatasetContext';

import theme from './SideMenu.module.scss';

function activateIO(service, datastore, dataset) {
	return (event) => {
		event.preventDefault();
		service.activateIO(datastore, dataset);
	};
}

function SideMenu() {
	const pathname = useLocation().pathname;
	const components = React.useContext(ComponentsContext.raw);
	const dataset = React.useContext(DatasetContext.raw);
	const datastore = React.useContext(DatastoreContext.raw);
	return (
		<nav className={theme.menu}>
			<ol>
				<li
					className={classnames({
						[theme.active]: pathname === '/' || pathname === '/project',
					})}
				>
					<Link to="/project" id="step-start">
						Start
					</Link>
				</li>
				{components.withIO ? (
					<>
						<li
							id="step-datastore"
							className={classnames({
								[theme.active]: pathname === '/datastore',
							})}
						>
							<Link to="/datastore">Datastore</Link>
						</li>
						<li
							id="step-dataset"
							className={classnames({
								[theme.active]: pathname === '/dataset',
							})}
						>
							<Link to="/dataset">Dataset</Link>
						</li>
					</>
				) : (
					<li id="step-activate-io">
						<a href="#/createNew" onClick={activateIO(components, datastore, dataset)}>
							Activate IO
						</a>
					</li>
				)}
				{components.components.map((component, i) => (
					<li
						id={`step-component-${i}`}
						className={classnames({
							[theme.active]: pathname === `/component/${i}`,
						})}
						key={i}
					>
						<ActionButton
							className="btn-icon-only btn-sm"
							bsStyle="link"
							hideLabel
							icon="talend-trash"
							label="Delete"
							onClick={() => components.deleteComponent(i)}
						/>
						<Link to={`/component/${i}`}>{component.configuration.name}</Link>
					</li>
				))}
				<li id="step-add-component">
					<Link to="/add-component">Add A Component</Link>
				</li>
				<li
					id="step-finish"
					className={classnames({
						[theme.active]: pathname === '/export',
					})}
				>
					<Link to="/export" id="go-to-finish-button">
						Finish
					</Link>
				</li>
			</ol>
		</nav>
	);
}

SideMenu.displayName = 'SideMenu';

export default SideMenu;
