import React from 'react';
import PropTypes from 'prop-types';
import Connection from '../Connection';
import theme from './Connections.scss';

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

	render() {
		return (
			<ul className={`${theme.Connections} connections`}>
				{this.props.connections.map(connection => (
					<Connection
						key={connection.name}
						connection={connection}
						theme={theme}
						readOnly={this.props.type === 'Input' && connection.name === 'MAIN'}
						type={this.props.type}
						removeConnection={this.removeConnection}
					/>
				))}
				{(!this.props.connections || this.props.connections.length === 0) && (
					<li className={theme.ConnectionEnd} />
				)}
			</ul>
		);
	}
}
