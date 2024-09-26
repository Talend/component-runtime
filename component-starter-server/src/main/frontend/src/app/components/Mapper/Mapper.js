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
import Toggle from '@talend/react-components/lib/Toggle';
import Help from '../Help';
import AppButton from '../AppButton';
import Schema from '../Schema';
import ComponentSchema from '../ComponentSchema';
import TileContext from '../../tile';

import theme from './Mapper.module.scss';
import { Tooltip } from '@talend/design-system/lib/';

export default class Mapper extends React.Component {
	static propTypes = {
		component: PropTypes.object,
	};
	constructor(props) {
		super(props);

		this.state = {
			configurationStructure: props.component.source.configurationStructure,
			outputStructure: props.component.source.outputStructure,
		};
		[
			'onStreamChange',
			'onConfigurationButtonClick',
			'onRecordTypeChange',
			'onRecordButtonClick',
		].forEach((i) => (this[i] = this[i].bind(this)));
	}

	componentWillReceiveProps(nextProps) {
		if (this.props.component !== nextProps.component) {
			this.setState({
				configurationStructure: nextProps.component.source.configurationStructure,
				outputStructure: nextProps.component.source.outputStructure,
			});
		}
	}

	onStreamChange() {
		this.props.component.source.stream = !this.props.component.source.stream;
		this.setState({});
	}

	onConfigurationButtonClick(tileService) {
		const tile = (
			<div>
				<h2>Configuration Model</h2>
				<ComponentSchema schema={this.props.component.source.configurationStructure} withDataset />
			</div>
		);

		if (tileService.tiles.length > 1) {
			tileService.resetTile([tileService.tiles[0], tile]);
		} else {
			tileService.addTile(tile);
		}
	}

	onRecordButtonClick(tileService) {
		const tile = (
			<div>
				<h2>Record Model</h2>
				<Schema schema={this.state.outputStructure} readOnly name="root" />
			</div>
		);

		if (tileService.tiles.length > 1) {
			tileService.resetTile([tileService.tiles[0], tile]);
		} else {
			tileService.addTile(tile);
		}
	}

	onRecordTypeChange(event) {
		this.props.component.source.genericOutput = event.target.value === 'generic';
		if (this.props.component.source.genericOutput) {
			delete this.props.component.source.outputStructure;
		} else {
			this.props.component.source.outputStructure = {
				entries: [],
			};
		}
		this.setState({
			recordType: event.target.value,
			outputStructure: this.props.component.source.outputStructure,
		});
	}

	render() {
		return (
			<div className={theme.Mapper}>
				<TileContext.Consumer>
					{(tileService) => (
						<AppButton
							text="Configuration Model"
							onClick={() => this.onConfigurationButtonClick(tileService)}
							help={
								<Help
									title="Configuration"
									i18nKey="mapper_configuration"
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
						Stream
						<Help
							title="Stream"
							i18nKey="mapper_stream"
							content={
								<div>
									<p>Activate this toggle if your input will issue a stream.</p>
									<p>
										It means that there is no real "end" of the data and that the job/pipeline using
										this component is not intended to be terminated.
									</p>
								</div>
							}
						/>
					</h2>
					<Toggle
						id="mapper-toggle"
						checked={this.props.component.source.stream}
						onChange={() => this.onStreamChange()}
					/>
				</div>
				<div className={theme['form-row']}>
					<h2>
						Record Type
						<Help
							title="Record"
							i18nKey="mapper_record_type"
							content={
								<div>
									<p>The input will issue some records.</p>
									<p>
										This configuration allows you to either define the schema of the records or to
										use a generic record type you will implement if the data structure can vary
										depending the component configuration.
									</p>
								</div>
							}
						/>
					</h2>
					<select
						className={theme.recordSelector}
						value={this.state.recordType}
						onChange={this.onRecordTypeChange}
					>
						<option value="generic">Generic</option>
						<option value="custom">Custom</option>
					</select>
					{!this.props.component.source.genericOutput && (
						<TileContext.Consumer>
							{(tileService) => (
								<AppButton
									text="Record Model"
									onClick={() => this.onRecordButtonClick(tileService)}
									help={
										<Help
											title="Record Model"
											i18nKey="mapper_record_model"
											content={
												<span>
													<p>
														In custom mode you can define the schema of the record you want to
														design by clicking on this button.
													</p>
													<p>
														The schema will be used to create POJO model you will use to instantiate
														the records sent by your input to the job.
													</p>
												</span>
											}
										/>
									}
								/>
							)}
						</TileContext.Consumer>
					)}
				</div>
			</div>
		);
	}
}
