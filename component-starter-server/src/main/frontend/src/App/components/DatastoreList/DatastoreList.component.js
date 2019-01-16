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
					<div className={theme.column}>
						<h2>Connection</h2>
						<Action
							id={`${theme['add-new-datastore']}`}
							label="Add new Datastore"
							bsStyle="info"
							icon="talend-plus-circle"
							onClick={() => {
								datastore.setCurrent();
							}}
						/>
						{datastore.datastores.length === 0 && (
							<div className="alert alert-warning">
								<div>
									<p>No connection found.</p>
									<p>
										A connection is required in many cases. They define the way to connect to the
										source of data.
									</p>
									<p>You should define at least one connection to hold metadata.</p>
									<p>
										In the case of JDBC, the connection has either, jdbc 'url' or 'host', 'port',
										'username', etc...
									</p>
								</div>
							</div>
						)}
						<ul>
							{datastore.datastores.map((d, index) => (
								<li key={index} className={theme.li}>
									<Action bsStyle="link" onClick={() => datastore.setCurrent(d)} label={d.name} />
								</li>
							))}
						</ul>
					</div>
					<DatastoreForm datastore={datastore.current} className={theme.column} />
				</div>
			)}
		</DatastoreContext.Consumer>
	);
}

DatastoreList.displayName = 'DatastoreList';
DatastoreList.propTypes = {};

export default DatastoreList;
