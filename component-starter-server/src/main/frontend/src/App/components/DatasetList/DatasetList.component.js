import React from 'react';
import PropTypes from 'prop-types';

import theme from './DatasetList.scss';

function DatasetList(props) {
	return (
		<div>
			{props.name}
		</div>
	);
}

DatasetList.displayName = 'DatasetList';
DatasetList.propTypes = {
	name: PropTypes.string,
	
};

export default DatasetList;
