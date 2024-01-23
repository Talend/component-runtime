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

import get from 'lodash/get'
import {
  GET_COMPONENT_LIST_LOADING,
  GET_COMPONENT_LIST_ERROR,
  GET_COMPONENT_LIST_OK,
  SELECT_COMPONENT_NODE,
  TOGGLE_COMPONENT_NODE,
  DOCUMENTATION_LOADED,
  CLOSE_DOCUMENTATION_MODAL,
} from '../constants';

function getCategory(node) {
  switch (node.$$type) {
    case 'component': return node.$$parent.$$parent;
    case 'family': return node.$$parent;
    case 'category': return node;
    default: return null;
  }
}

function getFamily(node) {
  switch (node.$$type) {
    case 'component': return node.$$parent;
    case 'family': return node;
    default: return null;
  }
}

function getComponent(node) {
  if (node.$$type === 'component') {
    return node;
  }
  return null;
}

function getPath(selectedNode) {
  return {
    category: selectedNode && getCategory(selectedNode),
    family: selectedNode && getFamily(selectedNode),
    component: selectedNode && getComponent(selectedNode),
    selectedNode,
  };
}

function updateToggle(tree, currentNode) {
  return tree.map(node => {
    if (currentNode.id === node.id) {
      return {
        ...node,
        isOpened: !node.isOpened,
      };
    } else {
      if (node.children) {
        return {
          ...node,
          children: updateToggle(node.children, currentNode),
        }
      }
      return node;
    }
  });
}

export default (state = {}, action) => {
  switch(action.type) {
    case GET_COMPONENT_LIST_LOADING:
      return {
        ...state,
        isLoading: true,
        configurationSelected: action.configuration,
      };
    case GET_COMPONENT_LIST_OK:
      return {
        ...state,
        isLoading: false,
        categories: action.categories,
        error: undefined,
      };
    case GET_COMPONENT_LIST_ERROR:
      return {
        ...state,
        error: action.error,
      };
    case SELECT_COMPONENT_NODE:
      return {
        ...state,
        selectedId: action.node.id,
        selectedNode: action.node,
      };
    case TOGGLE_COMPONENT_NODE:
      return {
        ...state,
        categories: updateToggle(state.categories, action.node),
      };
    case DOCUMENTATION_LOADED:
      return {
        ...state,
        documentation: action.documentation,
        displayDocumentation: true,
      };
    case CLOSE_DOCUMENTATION_MODAL:
      return {
        ...state,
        documentation: null,
        displayDocumentation: false,
      };
    default:
      return state;
  }
}
