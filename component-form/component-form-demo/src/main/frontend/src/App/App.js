import React from 'react';
import { BrowserRouter as Router, Route } from 'react-router-dom';
import IconsProvider from '@talend/react-components/lib/IconsProvider';
import HeaderBar from '@talend/react-components/lib/HeaderBar';
import BackToList from '../BackToList';
import ComponentsList from '../ComponentsList';
import ComponentForm from '../ComponentForm';

import './App.css';
import './favicon.ico';

export default function App() {
	return (
		<Router>
			<div className="App">
				<IconsProvider/>

				<div className="header">
					<HeaderBar logo={{ isFull: true }} brand={{ name: 'TCOMP DEMO' }} />
				</div>

				<BackToList/>

				<div className="content">
					<Route exact path="/" component={ComponentsList}/>
					<Route path="/detail/:componentId" component={ComponentForm}/>
				</div>
			</div>
		</Router>
	);
}