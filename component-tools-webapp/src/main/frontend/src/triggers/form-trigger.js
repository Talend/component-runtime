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

import get from 'lodash/get';
import flatten from './flatten';
import defaultRegistry from './service';

function extractRequestPayload(parameters = [], properties) {
	const payload = {};
	for (const param of parameters) {
		const value = get(properties, param.path);
		Object.assign(payload, flatten(value, param.key));
	}

	return payload;
}

export default function getDefaultTrigger({ url, customRegistry }) {
	return function onDefaultTrigger(event, { trigger, schema, properties, errors }) {
		const services = {
			...defaultRegistry,
			...customRegistry,
		};
		const payload = extractRequestPayload(trigger.parameters, properties);
		return fetch(
			`${url}?action=${trigger.action}&family=${trigger.family}&type=${trigger.type}`,
			{
				method: 'POST',
				headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
				body: JSON.stringify(payload),
			}
		)
			.then(resp => resp.json())
			.then(body => {
				return services[trigger.type]({
					body,
					errors,
					properties,
					schema,
					trigger,
				});
			});
	}
};
