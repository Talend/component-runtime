/**
 *  Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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
 */import React from 'react';
import PropTypes from 'prop-types';
import ComponentsContext from '../../ComponentsContext';
import DatastoreContext from '../../DatastoreContext';
import DatasetContext from '../../DatasetContext';
import ProjectContext from '../../ProjectContext';

const FinishContext = React.createContext({});

function FinishProvider(props) {
	return (
		<ProjectContext.Consumer>
			{project => (
				<ComponentsContext.Consumer>
					{components => (
						<DatastoreContext.Consumer>
							{datastore => (
								<DatasetContext.Consumer>
									{dataset => (
										<FinishContext.Provider
											value={{
												project,
												components,
												datastore,
												dataset,
											}}
										>
											{props.children}
										</FinishContext.Provider>
									)}
								</DatasetContext.Consumer>
							)}
						</DatastoreContext.Consumer>
					)}
				</ComponentsContext.Consumer>
			)}
		</ProjectContext.Consumer>
	);
}

FinishProvider.propTypes = {
	children: PropTypes.node,
};

export default {
	Provider: FinishProvider,
	Consumer: FinishContext.Consumer,
};
