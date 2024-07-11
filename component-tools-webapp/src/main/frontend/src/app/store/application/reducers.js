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
 FAMILY_RELOADING,
 FAMILY_RELOADED,
 FAMILY_RELOADED_ERROR,
 ADD_NOTIFICATION,
 REMOVE_NOTIFICATION,
 DOCUMENTATION_LOADING,
 DOCUMENTATION_LOADED,
 DOCUMENTATION_LOADED_ERROR,
} from '../constants';

function addNotification(notifications, notification) {
  if (!notification) {
    return notifications;
  }
  return notifications.concat([ notification ]);
}

function removeNotification(notifications, notification) {
  return notifications.filter(n => n != notification);
}

export default (state = { notifications: [] }, action) => {
 switch(action.type) {
   case ADD_NOTIFICATION:
    return {
      ...state,
      notifications: addNotification(state.notifications, action.notification)
    };
   case REMOVE_NOTIFICATION:
    return {
      ...state,
      notifications: removeNotification(state.notifications, action.notification)
    };
   case FAMILY_RELOADING:
   case FAMILY_RELOADED:
   case FAMILY_RELOADED_ERROR:
   case DOCUMENTATION_LOADING:
   case DOCUMENTATION_LOADED:
   case DOCUMENTATION_LOADED_ERROR:
     return {
       ...state,
       isLoading: action.type === FAMILY_RELOADING || action.type === DOCUMENTATION_LOADING,
       notifications: addNotification(state.notifications, action.notification)
     };
   default:
    return state;
 }
}
