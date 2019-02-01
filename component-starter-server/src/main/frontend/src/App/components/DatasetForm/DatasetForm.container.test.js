import React from 'react';
import { shallow } from 'enzyme';

import Component from './DatasetForm.component';
import Container from './DatasetForm.container';

describe('Container DatasetForm', () => {
	it('should render DatasetForm with props', () => {
		const wrapper = shallow(
			<Container />
		);
		expect(wrapper.find(Component).length).toBe(1);
		expect(wrapper.props()).toMatchSnapshot();
	});
});
