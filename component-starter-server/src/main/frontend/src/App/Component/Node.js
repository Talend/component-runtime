import React from 'react';
import PropTypes from 'prop-types';
import keycode from 'keycode';
import classnames from 'classnames';
import Action from '@talend/react-components/lib/Actions/ActionButton';
import Icon from '@talend/react-components/lib/Icon';
import Help from './Help';
import Input from './Input';

import theme from './Node.scss';

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

function isNativeType(type) {
	if (type === undefined) {
		return true;  // object
	}
	return NODE_TYPES.indexOf(type) !== -1;
}

export default class Node extends React.Component {
	static propTypes = {
		parent: PropTypes.object,
		type: PropTypes.string,
		readOnly: PropTypes.bool,
		node: PropTypes.shape({
			entries: PropTypes.array,
			type: PropTypes.string,
			model: PropTypes.object,
		}),
	};
	constructor(props) {
		super(props);
		this.state = {
			opened: !!this.props.parent || !!this.props.readOnly,
			type: this.props.node.type || 'object',
			edited: false,
			entries: (this.props.node.model || this.props.node).entries,
		};

		this.nodeTypes = NODE_TYPES.map(i => (
			{ value: i, label: i.substring(0, 1).toUpperCase() + i.substring(1) }
		));

		['addChild', 'onTypeChange', 'onEdit', 'deleteNode', 'onDeleteChild', 'validEdition'].forEach(
			i => (this[i] = this[i].bind(this)),
		);
	}

	componentWillReceiveProps(nextProps) {
		if (this.props !== nextProps) {
			this.setState({
				opened: !!nextProps.parent || !!nextProps.readOnly,
				type: nextProps.node.type || 'object',
				edited: false,
				entries: (nextProps.node.model || nextProps.node).entries,
			});
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
		}
		this.setState({
			type,
			entries: !!this.props.node.model ? this.props.node.model.entries : undefined,
		});
	}

	addChild() {
		this.setState(state => {
			state.entries.push({
				name: 'configuration' + (state.entries.length + 1),
				type: 'string',
            });
            if (!state.opened) {
                state.opened = true;
            }
            return Object.assign({}, state);
        });
	}

	onEdit() {
		if (!!this.props.readOnly) {
			return;
		}
		this.setState({ edited: true });
	}

	deleteNode() {
		!!this.props.onDeleteChild && this.props.onDeleteChild(this.props.node);
	}

	onDeleteChild(node) {
		const idx = this.state.entries.indexOf(node);
		if (idx >= 0) {
			this.setState(state => {
				state.entries.splice(idx, 1);
				(this.props.node.model || this.props.node).entries = state.entries;
			});
		}
	}

	validEdition(event) {
		switch (event.which) {
			case keycode.codes.enter:
				this.setState({ edited: false });
				break;
			default:
				break;
		}
	}

	render() {
		const nodeIcon = !!this.state.opened ? 'talend-caret-down' : 'talend-chevron-left';
		const nodeIconTransform = !this.state.opened ? 'flip-horizontal' : undefined;
		let nodeView;
		const readOnly = this.props.readOnly || !isNativeType(this.state.type);
		if (!!this.state.edited) {
			nodeView = (
				<span>
					<Input
						type="text"
						placeholder="Enter the configuration name..."
						required="required"
						minLength="1"
						aggregate={this.props.node}
						accessor="name"
						initialValue={this.props.node.name || this.props.name}
						onChange={() => this.setState({})}
						onKeyDown={this.validEdition}
					/>
					<select value={this.state.type} onChange={this.onTypeChange}>
						{this.nodeTypes.map(t => (
							<option selected={this.state.value === t.value} value={t.value}>
								{t.label}
							</option>
						))}
					</select>
					<Action
						bsStyle="link"
						className="btn-icon-only btn-sm"
						icon="talend-check"
						onClick={() => this.setState({ edited: false })}
					/>
				</span>
			);
		} else {
			nodeView = (
				<span className={classnames(theme.nodeView, {
					[theme.readonly]: readOnly,
					[theme.editable]: !readOnly,
				})}>
					{readOnly ? (
						<span className={theme.labelReadOnly}>
							{this.props.node.name || this.props.name} ({this.props.node.type || 'object'})
						</span>
					) : (
						<button onClick={this.onEdit} className={theme.nodeName}>
							{this.props.node.name || this.props.name} ({this.props.node.type || 'object'})
						</button>
					)}
					{!readOnly && (
						<Action
							bsStyle="link"
							className="btn-icon-only btn-sm"
							icon="talend-trash"
							onClick={this.deleteNode}
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
							icon={nodeIcon}
							iconTransform={nodeIconTransform}
							onClick={() => this.setState({ opened: !this.state.opened })}
						/>
					)}
					<label>{nodeView}</label>
				</span>
				{(!this.props.parent || !!this.state.entries) && (
					<ul className={theme.vLine}>
						{!!this.state.opened &&
							this.state.entries.map((node, index) => {
								return (
									<li key={index}>
										<Node node={node} parent={this.props.node} onDeleteChild={this.onDeleteChild} />
									</li>
								);
							})}
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
						onClick={this.addChild}
					/>
				)}
			</div>
		);
	}
}
