import React from 'react';
import PropTypes from 'prop-types';

import theme from './DatastoreList.scss';

function DatastoreList(props) {
	return (
		<div>
			{props.name}
		</div>
	);
}

DatastoreList.displayName = 'DatastoreList';
DatastoreList.propTypes = {
	name: PropTypes.string,
	
};

export default DatastoreList;
