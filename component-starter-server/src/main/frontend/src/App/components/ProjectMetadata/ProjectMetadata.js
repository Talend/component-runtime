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
import PropTypes from 'prop-types';
import { Actions, Icon } from '@talend/react-components';

import Help from '../Help';
import FacetSelector from '../FacetSelector';
import CategorySelector from '../CategorySelector';
import Input from '../Input';
import ProjectContext from '../../ProjectContext';

import theme from './ProjectMetadata.scss';

/* eslint-disable no-param-reassign */

export default class ProjectMetadata extends React.Component {
	static propTypes = {
		project: PropTypes.object,
	};
	constructor(props) {
		super(props);
		this.state = {
			project: props.project,
			buildToolActions: [],
			facets: {},
			view: {
				light: true,
			},
		};
	}

	// componentWillReceiveProps(nextProps) {
	// 	this.setState({ project: nextProps.project });
	// }

	// componentWillMount() {
	// 	fetch(`${CONFIGURATION_URL}`)
	// 		.then(resp => resp.json())
	// 		.then(payload => {
	// 			this.setState(current => {
	// 				current.configuration = payload;

	// 				payload.buildTypes.forEach(item =>
	// 					current.buildToolActions.push({
	// 						label: item,
	// 						onClick: () =>
	// 							this.setState(state => {
	// 								state.project.buildType = item;
	// 								state.buildToolActions.forEach(entry => {
	// 									if (entry.label === item) {
	// 										entry.className = theme.selected;
	// 									} else {
	// 										delete entry.className;
	// 									}
	// 								});
	// 							}),
	// 					}),
	// 				);

	// 				// select one build tool, preferrably maven since it is the standard
	// 				const maven = current.buildToolActions.filter(i => i.label === 'Maven');
	// 				if (maven.length > 0) {
	// 					const mvnIdx = current.buildToolActions.indexOf(maven[0]);
	// 					const saved = current.buildToolActions[0];
	// 					current.buildToolActions[0] = maven[0];
	// 					current.buildToolActions[mvnIdx] = saved;
	// 				} else {
	// 					current.project.buildType = current.buildToolActions[0].label;
	// 				}

	// 				const currentBuildTool = current.project.buildType || 'Maven';
	// 				const selected = current.buildToolActions.filter(i => i.label === currentBuildTool);
	// 				if (selected.length > 0) {
	// 					selected[0].className = theme.selected;
	// 				} else {
	// 					current.buildToolActions[0].className = theme.selected;
	// 				}
	// 				return Object.assign({}, current);
	// 			});
	// 		});
	// }

	onCategoryUpdate(value) {
		this.setState(current => (current.project.category = value.value));
	}

	showAll(event) {
		this.setState(current => (current.view.light = false));
		event.preventDefault();
	}

	showLight(event) {
		this.setState(current => (current.view.light = true));
		event.preventDefault();
	}

	render() {
		return (
			<ProjectContext.Consumer>
				{project => (
					<div className={theme.ProjectMetadata}>
						<div className={theme.main}>
							<div className={theme['form-row']}>
								<p className={theme.title}>Create a Talend Component Family Project</p>
								<div>
									<Actions
										actions={project.configuration.buildTypes.map(label => ({
											label,
											bsStyle: project.project.buildType === label ? 'info' : 'default',
											className: project.project.buildType !== label ? 'btn-inverse' : '',
											onClick: () => {
												project.selectBuildTool(label);
											},
										}))}
									/>
									<Help
										title="Build Tool"
										i18nKey="project_build_tool"
										content={
											<span>
												<p>
													Maven is the most commonly used build tool and Talend Component Kit
													integrates with it smoothly.
												</p>
												<p>
													Gradle is less used but get more and more attraction because it is
													communicated as being faster than Maven.
												</p>
												<p>
													<Icon name="talend-warning" /> Talend Component Kit does not provide as
													much features with Gradle than with Maven. The components validation is
													not yet supported for instance.
												</p>
											</span>
										}
									/>
								</div>
							</div>

							<div className={theme['form-row']}>
								{!!project.configuration && (
									<FacetSelector
										facets={project.configuration.facets}
										selected={project.project.facets}
									/>
								)}
							</div>

							<div className={theme['form-row']}>
								<p className={theme.title}>Component Metadata</p>
								<form noValidate onSubmit={e => e.preventDefault()}>
									<div className="field">
										<label htmlFor="projectFamily">Component Family</label>
										<Help
											title="Family"
											i18nKey="project_family"
											content={
												<span>
													<p>The family groups multiple components altogether.</p>
													<p>
														<Icon name="talend-info-circle" /> It is recommended to use a single
														family name per component module. The name must be a valid java name (no
														space, special characters, ...).
													</p>
												</span>
											}
										/>
										<Input
											className="form-control"
											id="projectFamily"
											type="text"
											placeholder="Enter the component family..."
											required="required"
											aggregate={project.project}
											accessor="family"
										/>
									</div>
									<div className="field">
										<label htmlFor="projectCategory">Category</label>
										<Help
											title="Category"
											i18nKey="project_category"
											content={
												<span>
													<p>
														The category is a group used by the Studio to organize components of
														different families in the same bucket into the <code>Palette</code>.
													</p>
													<p>
														It is recommended to use a two level category. The first level is
														generally very general and the second one is close to the family name.
													</p>
													<Icon name="talend-info-circle" /> The names must be valid java names (no
													space, special characters, ...).
												</span>
											}
										/>
										<CategorySelector
											initialValue={project.project.category}
											onChange={value => this.onCategoryUpdate(value)}
										/>
									</div>
								</form>
							</div>

							<p className={[theme.title, theme['form-row']].join(' ')}>Project Metadata</p>
							<form noValidate onSubmit={e => e.preventDefault()}>
								<div className="field">
									<label htmlFor="projectGroup">Group</label>
									<Help
										title="Project Group"
										i18nKey="project_group"
										content={
											<span>
												<p>
													The project group used when deployed on a repository (like a Nexus or
													central).
												</p>
												<p>
													The best practice recommends to use the reversed company hostname suffixed
													with something specific to the project.
												</p>
												<p>
													Example: <code>company.com</code> would lead to <code>com.company</code>{' '}
													package and for a component the used package would be, for instance,{' '}
													<code>com.company.talend.component</code>.
												</p>
											</span>
										}
									/>
									<Input
										className="form-control"
										id="projectGroup"
										type="text"
										placeholder="Enter the project group..."
										required="required"
										aggregate={this.state.project}
										accessor="group"
									/>
								</div>
								<div className="field">
									<label htmlFor="projectArtifact">Artifact</label>
									<Help
										title="Project Artifact"
										i18nKey="project_artifact"
										content={
											<span>
												<p>
													The project artifact used when deployed on a repository (like a Nexus or
													central).
												</p>
												<p>It must be a unique identifier in the group namespace.</p>
												<p>
													Talend recommendation is to follow the pattern{' '}
													<code>
														${'{'}component{'}'}-component
													</code>{' '}
													but you can use whatever you want.
												</p>
											</span>
										}
									/>
									<Input
										className="form-control"
										id="projectArtifact"
										type="text"
										placeholder="Enter the project artifact..."
										required="required"
										aggregate={this.state.project}
										accessor="artifact"
									/>
								</div>
								<div className="field">
									<label htmlFor="projectPackage">Package</label>
									<Help
										title="Project Root package"
										i18nKey="project_package"
										content={
											<span>
												<p>The root package represents a unique namespace in term of code.</p>
												<p>Talend recommendation is to align it on the selected group.</p>
											</span>
										}
									/>
									<Input
										className="form-control"
										id="projectPackage"
										type="text"
										placeholder="Enter the project base package..."
										required="required"
										aggregate={this.state.project}
										accessor="packageBase"
									/>
								</div>

								{this.state.view.light && (
									<a
										href="#/see-more"
										className="field"
										onClick={e => {
											e.preventDefault();
											this.showAll(e);
										}}
									>
										<Icon name="talend-plus-circle" />
										<span>See more options</span>
									</a>
								)}

								{!this.state.view.light && [
									<div className="field">
										<label htmlFor="projectVersion">Version</label>
										<Help
											title="Project Version"
											i18nKey="project_version"
											content={
												<span>
													<p>The version to use when deploying the artifact.</p>
													<p>
														Generally this generator is used for a first version so the default
														should fit without modification.
													</p>
												</span>
											}
										/>
										<Input
											className="form-control"
											id="projectVersion"
											type="text"
											placeholder="Enter the project group..."
											aggregate={this.state.project}
											accessor="version"
										/>
									</div>,
									<div className="field">
										<label htmlFor="projectName">Project Name</label>
										<Help
											title="Project Name"
											i18nKey="project_name"
											content={
												<span>
													<p>
														Giving a human readable name to the project is more friendly in an IDE
														or continuous integration platform.
													</p>
												</span>
											}
										/>
										<Input
											className="form-control"
											id="projectName"
											type="text"
											placeholder="Enter the project name..."
											aggregate={this.state.project}
											accessor="name"
										/>
									</div>,
									<div className="field">
										<label htmlFor="projectDescription">Project Description</label>
										<Help
											title="Project Description"
											i18nKey="project_description"
											content={
												<span>
													<p>
														Giving a human readable description to the project allows to share some
														goals of the project with other developers in a standard fashion.
													</p>
												</span>
											}
										/>
										<Input
											className="form-control"
											id="projectDescription"
											type="text"
											placeholder="Enter the project description..."
											aggregate={this.state.project}
											accessor="description"
										/>
									</div>,
									<a
										href="#/show-less"
										className="field"
										onClick={e => {
											e.preventDefault();
											this.showLight(e);
										}}
									>
										<Icon name="talend-zoomout" />
										<span>See less options</span>
									</a>,
								]}
							</form>
						</div>
					</div>
				)}
			</ProjectContext.Consumer>
		);
	}
}
