/**
 *  Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
 */
import React from 'react';
import { Link } from 'react-router-dom';
import { COMPONENTS_LIST_URL } from '../constants';
import './ComponentList.css';

export default class ComponentsList extends React.Component {
	constructor(props) {
		super(props);
		this.state = {};
	}

	componentWillMount() {
		fetch(COMPONENTS_LIST_URL)
			.then(resp => resp.json())
			.then(payload => this.setState(payload));
	}

	render() {
		if (! this.state.components) {
			return (<div>Loading ...</div>);
		}
		return (
			<ul className="ComponentList">
				{
					this.state.components.map(comp => (
						<li>
							<Link to={`/detail/${comp.id.id}`}>
								{comp.displayName}
							</Link>
						</li>
					))
				}
			</ul>
		);
	}
}
