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
import classnames from 'classnames';
// import { withRouter } from 'react-router';
import { ActionButton as Action } from '@talend/react-components/lib/Actions';
import Icon from '@talend/react-components/lib/Icon';
import Help from '../Help';
import Input from '../Input';

import theme from './Node.module.scss';

/* eslint-disable no-underscore-dangle */

const HELP_CONTENT = (
	<Help
		title="Designing your model"
		i18nKey="schema_designing_model"
		content={
			<div>
				<p>This view allows you to modelize your model.</p>
				<p>
					You can click on the <Icon name="talend-plus-circle" /> to add an object field, on the{' '}
					<Icon name="talend-trash" /> to delete one and on any field to edit its name and type.
				</p>
				<p>
					To add a nested object just add a field, click on it, customize its name and select the
					type object into the select box then valid your updates.
				</p>
				<p>
					<Icon name="talend-info-circle" /> The field names must be a valid java name (no space,
					special characters, ...). If the field name contains some specific keywords it will be
					considered as a credential and marked as such (for instance <code>password</code>,{' '}
					<code>token</code>, ...);
				</p>
			</div>
		}
	/>
);

const NODE_TYPES = ['object', 'boolean', 'double', 'integer', 'uri', 'url', 'string'];

function getReferenceName(id, values) {
	if (values) {
		const found = values.find((v) => v.$id === id);
		if (found) {
			return found.name;
		}
	}
	return 'Not FOUND';
}

export default class Node extends React.Component {
	static propTypes = {
		parent: PropTypes.object,
		name: PropTypes.string,
		type: PropTypes.string,
		readOnly: PropTypes.bool,
		node: PropTypes.shape({
			name: PropTypes.string,
			entries: PropTypes.array,
			type: PropTypes.string,
			model: PropTypes.object,
			reference: PropTypes.string,
			_model: PropTypes.object,
		}),
		extraTypes: PropTypes.arrayOf(PropTypes.string),
		onChangeValidate: PropTypes.func,
		onDeleteChild: PropTypes.func,
		references: PropTypes.object,
		addRefNewLocation: PropTypes.object,
		// history: PropTypes.shape({
		// 	push: PropTypes.func,
		// }),
	};

	constructor(props) {
		super(props);
		this.state = {
			opened: !!this.props.parent || !!this.props.readOnly,
			type: this.props.node.type || 'object',
			edited: false,
			entries: (this.props.node.model || this.props.node).entries,
		};

		this.nodeTypes = NODE_TYPES.map((i) => ({
			value: i,
			label: i.substring(0, 1).toUpperCase() + i.substring(1),
		}));
		if (props.extraTypes) {
			this.nodeTypes = this.nodeTypes.concat(
				props.extraTypes.map((t) => ({
					value: t,
					label: t.substring(0, 1).toUpperCase() + t.substring(1),
				})),
			);
		}
		this.onSubmit = this.onSubmit.bind(this);
		this.onChangeValidate = this.onChangeValidate.bind(this);
		this.onNameChange = this.onNameChange.bind(this);
		this.onClickDeleteNode = this.onClickDeleteNode.bind(this);
		this.onTypeChange = this.onTypeChange.bind(this);
		this.onEdit = this.onEdit.bind(this);
		this.onDeleteChild = this.onDeleteChild.bind(this);
		this.onEnterKey = this.onEnterKey.bind(this);
		this.onClickAddChild = this.onClickAddChild.bind(this);
		this.onReferenceChange = this.onReferenceChange.bind(this);
		this.isRef = this.isRef.bind(this);
	}

	componentWillReceiveProps(nextProps) {
		if (this.props.node !== nextProps.node) {
			this.setState({
				// opened: !!nextProps.parent || !!nextProps.readOnly,
				type: nextProps.node.type || 'object',
				// edited: false,
				entries: (nextProps.node.model || nextProps.node).entries,
			});
		}
	}

	onChangeValidate() {
		if (this.props.onChangeValidate) {
			this.props.onChangeValidate();
		}
	}

	onTypeChange(event) {
		const type = event.target.value;
		switch (type) {
			case 'object':
				this.props.node.model = this.props.node.model || this.props.node._model || { entries: [] };
				delete this.props.node.type;
				break;
			default:
				this.props.node.type = type;
				this.props.node._model = this.props.node.model || this.props.node._model;
				delete this.props.node.model;
				delete this.props.node.reference;
				if (this.isRef(type) && this.props.references[type].length > 0) {
					this.props.node.reference = this.props.references[type][0].$id;
				}
				break;
		}
		this.setState({
			type,
			entries: this.props.node.model ? this.props.node.model.entries : undefined,
		});
		this.onChangeValidate();
	}

	onClickAddChild() {
		this.onChangeValidate();
		this.setState((state) => {
			state.entries.push({
				name: `configuration${state.entries.length + 1}`,
				type: 'string',
			});
			if (!state.opened) {
				// eslint-disable-next-line no-param-reassign
				state.opened = true;
			}
			return Object.assign({}, state);
		});
	}

	onEdit() {
		if (this.props.readOnly) {
			return;
		}
		this.setState({ edited: true });
	}

	onClickDeleteNode() {
		this.onChangeValidate();
		if (this.props.onDeleteChild) {
			this.props.onDeleteChild(this.props.node);
		}
	}

	onDeleteChild(node) {
		const idx = this.state.entries.indexOf(node);
		if (idx >= 0) {
			this.setState((state) => {
				state.entries.splice(idx, 1);
				(this.props.node.model || this.props.node).entries = state.entries;
			}, this.onChangeValidate);
		}
	}

	onSubmit(event) {
		if (event) {
			event.stopPropagation();
			event.preventDefault();
		}
		this.onChangeValidate();
		this.setState({ edited: false });
	}

	onEnterKey(event) {
		if (event.key === 'Enter') {
			this.onSubmit();
		}
	}

	onNameChange() {
		this.onChangeValidate();
		this.setState({});
	}

	onReferenceChange(event) {
		const value = event.target.value;
		// if (value === 'add-new') {
		// 	if (this.props.addRefNewLocation[this.state.type]) {
		// 		this.props.history.push(this.props.addRefNewLocation[this.state.type]);
		// 	} else {
		// 		console.error(`no route configured for ${this.state.type}`);
		// 	}
		// } else if (value) {
		if (value) {
			this.props.node.reference = value;
			this.onChangeValidate();
		} else {
			delete this.props.node.reference;
			this.onChangeValidate();
		}
	}

	isRef(type) {
		if (this.props.references) {
			return !!this.props.references[type || this.state.type];
		}
		return false;
	}

	render() {
		const nodeIcon = this.state.opened ? 'talend-caret-down' : 'talend-chevron-left';
		const nodeIconTransform = !this.state.opened ? 'flip-horizontal' : undefined;
		let nodeView;
		const readOnly = this.props.readOnly; // || !isNativeType(this.state.type);
		if (this.state.edited) {
			nodeView = (
				<div className={theme.nodeConfig}>
					<Input
						type="text"
						placeholder="Enter the configuration name..."
						required
						minLength="1"
						aggregate={this.props.node}
						accessor="name"
						initialValue={this.props.node.name || this.props.name}
						onChange={this.onNameChange}
						onKeyDown={this.onEnterKey}
						className={theme.formcontrol}
					/>
					<select
						value={this.state.type}
						onChange={this.onTypeChange}
						className={theme.formcontrol}
					>
						{this.nodeTypes.map((t) => (
							<option value={t.value} key={t.value}>
								{t.label}
							</option>
						))}
					</select>
					{this.isRef(this.state.type) && (
						<select
							value={this.props.node.reference}
							onChange={this.onReferenceChange}
							className={theme.formcontrol}
						>
							{/* <option value="add-new">+ Add new</option> */}
							{this.props.references[this.state.type].map((t) => (
								<option value={t.$id} key={t.$id}>
									{t.name}
								</option>
							))}
						</select>
					)}
					<Action
						bsStyle="link"
						label="validate"
						hideLabel
						className="btn-icon-only btn-sm"
						icon="talend-check"
						onClick={this.onSubmit}
					/>
				</div>
			);
		} else {
			nodeView = (
				<span
					className={classnames(theme.nodeView, {
						[theme.readonly]: readOnly,
						[theme.editable]: !readOnly,
					})}
				>
					{readOnly ? (
						<span className={theme.labelReadOnly}>
							{this.props.node.name || this.props.name} ({this.props.node.type || 'object'})
						</span>
					) : (
						<button onClick={() => this.onEdit()} className={theme.nodeName}>
							{this.props.node.name || this.props.name} (
							{this.props.node.reference
								? `${getReferenceName(
										this.props.node.reference,
										this.props.references[this.state.type],
									)}: ${this.props.node.type}`
								: `${this.props.node.type || 'object'}`}
							)
						</button>
					)}
					{!readOnly && (
						<Action
							bsStyle="link"
							className="btn-icon-only btn-sm"
							icon="talend-trash"
							label="Delete node"
							hideLabel
							onClick={this.onClickDeleteNode}
						/>
					)}
					{!this.props.parent && HELP_CONTENT}
				</span>
			);
		}
		return (
			<div className={theme.node}>
				<span>
					{!this.props.node.type && (
						<Action
							bsStyle="link"
							className="btn-icon-only btn-sm"
							label="Open"
							hideLabel
							icon={nodeIcon}
							iconTransform={nodeIconTransform}
							onClick={() => this.setState({ opened: !this.state.opened })}
						/>
					)}
					<label htmlFor="node-view">{nodeView}</label>
				</span>
				{(!this.props.parent || !!this.state.entries) && (
					<ul className={theme.vLine}>
						{!!this.state.opened &&
							this.state.entries.map((node, index) => (
								<li key={index}>
									<Node
										node={node}
										parent={this.props.node}
										onDeleteChild={this.onDeleteChild}
										extraTypes={this.props.extraTypes}
										onChangeValidate={this.onChangeValidate}
										references={this.props.references}
										addRefNewLocation={this.props.addRefNewLocation}
									/>
								</li>
							))}
						{!!this.state.opened && !!this.state.entries && this.state.entries.length === 0 && (
							<li>No configuration yet, click on the plus bellow</li>
						)}
					</ul>
				)}
				{(!this.props.node.type || this.props.node.type === 'object') && (
					<Action
						bsStyle="link"
						className={`btn-icon-only btn-sm ${theme.btnplus}`}
						icon="talend-plus-circle"
						label="Add entry"
						hideLabel
						onClick={this.onClickAddChild}
					/>
				)}
			</div>
		);
	}
}

// const NodeWithRouter = withRouter(Node);
// export default NodeWithRouter;
