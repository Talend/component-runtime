/**
 *  Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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

const EMPTY = <div />;

const tileService = { tiles: [EMPTY] };
tileService.addTile = tile => {
	tileService.tiles = tileService.tiles.concat(tile);
};
tileService.resetTile = tile => {
	tileService.tiles = [tile || EMPTY];
};

const TileContext = React.createContext(tileService);

class Provider extends React.Component {
	static propTypes = {
		value: PropTypes.array,
		children: PropTypes.node,
	};
	constructor(props) {
		super(props);
		this.state = {
			tiles: props.value || [],
		};
		this.state.addTile = tile => {
			this.setState({ tiles: this.state.tiles.concat(tile) });
		};
		this.state.resetTile = tile => {
			if (Array.isArray(tile)) {
				this.setState({ tiles: tile });
			} else {
				this.setState({ tiles: [tile || EMPTY] });
			}
		};
		this.state.close = index => {
			this.setState({ tiles: this.state.tiles.filter((tile, tileIndex) => tileIndex !== index) });
		};
	}

	componentWillReceiveProps(nextProps) {
		if (nextProps.value !== this.state.tiles) {
			this.state.resetTile(nextProps.value);
		}
	}

	render() {
		return <TileContext.Provider value={this.state}>{this.props.children}</TileContext.Provider>;
	}
}

export default {
	Provider,
	Consumer: TileContext.Consumer,
};
