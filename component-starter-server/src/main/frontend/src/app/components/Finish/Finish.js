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
import { Action } from '@talend/react-components/lib/Actions';
import Dialog from '@talend/react-components/lib/Dialog';
import Toggle from '@talend/react-components/lib/Toggle';

import Help from '../Help';
import Input from '../Input';
import Summary from '../Summary';
import {
	GENERATOR_GITHUB_URL,
	COMPONENT_TYPE_SOURCE,
} from '../../constants';
import FinishContext from './FinishContext';

import theme from './Finish.scss';

function isEmpty(str) {
	return !str || str.trim().length === 0;
}

function createModel({ project, components, datastore, dataset }, openapi) {
	// we copy the model to compute sources and processors attributes
	const lightCopyModel = Object.assign({}, project.project);
	if (!openapi) {
		lightCopyModel.datastores = datastore.datastores;
		lightCopyModel.datasets = dataset.datasets;
		lightCopyModel.sources = components.components
			.filter(c => c.type === COMPONENT_TYPE_SOURCE)
			.map(c => {
				const source = Object.assign({}, c.source);
				source.name = c.configuration.name;
				return source;
			});
		lightCopyModel.processors = components.components
			.filter(
				c => c.processor.outputStructures.length !== 0 || c.processor.inputStructures.length !== 0,
			)
			.map(c => {
				const processor = Object.assign({}, c.processor);
				processor.name = c.configuration.name;
				return processor;
			});
	} else {
		if (!project.$$openapi) {
			lightCopyModel.openapi = { version: '3.0.0' }; // surely to replace with an error message?
		} else {
			try {
				const model = JSON.parse(project.$$openapi.openapi.trim());
				const paths = model.paths || {};
                const selectedEP = project.$$openapi.selectedEndpoints;
				lightCopyModel.openapi = {
				    ...{ version: '3.0.0' },
					...model,
					paths: Object.keys(paths)
						.map(path => ({
							key: path,  // endpoint
							value: Object.keys(paths[path]) // all verbs
								.filter(endpointVerb => selectedEP
									    .filter(it => it.verb + "_" + it.path == endpointVerb + "_"+ path)
							            .length == 1)
							    .reduce((agg, endpointVerb) => {
								    agg[endpointVerb] = paths[path][endpointVerb];
									return agg;
								}, {}),
						}))
						.reduce((agg, value) => {
							if (Object.keys(value.value).length > 0) {
								agg[value.key] = value.value;
							}
							return agg;
						}, {}),
				};
			} catch (e) {
				lightCopyModel.openapi = { version: '3.1.0' }; // todo: same as previous branch
			}
		}
		lightCopyModel.category = 'Cloud';
		lightCopyModel.datastores = [];
		lightCopyModel.datasets = [];
		lightCopyModel.sources = [];
		lightCopyModel.processors = [];
	}
	return lightCopyModel;
}

function getDownloadValue(model) {
	return btoa(JSON.stringify(model));
}

export default class Finish extends React.Component {
	constructor(props) {
		super(props);

		this.state = {
			github: {
				username: '',
				password: '',
				organization: '',
				// repository: model.artifact,
				useOrganization: true,
			},
		};

		[
			'onToggleOrganization',
			'onHideGithub',
			'onHideResults',
			'closeModal',
			'notifyProgressDone',
			'onGithub',
			'onClickCreateGithub',
		].forEach(i => (this[i] = this[i].bind(this)));
	}

	onClearGithubModal() {
		this.setState({ githubError: undefined, current: undefined });
	}

	onGithub(model) {
		return () => {
			if (
				isEmpty(this.state.github.username) ||
				isEmpty(this.state.github.password) ||
				(this.state.github.useOrganization && isEmpty(this.state.github.organization)) ||
				isEmpty(this.state.github.repository)
			) {
				this.setState({
					githubError: 'Please fill the form properly before launching the project creation.',
				});
			} else {
				this.doGithub(model).then(this.notifyProgressDone, this.notifyProgressDone);
			}
		};
	}

	onHideResults() {
		this.notifyProgressDone();
		this.closeModal();
	}
	onHideGithub() {
		this.notifyProgressDone();
		this.onClearGithubModal();
	}

	onClickCreateGithub() {
		this.setState({ current: 'github' });
	}
	onToggleOrganization() {
		this.setState(state => {
			// eslint-disable-next-line no-param-reassign
			state.github.useOrganization = !state.github.useOrganization;
			return Object.assign({}, state);
		});
	}

	closeModal() {
		this.setState({ current: undefined });
	}

	notifyProgressDone() {
		this.setState({ progress: undefined });
	}

	doGithub(model) {
		this.setState({ progress: 'github' });
		return fetch(`${GENERATOR_GITHUB_URL}`, {
			method: 'POST',
			body: JSON.stringify({ model, repository: this.state.github }),
			headers: new Headers({ Accept: 'application/json', 'Content-Type': 'application/json' }),
		}).then(d => {
			if (d.status > 299) {
				this.setState({
					statusOK: false,
					error: d.statusText,
				});
			} else {
				this.setState({
					statusOK: true,
				});
			}
		});
	}

	render() {
		const fieldClasses = classnames('form-group', theme.field);
		return (
			<FinishContext.Provider>
				<FinishContext.Consumer>
					{services => {
						const projectModel = createModel(services, !!this.props.openapi);
						return (
							<div className={theme.Finish}>
							<h2>Project Summary</h2>
							<Summary project={projectModel} useOpenAPI={this.props.openapi} openapi={services.project.$$openapi || { selectedEndpoints: [] }} />
							<div className={theme.bigButton}>
								<form id="download-zip-form" noValidate action={this.props.actionUrl} method="POST">
									<input type="hidden" name="project" value={getDownloadValue(projectModel)} />
									<Action
										label="Download as ZIP"
										form="download-zip-form"
										bsStyle="info"
										icon="fa-file-archive-o"
										type="submit"
										inProgress={this.state.progress === 'zip'}
										disabled={!!this.state.progress && this.state.progress !== 'zip'}
										className="btn btn-lg"
									/>
								</form>
								<form noValidate onSubmit={e => e.preventDefault()}>
									<Action
										label="Create on Github"
										bsStyle="primary"
										onClick={this.onClickCreateGithub}
										icon="fa-github"
										inProgress={this.state.progress === 'github'}
										disabled={!!this.state.progress && this.state.progress !== 'github'}
										className="btn btn-lg"
									/>
								</form>
							</div>
							<Dialog
								show={this.state.current === 'github'}
								header="Github Configuration"
								size="small"
								onHide={this.onHideGithub}
								action={{
									label: 'Create on Github',
									onClick: this.onGithub(projectModel),
									bsStyle: 'primary',
								}}
							>
								<form noValidate onSubmit={e => e.preventDefault()} className={theme.modal}>
									{!!this.state.githubError && (
										<p className={theme.error}>{this.state.githubError}</p>
									)}
									<div className={fieldClasses}>
										<label htmlFor="githubUser">User</label>
										<Help
											title="Github User"
											i18nKey="finish_github_user"
											content={
												<span>
													<p>The Github username to use to create the project.</p>
												</span>
											}
										/>
										<Input
											className="form-control"
											id="githubUser"
											type="text"
											placeholder="Enter your Github username..."
											required
											aggregate={this.state.github}
											accessor="username"
										/>
									</div>
									<div className={fieldClasses}>
										<label htmlFor="githubPassword">Password</label>
										<Help
											title="Github Password"
											i18nKey="finish_github_password"
											content={
												<span>
													<p>The Github password to use to create the project.</p>
												</span>
											}
										/>
										<Input
											className="form-control"
											id="githubPassword"
											type="password"
											placeholder="Enter your Github password..."
											required
											aggregate={this.state.github}
											accessor="password"
										/>
									</div>

									{!!this.state.github.useOrganization && (
										<div className={fieldClasses}>
											<label htmlFor="githubOrganization">Organization</label>
											<Help
												title="Github Organization"
												i18nKey="finish_github_organization"
												content={
													<span>
														<p>The Github organization to use to create the project.</p>
													</span>
												}
											/>
											<Input
												className="form-control"
												id="githubOrganization"
												type="text"
												placeholder="Enter your Github organization..."
												required
												aggregate={this.state.github}
												accessor="organization"
											/>
										</div>
									)}
									<div className={fieldClasses}>
										<label htmlFor="githubRepository">Repository</label>
										<Help
											title="Github Repository"
											i18nKey="finish_github_repository"
											content={
												<span>
													<p>The Github repository to create for the project.</p>
												</span>
											}
										/>
										<Input
											className="form-control"
											id="githubRepository"
											type="text"
											placeholder="Enter the Github repository to create..."
											required
											aggregate={this.state.github}
											accessor="repository"
										/>
									</div>

									<div className={fieldClasses}>
										<label htmlFor="githubUseOrganization">
											Create the repository for an organization
										</label>
										<Help
											title="Github Use Organization"
											i18nKey="finish_github_useOrganization"
											content={
												<span>
													<p>
														If checked an organization project will be created instead of a user
														project.
													</p>
												</span>
											}
										/>
										<Toggle
											id="githubUseOrganization"
											checked={this.state.github.useOrganization}
											onChange={this.onToggleOrganization}
										/>
									</div>
									{this.state.error && (
										<div className={theme.error}>
											<p>{this.state.error.message || JSON.stringify(this.state.error)}</p>
										</div>
									)}
									{this.state.statusOK && (
										<div className="alert alert-success">
											<p>
												Project
												<a
													target="_blank"
													rel="noopener noreferrer"
													href={`https://github.com/${
														this.state.github.useOrganization
															? this.state.github.organization
															: this.state.github.username
													}/${this.state.github.repository}`}
												>
													{this.state.github.repository}
												</a>
												created with success!
											</p>
										</div>
									)}
								</form>
							</Dialog>
						</div>);
					}}
				</FinishContext.Consumer>
			</FinishContext.Provider>
		);
	}
}
