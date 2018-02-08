/**
 *  Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
  GET_COMPONENT_LIST_LOADING,
  GET_COMPONENT_LIST_ERROR,
  GET_COMPONENT_LIST_OK,
  GET_ICONS_LIST_OK,
  FAMILY_RELOADING,
  FAMILY_RELOADED,
  FAMILY_RELOADED_ERROR
} from '../constants';

function createComponentNode(familyNode, component) {
  const { customIconType, icon } = component.icon;
  const componentId = component.id.id;

  const node = {
    ...component,
    name: component.displayName,
    familyId: component.id.family,
    icon: customIconType ? { name: `src-/api/v1/component/icon/${component.id.id}`} : icon,
    $$id: componentId,
    $$detail: component.links[0].path,
    $$type: 'component',
    $$parent: familyNode,
  };

  if (!node.categories || !node.categories.length) {
    node.categories = ['Others'];
  }

  return node;
}

function getOrCreateCategoryNode(categories, categoryId) {
  let categoryNode = categories.find(cat => cat.id === categoryId);
  // add missing category
  if (!categoryNode) {
    categoryNode = {
      id: categoryId,
      name: categoryId,
      children: [],
      toggled: false,
      $$type: 'category',
    };
    categories.push(categoryNode);
  }

  return categoryNode;
}

function getOrCreateFamilyNode(categoryNode, component, dispatch) {
  const familyId = component.id.familyId;
  const families = categoryNode.children;
  const { iconFamily, familyDisplayName } = component;
  let familyNode = families.find(fam => fam.id === familyId);
  // add missing family in category
  if (!familyNode) {
    familyNode = {
      id: familyId,
      name: familyDisplayName,
      icon: iconFamily.customIconType ?
        { name: `src-/api/v1/component/icon/family/${familyId}`} :
        iconFamily.icon,
      toggled: false,
      children: [],
      $$type: 'family',
      $$parent: categoryNode,
      actions: [
        {
          label: 'Reload',
          icon: 'talend-refresh',
          action: item => {
            dispatch(familyIsReloading());
            fetch(`api/v1/tools/admin/${familyId}`, { method: 'HEAD' })
              .then(noPayload => dispatch(onFamilyReload(familyDisplayName)))
              .catch(error => dispatch(onFamilyReloadError(error, familyDisplayName)));
          }
        }
      ]
    };
    families.push(familyNode);
  }

  return familyNode;
}

function createTree(components, dispatch) {
  const treeview = components.reduce((accu, component) => {
    component.categories.forEach(categoryId => {
      let categoryNode = getOrCreateCategoryNode(accu, categoryId);
      let familyNode = getOrCreateFamilyNode(categoryNode, component, dispatch);

      const node = createComponentNode(familyNode, component);
      familyNode.children.push(node);
    });

    return accu;
  }, []);

  // now open the first part of the tree
  let children = treeview;
  while (children && children.length) {
    children[0].toggled = true;
    children = children[0].children;
  }

  return treeview;
}

function isLoadingComponentsList() {
  return {
    type: GET_COMPONENT_LIST_LOADING,
  };
}

function getComponentsListOK(categories) {
  return {
    type: GET_COMPONENT_LIST_OK,
    categories,
  };
}

function getComponentsListERROR(error) {
  return {
    type: GET_COMPONENT_LIST_ERROR,
    error: error,
  };
}

function onFamilyReload(family) {
  return {
    type: FAMILY_RELOADED,
    notification: {
      id: `family-reloading-success_family_${new Date().getTime()}`,
      type: 'info',
      title: `Reloaded family ${family}`,
      message: `Family ${family} successfully reloaded`
    }
  };
}

function onFamilyReloadError(family, error) {
  return {
    type: FAMILY_RELOADED_ERROR,
    notification: {
      id: `family-reloading-error_family_${new Date().getTime()}`,
      type: 'error',
      title: `Error Reloading family ${family}`,
      autoLeaveError: true,
      message: JSON.stringify(error)
    }
  };
}

function familyIsReloading() {
  return {
    type: FAMILY_RELOADING
  };
}

export function getComponentsList() {
  return dispatch => {
    dispatch(isLoadingComponentsList());
    fetch('api/v1/application/index')
      .then(resp => resp.json())
      .then(({ components }) => createTree(components, dispatch))
      .then(categories => { dispatch(getComponentsListOK(categories)); })
      .catch(error => dispatch(getComponentsListERROR(error)))
  };
}
