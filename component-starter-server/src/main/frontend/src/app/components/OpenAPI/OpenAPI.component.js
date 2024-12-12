/**
 *  Copyright (C) 2006-2024 Talend Inc. - www.talend.com
 *
 *  Licensed under the Apache License, Version 2.0 (the 'License');
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an 'AS IS' BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
import React from 'react';
import { Outlet, Route, Routes } from 'react-router-dom';
import AceEditor from 'react-ace';
import 'ace-builds/src-noconflict/mode-javascript';
import 'ace-builds/src-noconflict/theme-chrome';
import 'ace-builds/src-noconflict/ext-language_tools';
import { withTranslation } from 'react-i18next';
import { GENERATOR_OPENAPI_ZIP_URL } from '../../constants';
import Help from '../Help';
import ProjectContext from '../../ProjectContext';
import ProjectMetadata from '../ProjectMetadata';
import Finish from '../Finish';
import StepStep from './StepByStep';
import theme from './OpenAPI.module.scss';
import generatorTheme from '../Generator/Generator.module.scss';

const defaultOpenAPIJson = JSON.stringify(
	{
		openapi: '3.0.0',
		info: {
			title: 'Sample OpenAPI.json',
			description: 'Simple simplified specification to generate a HTTP source.',
		},
		paths: {
			'/demo': {
				get: {
					description: 'Demo endpoint for the generator',
					operationId: 'getDemo',
					parameters: [],
				},
			},
		},
	},
	' ',
	4,
);

function readEndpoint(spec, existingEndpoints) {
	if (!spec) {
		return [];
	}
	try {
		const json = JSON.parse(spec.trim());
		if (!json.paths) {
			return [];
		}
		return Object.keys(json.paths).flatMap((path) =>
			Object.keys(json.paths[path]).map((verb) => {
				const operationId = `${verb}_${path}`;
				const existing = existingEndpoints.filter((it) => it.operationId == operationId);
				const checked = existing.length == 1 ? existing[0].checked : true;
				return {
					operationId,
					verb,
					path,
					checked,
				};
			}),
		);
	} catch (e) {
		return existingEndpoints;
	}
}

class EndpointItem extends React.Component {
	constructor(props) {
		super(props);
		this.onChange = this.onChange.bind(this);
	}

	onChange(event) {
		this.props.onChange({
			endpoint: this.props,
			checked: !!event.target.checked,
			project: this.props.project,
		});
	}

	render() {
		return (
			<div className="form-group">
				<div className="checkbox">
					<label>
						<input type="checkbox" checked={this.props.checked} onChange={this.onChange} />
						<span>
							{this.props.operationId}: {this.props.verb} {this.props.path}
						</span>
					</label>
				</div>
			</div>
		);
	}
}

function EndpointsSelector(props) {
	return (
		<form className={theme.formPadding} noValidate onSubmit={() => false}>
			<h2>
				{props.t('openapi_list_title', { defaultValue: 'Select the endpoints to use:' })}
				<Help
					title="OpenAPI Design"
					i18nKey="openapi_overview"
					content={
						<div>
							<p>The right editor enables you to edit the OpenAPI document.</p>
							<p>
								The left list will list the available endpoints and let you select the ones you want
								to handle in your generated component.
							</p>
						</div>
					}
				/>
			</h2>

			{props.items &&
				props.items.map((it, idx) => (
					<EndpointItem
						key={it.operationId}
						{...it}
						onChange={props.onUpdate}
						project={props.project}
					/>
				))}
		</form>
	);
}

const EndpointsSelectorTrans = withTranslation('Help')(EndpointsSelector);

class OpenAPI extends React.Component {
	constructor(props) {
		super(props);
		this.onSpecChange = this.onSpecChange.bind(this);
		this.onEndpointUpdate = this.onEndpointUpdate.bind(this);
		this.state = { endpoints: [] }; // mainly to manage the refresh only
	}

	onSpecChange(spec, project) {
		const openapi = (spec || '').trim();
		const endpoints = readEndpoint(openapi, project.$$openapi.endpoints);
		project.$$openapi = {
			...project.$$openapi,
			openapi,
			endpoints,
			selectedEndpoints: endpoints.filter((it) => it.checked),
		};
		this.setState({
			endpoints,
		});
	}

	onEndpointUpdate({ endpoint, checked, project }) {
		const endpoints = project.$$openapi.endpoints.map((it) =>
			it.operationId == endpoint.operationId ? { ...it, checked } : it,
		);
		project.$$openapi = {
			...project.$$openapi,
			endpoints,
			selectedEndpoints: endpoints.filter((it) => it.checked),
		};
		this.setState({ endpoints });
	}

	render() {
		return (
			<ProjectContext.Consumer>
				{(project) => {
					if (!project.$$openapi) {
						// note: we could use an openapicontext here
						const endpoints = readEndpoint(defaultOpenAPIJson, []);
						project.$$openapi = {
							endpoints,
							selectedEndpoints: endpoints,
							openapi: defaultOpenAPIJson,
						};
					}

					return (
						<div className={`${theme.OpenAPI} row`}>
							<div className={theme.selector}>
								<EndpointsSelectorTrans
									items={project.$$openapi.endpoints}
									spec={project.$$openapi.openapi}
									onUpdate={this.onEndpointUpdate}
									project={project}
								/>
							</div>
							<div className={theme.editor}>
								<AceEditor
									mode="javascript"
									theme="chrome"
									name="openapi-ace-editor"
									value={project.$$openapi.openapi}
									width="auto"
									height="100%"
									setOptions={{
										enableBasicAutocompletion: true,
										enableLiveAutocompletion: true,
									}}
									onChange={(value) => this.onSpecChange(value, project)}
								/>
							</div>
						</div>
					);
				}}
			</ProjectContext.Consumer>
		);
	}
}

export const OpenAPITrans = withTranslation('Help')(OpenAPI);

export default class OpenAPIWizard extends React.Component {
	render() {
		return (
			<div className={generatorTheme.Generator}>
				<div className={generatorTheme.container}>
					<div className={generatorTheme.wizard}>
						<StepStep />
					</div>
					<div className={generatorTheme.content}>
						<main>
							<Outlet />
						</main>
					</div>
				</div>
			</div>
		);
	}
}
