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
import { Actions, Icon } from '@talend/react-components';

import TileContext from '../tile';
import Input from '../Component/Input';
import Help from '../Component/Help';
import Mapper from './Mapper';
import Processor from './Processor';

import theme from './Component.scss';

const TYPE_INPUT = 'Input';
const TYPE_PROCESSOR = 'Processor';

function onSelectSetState(type) {
	return state => Object.assign({}, state, {
		type,
		componentTypeActions: state.componentTypeActions.map(action => {
			if (action.type !== type) {
				action.className = 'btn-inverse';
			} else {
				action.className = 'btn-info';
			}
			return action
		}),
	});
}

const EMPTY = <div />;

export default class Component extends React.Component {
	constructor(props) {
		super(props);
		this.state = {
			drawers: [EMPTY],
			type: TYPE_INPUT,
			componentTypeActions: [
				{
					label: 'Input',
					type: TYPE_INPUT,
					className: 'btn-info',
					onClick: () => {
						this.setState(onSelectSetState(TYPE_INPUT));
					},
				},
				{
					label: 'Processor/Output',
					type: TYPE_PROCESSOR,
					className: 'btn-inverse',
					onClick: () => {
						this.setState(onSelectSetState(TYPE_PROCESSOR));
					},
				},
			],
		};

		this.componentPerType = this.state.componentTypeActions.reduce((a, i) => {
			a[i.type] = i;
			return a;
		}, {});
	}

	render() {
		const cols = [
			(
			<div>
				<div className={theme['form-row']}>
					<p className={theme.title}>
						<em>{this.props.component.configuration.name || ''}</em> Configuration
					</p>
					<div>
						<Actions actions={this.state.componentTypeActions} />
						<Help
							title="Component Type"
							i18nKey="component_type"
							content={
								<div>
									<p>
										Talend Component Kit supports two types of components:
									</p>
									<ul>
										<li>
											Input: it is a component creating records from itself. It only supports
											to create a main output branch of records.
										</li>
										<li>
											Processor: this component type can read from 1 or multiple inputs the
											data, process them and create 0 or multiple outputs.
										</li>
									</ul>
								</div>
							}
						/>
					</div>
				</div>

				<div className={theme['form-row']}>
					<p className={theme.title}>Configuration</p>
					<form novalidate submit={e => e.preventDefault()}>
						<div className="field">
							<label forHtml="componentName">Name</label>
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
							<Input
								className="form-control"
								id="componentName"
								type="text"
								placeholder="Enter the component name..."
								required="required"
								minLength="1"
								onChange={() => !!this.props.onChange && this.props.onChange()}
								aggregate={this.props.component.configuration}
								accessor="name"
							/>
						</div>
					</form>
				</div>
				{this.state.type === TYPE_INPUT && (
					<Mapper
						component={this.props.component}
						theme={theme}
					/>
				)}
				{this.state.type === TYPE_PROCESSOR && (
					<Processor
						component={this.props.component}
						theme={theme}
						addInput={this.props.addInput}
						addOutput={this.props.addOutput}
					/>
				)}
			</div>
			),
		];
		return (
			<div className={theme.Component}>
				<TileContext.Provider value={cols}>
					<TileContext.Consumer>
						{(service) => service.tiles.map((col, index) => {
							if (index < (service.tiles.length - 2 )) {
								return (
									<div className={theme.hidden} key={index}>
										{col}
									</div>
								);
							} else {
								return (
									<div className={theme.column} key={index}>
										{index > 0 && <button onClick={() => service.close(index)} title="close">x</button>}
										{col}
									</div>
								)
							}
						})}
					</TileContext.Consumer>
				</TileContext.Provider>
			</div>
		);
	}
}
