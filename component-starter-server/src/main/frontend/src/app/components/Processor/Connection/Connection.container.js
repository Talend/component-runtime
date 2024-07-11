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
 */ import React from 'react';
import PropTypes from 'prop-types';
import { Action } from '@talend/react-components/lib/Actions';
import Dialog from '@talend/react-components/lib/Dialog';

import Help from '../../Help';
import Input from '../../Input';
import EmbeddableToggle from '../../EmbeddableToggle';
import theme from './Connection.module.scss';

export default class Connection extends React.Component {
	static propTypes = {
		type: PropTypes.string,
		readOnly: PropTypes.bool,
		connection: PropTypes.shape({
			name: PropTypes.string,
			generic: PropTypes.bool,
			structure: PropTypes.shape({
				entries: PropTypes.array,
			}),
		}),
		removeConnection: PropTypes.func,
	};
	constructor(props) {
		super(props);
		this.state = {
			custom: !this.props.connection.generic,
		};
		this.onClickShowModal = this.onClickShowModal.bind(this);
		this.onSubmit = this.onSubmit.bind(this);
		this.onToggleSwitchStructureType = this.onToggleSwitchStructureType.bind(this);
	}

	componentWillReceiveProps(nextProps) {
		if (this.props !== nextProps) {
			this.setState({
				custom: !nextProps.connection.generic,
			});
		}
	}

	onToggleSwitchStructureType() {
		this.props.connection.generic = !this.props.connection.generic;
		this.setState({
			custom: !this.props.connection.generic,
		});
	}

	onClickShowModal(event) {
		event.preventDefault();
		this.setState({ show: true });
	}

	onSubmit(event) {
		event.preventDefault();
		this.setState({ show: false });
	}

	render() {
		let issue;
		if (
			!this.props.connection.generic &&
			this.props.connection.structure.entries.length === 0
		) {
			issue = true;
		}
		return (
			<li className={theme.Connection}>
				<div>
					<a className="btn btn-link" href="#/show-drawer" onClick={this.onClickShowModal}>
						{this.props.connection.name}
						{issue && (
							<Help
								title="Custom record need schema"
								i18nKey="error_processor_custom_need_schema"
								icon="talend-warning"
								content={
									<p>When the record is not generic you must specify the structure schema</p>
								}
							/>
						)}
					</a>
					<Dialog
						show={this.state.show}
						header={`${this.props.connection.name} Record Model (${this.props.type})`}
						onHide={() => this.setState({ show: false })}
						action={{
							label: 'Save',
							bsStyle: 'primary',
							onClick: this.onSubmit,
							form: 'connection-form',
						}}
					>
						<form onSubmit={this.onSubmit} id="connection-form">
							<div className="form-group">
								<label htmlFor="connection-name">Name</label>
								<Help
									title="Branch Name"
									i18nKey="processor_branch_name"
									content={
										<span>
											<p>The name of the connection/branch.</p>
											<p>
												The main branch must be named <code>MAIN</code>.
											</p>
										</span>
									}
								/>
								<Input
									id="connection-name"
									className="form-control"
									type="text"
									placeholder="Enter the connection name..."
									aggregate={this.props.connection}
									accessor="name"
									autoFocus
								/>
							</div>
							<EmbeddableToggle
								connection={this.props.connection}
								theme={theme}
								onChange={this.onToggleSwitchStructureType}
							/>
						</form>
					</Dialog>

					{!this.props.readOnly && (
						<Action
							bsStyle="link"
							icon="talend-trash"
							label="Delete connection"
							className="btn-icon-only"
							hideLabel
							onClick={e => {
								e.preventDefault();
								this.props.removeConnection(this.props.connection);
							}}
						/>
					)}
				</div>
			</li>
		);
	}
}
