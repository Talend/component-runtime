/**
 *  Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
import PropTypes from 'prop-types';
import classnames from 'classnames';
import Node from '../Node';

// import theme from './Schema.module.scss';

export default class Schema extends React.Component {
	static propTypes = {
		onChangeValidate: PropTypes.func,
		schema: PropTypes.object,
		parent: PropTypes.object,
		extraTypes: PropTypes.arrayOf(PropTypes.string),
		readOnly: PropTypes.bool,
		name: PropTypes.string,
		references: PropTypes.object,
		addRefNewLocation: PropTypes.object,
		onReload: PropTypes.func,
	};
	constructor(props) {
		super(props);
		this.state = {
			schema: props.schema,
			messages: [],
		};
		this.onChangeValidate = this.onChangeValidate.bind(this);
	}

	componentWillReceiveProps(nextProps) {
		if (this.props.schema !== nextProps.schema) {
			this.setState({
				schema: nextProps.schema,
			});
			if (this.props.onReload) {
				this.props.onReload();
			}
		}
	}

	onChangeValidate() {
		if (this.props.onChangeValidate) {
			this.setState({
				messages: this.props.onChangeValidate(this.state.schema),
			});
		}
	}

	render() {
		return (
			<div className="form-group">
				<label htmlFor="schema-model">Model</label>
				<Node
					node={this.state.schema}
					readOnly={this.props.readOnly || !!this.props.parent}
					name={this.props.name}
					onChangeValidate={this.onChangeValidate}
					extraTypes={this.props.extraTypes}
					references={this.props.references}
					addRefNewLocation={this.props.addRefNewLocation}
				/>
				{this.state.messages.map(message => (
					<p
						className={classnames({
							'text-danger': message.type === 'error',
						})}
					>
						{message.message}
					</p>
				))}
			</div>
		);
	}
}
