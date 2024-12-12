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
 */import React from 'react';
import PropTypes from 'prop-types';
import { Action } from '@talend/react-components/lib/Actions';
// import { Prompt } from 'react-router';

// import theme from './DatasetForm.module.scss';
import DatasetContext from '../../DatasetContext';
// import Node from '../Node';
import DatasetSchema from '../DatasetSchema/DatasetSchema.container';
import getUUID from '../../uuid';

/* eslint-disable no-param-reassign */

function onChangeValidate(structure) {
	const messages = [];
	let hasOneDatastore = false;
	structure.entries.forEach(entry => {
		if (entry.type === 'datastore') {
			if (!entry.reference) {
				messages.push({
					type: 'error',
					message: `The attribute ${entry.name} has no datastore reference.`,
				});
			}
			hasOneDatastore = true;
		}
	});
	if (!hasOneDatastore) {
		messages.push({ type: 'error', message: 'A dataset must have a datastore' });
	}
	return messages;
}

class DatasetForm extends React.Component {
	static propTypes = {
		dataset: PropTypes.object,
	};

	constructor(props) {
		super(props);
		this.onSubmit = this.onSubmit.bind(this);
		this.onNameChange = this.onNameChange.bind(this);
		const dataset = this.props.dataset || {
			$id: getUUID(),
			name: 'Dataset1',
			structure: {
				entries: [],
			},
		};
		this.state = {
			dirty: false,
			messages: [],
			dataset,
		};
		this.onChangeValidate = this.onChangeValidate.bind(this);
	}

	componentWillReceiveProps(nextProps) {
		if (nextProps.dataset !== this.props.dataset) {
			if (nextProps.dataset) {
				this.setState({ dataset: nextProps.dataset });
			} else {
				// from edit mode to add mode
				this.setState({
					dirty: false,
					dataset: {
						$id: getUUID(),
						name: `Dataset${this.service.datasets.length}`,
						structure: {
							entries: [],
						},
					},
				});
			}
		}
	}

	onSubmit(service) {
		this.service = service;
		return event => {
			event.preventDefault();
			if (this.props.dataset) {
				try {
					service.edit(this.props.dataset, this.state.dataset);
				} catch (error) {
					this.setState({
						error,
					});
				}
			} else {
				try {
					service.add(this.state.dataset);
					service.setCurrent(this.state.dataset);
				} catch (error) {
					this.setState({
						error,
					});
				}
			}
		};
	}

	onNameChange(event) {
		const value = event.target.value;
		this.setState(prevState => {
			prevState.dirty = true;
			prevState.dataset.name = value;
			return Object.assign({}, prevState);
		});
	}

	onChangeValidate(structure) {
		const messages = onChangeValidate(structure);
		this.setState({
			edited: new Date(),
			dirty: true,
			messages,
		});
		return messages;
	}

	render() {
		const hasError = this.state.error || this.state.messages.length > 0;
		return (
			<DatasetContext.Consumer>
				{dataset => (
					<form className="form" onSubmit={this.onSubmit(dataset)} noValidate>
						<h2>{this.props.dataset ? 'Edit the dataset' : 'Create a new dataset'}</h2>
						<div className="form-group required">
							<label htmlFor="dataset-name" className="control-label">
								Name
							</label>
							<input
								className="form-control"
								required
								type="text"
								value={this.state.dataset.name}
								onChange={this.onNameChange}
							/>
						</div>
						<DatasetSchema
							schema={this.state.dataset.structure}
							onChangeValidate={this.onChangeValidate}
						/>
						{this.state.error && (
							<div className="alert alert-danger">{this.state.error.message}</div>
						)}
						<Action
							disabled={hasError}
							label={`${this.props.dataset ? 'Save' : 'Add'}`}
							type="submit"
							bsStyle="primary"
						/>
					</form>
				)}
			</DatasetContext.Consumer>
		);
	}
}

DatasetForm.displayName = 'DatasetForm';

export default DatasetForm;
