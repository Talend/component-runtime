import React from 'react';
import { shallow } from 'enzyme';

import Component from './DatasetSchema.component';
import Container from './DatasetSchema.container';

describe('Container DatasetSchema', () => {
	it('should render DatasetSchema with props', () => {
		const wrapper = shallow(
			<Container />
		);
		expect(wrapper.find(Component).length).toBe(1);
		expect(wrapper.props()).toMatchSnapshot();
	});
});
