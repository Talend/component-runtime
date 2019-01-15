import React from 'react';
import PropTypes from 'prop-types';
import { Action } from '@talend/react-components';

import theme from './DatasetForm.scss';
import DatasetContext from '../../DatasetContext';
import Node from '../../Component/Node';

class DatasetForm extends React.Component {
	static propTypes = {
		dataset: PropTypes.object,
	};

	constructor(props) {
		super(props);
		this.onSubmit = this.onSubmit.bind(this);
		this.onNameChange = this.onNameChange.bind(this);
		this.state = this.props.dataset || {
			name: 'Dataset',
			schema: {
				entries: [],
			},
		};
	}

	componentWillReceiveProps(nextProps) {
		if (nextProps.dataset !== this.props.dataset) {
			this.setState(nextProps.dataset);
		}
	}

	onSubmit(service) {
		return event => {
			event.preventDefault();
			if (this.props.dataset) {
				try {
					service.edit(this.props.dataset, this.state);
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
			<DatasetContext.Consumer>
				{dataset => (
					<form className="form" onSubmit={this.onSubmit(dataset)} noValidate>
						<h2>{this.props.dataset ? 'Create a new dataset' : 'Edit the dataset'}</h2>
						<div className="form-group required">
							<label htmlFor="dataset-name" className="control-label">Name</label>
							<input
								className="form-control"
								required
								type="text"
								value={this.state.name}
								onChange={this.onNameChange}
							/>
						</div>
						<div className="form-group">
							<label htmlFor="dataset-model">Model</label>
							<Node id="dataset-model" node={this.state.schema} readOnly name={this.props.name} />
						</div>
						{this.state.error && (
							<div className="alert alert-danger">
								{this.state.error.message}
							</div>
						)}
						<Action label="Save" type="submit" />
					</form>
				)}
			</DatasetContext.Consumer>
		);
	}
}

DatasetForm.displayName = 'DatasetForm';

export default DatasetForm;
