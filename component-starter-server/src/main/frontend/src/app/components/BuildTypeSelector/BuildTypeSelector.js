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
import Icon from '@talend/react-components/lib/Icon';
import { Actions } from '@talend/react-components/lib/Actions';
import Help from '../Help';
import theme from './BuildTypeSelector.module.scss';

export default class BuildTypeSelector extends React.Component {
	static propTypes = {
		project: PropTypes.object,
    };

	constructor(props) {
		super(props);
	}

	render() {
        const project = this.props.project;
		return (
			<div className="form-group">
                <label htmlFor="build-tools">Build Tool</label>
                <Actions
                    id="build-tools"
                    className={theme.buildactions}
                    actions={project.configuration.buildTypes.map(label => ({
                        label,
                        bsStyle: project.project.buildType === label ? 'info' : 'default',
                        className: project.project.buildType !== label ? 'btn-inverse' : '',
                        onClick: () => {
                            project.selectBuildTool(label);
                        },
                    }))}
                />
                <Help
                    title="Build Tool"
                    i18nKey="project_build_tool"
                    content={
                        <div>
                            <p>
                                Maven is the most commonly used build tool and Talend Component Kit
                                integrates with it smoothly.
                            </p>
                            <p>
                                Gradle is less used but get more and more attraction because it is
                                communicated as being faster than Maven.
                            </p>
                            <p>
                                <Icon name="talend-warning" /> Talend Component Kit does not provide as much
                                features with Gradle than with Maven. The components validation is not yet
                                supported for instance.
                            </p>
                        </div>
                    }
                />
            </div>
		);
	}
}
