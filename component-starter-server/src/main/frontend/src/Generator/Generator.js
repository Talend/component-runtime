/**
 *  Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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
import StepZilla from 'react-stepzilla';

import theme from './Generator.scss';

// import Project from './Project';

function WrappingContent (props) {
  return <div className={theme.content}>{props.value}</div>;
}

function Sample (props) {
  return <div>{props.value}</div>;
}


// override the theme to use our customization
class StyledStepZilla extends StepZilla {
  renderSteps() {
    const jumpUsingParentDomElement = evt => {
      evt.target = evt.target.parentElement.parentElement;
      this.jumpToStep(evt);
    };

    return this.props.steps.map((s, i)=> {
      const classes = [this.getClassName(theme.progtrckr, i)];
      if (this.state.navState.current === i) {
        classes.push(theme.active);
      }
      return (
          <li className={classes.join(' ')} key={i} value={i}>
            <div>
              <span className={theme.stepCounter} onClick={e => jumpUsingParentDomElement(e)}>{i + 1}</span>
              <span className={theme.sectionLabel} onClick={e => jumpUsingParentDomElement(e)}>{this.props.steps[i].name}</span>
            </div>
        </li>
      )
    });
  }

  render() {
    let result = super.render();
    if (!!result) {
      if (result.type === 'div') {
        result.props.className = theme.container;
        result.props.children[0].props.className = theme.progtrckr; // tc-layout-two-columns-left
      }
    }
    return result;
  }
}

export default class Generator extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
         project: {
           buildType: 'Maven',
           version: '0.0.1-SNAPSHOT',
           group: 'com.company',
           artifact: 'company-component',
           name: 'A Component',
           description: 'A generated component project',
           packageBase: 'com.company.talend.components',
           facets: []
         },
         steps: [
           {name: 'Start', component: <WrappingContent value={<Sample value="s1" />} />},
           {name: 'Step 2', component: <WrappingContent value={<Sample value="s2" />} />},
           {name: 'Finish', component: <WrappingContent value={<Sample value="s3" />} />}
         ]
    };
  }

  // <Project project={this.state.project} />
  render() {
    return (
      <div className={theme.Generator}>
        <StyledStepZilla showNavigation={false} steps={this.state.steps} />
      </div>
    );
  }
}
