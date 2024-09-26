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
import PropTypes from 'prop-types';

import Icon from '@talend/react-components/lib/Icon';

import Input from '../Input';
import Help from '../Help';
import Mapper from '../Mapper';
import Processor from '../Processor';

import theme from './ComponentEditForm.module.scss';
import {
	COMPONENT_TYPES,
	COMPONENT_TYPE_SOURCE,
	COMPONENT_TYPE_PROCESSOR,
	COMPONENT_TYPE_SINK,
} from '../../constants';

function onComponentNameChange(service, component) {
	return (newName) => {
		// eslint-disable-next-line no-param-reassign
		component.name = newName;
		service.updateComponent();
	};
}

function ComponentEditForm(props) {
	if (!props.component) {
		return null;
	}
	return (
		<div>
			<div className={theme['form-row']}>
				<h1>
					<em>{props.component.configuration.name || ''}</em> Configuration
				</h1>
			</div>

			<div className={theme['form-row']}>
				<h2>Configuration</h2>
				<form noValidate onSubmit={(e) => e.preventDefault()} className="form">
					<div className="form-group">
						<div className={theme.label}>
							<label className="control-label" htmlFor="componentName">
								Name
							</label>
							<Help
								title="Component Name"
								i18nKey="component_name"
								content={
									<div>
										<p>Each component has a name which must be unique into a family.</p>
										<p>
											<Icon name="talend-info-circle" /> The name must be a valid java name (no
											space, special characters, ...).
										</p>
									</div>
								}
							/>
						</div>
						<Input
							className="form-control"
							id="componentName"
							type="text"
							placeholder="Enter the component name..."
							required
							minLength="1"
							onChange={onComponentNameChange(props.service, props.component)}
							aggregate={props.component.configuration}
							accessor="name"
						/>
					</div>
				</form>
			</div>
			{props.component.type === COMPONENT_TYPE_SOURCE && <Mapper component={props.component} />}
			{props.component.type === COMPONENT_TYPE_PROCESSOR && (
				<Processor
					component={props.component}
					addInput={props.service.addInput}
					addOutput={props.service.addOutput}
				/>
			)}
			{props.component.type === COMPONENT_TYPE_SINK && (
				<Processor component={props.component} addInput={props.service.addInput} sink />
			)}
		</div>
	);
}

ComponentEditForm.displayName = 'ComponentEditForm';
ComponentEditForm.propTypes = {
	component: PropTypes.shape({
		name: PropTypes.string,
		type: PropTypes.oneOf(COMPONENT_TYPES),
		configuration: PropTypes.object,
	}),
	service: PropTypes.shape({
		addInput: PropTypes.func,
		addOutput: PropTypes.func,
		setComponentType: PropTypes.func,
		TYPES: PropTypes.array,
	}),
	withIO: PropTypes.bool,
};

export default ComponentEditForm;
