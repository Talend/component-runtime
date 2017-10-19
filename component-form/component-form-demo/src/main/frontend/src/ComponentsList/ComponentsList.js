import React from 'react';
import { Link } from 'react-router-dom';
import { COMPONENTS_LIST_URL } from '../constants';
import './ComponentList.css';

export default class ComponentsList extends React.Component {
	constructor(props) {
		super(props);
		this.state = {};
	}

	componentWillMount() {
		fetch(COMPONENTS_LIST_URL)
			.then(resp => resp.json())
			.then(payload => this.setState(payload));
	}

	render() {
		if (! this.state.components) {
			return (<div>Loading ...</div>);
		}
		return (
			<ul className="ComponentList">
				{
					this.state.components.map(comp => (
						<li>
							<Link to={`/detail/${comp.id.id}`}>
								{comp.displayName}
							</Link>
						</li>
					))
				}
			</ul>
		);
	}
}
