import React from 'react';
import PropTypes from 'prop-types';

import { Drawer, Icon } from '@talend/react-components';
import Help from '../../Help';
import Input from '../../Input';
import EmbeddableToggle from '../../EmbeddableToggle';
import theme from './Connection.scss';

function drawerActions(props) {
	return {
		actions: {
			right: [
				{
					label: 'Close',
					bsStyle: 'primary',
					onClick: () => props.onUpdateDrawers([]),
				},
			],
		},
	};
}

export default class Connection extends React.Component {
	static propTypes = {
		type: PropTypes.string,
		readOnly: PropTypes.bool,
		connection: PropTypes.shape({
			name: PropTypes.string,
			generic: PropTypes.bool,
		}),
		onUpdateDrawers: PropTypes.func,
		removeConnection: PropTypes.func,
	};
	constructor(props) {
		super(props);
		this.state = {
			custom: !this.props.connection.generic,
		};
		this.onClickShowDrawer = this.onClickShowDrawer.bind(this);
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

	onClickShowDrawer(event) {
		event.preventDefault();
		this.props.onUpdateDrawers([
			<Drawer
				key="first"
				title={`${this.props.connection.name} Record Model (${this.props.type})`}
				footerActions={drawerActions(this.props)}
			>
				<div className="field">
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
					/>
				</div>
				<EmbeddableToggle
					connection={this.props.connection}
					theme={theme}
					onChange={this.onToggleSwitchStructureType}
				/>
			</Drawer>,
		]);
	}

	render() {
		return (
			<li className={theme.Connection}>
				<div>
					<a href="#/show-drawer" onClick={this.onClickShowDrawer}>
						{this.props.connection.name}&nbsp;
						<span className={theme.recordType}>
							({!this.props.connection.generic ? 'custom' : 'generic'})
						</span>
						&nbsp;
					</a>
					{!this.props.readOnly && (
						<span>
							(
							<Icon
								name="talend-trash"
								onClick={e => {
									e.preventDefault();
									this.props.removeConnection(this.props.connection);
								}}
							/>
							)
						</span>
					)}
				</div>
			</li>
		);
	}
}
