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
import jsonpath from 'jsonpath';
import { COMPONENT_ACTION_URL } from '../constants';
import registry from './tcomp-triggers';

function getRequestPayload(parameters, properties = {}) {
	if (!parameters) {
		return properties;
	}

	const payload = {};
	for(const param of parameters) {
		payload[param.key] = jsonpath.query(properties, `$.${param.path}`, 1)[0];
	}

	return payload;
}

export default function onDefaultTrigger(registryCallback) {
	const customRegistry = {
		...registry,
		...registryCallback,
	};
	return function (event, { trigger, schema, properties }) {
		const payload = getRequestPayload(trigger.parameters, properties);
		return fetch(
			`${COMPONENT_ACTION_URL}?action=${trigger.action}&family=${trigger.family}&type=${trigger.type}`,
			{
				method: 'post',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify(payload),
			}
		)
			.then(resp => resp.json())
			.then(body => {
				return customRegistry[trigger.type]({
					schema,
					body,
					trigger,
					properties,
				});
			});
	}
}
