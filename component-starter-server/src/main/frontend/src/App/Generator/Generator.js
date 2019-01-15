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
import classnames from 'classnames';
import { Action, Icon } from '@talend/react-components';
import { Route, Link } from 'react-router-dom';

import theme from './Generator.scss';

import DatasetContext from '../DatasetContext';
import ProjectMetadata from './ProjectMetadata';
import Component from './Component';
import Finish from './Finish';
import ComponentsContext from '../ComponentsContext';

export default class Generator extends React.Component {
	constructor(props) {
		super(props);

		let project = {
			buildType: 'Maven',
			version: '0.0.1-SNAPSHOT',
			group: 'com.company',
			artifact: 'company-component',
			name: 'A Component Family',
			description: 'A generated component project',
			packageBase: 'com.company.talend.components',
			family: 'CompanyFamily',
			category: 'Misc',
			facets: [],
		};

		this.state = {
			currentStep: 0,
			project: project,
			configuration: {
				buildTypes: [],
			},
			components: [],
			datastores: [],
			datasets: [
				{
					name: 'JDBC',
					structure: {
						entries: [], // same as component.configurationStructure.entries
					},
				},
			],
		};
		// ['isComponentStep', 'onClickSetStep', 'onAddComponent', 'onGoToFinishPage'].forEach(
		// 	action => (this[action] = this[action].bind(this)),
		// );
	}

	// updateComponent(component) {
	// 	this.setState({ components: [].concat(this.state.components) });
	// }

	// deleteComponent(index) {
	// 	this.setState(state => {
	// 		var idx = state.components.indexOf(comp[0].component.props.component);
	// 		if (idx >= 0) {
	// 			state.components.splice(idx, 1);
	// 		}
	// 		return Object.assign({}, state);
	// 	});
	// }

	// isComponentStep(index) {
	// 	return index > 2 && index !== this.state.components.length + 3;
	// }

	render() {
		// let mainContent = null;
		// if (this.state.currentStep === 0) {
		// 	mainContent = (
		// 		<ProjectMetadata
		// 			project={this.state.project}
		// 			buildTypes={this.state.configuration.buildTypes}
		// 		/>
		// 	);
		// } else if (this.isComponentStep(this.state.currentStep)) {
		// 	const component = this.state.components[this.state.currentStep - 3];
		// 	mainContent = (
		// 		<Component component={component} onChange={() => this.updateComponent(component)} />
		// 	);
		// } else if (this.state.currentStep === this.state.components.length + 3) {
		// 	mainContent = (
		// 		<Finish project={this.state.project} components={() => this.state.components} />
		// 	);
		// }
		return (
			<div className={theme.Generator}>
				<div className={theme.container}>
					<div className={theme.wizard}>
						<nav>
							<ol>
								<li
									className={classnames({
										[theme.active]: this.state.currentStep === 0,
									})}
								>
									<Link to="/project">Start</Link>
								</li>
								<li
									className={classnames({
										[theme.active]: this.state.currentStep === 1,
									})}
								>
									<Link to="/datastore">Datastore</Link>
								</li>
								<li
									className={classnames({
										[theme.active]: this.state.currentStep === 2,
									})}
								>
									<Link to="/dataset">Dataset</Link>
								</li>
								<ComponentsContext.Consumer>
									{components =>
										components.components.map((component, i) => (
											<li
												className={classnames({
													[theme.active]: this.state.currentStep === i + 3,
												})}
												key={i}
											>
												<Link to={`/component/${i}`}>{component.configuration.name}</Link>
											</li>
										))
									}
								</ComponentsContext.Consumer>
								<li
									className={classnames({
										[theme.active]: this.state.currentStep === this.state.components.length + 3,
									})}
								>
									<Link to="/export">Finish</Link>
								</li>
							</ol>
						</nav>
					</div>
					<div className={theme.content}>
						<main>
							<DatasetContext.Provider value={this.state.datasets}>
								<Route exact path="/" component={ProjectMetadata} />
								<Route exact path="/project" component={ProjectMetadata} />
								<Route exact path="/datastore" component={ProjectMetadata} />
								<Route exact path="/dataset" component={ProjectMetadata} />
								<Route path="/component/:componentId" component={Component} />
								<Route path="/export" component={Finish} />
							</DatasetContext.Provider>
						</main>
						{this.state.currentStep !== this.state.components.length + 3 && (
							<footer>
								<ComponentsContext.Consumer>
									{components => (
										// <Action
										// 	id="add-component-button"
										// 	label="Add A Component"
										// 	bsStyle="info"
										// 	onClick={() => components.addComponent()}
										// />
										<Link to="/component/last" className="btn btn-info" onClick={() => components.addComponent()}>Add A Component</Link>
									)}
								</ComponentsContext.Consumer>
								<Link to="/export" className="btn btn-primary">Go to Finish</Link>
							</footer>
						)}
					</div>
				</div>
			</div>
		);
	}
}
