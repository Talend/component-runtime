import React from 'react';
import PropTypes from 'prop-types';

import { Action } from '@talend/react-components';
import DatastoreContext from '../../DatastoreContext';
import DatastoreForm from '../DatastoreForm';
import theme from './DatastoreList.scss';

function DatastoreList() {
	return (
		<DatastoreContext.Consumer>
			{datastore => (
				<div className={theme.container}>
					<DatastoreForm datastore={datastore.current} />
					<div>
						<h2>Datastore List</h2>
						<ul>
							{datastore.datastores.map((d, index) => (
								<li key={index}>
									<Action
										bsStyle="link"
										onClick={() => datastore.setCurrent(d)}
										label={d.name}
									/>
								</li>
							))}
						</ul>
					</div>
				</div>
			)}
		</DatastoreContext.Consumer>
	);
}

DatastoreList.displayName = 'DatastoreList';
DatastoreList.propTypes = {

};

export default DatastoreList;
