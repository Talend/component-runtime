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

import theme from './Generator.scss';

import DatasetContext from '../dataset';
import ProjectMetadata from './ProjectMetadata';
import Component from './Component';
import Finish from './Finish';

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
			datasets: [ { id: 'jdbc', name: 'JDBC' }],
		};
		['isComponentStep', 'onClickSetStep', 'onAddComponent', 'onGoToFinishPage'].forEach(
			action => (this[action] = this[action].bind(this)),
		);
	}

	onAddComponent() {
		let component = {
			configuration: {
				name: `CompanyComponent${this.state.components.length + 1}`,
			},
			source: {
				genericOutput: true,
				stream: false,
				configurationStructure: {
					entries: [],
				},
				outputStructure: {
					entries: [],
				},
			},
			processor: {
				configurationStructure: {
					entries: [],
				},
				inputStructures: [
					{
						name: 'MAIN',
						generic: false,
						structure: {
							entries: [],
						},
					},
				],
				outputStructures: [],
			},
		};
		this.setState(state => {
			state.components.push(component);
			state.currentStep = state.components.length;
			return Object.assign({}, state);
		});
	}

	updateComponent(component) {
		this.setState({ components: [].concat(this.state.components) });
	}

	deleteComponent(index) {
		this.setState(state => {
			var idx = state.components.indexOf(comp[0].component.props.component);
			if (idx >= 0) {
				state.components.splice(idx, 1);
			}
			return Object.assign({}, state);
		});
	}

	onGoToFinishPage() {
		this.setState({ currentStep: this.state.components.length + 1 });
	}

	onClickSetStep(event, step) {
		event.preventDefault();
		this.setState({ currentStep: step });
	}

	isComponentStep(index) {
		return index > 0 && index !== this.state.components.length + 1;
	}

	render() {
		let mainContent = null;
		if (this.state.currentStep === 0) {
			mainContent = (
				<ProjectMetadata
					project={this.state.project}
					buildTypes={this.state.configuration.buildTypes}
				/>
			);
		} else if (this.isComponentStep(this.state.currentStep)) {
			const component = this.state.components[this.state.currentStep - 1];
			mainContent = (
				<Component
					component={component}
					onChange={() => this.updateComponent(component)}
				/>
			);

		} else if (this.state.currentStep === this.state.components.length + 1) {
			mainContent = (
				<Finish project={this.state.project} components={() => this.state.components} />
			);
		}
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
									onClick={e => this.onClickSetStep(e, 0)}
								>
									<section-label>Start</section-label>
								</li>
								{this.state.components.map((component, i) => {
									return (
										<li
											className={classnames({
												[theme.active]: this.state.currentStep === i + 1,
											})}
											onClick={e => this.onClickSetStep(e, i + 1)}
											key={i}
										>
											<section-label>
												{component.configuration.name}
											</section-label>
											<trash-icon onClick={() => this.deleteComponent(i)}>
												<Icon name="talend-trash" />
											</trash-icon>
										</li>
									);
								})}
								<li
									className={classnames({
										[theme.active]: this.state.currentStep === this.state.components.length + 1,
									})}
									onClick={e => this.onClickSetStep(e, this.state.components.length + 1)}
								>
									<section-label>Start</section-label>
								</li>

							</ol>
						</nav>
					</div>
					<div className={theme.content}>
						<main>
							<DatasetContext.Provider value={this.state.datasets}>
								{mainContent}
							</DatasetContext.Provider>
						</main>
						{this.state.currentStep !== this.state.components.length + 1 && (
							<footer>
								<Action
									id="add-component-button"
									label="Add A Component"
									bsStyle="info"
									onClick={() => this.onAddComponent()}
								/>
								<Action
									id="go-to-finish-button"
									label="Go to Finish"
									bsStyle="primary"
									onClick={() => this.onGoToFinishPage()}
								/>
							</footer>
						)}
					</div>
				</div>
			</div>
		);
	}
}
