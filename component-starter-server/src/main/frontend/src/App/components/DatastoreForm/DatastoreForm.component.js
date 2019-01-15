import React from 'react';
import PropTypes from 'prop-types';
import { Action } from '@talend/react-components';

import theme from './DatastoreForm.scss';
import DatastoreContext from '../../DatastoreContext';
import Node from '../../Component/Node';

class DatastoreForm extends React.Component {
	static propTypes = {
		datastore: PropTypes.object,
	};

	constructor(props) {
		super(props);
		this.onSubmit = this.onSubmit.bind(this);
		this.onNameChange = this.onNameChange.bind(this);
		this.state = this.props.datastore || {
			name: 'Datastore',
			schema: {
				entries: [],
			},
		};
	}

	componentWillReceiveProps(nextProps) {
		if (nextProps.datastore !== this.props.datastore) {
			this.setState(nextProps.datastore);
		}
	}

	onSubmit(service) {
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

	render() {
		return (
			<DatastoreContext.Consumer>
				{datastore => (
					<form className="form" onSubmit={this.onSubmit(datastore)} noValidate>
						<h2>{this.props.datastore ? 'Create a new datastore' : 'Edit the datastore'}</h2>
						<div className="form-group required">
							<label htmlFor="datastore-name" className="control-label">Name</label>
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
							<Node id="datastore-model" node={this.state.schema} readOnly name={this.props.name} />
						</div>
						{this.state.error && (
							<div className="alert alert-danger">
								{this.state.error.message}
							</div>
						)}
						<Action label="Save" type="submit" />
					</form>
				)}
			</DatastoreContext.Consumer>
		);
	}
}

DatastoreForm.displayName = 'DatastoreForm';

export default DatastoreForm;
