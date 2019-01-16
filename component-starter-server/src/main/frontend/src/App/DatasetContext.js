import React from 'react';
import PropTypes from 'prop-types';

const DatasetContext = React.createContext({ datasets: [] });

class Provider extends React.Component {
	static propTypes = {
		value: PropTypes.object,
		datastore: PropTypes.object,
		children: PropTypes.node,
	};
	constructor(props) {
		super(props);
		this.state = {
			datasets: props.value || [],
			datastore: props.datastore,
		};
		this.state.add = dataset => {
			if (!dataset.name) {
				throw new Error('Dataset name is required');
			}
			this.setState(prevState => {
				const exists = prevState.datasets.find(d => d.name === dataset.name);
				if (exists) {
					throw new Error('Dataset name is required');
				}
				// eslint-disable-next-line no-param-reassign
				prevState.datasets = prevState.datasets.concat(dataset);
				return Object.assign({}, prevState);
			});
		};
		this.state.edit = (dataset, newValues) => {
			if (!newValues.name) {
				throw new Error('Dataset name is required');
			}
			this.setState(prevState => {
				const index = prevState.datasets.indexOf(dataset);
				if (index === -1) {
					throw new Error('Can t edit. Dataset not found');
				}
				Object.assign(dataset, newValues);
				return Object.assign({}, prevState);
			});
		};
		this.state.setCurrent = dataset => {
			this.setState({ current: dataset });
		};
	}

	componentWillReceiveProps(nextProps) {
		if (nextProps.value !== this.state.datasets) {
			this.setState(nextProps.value);
		}
	}

	render() {
		return (
			<DatasetContext.Provider value={this.state}>{this.props.children}</DatasetContext.Provider>
		);
	}
}

export default {
	Provider,
	Consumer: DatasetContext.Consumer,
};
