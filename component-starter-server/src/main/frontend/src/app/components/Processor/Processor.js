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
import classnames from 'classnames';
import Icon from '@talend/react-components/lib/Icon';
import Help from '../Help';
import AppButton from '../AppButton';
import TileContext from '../../tile';
import theme from './Processor.module.scss';
import Connections from './Connections';
import ComponentSchema from '../ComponentSchema';

/* eslint-disable no-param-reassign */

function newStructure(prefix, array) {
	array.push({
		name: array.length === 0 ? 'MAIN' : `${prefix}_${array.length + 1}`,
		generic: true,
		structure: {
			entries: [],
		},
	});
	return array;
}

export default class Processor extends React.Component {
	static propTypes = {
		component: PropTypes.shape({
			processor: PropTypes.shape({
				inputStructures: PropTypes.array,
				outputStructures: PropTypes.array,
				configurationStructure: PropTypes.object,
			}),
		}),
		sink: PropTypes.bool,
	};
	constructor(props) {
		super(props);

		this.state = {
			inputs: this.props.component.processor.inputStructures,
			outputs: this.props.component.processor.outputStructures,
		};
		this.onConfigurationButtonClick = this.onConfigurationButtonClick.bind(this);
	}

	componentWillReceiveProps(nextProps) {
		if (this.props !== nextProps) {
			this.setState({
				inputs: nextProps.component.processor.inputStructures,
				outputs: nextProps.component.processor.outputStructures,
			});
		}
	}

	onConfigurationButtonClick(tileService) {
		const props = {
			schema: this.props.component.processor.configurationStructure,
		};
		if (this.props.component.processor.outputStructures.length === 0) {
			props.withDataset = true;
		}
		const tile = (
			<div>
				<h2>Configuration Model</h2>
				<ComponentSchema {...props} />
			</div>
		);

		if (tileService.tiles.length > 1) {
			tileService.resetTile([tileService.tiles[0], tile]);
		} else {
			tileService.addTile(tile);
		}
	}

	render() {
		return (
			<div className={theme.Processor}>
				<TileContext.Consumer>
					{tileService => (
						<AppButton
							text="Configuration Model"
							onClick={() => this.onConfigurationButtonClick(tileService)}
							help={
								<Help
									title="Configuration"
									i18nKey="processor_configuration"
									content={
										<span>
											<p>
												The component configuration schema design can be configured by clicking on
												this button.
											</p>
											<p>
												It allows you to define the data you need to be able to execute the
												component logic.
											</p>
										</span>
									}
								/>
							}
						/>
					)}
				</TileContext.Consumer>

				<div className={theme['form-row']}>
					<h2>
						Input(s) / Ouput(s)
						<Help
							title="Connectivity"
							i18nKey="processor_connectivity"
							content={
								<div>
									<p>This section allows you to configure the processor connectivity.</p>
									<p>
										Click on <Icon name="talend-plus-circle" /> buttons to add inputs/outputs and
										the <Icon name="talend-trash" /> to remove one.
									</p>
									<p>
										Clicking on an input/output branch you can edit its specification (name, type).
									</p>
								</div>
							}
						/>
					</h2>
					{!this.props.sink && (<div className={classnames(theme.ComponentButtons, 'col-sm-12')}>
						<div className="col-sm-6">
							<AppButton
								text="Add"
								icon="talend-plus-circle"
								onClick={() =>
									this.setState(s => (s.inputs = newStructure('input', this.state.inputs)))
								}
								help={
									<Help
										title="Inputs"
										i18nKey="processor_inputs"
										content={
											<span>
												<p>Clicking on this button you add an input to the processor.</p>
												<p>
													<Icon name="talend-info-circle" /> <code>MAIN</code> input is mandatory.
												</p>
											</span>
										}
									/>
								}
							/>
						</div>
						<div className="col-sm-6">
							<AppButton
								text="Add"
								icon="talend-plus-circle"
								side="right"
								iconPosition="left"
								onClick={() =>
									this.setState(s => (s.outputs = newStructure('output', this.state.outputs)))
								}
								help={
									<Help
										title="Outputs"
										i18nKey="processor_outputs"
										placement="left"
										content={
											<span>
												<p>Clicking on this button you add an output to the processor.</p>
											</span>
										}
									/>
								}
							/>
						</div>
					</div>)}
					<div className={classnames(theme.ComponentWrapper, 'col-sm-12')}>
						<div className={classnames(theme.Inputs, 'processor-inputs col-sm-5')}>
							{this.props.sink ? (
								<Connections
									connections={[this.props.component.processor.inputStructures[0]]}
									type="Input"
								/>
							) : (
								<Connections
									connections={this.props.component.processor.inputStructures}
									type="Input"
								/>
							)}
						</div>
						<div className={classnames(theme.ComponentBox, 'col-sm-2', { processor: !this.props.sink })}>
							<component-box>component</component-box>
						</div>
						<div className={classnames(theme.Outputs, 'processor-outputs col-sm-5')}>
							{!this.props.sink && (<Connections
								connections={this.props.component.processor.outputStructures}
								type="Output"
							/>)}
						</div>
					</div>
				</div>
			</div>
		);
	}
}
