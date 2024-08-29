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
import { Navigate, useMatch } from 'react-router';
import { Action } from '@talend/react-components/lib/Actions';

import ComponentEditForm from '../ComponentEditForm';
import TileContext from '../../tile';
import ComponentsContext from '../../ComponentsContext';

import theme from './Component.module.scss';

function getComponent(service, index) {
	return service.components[parseInt(index, 10)];
}

export default function Component(props) {
	const match = useMatch('/component/:componentId');
	const componentIndex = match.params.componentId;
	return (
		<ComponentsContext.Consumer>
			{(components) => {
				if (componentIndex === 'last') {
					return <Navigate to={`/component/${components.components.length - 1}`} />;
				}
				if (componentIndex === 0 && components.components.length === 0) {
					components.addComponent();
					return null;
				}
				const component = getComponent(components, componentIndex);
				const cols = [
					<ComponentEditForm
						component={component}
						service={components}
						withIO={components.withIO}
					/>,
				];
				return (
					<div className={theme.Component}>
						<TileContext.Provider value={cols}>
							<TileContext.Consumer>
								{(service) =>
									service.tiles.map((col, index) => {
										if (index < service.tiles.length - 2) {
											return (
												<div className={theme.hidden} key={index}>
													{col}
												</div>
											);
										}
										return (
											<div className={theme.column} key={index}>
												{index > 0 && (
													<Action
														className="btn-icon-only"
														bsStyle="link"
														onClick={() => service.close(index)}
														label="close"
														hideLabel
														icon="talend-cross"
													/>
												)}
												{col}
											</div>
										);
									})
								}
							</TileContext.Consumer>
						</TileContext.Provider>
					</div>
				);
			}}
		</ComponentsContext.Consumer>
	);
}
