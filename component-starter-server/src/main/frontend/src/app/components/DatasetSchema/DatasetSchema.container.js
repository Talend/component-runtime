/**
 *  Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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
// import classnames from 'classnames';
import PropTypes from 'prop-types';
//
// import SelectDataset from '../SelectDataset';
import DatastoreContext from '../../DatastoreContext';
import Schema from '../Schema';
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
