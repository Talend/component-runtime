/**
 *  Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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
 */function getId() {
	// eslint-disable-next-line no-bitwise
	return (((1 + Math.random()) * 0x10000) | 0).toString(16).substring(1);
}

export default function getUUID() {
	return `${getId()}${getId()}-${getId()}-${getId()}-${getId()}-${getId()}${getId()}${getId()}`;
}
