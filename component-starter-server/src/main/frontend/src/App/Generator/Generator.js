/**
 *  Copyright (C) 2006-2019 Talend Inc. - www.talend.com
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
import { Route, Link } from 'react-router-dom';

import theme from './Generator.scss';

import ProjectMetadata from './ProjectMetadata';
import Component from './Component';
import Finish from './Finish';
import ComponentsContext from '../ComponentsContext';
import SideMenu from '../components/SideMenu';
import DatastoreList from '../components/DatastoreList';
import DatasetList from '../components/DatasetList';

export default function Generator() {
	return (
		<div className={theme.Generator}>
			<div className={theme.container}>
				<div className={theme.wizard}>
					<SideMenu />
				</div>
				<div className={theme.content}>
					<main>
						<Route exact path="/" component={ProjectMetadata} />
						<Route exact path="/project" component={ProjectMetadata} />
						<Route exact path="/datastore" component={DatastoreList} />
						<Route exact path="/dataset" component={DatasetList} />
						<Route path="/component/:componentId" component={Component} />
						<Route path="/export" component={Finish} />
					</main>
					{location.pathname !== '/export' && (
						<footer>
							<ComponentsContext.Consumer>
								{components => (
									<Link
										to="/component/last"
										className="btn btn-info"
										onClick={() => components.addComponent()}
									>
										Add A Component
									</Link>
								)}
							</ComponentsContext.Consumer>
							<Link to="/export" className="btn btn-primary">
								Go to Finish
							</Link>
						</footer>
					)}
				</div>
			</div>
		</div>
	);
}
