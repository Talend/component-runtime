import React from 'react';

const EMPTY = <div />;

const tileService = { tiles: [EMPTY] };
tileService.addTile = (tile) => {
	tileService.tiles = tileService.tiles.concat(tile);
};
tileService.resetTile = (tile) => {
	tileService.tiles = [tile || EMPTY];
};

const TileContext = React.createContext(tileService);

class Provider extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            tiles: props.value || [],
        };
        this.state.addTile = (tile) => {
            this.setState({ tiles: this.state.tiles.concat(tile) });
        };
        this.state.resetTile = (tile) => {
            if (Array.isArray(tile)) {
                this.setState({ tiles: tile });
            } else {
                this.setState({ tiles: [tile || EMPTY] });
            }
        };
        this.state.close = (index) => {
            this.setState({ tiles: this.state.tiles.filter((tile, tileIndex) => tileIndex !== index)});
        }
    }

    componentWillReceiveProps(nextProps) {
        if (nextProps.value !== this.state.tiles) {
            this.state.resetTile(nextProps.value);
        }
    }

    render() {
        return (
            <TileContext.Provider value={this.state}>
                {this.props.children}
            </TileContext.Provider>
        );
    }
}

export default {
    Provider,
    Consumer: TileContext.Consumer,
};
