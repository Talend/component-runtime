import React from 'react';
import { shallow } from 'enzyme';

import Component from './ComponentEditForm.component';

describe('Component ComponentEditForm', () => {
	it('should render', () => {
		const wrapper = shallow(
			<Component />
		);
		expect(wrapper.getElement()).toMatchSnapshot();
	});
});
