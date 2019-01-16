import React from 'react';
import PropTypes from 'prop-types';
import classnames from 'classnames';
import { withRouter, Link } from 'react-router-dom';

import ComponentsContext from '../../ComponentsContext';

import theme from './SideMenu.scss';

function activateIO(service) {
	return event => {
		event.preventDefault();
		service.activateIO();
	};
}

function SideMenu(props) {
	return (
		<nav className={theme.menu}>
			<ol>
				<li
					className={classnames({
						[theme.active]:
							props.location.pathname === '/' || props.location.pathname === '/project',
					})}
				>
					<Link to="/project" id="step-start">Start</Link>
				</li>
				<ComponentsContext.Consumer>
					{components => {
						if (components.withIO) {
							return (
								<React.Fragment>
									<li
										id="step-datastore"
										className={classnames({
											[theme.active]: props.location.pathname === '/datastore',
										})}
									>
										<Link to="/datastore">Datastore</Link>
									</li>
									<li
										id="step-dataset"
										className={classnames({
											[theme.active]: props.location.pathname === '/dataset',
										})}
									>
										<Link to="/dataset">Dataset</Link>
									</li>
								</React.Fragment>
							);
						}
						return (
							<li id="step-activate-io">
								<a href="#/createNew" onClick={activateIO(components)}>
									Activate IO
								</a>
							</li>
						);
					}}
				</ComponentsContext.Consumer>
				<ComponentsContext.Consumer>
					{components =>
						components.components.map((component, i) => (
							<li
								id={`step-component-${i}`}
								className={classnames({
									[theme.active]: props.location.pathname === `/component/${i}`,
								})}
								key={i}
							>
								<Link to={`/component/${i}`}>{component.configuration.name}</Link>
							</li>
						))
					}
				</ComponentsContext.Consumer>
				<ComponentsContext.Consumer>
					{components => (
						<li id="step-add-component">
							<Link to="/component/last" onClick={() => components.addComponent()}>
								Add A Component
							</Link>
						</li>
					)}
				</ComponentsContext.Consumer>
				<li
					id="step-finish"
					className={classnames({
						[theme.active]: props.location.pathname === '/export',
					})}
				>
					<Link to="/export">Finish</Link>
				</li>
			</ol>
		</nav>
	);
}

SideMenu.displayName = 'SideMenu';
SideMenu.propTypes = {
	location: PropTypes.shape({
		pathname: PropTypes.string,
	}),
};

export default withRouter(SideMenu);
