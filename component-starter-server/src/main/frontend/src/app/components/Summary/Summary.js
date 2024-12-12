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

import theme from './Summary.module.scss';

function Info(props) {
	return (
		<div className={theme.Info}>
			<label htmlFor={props.id}>{props.name}:</label>
			<span id={props.id}>{props.value}</span>
		</div>
	);
}

Info.propTypes = {
	id: PropTypes.string,
	name: PropTypes.string,
	value: PropTypes.string,
};

export default function Summary(props) {
	const p = props.project; // just to make it shorter in the template
	if (!p) {
		return <div />;
	}
	const outputLength = p.processors.filter((proc) => proc.outputStructures.length === 0).length;
	const processorsLength = p.processors.length - outputLength;
	const endpointsLength =
		props.openapi && props.openapi.selectedEndpoints ? props.openapi.selectedEndpoints.length : 0;
	return (
		<div className={theme.Summary}>
			<Info name="Name" value={p.name} />
			<Info name="Build Tool" value={p.buildType} />
			<Info name="Coordinates" value={`${p.group}:${p.artifact}:${p.version}`} />
			{!props.useOpenAPI && (
				<>
					<Info
						name="Datasets"
						value={`${p.datasets.length} datasets and ${p.datastores.length} datastores`}
					/>
					<Info
						name="Components"
						value={`${p.sources.length} inputs, ${processorsLength} processors and ${outputLength} outputs`}
					/>
				</>
			)}
			{!!props.useOpenAPI && (
				<Info name="Endpoints" value={`${endpointsLength} endpoints selected`} />
			)}
		</div>
	);
}

Summary.propTypes = {
	project: PropTypes.shape({
		name: PropTypes.string,
		buildType: PropTypes.string,
		group: PropTypes.string,
		artifact: PropTypes.string,
		version: PropTypes.string,
		sources: PropTypes.array,
		processors: PropTypes.array,
	}),
};
