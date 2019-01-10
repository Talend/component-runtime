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
import Node from './Node';

import theme from './Schema.scss';



export default class Schema extends React.Component {
	constructor(props) {
		super(props);
		this.state = {
			schema: props.schema,
		};
	}

	componentWillReceiveProps(nextProps) {
		if (this.props !== nextProps) {
			this.setState({
				schema: nextProps.schema,
			});
			!!this.props.onReload && this.props.onReload();
		}
	}

	render() {
		return (
			<div className={theme.Schema}>
				<Node
					node={this.state.schema}
					readOnly={this.props.readOnly || !!this.props.parent}
					name={this.props.name}
				/>
			</div>
		);
	}
}
