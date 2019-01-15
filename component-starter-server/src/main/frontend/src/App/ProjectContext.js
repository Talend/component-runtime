import React from 'react';
import { CONFIGURATION_URL } from './constants';

const ProjectContext = React.createContext({});

class Provider extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            project: {
                buildType: 'Maven',
                version: '0.0.1-SNAPSHOT',
                group: 'com.company',
                artifact: 'company-component',
                name: 'A Component Family',
                description: 'A generated component project',
                packageBase: 'com.company.talend.components',
                family: 'CompanyFamily',
                category: 'Misc',
                facets: [],
            },
            configuration: {
                buildTypes: [],
                facets: {},
            },
            buildToolActions: [],
			facets: {},
			view: {
				light: true,
			},
        };
        this.state.notify = this.updateMe.bind(this);
        this.state.selectBuildTool = this.selectBuildTool.bind(this);
    }

    componentWillReceiveProps(nextProps) {
        if (nextProps.value !== this.state.datasets) {
            this.setState(nextProps.value);
        }
    }
	componentWillMount() {
		fetch(`${CONFIGURATION_URL}`)
			.then(resp => resp.json())
			.then(payload => {
				this.setState(current => {
					current.configuration = payload;
                    if (!current.project.buildType) {
                        current.project.buildType = 'Maven';
                    }
					return Object.assign({}, current);
				});
			});
	}

    updateMe() {
        this.setState(prevState => Object.assign({}, prevState));
    }

    selectBuildTool(label) {
        this.setState(prevState => {
            prevState.project.buildType = label;
            return Object.assign({}, prevState);
        });
    }

    render() {
        if (!this.state.configuration) {
			return <div>Loading ...</div>;
		}
        return (
            <ProjectContext.Provider value={this.state}>
                {this.props.children}
            </ProjectContext.Provider>
        );
    }
}

export default {
    Provider,
    Consumer: ProjectContext.Consumer,
};

