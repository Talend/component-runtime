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
 */
import React from 'react';
import PropTypes from 'prop-types';
import classnames from 'classnames';
import { Action } from '@talend/react-components/lib/Actions';

import theme from './AppButton.module.scss';

export default class AppButton extends React.Component {
	static propTypes = {
		side: PropTypes.string,
		icon: PropTypes.string,
		iconPosition: PropTypes.string,
		text: PropTypes.string,
		help: PropTypes.node,
		onClick: PropTypes.func,
	};

	constructor(props) {
		super(props);
		this.state = {};
	}

	render() {
		const { side, text, ...rest } = this.props;
		return (
			<div
				className={classnames(theme.AppButton, {
					[theme.right]: this.props.side === 'right',
				})}
			>
				<Action
					onClick={(e) => e.preventDefault() || (!!this.props.onClick && this.props.onClick())}
					iconPosition={this.props.iconPosition || 'right'}
					className="btn-inverse"
					label={text || 'Schema'}
					{...rest}
				/>
				{this.props.help}
			</div>
		);
	}
}
