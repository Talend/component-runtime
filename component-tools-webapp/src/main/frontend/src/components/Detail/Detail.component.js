/**
 *  Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
// import { CircularProgress } from '@talend/react-components';
// import { UIForm } from '@talend/react-forms/lib/UIForm';
import TCompForm from 'component-kit.js/lib/TCompForm';

// function NoSelectedComponent() {
//   return (
//     <div>
//       <h1>No component selected</h1>
//       <p>Click on a component to see its form</p>
//     </div>
//   );
// }

class Detail extends React.Component {
  constructor(props) {
    super(props);
    // this.trigger = kit.createTriggers({ url: 'api/v1/application/action' });
    // this.onTrigger = this.onTrigger.bind(this);
  }

  // onTrigger(event, payload) {
  //   return this.trigger(event, payload)
  //     .then(triggerResult => {
  //       if (triggerResult.properties) {
  //         this.props.onChange(event, triggerResult);
  //       }
  //       if (triggerResult.errors) {
  //         this.props.onErrors(event, triggerResult.errors);
  //       }
  //     });
  // }

  render() {
    return (
      <TCompForm
        definitionURL="/api/v1/forms/add"
      />
    );
    // } else if (!this.props.uiSpec) {
    //   return (<NoSelectedComponent/>);
    // } else if (this.props.submitted) {
    //     const configuration = kit.flatten(this.props.uiSpec.properties);
    //   return (
    //     <div>
    //       <pre>{JSON.stringify(configuration, undefined, 2)}</pre>
    //       <button className="btn btn-success" onClick={this.props.backToComponentEdit}>Back to form</button>
    //     </div>
    //   );
    // } else {
    //   return (
    //     <UIForm
    //       data={this.props.uiSpec}
    //       onChange={this.props.onChange}
    //       onErrors={this.props.onErrors}
    //       onTrigger={this.onTrigger}
    //       onSubmit={this.props.onSubmit}
    //       actions={
    //         [
    //           {
    //             bsStyle: 'primary',
    //             label: 'Show Configuration',
    //             type: 'submit',
    //             widget: 'button',
    //           }
    //         ]
    //       }
    //     />
    //   );
    // }
  }
}

export default Detail;
