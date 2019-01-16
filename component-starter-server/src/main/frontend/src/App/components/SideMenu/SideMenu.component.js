import React from 'react';
import PropTypes from 'prop-types';
import classnames from 'classnames';
import { withRouter, Link } from 'react-router-dom';

import ComponentsContext from '../../ComponentsContext';

import theme from './SideMenu.scss';

function SideMenu(props) {
	return (
		<nav className={theme.menu}>
			<ol>
				<li
					className={classnames({
						[theme.active]: props.location.pathname === '/' || props.location.pathname === '/project',
					})}
				>
					<Link to="/project">Start</Link>
				</li>
				<li
					className={classnames({
						[theme.active]: props.location.pathname === '/datastore',
					})}
				>
					<Link to="/datastore">Connection</Link>
				</li>
				<li
					className={classnames({
						[theme.active]: props.location.pathname === '/dataset',
					})}
				>
					<Link to="/dataset">Dataset</Link>
				</li>
				<ComponentsContext.Consumer>
					{components =>
						components.components.map((component, i) => (
							<li
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
				<li
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
