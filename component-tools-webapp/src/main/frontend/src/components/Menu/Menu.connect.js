/**
 *  Copyright (C) 2006-2023 Talend Inc. - www.talend.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';

import { getComponentsList, switchTree, closeDocumentationModal } from '../../store/componentsList/actions';
import { selectComponent, toggleComponent } from '../../store/component/actions';
import Menu from './Menu.component';

function mapDispatchToProps(dispatch) {
	return {
		getComponentsList: bindActionCreators(getComponentsList, dispatch),
		selectComponent: bindActionCreators(selectComponent, dispatch),
		toggleComponent: bindActionCreators(toggleComponent, dispatch),
		onSwitch: () => dispatch(switchTree()),
		onDocumentationModalClose: () => dispatch(closeDocumentationModal()),
	}
}

function mapStateToProps(state) {
	return (state.app || {}).componentsList || {};
}

export default connect(mapStateToProps, mapDispatchToProps)(Menu);
