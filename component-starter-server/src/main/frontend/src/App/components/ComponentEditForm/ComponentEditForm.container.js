import React from 'react';
// import PropTypes from 'prop-types';

import Component from './ComponentEditForm.component';

class ComponentEditForm extends React.Component {
	static displayName = 'Container(ComponentEditForm)';
	static propTypes = {
		
	};
	static ACTION_TYPE_ON_EVENT = 'ComponentEditForm.onClick';

	constructor(props) {
		super(props);
		this.state = {};
		this.onClick = this.onClick.bind(this);
	}

	onClick(event) {
		if (this.props.onClick) {
			this.props.onClick(event);
		}
		this.props.dispatch({
			type: ComponentEditForm.ACTION_TYPE_ON_EVENT,
			componentId: this.props.componentId,
		});
	}

	render() {
		const props = this.props;
		return (
			<Component
				{...props}
				onClick={this.onClick}
			/>
		);
	}
}

export default ComponentEditForm;
