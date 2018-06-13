import React from 'react';
import { cmfConnect } from '@talend/react-cmf';
import { UIForm } from '@talend/react-forms/lib/UIForm';
import omit from 'lodash/omit';
import { Map } from 'immutable';
import { CircularProgress } from '@talend/react-components';
import createTriggers from '../form-trigger';

export const DEFAULT_STATE = new Map({
	dirty: false,
});


class ComponentForm extends React.Component {
	static displayName = 'ComponentForm';
	static propTypes = {
		...cmfConnect.propTypes,
	};
	static defaultProps = {
		customTriggers: () => {},
	};
	static getCollectionId = componentId => `tcomp-form${componentId}`;

	constructor(props) {
		super(props);
		this.state = {};
		this.onTrigger = this.onTrigger.bind(this);
		this.onChange = this.onChange.bind(this);
		this.getUISpec = this.getUISpec.bind(this);
		this.trigger = createTriggers({
			url: this.props.triggerURL,
			customRegistry: this.props.customTriggers(this),
		});
	}

	onChange(event, data) {
		this.setState({ properties: data.properties });
		this.props.dispatch({
			type: 'TCOMP_FORM_ON_CHANGE',
			event: {
				type: 'onChange',
				component: 'TCompForm',
				props: this.props,
				state: this.state,
				source: event,
			},
			data,
			uiSpec: this.getUISpec(),
		});
	}

	onTrigger(event, payload) {
		console.log('onTrigger');
		this.trigger(event, payload).then(data => {
			if (data.properties) {
				this.setState({ properties: data.properties });
			}
			this.props.dispatch({
				type: 'TCOMP_FORM_ON_TRIGGER',
				event: {
					type: 'onTrigger',
					component: 'TCompForm',
					componentId: this.props.componentId,
					props: this.props,
					state: this.state,
				},
				data,
				uiSpec: this.getUISpec(),
			});
		});
	}

	getUISpec() {
		const spec = { properties: this.state.properties };
		let immutableSpec = this.props.state;
		if (this.props.uiSpecPath) {
			immutableSpec = immutableSpec.getIn(this.props.uiSpecPath.split('.'), new Map());
		}
		spec.jsonSchema = immutableSpec.get('jsonSchema', new Map()).toJS();
		spec.uiSchema = immutableSpec.get('uiSchema', new Map()).toJS();
		return spec;
	}

	render() {
		const props = Object.assign(
			{},
			omit(this.props, cmfConnect.INJECTED_PROPS),
			this.getUISpec(),
			{
				onTrigger: this.onTrigger,
				onChange: this.onChange,
			},
		);
		if (!props.jsonSchema) {
			return <CircularProgress />;
		}
		if (this.state.properties) {
			props.properties = this.state.properties;
		}
		return <UIForm {...props} />;
	}
}

export function mapStateToProps(state, ownProps) {
	const props = {};
	// if (ownProps.definitionURL) {
	// 	let collectionPath = TCompForm.getCollectionId(ownProps.componentId);
	// 	if (ownProps.uiSpecPath) {
	// 		collectionPath = `${collectionPath}.${ownProps.uiSpecPath}`;
	// 	}
	// 	const schema = cmf.selectors.collections.toJS(state, collectionPath);
	// 	if (schema) {
	// 		Object.assign(props, schema);
	// 	}
	// }
	return props;
}

export default cmfConnect({
	defaultState: DEFAULT_STATE,
	defaultProps: {
		saga: 'ComponentForm#default',
	},
	mapStateToProps,
})(ComponentForm);
