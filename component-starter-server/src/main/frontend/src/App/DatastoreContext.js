import React from 'react';


const DatastoreContext = React.createContext({ datastores: [] });

class Provider extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            datastores: props.value || [],
        };
        this.state.add = (datastore) => {
            this.setState({ datastores: this.state.datastores.concat(datastore) });
        };
    }

    componentWillReceiveProps(nextProps) {
        if (nextProps.value !== this.state.datastores) {
            this.setState(nextProps.value);
        }
    }

    render() {
        return (
            <DatastoreContext.Provider value={this.state}>
                {this.props.children}
            </DatastoreContext.Provider>
        );
    }
}

export default {
    Provider,
    Consumer: DatastoreContext.Consumer,
};
