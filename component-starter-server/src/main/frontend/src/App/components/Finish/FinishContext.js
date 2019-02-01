import React from 'react';
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
