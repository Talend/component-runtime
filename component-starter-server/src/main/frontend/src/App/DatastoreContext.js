import React from 'react';
import PropTypes from 'prop-types';

const DatastoreContext = React.createContext({ datastores: [] });

class Provider extends React.Component {
	static propTypes = {
		value: PropTypes.array,
		children: PropTypes.node,
	};

	constructor(props) {
		super(props);
		this.state = {
			datastores: props.value || [],
		};
		this.state.add = datastore => {
			if (!datastore.name) {
				throw new Error('Datastore name is required');
			}
			this.setState(prevState => {
				const exists = prevState.datastores.find(d => d.name === datastore.name);
				if (exists) {
					throw new Error('Datastore name is required');
				}
				prevState.datastores = prevState.datastores.concat(datastore);
				return Object.assign({}, prevState);
			});
		};
		this.state.edit = (datastore, newValues) => {
			if (!newValues.name) {
				throw new Error('Datastore name is required');
			}
			this.setState(prevState => {
				const index = prevState.datastores.indexOf(datastore);
				if (index === -1) {
					throw new Error('Can t edit. Datastore not found');
				}
				Object.assign(datastore, newValues);
				return Object.assign({}, prevState);
			});
		};
		this.state.setCurrent = datastore => {
			this.setState({ current: datastore });
		};
	}

	componentWillReceiveProps(nextProps) {
		if (nextProps.value !== this.state.datastores) {
			this.setState(nextProps.value);
		}
	}

	render() {
		return (
			<DatastoreContext.Provider value={this.state}>
				{this.props.children}
			</DatastoreContext.Provider>
		);
	}
}

export default {
	Provider,
	Consumer: DatastoreContext.Consumer,
};
