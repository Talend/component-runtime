/**
 *  Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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
				// eslint-disable-next-line no-param-reassign
				prevState.datastores = prevState.datastores.concat(datastore);
				// eslint-disable-next-line no-param-reassign
				prevState.current = datastore;
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
		this.state.delete = datastore => {
			const index = this.state.datastores.indexOf(datastore);
			this.setState(prevState => {
				if (prevState.current === datastore) {
					// eslint-disable-next-line no-param-reassign
					delete prevState.current;
				}
				prevState.datastores.splice(index, 1);
				return Object.assign({}, prevState);
			});
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
	raw: DatastoreContext,
};
