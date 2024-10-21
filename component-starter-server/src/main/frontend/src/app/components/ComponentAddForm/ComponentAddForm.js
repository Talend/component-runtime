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
import React from 'react';

import { ActionButton } from '@talend/react-components/lib/Actions';
import './ComponentAddForm.scss';

import {
	COMPONENT_TYPE_SOURCE,
	COMPONENT_TYPE_PROCESSOR,
	COMPONENT_TYPE_SINK,
} from '../../constants';
import ComponentsContext from '../../ComponentsContext';
import { useNavigate } from 'react-router';

export default function ComponentAddForm(props) {
	const navigate = useNavigate();
	const components = React.useContext(ComponentsContext.raw);
	const [once, setOnce] = React.useState(false);
	const callback = (c) => {
		navigate(`/component/${components.components.indexOf(c)}`);
	};
	if (once) {
		return null;
	}
	if (!components.withIO) {
		components.addComponent(COMPONENT_TYPE_PROCESSOR, callback);
		setOnce(true);
		return null;
	}
	return (
		<div style={{ margin: 20 }} className="component-add-form_wrapper">
			<h1>Add a Component</h1>
			<p>Talend Component Kit supports the following types of components:</p>
			<div style={{ display: 'flex' }}>
				<div className="panel panel-default">
					<div className="panel-heading">{COMPONENT_TYPE_SOURCE}</div>
					<div className="panel-body">
						it is a component creating records from itself. It only supports to create a main output
						branch of records.
					</div>
					<div className="panel-footer">
						<ActionButton
							bsStyle="info"
							label={`Add ${COMPONENT_TYPE_SOURCE}`}
							onClick={() => components.addComponent(COMPONENT_TYPE_SOURCE, callback)}
						/>
					</div>
				</div>
				<div className="panel panel-default">
					<div className="panel-heading">{COMPONENT_TYPE_PROCESSOR}</div>
					<div className="panel-body">
						this component type can read from 1 or multiple inputs the data, process them and create
						0 or multiple outputs.
					</div>
					<div className="panel-footer">
						<ActionButton
							bsStyle="info"
							label={`Add ${COMPONENT_TYPE_PROCESSOR}`}
							onClick={() => components.addComponent(COMPONENT_TYPE_PROCESSOR, callback)}
						/>
					</div>
				</div>
				<div className="panel panel-default">
					<div className="panel-heading">{COMPONENT_TYPE_SINK}</div>
					<div className="panel-body">
						it is a component type which can write from 1 input the data. It has no output.
					</div>
					<div className="panel-footer">
						<ActionButton
							bsStyle="info"
							label={`Add ${COMPONENT_TYPE_SINK}`}
							onClick={() => components.addComponent(COMPONENT_TYPE_SINK, callback)}
						/>
					</div>
				</div>
			</div>
		</div>
	);
}
