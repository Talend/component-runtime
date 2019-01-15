import React from 'react';
import { shallow } from 'enzyme';

import Component from './DatastoreList.component';

describe('Component DatastoreList', () => {
	it('should render', () => {
		const wrapper = shallow(
			<Component />
		);
		expect(wrapper.getElement()).toMatchSnapshot();
	});
});
