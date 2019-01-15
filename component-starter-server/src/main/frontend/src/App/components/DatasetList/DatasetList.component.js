import React from 'react';
import PropTypes from 'prop-types';

import { Action } from '@talend/react-components';
import DatasetContext from '../../DatasetContext';
import DatasetForm from '../DatasetForm';
import theme from './DatasetList.scss';

function DatasetList() {
	return (
		<DatasetContext.Consumer>
			{dataset => (
				<div className={theme.container}>
					<DatasetForm dataset={dataset.current} />
					<div className={theme.list}>
						<h2>Dataset List</h2>
						<ul>
							{dataset.datasets.map((d, index) => (
								<li key={index} className={theme.li}>
									<Action
										bsStyle="link"
										onClick={() => dataset.setCurrent(d)}
										label={d.name}
									/>
								</li>
							))}
						</ul>
					</div>
				</div>
			)}
		</DatasetContext.Consumer>
	);
}

DatasetList.displayName = 'DatasetList';
DatasetList.propTypes = {

};

export default DatasetList;
