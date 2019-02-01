import React from 'react';
import { shallow } from 'enzyme';

import Component from './DatastoreForm.component';

describe('Component DatastoreForm', () => {
	it('should render', () => {
		const wrapper = shallow(
			<Component />
		);
		expect(wrapper.getElement()).toMatchSnapshot();
	});
});
