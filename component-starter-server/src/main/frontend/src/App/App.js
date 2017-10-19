import React from 'react';
import { BrowserRouter as Router, Route } from 'react-router-dom';
import IconsProvider from '@talend/react-components/lib/IconsProvider';
import HeaderBar from '@talend/react-components/lib/HeaderBar';
import Generator from '../Generator';

import theme from './App.scss';
import './favicon.ico';

export default function App() {
	return (
		<Router>
			<div className={theme.App}>
				<IconsProvider/>

				<div className={theme.header}>
					<HeaderBar logo={{ isFull: true }} brand={{ name: 'Component Kit Starter' }} />
				</div>

				<div className={theme.content}>
					<Route exact path="/" component={Generator}/>
				</div>
			</div>
		</Router>
	);
}
