/**
 *  Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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
// import PropTypes from 'prop-types';

import Component from './SelectDataset.component';

import DatasetContext from '../../DatasetContext';

class SelectDataset extends React.Component {
	static displayName = 'Container(SelectDataset)';
	static propTypes = {

	};

	constructor(props) {
		super(props);
		this.state = {};
		this.onChange = this.onChange.bind(this);
	}

	onChange(event) {
		this.setState({value: event.target.value});
		if (this.props.onChange) {
			this.props.onChange(event, event.target.value);
		}
	}

	render() {
		return (
			<DatasetContext.Consumer>
				{({datasets}) => (
					<Component
						datasets={datasets}
						value={this.state.value}
						onChange={this.onChange}
					/>
				)}
			</DatasetContext.Consumer>
		);
	}
}

export default SelectDataset;
