import React from 'react';
import { shallow } from 'enzyme';

import Component from './SelectDataset.component';
import Container from './SelectDataset.container';

describe('Container SelectDataset', () => {
	it('should render SelectDataset with props', () => {
		const wrapper = shallow(
			<Container />
		);
		expect(wrapper.find(Component).length).toBe(1);
		expect(wrapper.props()).toMatchSnapshot();
	});
});
