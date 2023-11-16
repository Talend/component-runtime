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

import {
    ADD_NOTIFICATION,
	CHANGE_COMPONENT_ERRORS,
	CHANGE_COMPONENT_PROPERTIES,
	GET_COMPONENT_LOADING,
	GET_COMPONENT_ERROR,
	GET_COMPONENT_OK,
	SELECT_COMPONENT_NODE,
	TOGGLE_COMPONENT_NODE,
	BACK_TO_COMPONENT_EDIT,
	SUBMIT_COMPONENT,
} from '../constants';

export function isLoadingComponent() {
	return {
		type: GET_COMPONENT_LOADING,
	};
}

export function getComponentOK(uiSpec) {
	return {
		type: GET_COMPONENT_OK,
		uiSpec,
	};
}

export function getComponentERROR(error) {
	return {
		type: GET_COMPONENT_ERROR,
		error: error,
	};
}

export function backToComponentEdit(event, properties) {
	return {
		type: BACK_TO_COMPONENT_EDIT,
		event,
		properties,
	};
}

export function onComponentPropertiesChange(event, { properties }) {
	return {
		type: CHANGE_COMPONENT_PROPERTIES,
		event,
		properties,
	};
}

export function onComponentErrorsChange(event, errors) {
	return {
		type: CHANGE_COMPONENT_ERRORS,
		event,
		errors,
	};
}

export function submitComponent(event, properties) {
	return {
		type: SUBMIT_COMPONENT,
		event,
		properties,
	};
}

export function onNotification( notification) {
	return {
		type: ADD_NOTIFICATION,
		notification,
	};
}

export function selectNode(node) {
	return {
		type: SELECT_COMPONENT_NODE,
		node,
	};
}

export function selectComponent(node) {
	return selectNode(node);
}

export function toggleComponent(node) {
	return {
        type: TOGGLE_COMPONENT_NODE,
        node,
    };
}
