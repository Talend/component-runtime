import React from 'react';
import { shallow } from 'enzyme';

import Component from './Connection.component';
import Container from './Connection.container';

describe('Container Connection', () => {
	it('should render Connection with props', () => {
		const wrapper = shallow(
			<Container />
		);
		expect(wrapper.find(Component).length).toBe(1);
		expect(wrapper.props()).toMatchSnapshot();
	});
});
