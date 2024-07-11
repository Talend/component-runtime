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
 */import React from 'react';

import { Action } from '@talend/react-components/lib/Actions';
import DatastoreContext from '../../DatastoreContext';
import DatastoreForm from '../DatastoreForm';
import DatastoreDelete from '../DatastoreDelete';
import theme from './DatastoreList.module.scss';

function DatastoreList() {
	return (
		<DatastoreContext.Consumer>
			{datastore => (
				<div className={theme.container}>
					<div className={theme.column}>
						<h2>Datastore</h2>
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
									<p>No datstore found.</p>
									<p>
										A datastore is required in many cases. They define the way to connect to the
										source of data.
									</p>
									<p>You should define at least one datastore to hold metadata.</p>
									<p>
										In the case of JDBC, the datastore has either, jdbc 'url' or 'host', 'port',
										'username', etc...
									</p>
								</div>
							</div>
						)}
						<ul>
							{datastore.datastores.map((d, index) => (
								<li key={index} className={theme.li}>
									<Action bsStyle="link" onClick={() => datastore.setCurrent(d)} label={d.name} />
									<DatastoreDelete item={d} />
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
