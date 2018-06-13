import cmf from '@talend/react-cmf';
import { call, put } from 'redux-saga/effects';

// function nameComparator() {
//     return (a, b) => {
//       const v1 = a.name.toLowerCase();
//       const v2 = b.name.toLowerCase();
//       if (v1 < v2) {
//         return -1;
//       }
//       if (v1 > v2) {
//         return 1;
//       }
//       return 0;
//     };
//   }
  
//   function createComponentNode(familyNode, component) {
//     const { customIconType, icon } = component.icon;
//     const componentId = component.id.id;
  
//     const node = {
//       ...component,
//       name: component.displayName,
//       familyId: component.id.family,
//       icon: customIconType ? { name: `src-/api/v1/component/icon/${component.id.id}`} : icon,
//       $$id: componentId,
//       $$detail: component.links[0].path,
//       $$type: 'component',
//       $$parent: familyNode,
//     };
  
//     if (!node.categories || !node.categories.length) {
//       node.categories = ['Others'];
//     }
  
//     return node;
//   }
  
//   function getOrCreateCategoryNode(categories, categoryId) {
//     let categoryNode = categories.find(cat => cat.id === categoryId);
//     // add missing category
//     if (!categoryNode) {
//       categoryNode = {
//         id: categoryId,
//         name: categoryId,
//         children: [],
//         toggled: false,
//         $$type: 'category',
//       };
//       categories.push(categoryNode);
//       categories.sort(nameComparator());
//     }
  
//     return categoryNode;
//   }
  
//   function getOrCreateFamilyNode(categoryNode, component, dispatch) {
//     const familyId = component.id.familyId;
//     const families = categoryNode.children;
//     const { iconFamily, familyDisplayName } = component;
//     let familyNode = families.find(fam => fam.id === familyId);
//     // add missing family in category
//     if (!familyNode) {
//       familyNode = {
//         id: familyId,
//         name: familyDisplayName,
//         icon: iconFamily.customIconType ?
//           { name: `src-/api/v1/component/icon/family/${familyId}`} :
//           iconFamily.icon,
//         toggled: false,
//         children: [],
//         $$type: 'family',
//         $$parent: categoryNode,
//         actions: [
//           {
//             label: 'Reload',
//             icon: 'talend-refresh',
//             action: item => {
//               dispatch(familyIsReloading());
//               fetch(`api/v1/tools/admin/${familyId}`, { method: 'HEAD' })
//                 .then(noPayload => dispatch(onFamilyReload(familyDisplayName)))
//                 .catch(error => dispatch(onFamilyReloadError(error, familyDisplayName)));
//             }
//           }
//         ]
//       };
//       families.push(familyNode);
//       families.sort(nameComparator());
//     }
  
//     return familyNode;
//   }
// function createTree(components, dispatch) {
//     const treeview = components.reduce((accu, component) => {
//       component.categories.forEach(categoryId => {
//         let categoryNode = getOrCreateCategoryNode(accu, categoryId);
//         let familyNode = getOrCreateFamilyNode(categoryNode, component, dispatch);
  
//         const node = createComponentNode(familyNode, component);
//         familyNode.children.push(node);
//         familyNode.children.sort(nameComparator());
//       });
  
//       return accu;
//     }, []);

//   // now open the first part of the tree
//   let children = treeview;
//   while (children && children.length) {
//     children[0].toggled = true;
//     children = children[0].children;
//   }

//   return treeview;
// }

    
function* handle() {
    const { response, data } = yield call(cmf.sagas.http.get, 'api/v1/application/index');
    if (!response.ok) {
        return;
    }
    yield put(cmf.actions.collections.addOrReplace('components', data.components));
}

export default {
    'Menu#default': handle,
}