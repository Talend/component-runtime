import React from 'react';
import { shallow } from 'enzyme';

import Component from './EmbeddableToggle.component';
import Container from './EmbeddableToggle.container';

describe('Container EmbeddableToggle', () => {
	it('should render EmbeddableToggle with props', () => {
		const wrapper = shallow(
			<Container />
		);
		expect(wrapper.find(Component).length).toBe(1);
		expect(wrapper.props()).toMatchSnapshot();
	});
});
