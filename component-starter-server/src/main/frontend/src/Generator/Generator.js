import React from 'react';
import { Typeahead } from '@talend/react-components';
import { GENERATOR_URL, CONFIGURATION_URL } from '../constants';

import theme from './Generator.scss';

export default class ComponentsForm extends React.Component {
	constructor(props) {
		super(props);
		this.state = {
            value: {
                 project: {
                   buildType: 'Maven',
                   version: '0.0.1-SNAPSHOT',
                   group: 'com.company',
                   artifact: 'company-component',
                   name: 'A Component',
                   description: 'A generated component project',
                   packageBase: 'com.company.talend.components',
                   facets: []
                 }
             },
             view: {
               light: true
             }
        };
		this.handleSubmit = this.handleSubmit.bind(this);
		this.showAll = this.showAll.bind(this);
		this.showLight = this.showLight.bind(this);
		this.onFacetChange = this.onFacetChange.bind(this);
		this.onFacetSelect = this.onFacetSelect.bind(this);
		this.onFacetBlur = this.onFacetBlur.bind(this);
		this.onBuildTypeChange = this.onBuildTypeChange.bind(this);
	}

	componentWillMount() {
		fetch(`${CONFIGURATION_URL}`)
			.then(resp => resp.json())
			.then(payload => {
                const typeaheadConfig = Object.keys(payload.facets)
                    .map(item => {
                        const facets = payload.facets[item];
                        return {
                          title: item,
                          icon: {name: "talend-filter", title: "icon"},
                          suggestions: facets.map(f => {
                            return {
                                title: f.name,
                                description: f.description
                            };
                          })
                        }
                    });
			    this.setState((current) => {
			        current.configuration = payload;
			        current.facetTypeahead = typeaheadConfig;
			    });
			});
	}

	onFacetBlur(evt) {
        delete this.state.typeaheadCurrentConfig;
	    this.setState(this.state);
	}

	onFacetChange(evt, item) {
	    this.setState((current) => {
            current.typeaheadCurrentConfig = current.facetTypeahead;
            current.typeaheadCurrentValue = item.value;
        });
	}

	onFacetSelect(evt, item) {
	    const value = this.state.typeaheadCurrentConfig[item.sectionIndex].suggestions[item.itemIndex].title;
        this.setState((current) => {
            current.typeaheadCurrentValue = '';
            delete current.typeaheadCurrentConfig;

            const idx = current.value.project.facets.indexOf(value);
            if (idx < 0) {
                current.value.project.facets.push(value);
            }
        });
	}

	onBuildTypeChange(event) {
	    const value = event.target.value;
	    this.setState((current) => current.value.project.buildType = value);
	}

	handleSubmit(event) {
		fetch(`${GENERATOR_URL}`, {
				method: 'POST',
				body: JSON.stringify(this.state.value),
				headers: new Headers({'Accept': 'application/zip', 'Content-Type': 'application/json'})
			})
			.then(response => response.blob())
			.then(blob => {
					let a = document.createElement("a");
					const url = window.URL.createObjectURL(blob);
					try {
				    document.body.appendChild(a);
				    a.style = "display: none";
						a.href = url;
		        a.download = this.state.value.project.artifact + '.zip';
		        a.click();
					} finally {
						window.URL.revokeObjectURL(url);
						document.body.removeChild(a);
					}
			});
	    event.preventDefault();
	}

    showAll(event) {
        this.setState((current) => current.view.light = false);
        event.preventDefault();
    }

    showLight(event) {
        this.setState((current) => current.view.light = true);
        event.preventDefault();
    }

    removeFacet(index, event) {
        this.setState((current) => current.value.project.facets.splice(index, 1));
        event.preventDefault();
    }

	render() {
		if(! this.state.configuration) {
			return (<div>Loading ...</div>);
		}

		return (
			<div className="Generator">
               <form onSubmit={this.handleSubmit} className="row col-sm-12">
                <h1 className="text-center form-inline">Create a&nbsp;
                   <select className={`form-control ${theme.select}`} value={this.state.value.project.buildType} onChange={this.onBuildTypeChange}>
                      {
                        !!this.state.configuration.buildTypes && this.state.configuration.buildTypes.map((option, index) => {
                            const optionProps = {
                                key: index,
                                value: option,
                            };
                            return (
                                <option {...optionProps}>{option}</option>
                            );
                        })
                      }
                   </select>
                   <span>&nbsp;Talend Component Kit Project</span>
                </h1>
                <div className="row">
                  <div className="col-sm-12">
                    <h2 className="text-center"><i className="fa fa-check"></i>&nbsp;Facets configuration</h2>
                    <div className="row" id="search-box">
                      <div className={`col-md-6 col-md-offset-3 ${theme['Typeahead-container']}`}>
                          <i className="fa fa-search" id="search-icon"></i>
                          <Typeahead
                            value={this.state.typeaheadCurrentValue}
                            placeholder="Select a facet to add to your project"
                            items={this.state.typeaheadCurrentConfig}
                            onBlur={this.onFacetBlur}
                            onChange={this.onFacetChange}
                            onSelect={this.onFacetSelect}
                          />
                      </div>
                    </div>
                    <div className="clearfix"></div>
                    <div className="row text-center" id="zip-button">
                      <button className="btn btn-lg btn-primary relief-button" type="submit">
                        <i className="fa fa-file-archive-o"></i>&nbsp;Download as zip
                      </button>
                    </div>
                    <div className="row" id="selectedFacets">
                      <div className="form-group form-horizontal col-md-6 col-md-offset-3 text-center">
                        <label forHtml="">Selected Facets</label>
                        <div className="clearfix"></div>
                        {
                            this.state.value.project.facets && this.state.value.project.facets.map((value, index) => {
                              return (
                                <div className="btn-group box-vspace">
                                 <button className="btn btn-primary" type="button" onClick={(evt) => this.removeFacet(index, evt)}>
                                    {value}
                                    <i className="fa fa-times facet-cross-space"></i>
                                  </button>
                                  {
                                    (index !== this.state.value.project.facets.length - 1) && (<span className="box-space"></span>)
                                  }
                                </div>);
                            })
                        }
                        <div className="clearfix"></div>
                      </div>
                    </div>
                  </div>
                </div>
                <div className="row">
                  <h2 className="text-center">Project Metadata</h2>

                  <div className="text-right">
                  {
                    this.state.view.light &&
                    (<button type="button" className="btn btn-link" onClick={this.showAll}>
                        Show All Options&nbsp;<i className="fa fa-caret-down"></i>
                    </button>)
                  }
                  {
                    !this.state.view.light &&
                    (<button type="button" className="btn btn-link" onClick={this.showLight}>
                        Show Less Options&nbsp;<i className="fa fa-caret-up"></i>
                    </button>)
                  }
                  </div>
                  <div className="col-sm-4">
                    <div className="form-group">
                      <label forHtml="projectGroup">Group</label>
                      <input className="form-control" id="projectGroup" type="text" placeholder="Enter the project group..."
                             required="required" value={this.state.value.project.group}/>
                    </div>
                  </div>
                  <div className="col-sm-4">
                    <div className="form-group">
                      <label forHtml="projectArtifact">Artifact</label>
                      <input className="form-control" id="projectArtifact" type="text" placeholder="Enter the project group..."
                             required="required" value={this.state.value.project.artifact}/>
                    </div>
                  </div>
                  {
                    !this.state.view.light &&
                      [<div className="col-sm-4">
                        <div className="form-group">
                          <label forHtml="projectVersion">Version</label>
                          <input className="form-control" id="projectVersion" type="text" placeholder="Enter the project group..." required="required"
                                 value={this.state.value.project.version}/>
                        </div>
                      </div>,
                      <div className="col-sm-4">
                        <div className="form-group">
                          <label forHtml="projectName">Name</label>
                          <input className="form-control" id="projectName" type="text" placeholder="Enter the project group..." required="required"
                                 value={this.state.value.project.name}/>
                        </div>
                      </div>,
                      <div className="col-sm-4">
                        <div className="form-group">
                          <label forHtml="projectDescription">Description</label>
                          <input className="form-control" id="projectDescription" type="text" placeholder="Enter the project group..."
                                 required="required" value={this.state.value.project.description}/>
                        </div>
                      </div>]
                  }
                  <div className="col-sm-4">
                    <div className="form-group">
                      <label forHtml="projectPackage">Package</label>
                      <input className="form-control" id="projectPackage" type="text" placeholder="Enter the project group..."
                             required="required" value={this.state.value.project.packageBase}/>
                    </div>
                  </div>
                </div>
               </form>
			</div>
		);
	}
}
