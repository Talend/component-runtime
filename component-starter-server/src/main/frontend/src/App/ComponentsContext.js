import React from 'react';

const Context = React.createContext({});

const INPUT = 'Input';
const PROCESSOR = 'Processor';

class Provider extends React.Component {
	constructor(props) {
		super(props);
		this.state = {
			components: props.value || [],
		};
		this.state.setComponentType = this.setComponentType.bind(this);
		this.state.addComponent = this.addComponent.bind(this);
		this.state.deleteComponent = this.deleteComponent.bind(this);
		this.state.updateComponent = this.updateComponent.bind(this);
		this.state.setCurrentComponent = this.setCurrentComponent.bind(this);
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
			component.type = type;
			// business rules
			if (type === INPUT) {
			} else {
			}
			return Object.assign({}, prevState);
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
			type: 'Input',
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
						generic: false,
						structure: {
							entries: [],
						},
					},
				],
				outputStructures: [],
			},
		};
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
