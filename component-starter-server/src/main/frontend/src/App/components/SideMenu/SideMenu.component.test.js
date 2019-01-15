import React from 'react';
import { shallow } from 'enzyme';

import Component from './SideMenu.component';

describe('Component SideMenu', () => {
	it('should render', () => {
		const wrapper = shallow(
			<Component />
		);
		expect(wrapper.getElement()).toMatchSnapshot();
	});
});
