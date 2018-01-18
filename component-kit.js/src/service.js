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
'use strict';

import clonedeep from 'lodash.clonedeep';

export default class {
  extractPropertyByPath(root, path) {
    let object = root;
    const levels = path.split('.');
    for (const next of levels) {
      object = object[next];
      if (object === undefined) { // undefined, not falsy!
        return undefined;
      }
    }
    return object;
  }

  validation({ schema, body }) {
    return {
      errors: {
        [schema.key]: body.status === 'KO' ? body.comment : undefined,
      }
    };
  }

  schema({ schema, body, properties, trigger }) {
    if (!body.entries || !trigger.options || trigger.options.length == 0) {
      return {
        properties,
        errors: {
          [schema.key]: body.error
        }
      };
    }
    let newProperties = clonedeep(properties);
    for (const option of trigger.options) {
      const lastDot = option.path.lastIndexOf('.');
      const parentPath = lastDot > 0 ? option.path.substring(0, lastDot) : option.path;
      const directChildPath = lastDot > 0 ? option.path.substring(lastDot + 1) : option.path;
      let mutable = parentPath === option.path ? newProperties : this.extractPropertyByPath(newProperties, parentPath);
      if (!mutable) {
        continue;
      }
      mutable[directChildPath] = option.type === 'array' ? body.entries.map(e => e.name) : body.entries.reduce({}, (a, e) => {
        a[e.name] = e.type;
        return a;
      });
    }
    return {
      properties: newProperties,
      errors: {
        [schema.key]: body.error
      }
    };
  }

  dynamic_values({ schema, body, properties, trigger }) {
    // for now it is set on the server side so no-op is ok
    return { properties };
  }

  extractRequestPayload(parameters, properties) {
    if (!parameters) {
      return {};
    }

    const payload = {};
    for(const param of parameters) {
      const value = this.extractPropertyByPath(properties, param.path);
      if (value !== undefined) {
        if (Array.isArray(value)) {
          if (value.length > 0) { // TODO: support array of objects
            let index = 0;
            for (const item of value) {
              payload[param.key + '[' + index + ']'] = '' + item;
              index++;
            }
          }
        } else {
          payload[param.key] = '' + this.extractPropertyByPath(properties, param.path);
        }
      }
    }

    return payload;
  }
};
