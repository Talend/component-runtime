/**
 *  Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
import { CircularProgress, TreeView } from '@talend/react-components';

import theme from './Menu.scss';

class Menu extends React.Component {
	constructor(props) {
		super(props);
		this.onSelect = this.onSelect.bind(this);
		this.noOp = () => {};
	}

	componentDidMount() {
			this.props.getComponentsList();
	}

	onSelect(node) {
		if (node.$$type ==='component' && node.selected) {
			return;
		}
		this.props.selectComponent(node);
	}

	render() {
		if (this.props.isLoading) {
			return (<CircularProgress light />);
		}
		return (
			<TreeView
				headerText="Components"
				className={theme.menu}
				structure={this.props.categories || []}
				onClick={this.noOp}
				onSelect={this.onSelect}
			/>
		);
	}
}

export default Menu;
