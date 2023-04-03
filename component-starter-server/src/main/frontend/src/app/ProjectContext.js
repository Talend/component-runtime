/**
 *  Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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
import PropTypes from 'prop-types';

import { CONFIGURATION_URL } from './constants';

/* eslint-disable no-param-reassign */

const ProjectContext = React.createContext({});

class Provider extends React.Component {
	static propTypes = {
		children: PropTypes.node,
		// eslint-disable-next-line react/no-unused-prop-types
		value: PropTypes.object,
	};
	constructor(props) {
		super(props);
		this.state = {
			project: {
				buildType: 'Maven',
				version: '0.0.1-SNAPSHOT',
				group: 'com.company',
				artifact: 'company-component',
				name: 'A Component Family',
				description: 'A generated component project',
				packageBase: 'com.company.talend.components',
				family: 'CompanyFamily',
				category: 'Misc',
				facets: [],
			},
			// configuration: {
			//     buildTypes: [],
			//     facets: {},
			// },
			buildToolActions: [],
			facets: {},
			view: {
				light: true,
			},
		};
		this.state.notify = this.updateMe.bind(this);
		this.state.selectBuildTool = this.selectBuildTool.bind(this);
	}

	componentWillMount() {
		fetch(`${CONFIGURATION_URL}`)
			.then(resp => resp.json())
			.then(payload => {
				this.setState(current => {
					current.configuration = payload;
					if (!current.project.buildType) {
						current.project.buildType = 'Maven';
					}
					return Object.assign({}, current);
				});
			});
	}

	componentWillReceiveProps(nextProps) {
		if (nextProps.value !== this.state.datasets) {
			this.setState(nextProps.value);
		}
	}

	updateMe() {
		this.setState(prevState => Object.assign({}, prevState));
	}

	selectBuildTool(label) {
		this.setState(prevState => {
			prevState.project.buildType = label;
			return Object.assign({}, prevState);
		});
	}

	render() {
		if (!this.state.configuration) {
			return <div>Loading ...</div>;
		}
		return (
			<ProjectContext.Provider value={this.state}>{this.props.children}</ProjectContext.Provider>
		);
	}
}

export default {
	Provider,
	Consumer: ProjectContext.Consumer,
};
