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

import { cmfConnect } from '@talend/react-cmf';
import { bindActionCreators } from 'redux';
import get from 'lodash/get';
import {
	backToComponentEdit,
	onComponentPropertiesChange,
	onComponentErrorsChange,
	submitComponent,
	onNotification,
} from '../../store/component/actions';
import startSaga from './Detail.saga';

import Detail from './Detail.component';

function mapStateToProps(state) {
	return {
		definitionURL: get(state, 'app.componentsList.selectedNode.$$detail'),
		...(state.app || {}).component || {},
	}
}

function mapDispatchToProps(dispatch) {
	return {
		backToComponentEdit: bindActionCreators(backToComponentEdit, dispatch),
		onChange: bindActionCreators(onComponentPropertiesChange, dispatch),
		onErrors: bindActionCreators(onComponentErrorsChange, dispatch),
		onNotification: bindActionCreators(onNotification, dispatch),
	};
}

Detail.displayName = 'Detail';
Detail.sagas = {
    'Detail::start': startSaga,
};
export default cmfConnect({ mapStateToProps, mapDispatchToProps })(Detail);
