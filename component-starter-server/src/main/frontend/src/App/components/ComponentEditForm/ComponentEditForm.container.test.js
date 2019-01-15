import React from 'react';
import { shallow } from 'enzyme';

import Component from './ComponentEditForm.component';
import Container from './ComponentEditForm.container';

describe('Container ComponentEditForm', () => {
	it('should render ComponentEditForm with props', () => {
		const wrapper = shallow(
			<Container />
		);
		expect(wrapper.find(Component).length).toBe(1);
		expect(wrapper.props()).toMatchSnapshot();
	});
});
