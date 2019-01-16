import React from 'react';
// import classnames from 'classnames';
import PropTypes from 'prop-types';
//
// import SelectDataset from '../SelectDataset';
import DatastoreContext from '../../DatastoreContext';
import Schema from '../../Component/Schema';
// import TileContext from '../../tile';

function getDatastoreReference(datastore) {
	return {
		$id: datastore.$id,
		name: datastore.name,
	};
}

function DatasetSchema(props) {
	return (
		<DatastoreContext.Consumer>
			{datastore => {
				const references = {
					datastore: datastore.datastores.map(getDatastoreReference),
				};
				const addRefNewLocation = {
					datastore: '/datastore',
				};
				return (
					<Schema
						schema={props.schema}
						onChangeValidate={props.onChangeValidate}
						readOnly
						extraTypes={['datastore']}
						references={references}
						addRefNewLocation={addRefNewLocation}
					/>
				);
			}}
		</DatastoreContext.Consumer>
	);
}

DatasetSchema.propTypes = {
	schema: PropTypes.object,
	onChangeValidate: PropTypes.func,
};

export default DatasetSchema;
