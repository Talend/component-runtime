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
import Icon from '@talend/react-components/lib/Icon';
import { Action } from '@talend/react-components/lib/Actions';

import Help from '../Help';
import FacetSelector from '../FacetSelector';
import BuildTypeSelector from '../BuildTypeSelector';
import CategorySelector from '../CategorySelector';
import Input from '../Input';
import ProjectContext from '../../ProjectContext';

import theme from './ProjectMetadata.module.scss';

function onCategoryUpdate(value, project) {
	// eslint-disable-next-line no-param-reassign
	project.project.category = value.value;
}
/* eslint-disable no-param-reassign */

export default class ProjectMetadata extends React.Component {
	static propTypes = {
		project: PropTypes.object,
		hideFacets: PropTypes.bool,
		hideCategory: PropTypes.bool,
	};
	constructor(props) {
		super(props);
		this.state = {
			project: props.project,
			buildToolActions: [],
			facets: {},
			showAll: false,
		};
		this.showAll = this.showAll.bind(this);
		this.showLight = this.showLight.bind(this);
	}

	showAll(event) {
		this.setState({ showAll: true });
		event.preventDefault();
	}

	showLight(event) {
		this.setState({ showAll: false });
		event.preventDefault();
	}

	render() {
		return (
			<ProjectContext.Consumer>
				{(project) => (
					<div className={theme.ProjectMetadata}>
						<h1>{this.props.title || 'Create a Talend Component Family Project'}</h1>
						<form className={theme.main} noValidate>
							<BuildTypeSelector project={project} />

							{!this.props.hideFacets && (
								<div className="form-group">
									{!!project.configuration && (
										<FacetSelector
											facets={project.configuration.facets}
											selected={project.project.facets}
										/>
									)}
								</div>
							)}

							<div className="form-group">
								<h2>Component Metadata</h2>
								<div className="form-group">
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
										required
										aggregate={project.project}
										accessor="family"
									/>
								</div>
								{!this.props.hideCategory && (
									<div className="form-group">
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
											onChange={(value) => onCategoryUpdate(value, project)}
										/>
									</div>
								)}
							</div>

							<h2>Project Metadata</h2>
							<div className="form-group">
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
									required
									aggregate={project.project}
									accessor="group"
								/>
							</div>
							<div className="form-group">
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
									required
									aggregate={project.project}
									accessor="artifact"
								/>
							</div>
							<div className="form-group">
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
									required
									aggregate={project.project}
									accessor="packageBase"
								/>
							</div>

							{!this.state.showAll && (
								<Action
									bsStyle="link"
									onClick={this.showAll}
									label="See more options"
									className="btn-xs"
									icon="talend-plus-circle"
								/>
							)}

							{this.state.showAll && (
								<>
									<div className="form-group">
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
											placeholder="Enter the project version..."
											aggregate={project.project}
											accessor="version"
										/>
									</div>
									<div className="form-group">
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
											aggregate={project.project}
											accessor="name"
										/>
									</div>
									<div className="form-group">
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
											aggregate={project.project}
											accessor="description"
										/>
									</div>
									<Action
										icon="talend-zoomout"
										label="See less options"
										bsStyle="link"
										className="btn-xs"
										onClick={this.showLight}
									/>
								</>
							)}
						</form>
					</div>
				)}
			</ProjectContext.Consumer>
		);
	}
}
