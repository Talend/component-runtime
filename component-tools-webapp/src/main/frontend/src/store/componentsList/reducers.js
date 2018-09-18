/**
 *  Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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

function getNextLevel(level) {
  switch (level) {
    case 'category': return 'family';
    case 'family': return 'component';
    default: return null;
  }
}

function updateSelected(tree, level, paths) {
  const nextLevel = getNextLevel(level);

  return tree.map(node => {
    const isOldPath = node.id === get(paths.old, [level, 'id']);
    const isNewPath = node.id === get(paths.new, [level, 'id']);
    const isNewSelectedNode = node === paths.new.selectedNode;
    if (isOldPath || isNewPath) {
      const newNode = {
        ...node,
        selected: isNewSelectedNode,
      };
      if (isNewSelectedNode && level !== 'component') {
        newNode.toggled = !newNode.toggled;
      }
      if (nextLevel) {
        newNode.children = updateSelected(node.children, nextLevel, paths);
      }
      return newNode;
    }
    return node;
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
      };
    case GET_COMPONENT_LIST_ERROR:
    case SELECT_COMPONENT_NODE:
      return {
        ...state,
        selectedNode: action.node,
        categories: updateSelected(
          state.categories,
          'category',
          {
            old: getPath(state.selectedNode),
            new: getPath(action.node),
          },
        ),
      };
    default:
      return state;
  }
}
