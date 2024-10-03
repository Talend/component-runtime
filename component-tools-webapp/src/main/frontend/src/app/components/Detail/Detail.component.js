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

import React from "react";
import { Inject } from "@talend/react-cmf";
import { ComponentForm } from "@talend/react-containers";
// import service from "@talend/react-containers/lib/ComponentForm/kit/defaultRegistry";

const service = ComponentForm.kit.defaultRegistry;

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
	let submitted = null;
	let form = null;

	const validationWithSuccessFeedback = ({ trigger, schema, body, errors }) => {
		if (body.status === "OK" && trigger.type === "healthcheck") {
			props.onNotification({
				id: `healthcheck_${new Date().getTime()}`,
				title: "Success",
				message:
					body.comment ||
					`Trigger ${trigger.type} / ${trigger.family} / ${trigger.action} succeeded`,
			});
		}
		return service.validation({ schema, body, errors });
	};
	const registry = {
		healthcheck: validationWithSuccessFeedback,
		built_in_suggestable: function () {
			return {
				titleMap: Array(10)
					.fill()
					.map((x, i) => ({ name: "Proposal " + i, value: "value_" + i })),
			};
		},
	};

	if (!props.definitionURL) {
		notSelected = <NoSelectedComponent />;
	} else {
		const lang =
			new URLSearchParams(window.location.search).get("language") || "en";
		form = (
			<Inject
				id="detail-form"
				componentId="detail-form"
				component="ComponentForm"
				definitionURL={`/api/v1${props.definitionURL}${props.definitionURL.indexOf("?") > 0 ? "&" : "?"}language=${lang}`}
				triggerURL={`/api/v1/application/action`}
				lang={lang}
				customTriggers={registry}
			/>
		);
		if (props.submitted) {
			const configuration = ComponentForm.kit.flatten(props.uiSpec.properties);
			submitted = (
				<div>
					<pre>{JSON.stringify(configuration, undefined, 2)}</pre>
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
			<div className="col-md-6">{submitted}</div>
		</div>
	);
}

export default Detail;
