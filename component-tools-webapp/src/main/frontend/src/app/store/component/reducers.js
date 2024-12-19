/**
 *  Copyright (C) 2006-2024 Talend Inc. - www.talend.com
 *
 *  Licensed under the Apache License, Version 2.0 (the 'License');
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an 'AS IS' BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import {
	CHANGE_COMPONENT_ERRORS,
	CHANGE_COMPONENT_PROPERTIES,
	GET_COMPONENT_LOADING,
	GET_COMPONENT_OK,
	BACK_TO_COMPONENT_EDIT,
	SUBMIT_COMPONENT,
} from '../constants';

export default (state = {}, action) => {
	switch (action.type) {
		case GET_COMPONENT_LOADING:
			return {
				...state,
				isLoading: true,
			};
		case GET_COMPONENT_OK:
			return {
				...state,
				uiSpec: action.uiSpec,
				isLoading: false,
				submitted: false,
			};
		case CHANGE_COMPONENT_ERRORS:
			return {
				...state,
				uiSpec: {
					...state.uiSpec,
					errors: action.errors,
				}
			};
		case CHANGE_COMPONENT_PROPERTIES:
			return {
				...state,
				uiSpec: {
					...state.uiSpec,
					properties: action.properties,
				}
			};
		case SUBMIT_COMPONENT:
			return {
				...state,
				uiSpec: {
					...state.uiSpec,
					properties: action.properties,
				},
				submitted: true,
			};
		case BACK_TO_COMPONENT_EDIT:
			return {
				...state,
				submitted: false,
			};
		default:
			return state;
	}
}
