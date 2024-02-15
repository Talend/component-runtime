/**
 *  Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
export const SERVER_URL = '/api';
export const CONFIGURATION_URL = `${SERVER_URL}/project/configuration`;
export const GENERATOR_ZIP_URL = `${SERVER_URL}/project/zip/form`;
export const GENERATOR_OPENAPI_ZIP_URL = `${SERVER_URL}/project/openapi/zip/form`;
export const GENERATOR_GITHUB_URL = `${SERVER_URL}/project/github`;
export const GENERATOR_OPENAPI_GITHUB_URL = `${SERVER_URL}/project/openapi/github`;
export const COMPONENT_TYPE_SOURCE = 'Input';
export const COMPONENT_TYPE_PROCESSOR = 'Processor';
export const COMPONENT_TYPE_SINK = 'Output';
export const COMPONENT_TYPES = [
	COMPONENT_TYPE_SOURCE,
	COMPONENT_TYPE_PROCESSOR,
	COMPONENT_TYPE_SINK,
];
