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


/**
 * [
 *  { categories: ['Business'], familyDisplayName: 'ServiceNow Input', displayNane: 'Service Now'
 * ]
 * {
 * item: {
 *  $$type: category
 *   name: 
 * id:
 *  children: [{}]
 *   }
 * }
 */

function createTree(components) {
    const treeview = components.reduce((accu, component) => {
      component.categories.forEach(categoryId => {
        let categoryNode = getOrCreateCategoryNode(accu, categoryId);
        let familyNode = getOrCreateFamilyNode(categoryNode, component, dispatch);
  
        const node = createComponentNode(familyNode, component);
        familyNode.children.push(node);
        familyNode.children.sort(nameComparator());
      });
  
      return accu;
    }, []);

}

const cache = {}

function componentsToTree({ context, payload}, path) {
    const components = context.store.getState().cmf.collections.getIn([path.split('.')]);
    if (!components) {
        return;
    }
    if (cache.key === components) {
        return cache.value;
    }
    cache.key = components;
    cache.value = createTree(components);
    return cache.value;
}