/**
 *  Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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
import UIForm from '@talend/react-forms/lib/UIForm/UIForm.container';
import { COMPONENT_URL } from '../constants';
import onDefaultTrigger from './triggers';

import './ComponentForm.css';

export default class ComponentsForm extends React.Component {
	constructor(props) {
		super(props);
		this.state = {};
		this.changeForm = this.changeForm.bind(this);
		this.onSubmit = this.onSubmit.bind(this);
	}

	componentWillMount() {
		const componentId = this.props.match.params.componentId;
		fetch(`${COMPONENT_URL}/${componentId}`)
			.then(resp => resp.json())
			.then(payload => this.setState({ uiSpec: payload }));
	}

	onSubmit(event, payload) {
		console.log('submit', payload);
		this.setState({
			submitted: true,
			payload,
		});
	}

	changeForm() {
		this.setState({
			submitted: false,
			uiSpec: {
				...this.state.uiSpec,
				properties: this.state.payload,
			}
		});
	}

	render() {
		if(! this.state.uiSpec) {
			return (<div>Loading ...</div>);
		}

		return (
			<div className="ComponentForm">
				{
					this.state.submitted &&
					<div className="submitValidation">
						<pre>
							{JSON.stringify(this.state.payload, null, 4)}
						</pre>
						<button className="btn btn-warning" onClick={this.changeForm}>Change form</button>
					</div>
				}
				{
					!this.state.submitted &&
					<UIForm
						data={this.state.uiSpec}
						onTrigger={onDefaultTrigger}
						onSubmit={this.onSubmit}
					/>
				}
			</div>
		);
	}
}