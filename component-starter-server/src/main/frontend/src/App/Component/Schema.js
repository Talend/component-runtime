/**
 *  Copyright (C) 2006-2019 Talend Inc. - www.talend.com
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
import classnames from 'classnames';
import Node from './Node';
import TileContext from '../tile';

import theme from './Schema.scss';
import SelectDataset from '../components/SelectDataset';

export default class Schema extends React.Component {
	constructor(props) {
		super(props);
		this.state = {
			schema: props.schema,
		};
		this.onSelectDataset = this.onSelectDataset.bind(this);
	}

	componentWillReceiveProps(nextProps) {
		if (this.props !== nextProps) {
			this.setState({
				schema: nextProps.schema,
			});
			!!this.props.onReload && this.props.onReload();
		}
	}

	onSelectDataset(service) {
		return (event, dataset) => {
			if (event.target.value==='create-new') {
				const tile = (
					<form className="form">
						<h2>Create a new dataset</h2>
						<div className="form-group">
							<label></label>
							<Schema schema={{entries: []}} readOnly={true} name="dataset" />
						</div>
					</form>
				);
				service.addTile(tile);
			} else {
				this.setState(prevState => {
					let replace = false;
					if (prevState.schema.entries.length > 0) {
						if (prevState.schema.entries[0].type === 'dataset') {
							replace = true;
							prevState.schema.entries[0].name = dataset;
						}
					}
					if (!replace) {
						prevState.schema.entries.push({ name: dataset, type: 'dataset' });
					}
					return Object.assign({}, prevState);
				});
			}
		}
	}

	render() {
		return (
			<form className={classnames('form', theme.Schema)}>
				{this.props.name === 'configuration' && (
					<TileContext.Consumer>
						{(service) => (
							<SelectDataset onChange={this.onSelectDataset(service)} />
						)}
					</TileContext.Consumer>
				)}
				<div className="form-group">
					<label htmlFor="schema-model">Model</label>
					<Node
						node={this.state.schema}
						readOnly={this.props.readOnly || !!this.props.parent}
						name={this.props.name}
					/>
				</div>
			</form>
		);
	}
}
