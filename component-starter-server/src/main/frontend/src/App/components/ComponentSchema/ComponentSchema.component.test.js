import React from 'react';
import { shallow } from 'enzyme';

import Component from './ComponentSchema.component';

describe('Component ComponentSchema', () => {
	it('should render', () => {
		const wrapper = shallow(
			<Component />
		);
		expect(wrapper.getElement()).toMatchSnapshot();
	});
});
