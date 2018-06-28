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

import React from 'react';
import { Inject } from '@talend/react-cmf';

function NoSelectedComponent() {
	return (
		<div>
			<h1>No component selected</h1>
			<p>Click on a component to see its form</p>
		</div>
	);
}

function Detail(props) {
	let notSelected = null;
	let submited = null;
	let form = null;
	if (!props.definitionURL) {
		notSelected = (<NoSelectedComponent/>);
	}  else {
		form = (
			<Inject
				component="ComponentForm"
				definitionURL={`/api/v1/${props.definitionURL}`}
				triggerURL="/api/v1/application/action"
				onSubmit={props.onSubmit}
			/>
		);
		if (props.submitted) {
			const configuration = kit.flatten(props.uiSpec.properties);
			submited = (
				<div>
					<pre>{JSON.stringify(configuration, undefined, 2)}</pre>
					<button className="btn btn-success" onClick={props.backToComponentEdit}>Back to form</button>
				</div>
			);
		}
	}
	return (
		<div>
			<div className="col-md-6">
				{notSelected}
				{form}
			</div>
			<div className="col-md-6">
				{submited}
			</div>
		</div>
	);
}

export default Detail;
