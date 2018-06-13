
function componentsToTree({ context, payload}, path) {
    const components = context.store.getState().cmf.collections.getIn([path.split('.')]);
    /**
     * [
     *  { categories: ['Business'], familyDisplayName: 'ServiceNow Input', displayNane: 'Service Now'
     * ]
     */
    return {
        
    }
}