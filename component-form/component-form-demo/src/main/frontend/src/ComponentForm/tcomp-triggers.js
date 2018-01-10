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

import deepClone from 'lodash.clonedeep';

function validation({ schema, body }) {
	if(body.rawData.status === 'KO') {
		return {
			errors: {
				[schema.key]: body.rawData.comment,
			}
		}
	}
}

function schema({ schema, body, properties, trigger }) {
	const newProperties = deepClone(properties);
	// TODO
	return { properties: newProperties };
}

function dynamic_values({ schema, body, properties, trigger }) {
	return;
}

const registry = {
	dynamic_values,
	schema,
	healthcheck: validation,
	validation
};

export default registry;
