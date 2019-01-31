import React from 'react';
import PropTypes from 'prop-types';
import Connection from '../Connection';
import theme from './Connections.scss';
import ComponentsContext from '../../../ComponentsContext';

export default class Connections extends React.Component {
	static propTypes = {
		connections: PropTypes.array,
		type: PropTypes.string,
	};

	constructor(props) {
		super(props);
		this.state = {
			connections: props.connections,
		};
		this.removeConnection = this.removeConnection.bind(this);
		this.isReadOnly = this.isReadOnly.bind(this);
	}

	componentWillReceiveProps(nextProps) {
		if (this.props !== nextProps) {
			this.setState({
				connections: nextProps.connections,
			});
		}
	}

	removeConnection(connection) {
		this.props.connections.splice(this.props.connections.indexOf(connection), 1);
		this.setState({
			connections: this.props.connections.map(i => i),
		});
	}
	isReadOnly(connection, components) {
		return (
			(this.props.type === 'Input' && connection.name === 'MAIN') ||
			(this.props.type === 'Output' && connection.name === 'MAIN' && !components.withIO)
		);
	}
	render() {
		return (
			<ComponentsContext.Consumer>
				{components => (
					<ul className={`${theme.Connections} connections`}>
						{this.props.connections.map(connection => (
							<Connection
								key={connection.name}
								connection={connection}
								theme={theme}
								readOnly={this.isReadOnly(connection, components)}
								type={this.props.type}
								removeConnection={this.removeConnection}
							/>
						))}
						{(!this.props.connections || this.props.connections.length === 0) && (
							<li className={theme.ConnectionEnd} />
						)}
					</ul>
				)}
			</ComponentsContext.Consumer>
		);
	}
}
