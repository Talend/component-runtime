import React from 'react';
import PropTypes from 'prop-types';
import Schema from '../../Component/Schema';
import ComponentsContext from '../../ComponentsContext';
import DatasetContext from '../../DatasetContext';

function getReference(dataset) {
	return {
		$id: dataset.$id,
		name: dataset.name,
	};
}

function onChangeValidate(schema) {
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


function ComponentSchema(props) {
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
						schema={props.component.source.configurationStructure}
						onChangeValidate={onChangeValidate}
						readOnly
						name="configuration"
						references={references}
						addRefNewLocation={addRefNewLocation}
						extraTypes={['dataset']}
					/>
				);
			}}
		</DatasetContext.Consumer>
	);
}

ComponentSchema.displayName = 'ComponentSchema';
ComponentSchema.propTypes = {
	component: PropTypes.object,
};

export default ComponentSchema;
