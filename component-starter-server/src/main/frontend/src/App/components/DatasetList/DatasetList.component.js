import React from 'react';

import { Action } from '@talend/react-components';
import DatasetContext from '../../DatasetContext';
import DatasetForm from '../DatasetForm';
import theme from './DatasetList.scss';

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
								</li>
							))}
						</ul>
					</div>
					<DatasetForm dataset={dataset.current} className={theme.column} />
				</div>
			)}
		</DatasetContext.Consumer>
	);
}

DatasetList.displayName = 'DatasetList';
DatasetList.propTypes = {};

export default DatasetList;
