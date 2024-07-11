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
import DatasetContext from '../../DatasetContext';
import DatasetForm from '../DatasetForm';
import theme from './DatasetList.module.scss';
import DatasetDelete from '../DatasetDelete';

function DatasetList() {
	return (
		<DatasetContext.Consumer>
			{dataset => (
				<div className={theme.container}>
					<div className={theme.column}>
						<h2>Dataset</h2>
						<Action
							id={`${theme['add-new-dataset']}`}
							label="Add new Dataset"
							bsStyle="info"
							icon="talend-plus-circle"
							onClick={() => {
								dataset.setCurrent();
							}}
						/>
						{dataset.datasets.length === 0 && (
							<div className="alert alert-warning">
								<div>
									<p>No dataset found.</p>
									<p>
										Datasets are required in many cases. They define the way to take data throw a
										datastore.
									</p>
									<p>You should define at least one dataset to get data from a datastore.</p>
									<p>In the case of JDBC, the dataset is a SQL query.</p>
								</div>
							</div>
						)}

						<ul>
							{dataset.datasets.map((d, index) => (
								<li key={index} className={theme.li}>
									<Action bsStyle="link" onClick={() => dataset.setCurrent(d)} label={d.name} />
									<DatasetDelete item={d} />
								</li>
							))}
						</ul>
					</div>
					<div className={theme.column}>
						<DatasetForm dataset={dataset.current} className={theme.column} />
					</div>
				</div>
			)}
		</DatasetContext.Consumer>
	);
}

DatasetList.displayName = 'DatasetList';
DatasetList.propTypes = {};

export default DatasetList;
