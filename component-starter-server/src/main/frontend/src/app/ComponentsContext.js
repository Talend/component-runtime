/**
 *  Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
import getUUID from './uuid';
import {
	COMPONENT_TYPES,
	COMPONENT_TYPE_SOURCE,
	COMPONENT_TYPE_PROCESSOR,
	COMPONENT_TYPE_SINK,
} from './constants';

const Context = React.createContext({});

class Provider extends React.Component {
	static propTypes = {
		value: PropTypes.object,
		children: PropTypes.node,
	};
	constructor(props) {
		super(props);
		this.state = {
			components: props.value || [],
			withIO: false,
		};
		this.state.setComponentType = this.setComponentType.bind(this);
		this.state.addComponent = this.addComponent.bind(this);
		this.state.deleteComponent = this.deleteComponent.bind(this);
		this.state.updateComponent = this.updateComponent.bind(this);
		this.state.setCurrentComponent = this.setCurrentComponent.bind(this);
		this.state.activateIO = this.activateIO.bind(this);
	}

	componentWillReceiveProps(nextProps) {
		if (nextProps.value !== this.state.components) {
			this.setState(nextProps.value);
		}
	}

	setCurrentComponent(index) {
		this.setState({ current: index });
	}

	setComponentType(component, type) {
		// type must be in Input / Processor
		if (COMPONENT_TYPES.indexOf(type) === -1) {
			throw new Error(`Invalid type ${type}. Only ${COMPONENT_TYPES.join(', ')} are valid`);
		}
		this.setState(prevState => {
			// eslint-disable-next-line no-param-reassign
			component.type = type;
			return Object.assign({}, prevState);
		});
	}

	activateIO(datastore, dataset) {
		this.setState({ withIO: true });
		this.dataset = dataset; // keep a ref on it
		const datastoreId = getUUID();
		const datasetId = getUUID();
		datastore.add({
			$id: datastoreId,
			name: 'Datastore',
			structure: {
				entries: [],
			},
		});
		dataset.add(
			{
				$id: datasetId,
				name: 'Dataset',
				structure: {
					entries: [
						{
							name: 'datastore',
							type: 'datastore',
							reference: datastoreId,
						},
					],
				},
			},
			() => {
				this.addComponent(COMPONENT_TYPE_SOURCE);
				this.addComponent(COMPONENT_TYPE_SINK);
			},
		);
	}

	/**
	 * example of component with dataset
	 * entries: [
	 *  {
	 *     name: 'mondataset',
	 *     type: 'dataset',
	 *     ref: 'JDBC'
	 *  }
	 * ]
	 */
	addComponent(type, callback) {
		let name = `Company${type}`;
		const found = this.state.components.find(c => c.configuration.name === name);
		if (found) {
			name = `${name}${this.state.components.length}`;
		}
		const dataset = {
			name: 'dataset',
			type: 'dataset',
			reference: this.dataset && this.dataset.datasets[0].$id,
		};
		const component = {
			type,
			configuration: {
				name,
			},
			source: {
				genericOutput: true,
				stream: false,
				configurationStructure: {
					entries: [],
				},
				outputStructure: {
					entries: [],
				},
			},
			processor: {
				configurationStructure: {
					entries: [],
				},
				inputStructures: [],
				outputStructures: [],
			},
		};
		if (type === COMPONENT_TYPE_PROCESSOR) {
			component.processor.inputStructures.push({
				name: 'MAIN',
				generic: true,
				structure: {
					entries: [],
				},
			});
			component.processor.outputStructures.push({
				name: 'MAIN',
				generic: true,
				structure: {
					entries: [],
				},
			});
		} else if (type === COMPONENT_TYPE_SOURCE) {
			component.source.configurationStructure.entries.push(dataset);
		} else if (type === COMPONENT_TYPE_SINK) {
			component.processor.configurationStructure.entries.push(dataset);
			component.processor.inputStructures.push({
				name: 'MAIN',
				generic: true,
				structure: {
					entries: [],
				},
			});
		}
		this.setState(
			state => {
				state.components.push(component);
				return Object.assign({}, state);
			},
			() => callback && callback(component),
		);
	}

	updateComponent() {
		this.setState(state => Object.assign({}, state));
	}

	deleteComponent(index) {
		this.setState(state => {
			state.components.splice(index, 1);
			return Object.assign({}, state);
		});
	}

	render() {
		return <Context.Provider value={this.state}>{this.props.children}</Context.Provider>;
	}
}

export default {
	Provider,
	Consumer: Context.Consumer,
	raw: Context,
};
