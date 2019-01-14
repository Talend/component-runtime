import React from 'react';
import { shallow } from 'enzyme';

import Component from './SelectDataset.component';

describe('Component SelectDataset', () => {
	it('should render', () => {
		const wrapper = shallow(
			<Component />
		);
		expect(wrapper.getElement()).toMatchSnapshot();
	});
});
