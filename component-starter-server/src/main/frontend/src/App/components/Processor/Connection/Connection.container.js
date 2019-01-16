import React from 'react';
import PropTypes from 'prop-types';
import { Dialog, Action } from '@talend/react-components';

import Help from '../../Help';
import Input from '../../Input';
import EmbeddableToggle from '../../EmbeddableToggle';
import theme from './Connection.scss';

export default class Connection extends React.Component {
	static propTypes = {
		type: PropTypes.string,
		readOnly: PropTypes.bool,
		connection: PropTypes.shape({
			name: PropTypes.string,
			generic: PropTypes.bool,
		}),
		removeConnection: PropTypes.func,
	};
	constructor(props) {
		super(props);
		this.state = {
			custom: !this.props.connection.generic,
		};
		this.onClickShowModal = this.onClickShowModal.bind(this);
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

	render() {
		return (
			<li className={theme.Connection}>
				<div>
					<a href="#/show-drawer" onClick={this.onClickShowModal}>
						{this.props.connection.name}&nbsp;
						<span className={theme.recordType}>
							({!this.props.connection.generic ? 'custom' : 'generic'})
						</span>
						&nbsp;
					</a>
					<Dialog
						show={this.state.show}
						header={`${this.props.connection.name} Record Model (${this.props.type})`}
						onHide={() => this.setState({ show: false })}
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
					</Dialog>

					{!this.props.readOnly && (
						<span>
							(
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
							)
						</span>
					)}
				</div>
			</li>
		);
	}
}
