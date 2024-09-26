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
import { Trans } from 'react-i18next';
import { OverlayTrigger, Popover } from '@talend/react-bootstrap';
import Icon from '@talend/react-components/lib/Icon';

import theme from './Help.module.scss';

function Help(props) {
	const overlay = (
		<Popover
			id={props.i18nKey}
			title={
				<Trans ns="Help" i18nKey={`${props.i18nKey}_title`}>
					{props.title}
				</Trans>
			}
		>
			<div className={theme.HelpContent}>
				<Trans ns="Help" i18nKey={props.i18nKey}>
					{props.content}
				</Trans>
			</div>
		</Popover>
	);
	return (
		<OverlayTrigger
			trigger={['hover', 'focus']}
			placement={props.placement || 'right'}
			overlay={overlay}
		>
			{props.children ? (
				props.children
			) : (
				<span className={theme.Help}>
					<Icon name={props.icon || 'talend-question-circle'} />
				</span>
			)}
		</OverlayTrigger>
	);
}

export default Help;

Help.propTypes = {
	icon: PropTypes.string,
	i18nKey: PropTypes.string,
	content: PropTypes.object,
	placement: PropTypes.string,
	title: PropTypes.string,
};
