import React from 'react';
import { Link, withRouter } from 'react-router-dom';
import Icon from '@talend/react-components/lib/Icon';

import './BackToList.css';

function BackToList({ location }) {
	if(location.pathname === '/') {
		return null;
	}
	return (
		<Link to="/" className="BackToList">
			<Icon name="talend-arrow-left"/>
			<span>Back to Components List</span>
		</Link>
	);
}

export default withRouter(BackToList);
