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

import TalendComponentKitService from './service';

export default class {
  constructor(props) {
    this.url = props.url;
    this.service = new TalendComponentKitService();
    this.registry = Object.assign({
      dynamic_values: this.service.dynamic_values,
      schema: this.service.schema,
      healthcheck: this.service.validation,
      validation: this.service.validation
    }, props.callbackRegistry || {});
  }

  onDefaultTrigger(event, { trigger, schema, properties }) {
    const payload = this.service.extractRequestPayload(trigger.parameters, properties);
    return fetch(
      `${this.url}?action=${trigger.action}&family=${trigger.family}&type=${trigger.type}`,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
        body: JSON.stringify(payload),
      }
    ).then(resp => resp.json())
    .then(body => {
      return this.registry[trigger.type]({
        schema,
        body,
        trigger,
        properties,
      });
    });
  }
};
