/**
 *  Copyright (C) 2006-2019 Talend Inc. - www.talend.com
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
import getUUID from './uuid';

const Context = React.createContext({});

const INPUT = 'Input';
const PROCESSOR = 'Processor';

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
		if (type !== INPUT && type !== PROCESSOR) {
			throw new Error(`Invalid type ${type}. Only Input and Processor are valid`);
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
		datastore.add({
			$id: datastoreId,
			name: 'Datastore1',
			structure: {
				entries: [],
			},
		});
		dataset.add({
			$id: getUUID(),
			name: 'Dataset1',
			structure: {
				entries: [{
					name: 'datastore',
					type: 'datastore',
					reference: datastoreId,
				}],
			},
		});
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
	addComponent(callback) {
		const component = {
			type: this.state.withIO ? INPUT : PROCESSOR,
			configuration: {
				name: `CompanyComponent${this.state.components.length + 1}`,
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
				inputStructures: [
					{
						name: 'MAIN',
						generic: true,
						structure: {
							entries: [],
						},
					},
				],
				outputStructures: [],
			},
		};
		if (this.state.withIO && this.dataset.datasets.length > 0) {
			component.source.configurationStructure.entries.push({
				name: 'dataset',
				type: 'dataset',
				reference: this.dataset.datasets[0].$id,
			});
		}
		if (!this.state.withIO) {
			component.processor.outputStructures.push({
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
};
