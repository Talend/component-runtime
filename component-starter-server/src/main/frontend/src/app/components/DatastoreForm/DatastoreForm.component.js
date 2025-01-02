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
import classnames from 'classnames';
import { Action } from '@talend/react-components/lib/Actions';

// import theme from './DatastoreForm.module.scss';
import DatastoreContext from '../../DatastoreContext';
import Node from '../Node';
import getUUID from '../../uuid';

class DatastoreForm extends React.Component {
	static propTypes = {
		datastore: PropTypes.object,
		className: PropTypes.string,
		name: PropTypes.string,
	};

	constructor(props) {
		super(props);
		this.onSubmit = this.onSubmit.bind(this);
		this.onNameChange = this.onNameChange.bind(this);
		this.onChangeValidate = this.onChangeValidate.bind(this);
		this.state = this.props.datastore || {
			$id: getUUID(),
			name: 'Datastore1',
			structure: {
				entries: [],
			},
		};
	}

	componentWillReceiveProps(nextProps) {
		if (nextProps.datastore !== this.props.datastore) {
			if (nextProps.datastore) {
				this.setState(nextProps.datastore);
			} else {
				// from edit mode to add mode
				this.setState({
					$id: getUUID(),
					name: `Datastore${this.service.datastores.length}`,
					structure: {
						entries: [],
					},
				});
			}
		}
	}

	onSubmit(service) {
		this.service = service;
		return event => {
			event.preventDefault();
			if (this.props.datastore) {
				try {
					service.edit(this.props.datastore, this.state);
				} catch (error) {
					this.setState({
						error,
					});
				}
			} else {
				try {
					service.add(this.state);
					service.setCurrent(this.state);
				} catch (error) {
					this.setState({
						error,
					});
				}
			}
		};
	}
	onNameChange(event) {
		this.setState({ name: event.target.value });
	}

	onChangeValidate() {
		this.setState({
			edited: new Date(),
		});
	}

	render() {
		return (
			<DatastoreContext.Consumer>
				{datastore => (
					<form
						onSubmit={this.onSubmit(datastore)}
						noValidate
						className={classnames('form', this.props.className)}
					>
						<h2>{this.props.datastore ? 'Edit the datastore' : 'Create a new datastore'}</h2>
						<div className="form-group required">
							<label htmlFor="datastore-name" className="control-label">
								Name
							</label>
							<input
								className="form-control"
								required
								type="text"
								value={this.state.name}
								onChange={this.onNameChange}
							/>
						</div>
						<div className="form-group">
							<label htmlFor="datastore-model">Model</label>
							<Node id="datastore-model" node={this.state.structure} readOnly name={this.props.name} onChangeValidate={this.onChangeValidate} />
						</div>
						{this.state.error && (
							<div className="alert alert-danger">{this.state.error.message}</div>
						)}
						<Action
							label={`${this.props.datastore ? 'Save' : 'Add'}`}
							type="submit"
							bsStyle="primary"
						/>
					</form>
				)}
			</DatastoreContext.Consumer>
		);
	}
}

DatastoreForm.displayName = 'DatastoreForm';

export default DatastoreForm;
