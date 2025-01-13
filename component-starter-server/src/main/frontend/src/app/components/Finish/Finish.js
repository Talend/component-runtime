/**
 *  Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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
	COMPONENT_TYPE_SOURCE,
} from '../../constants';
import FinishContext from './FinishContext';

import theme from './Finish.module.scss';
import { ButtonPrimary, ButtonSecondary } from '@talend/design-system';

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
			.filter((c) => c.type === COMPONENT_TYPE_SOURCE)
			.map((c) => {
				const source = Object.assign({}, c.source);
				source.name = c.configuration.name;
				return source;
			});
		lightCopyModel.processors = components.components
			.filter(
				(c) =>
					c.processor.outputStructures.length !== 0 || c.processor.inputStructures.length !== 0,
			)
			.map((c) => {
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
						.map((path) => ({
							key: path, // endpoint
							value: Object.keys(paths[path]) // all verbs
								.filter(
									(endpointVerb) =>
										selectedEP.filter((it) => it.verb + '_' + it.path == endpointVerb + '_' + path)
											.length == 1,
								)
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

		this.state = {};

	}

	render() {
		const fieldClasses = classnames('form-group', theme.field);
		return (
			<FinishContext.Provider>
				<FinishContext.Consumer>
					{(services) => {
						const projectModel = createModel(services, !!this.props.openapi);
						return (
							<div className={theme.Finish}>
								<h2>Project Summary</h2>
								<Summary
									project={projectModel}
									useOpenAPI={this.props.openapi}
									openapi={services.project.$$openapi || { selectedEndpoints: [] }}
								/>
								<div className={theme.bigButton}>
									<form
										id="download-zip-form"
										noValidate
										action={this.props.actionUrl}
										method="POST"
									>
										<input type="hidden" name="project" value={getDownloadValue(projectModel)} />
										<ButtonPrimary
											form="download-zip-form"
											icon="talend-file-o"
											type="submit"
											inProgress={this.state.progress === 'zip'}
											disabled={!!this.state.progress && this.state.progress !== 'zip'}
										>
											Download as ZIP
										</ButtonPrimary>
									</form>
								</div>
							</div>
						);
					}}
				</FinishContext.Consumer>
			</FinishContext.Provider>
		);
	}
}
