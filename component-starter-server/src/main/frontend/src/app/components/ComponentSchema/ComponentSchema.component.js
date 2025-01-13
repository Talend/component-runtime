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
 */import React from 'react';
import PropTypes from 'prop-types';
import Schema from '../Schema';
// import ComponentsContext from '../../ComponentsContext';
import DatasetContext from '../../DatasetContext';

function getReference(dataset) {
	return {
		$id: dataset.$id,
		name: dataset.name,
	};
}

function validateWithDataset(schema) {
	const messages = [];
	let hasOneDataset = false;
	schema.entries.forEach(entry => {
		if (entry.type === 'dataset') {
			if (!entry.reference) {
				messages.push({
					type: 'error',
					message: `The attribute ${entry.name} has no dataset reference.`,
				});
			}
			hasOneDataset = true;
		}
	});
	if (!hasOneDataset) {
		messages.push({ type: 'error', message: 'A component model must have a dataset' });
	}
	return messages;
}


function validateNoOp() {
	return [];
}

function ComponentSchema(props) {
	let onChangeValidate = validateNoOp;
	const extraTypes = [];
	if (props.withDataset) {
		onChangeValidate = validateWithDataset;
		extraTypes.push('dataset');
	}
	return (
		<DatasetContext.Consumer>
			{dataset => {
				const references = {
					dataset: dataset.datasets.map(getReference),
				};
				const addRefNewLocation = {
					datastore: '/dataset',
				};
				return (
					<Schema
						schema={props.schema}
						onChangeValidate={onChangeValidate}
						readOnly
						name="configuration"
						references={references}
						addRefNewLocation={addRefNewLocation}
						extraTypes={extraTypes}
					/>
				);
			}}
		</DatasetContext.Consumer>
	);
}

ComponentSchema.displayName = 'ComponentSchema';
ComponentSchema.propTypes = {
	schema: PropTypes.object,
	withDataset: PropTypes.bool,
};

export default ComponentSchema;
