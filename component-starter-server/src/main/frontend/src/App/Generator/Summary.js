/**
 *  Copyright (C) 2006-2019 Talend Inc. - www.talend.com
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
import React from 'react';

import theme from './Summary.scss';

function Info(props) {
	return (
		<div className={theme.Info}>
			<label>{props.name}:</label>
			<span>{props.value}</span>
		</div>
	);
}

export default function Finish(props) {
	const p = props.project; // just to make it shorter in the template
	if (!p) {
		return <div />;
	}
	return (
		<div className={theme.Summary}>
			<Info name="Name" value={p.name} />
			<Info name="Build Tool" value={p.buildType} />
			<Info name="Coordinates" value={`${p.group}:${p.artifact}:${p.version}`} />
			<Info
				name="Components"
				value={`${p.sources.length} inputs and ${p.processors.length} processors`}
			/>
		</div>
	);
}
