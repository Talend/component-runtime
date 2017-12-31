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

export default function onDefaultTrigger(event, { trigger, schema, properties }) {
	const payload = {};
	for(const param of trigger.parameters) {
		payload[param.key] = jsonpath.query(properties, `$.${param.path}`, 1)[0];
	}
	return fetch(
		`${COMPONENT_ACTION_URL}?action=${trigger.action}&family=${trigger.family}&type=${trigger.type}`,
		{
			method: 'post',
			headers: { 'Content-Type': 'application/json' },
			body: JSON.stringify(payload),
		}
	).then(resp => {
		if(resp.ok) {
			return {};
		}
		return resp.json()
			.then(body => {
				const errorMessage = body && body.error ?
					body.error :
					`${resp.status}: ${resp.statusText}`;
				return { errors: {
					[schema.key]: errorMessage,
				} };
			});
	});
}