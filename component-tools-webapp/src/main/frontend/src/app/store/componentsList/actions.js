/**
 *  Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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

import { info as talendIcons } from "@talend/icons";
import {
  GET_COMPONENT_LIST_LOADING,
  GET_COMPONENT_LIST_ERROR,
  GET_COMPONENT_LIST_OK,
  FAMILY_RELOADING,
  FAMILY_RELOADED,
  FAMILY_RELOADED_ERROR,
  DOCUMENTATION_LOADING,
  DOCUMENTATION_LOADED,
  DOCUMENTATION_LOADED_ERROR,
  CLOSE_DOCUMENTATION_MODAL,
} from "../constants";
import asciidoctorFactory from "asciidoctor";

const asciidoctor = (function () {
  const cache = {};
  return () => {
    if (!cache.instance) {
      cache.instance = asciidoctorFactory();
    }
    return cache.instance;
  };
})();

function isInBundle(icon) {
  return !!Object.keys(talendIcons).find((elt) => elt === `talend-${icon}`);
}

function nameComparator() {
  return (a, b) => {
    const v1 = a.name.toLowerCase();
    const v2 = b.name.toLowerCase();
    if (v1 < v2) {
      return -1;
    }
    if (v1 > v2) {
      return 1;
    }
    return 0;
  };
}

function createComponentNode(familyNode, component, dispatch) {
  const { icon } = component.icon;
  const componentId = component.id.id;

  const node = {
    ...component,
    id: componentId,
    name: component.displayName,
    familyId: component.id.family,
    $$id: componentId,
    $$detail: `${component.links[0].path}${component.links[0].path.indexOf("?") > 0 ? "&" : "?"}language=${new URLSearchParams(window.location.search).get("language") || "en"}`,
    $$type: "component",
    $$parent: familyNode,
    actions: [
      {
        label: "Documentation",
        icon: "talend-question-circle",
        action: (item) => {
          dispatch(documentationIsLoading());
          fetch(
            `api/v1/documentation/component/${componentId}?language=${new URLSearchParams(window.location.search).get("language") || "en"}`
          )
            .then((response) => response.json())
            .then((response) => dispatch(onDocumentation(component, response)))
            .catch((error) => dispatch(onDocumentationError(error, component)));
        },
      },
    ],
  };

  if (isInBundle(icon)) {
    node.icon = { name: `talend-${icon}` };
  } else {
    node.icon = { name: `src-/api/v1/component/icon/${component.id.id}` };
  }
  if (!node.categories || !node.categories.length) {
    node.categories = ["Others"];
  }

  return node;
}

function getOrCreateCategoryNode(categories, categoryId) {
  let categoryNode = categories.find((cat) => cat.id === categoryId);
  // add missing category
  if (!categoryNode) {
    categoryNode = {
      id: categoryId,
      name: categoryId,
      children: [],
      isOpened: false,
      $$type: "category",
    };
    categories.push(categoryNode);
    categories.sort(nameComparator());
  }

  return categoryNode;
}

function getOrCreateFamilyNode(categoryNode, component, dispatch) {
  const familyId = component.id.familyId;
  const families = categoryNode.children;
  const { iconFamily, familyDisplayName } = component;
  let familyNode = families.find((fam) => fam.id === familyId);
  // add missing family in category
  if (!familyNode) {
    familyNode = {
      id: familyId,
      name: familyDisplayName,
      isOpened: false,
      children: [],
      $$type: "family",
      $$parent: categoryNode,
      actions: [
        {
          label: "Reload",
          icon: "talend-refresh",
          action: (item) => {
            dispatch(familyIsReloading());
            fetch(`api/v1/tools/admin/${familyId}`, { method: "HEAD" })
              .then((noPayload) => dispatch(onFamilyReload(familyDisplayName)))
              .catch((error) =>
                dispatch(onFamilyReloadError(error, familyDisplayName))
              );
          },
        },
      ],
    };

    if (isInBundle(iconFamily.icon)) {
      familyNode.icon = { name: `talend-${iconFamily.icon}` };
    } else {
      familyNode.icon = {
        name: `src-/api/v1/component/icon/family/${familyId}`,
      };
    }
    families.push(familyNode);
    families.sort(nameComparator());
  }

  return familyNode;
}

function doOpen(treeview) {
  let children = treeview;
  while (children && children.length) {
    children[0].isOpened = true;
    children = children[0].children;
  }
  return treeview;
}

function createTree(components, dispatch) {
  const treeview = components.reduce((accu, component) => {
    component.categories.forEach((categoryId) => {
      let categoryNode = getOrCreateCategoryNode(accu, categoryId);
      let familyNode = getOrCreateFamilyNode(categoryNode, component, dispatch);

      const node = createComponentNode(familyNode, component, dispatch);
      familyNode.children.push(node);
      familyNode.children.sort(nameComparator());
    });

    return accu;
  }, []);

  return doOpen(treeview);
}

function getParentNode(accu, id) {
  const found = accu.filter((it) => it.id == id);
  if (!found || found.length === 0) {
    const nested = accu
      .map((it) => it.children)
      .filter((it) => it)
      .map((children) => getParentNode(children, id))
      .filter((it) => it);
    return nested && nested.length > 0 && nested[0];
  }
  return found[0];
}

function createConfigTree(wrapper, dispatch) {
  const values = Object.values(wrapper.nodes);
  values.sort((v1, v2) => {
    // todo: better impl, this one works with simple components only
    if (v1.parentId && !v2.parentId) {
      return 1;
    }
    if (v2.parentId && !v1.parentId) {
      return -1;
    }
    if (v1.parentId == v2.id) {
      return 1;
    }
    if (v1.id == v2.parentId) {
      return -1;
    }
    if (
      v1.configurationType === "datastore" &&
      v2.configurationType === "dataset"
    ) {
      return -1;
    }
    if (
      v2.configurationType === "datastore" &&
      v1.configurationType === "dataset"
    ) {
      return 1;
    }
    return v1.id.localeCompare(v2.id);
  });
  const treeview = values.reduce((accu, node) => {
    const familyId = atob(node.id).split("#")[1];
    const treeNode = {
      ...node,
      familyId,
      $$id: node.id,
      $$type: "configuration",
      // TBD: icon:{ name: `src-/api/v1/icon/family/${familyId}`},
    };
    if (node.parentId) {
      const parent = getParentNode(accu, node.parentId);
      treeNode.$$parent = parent;
      (treeNode.name = `${node.displayName} (${node.configurationType})`),
        (treeNode.$$detail = `/application/detail/${node.id}?configuration=true&language=${new URLSearchParams(window.location.search).get("language") || "en"}`);
      parent.children = parent.children || [];
      parent.children.push(treeNode);
      parent.children.sort(nameComparator());
    } else {
      (treeNode.name = `${node.displayName}`), accu.push(treeNode);
    }
    return accu;
  }, []);

  return doOpen(treeview);
}

function isLoadingComponentsList(configuration) {
  return {
    type: GET_COMPONENT_LIST_LOADING,
    configuration,
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
      type: "info",
      title: `Reloaded family ${family}`,
      message: `Family ${family} successfully reloaded, refresh the page when you want.`,
    },
  };
}

function onFamilyReloadError(family, error) {
  return {
    type: FAMILY_RELOADED_ERROR,
    notification: {
      id: `family-reloading-error_family_${new Date().getTime()}`,
      type: "error",
      title: `Error Reloading family ${family}`,
      autoLeaveError: true,
      message: JSON.stringify(error),
    },
  };
}

function onDocumentation(component, response) {
  return {
    type: DOCUMENTATION_LOADED,
    documentation: asciidoctor().convert(response.source),
  };
}

function onDocumentationError(error, component) {
  return {
    type: DOCUMENTATION_LOADED_ERROR,
    notification: {
      id: `component-documentation-error_${new Date().getTime()}`,
      type: "error",
      title: `Error Loading documentation for ${component.displayName}`,
      autoLeaveError: true,
      message: JSON.stringify(error),
    },
  };
}

function familyIsReloading() {
  return {
    type: FAMILY_RELOADING,
  };
}

function documentationIsLoading() {
  return {
    type: DOCUMENTATION_LOADING,
  };
}

export function closeDocumentationModal() {
  return {
    type: CLOSE_DOCUMENTATION_MODAL,
  };
}

export function getComponentsList(data) {
  const configuration = (data && data.configuration) || false;
  return (dispatch) => {
    dispatch(isLoadingComponentsList(configuration));
    fetch(`api/v1/application/index?configuration=${configuration}`)
      .then((resp) => resp.json())
      .then((data) =>
        data.components
          ? createTree(data.components, dispatch)
          : createConfigTree(data, dispatch)
      )
      .then((categories) => {
        dispatch(getComponentsListOK(categories));
      })
      .catch((error) => dispatch(getComponentsListERROR(error)));
  };
}
