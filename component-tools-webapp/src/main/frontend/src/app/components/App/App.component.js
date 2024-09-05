/**
 *  Copyright (C) 2006-2024 Talend Inc. - www.talend.com
 *
 *  Licensed under the Apache License, Version 2.0 (the 'License');
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an 'AS IS' BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import React from "react";

import {
  IconsProvider,
  HeaderBar,
  Layout,
  Notification,
} from "@talend/react-components";

import Menu from "../Menu";
import Detail from "../Detail";

import theme from "./App.module.scss";

function App(props) {
  const header = (
    <HeaderBar
      logo={{ isFull: true }}
      brand={{
        id: "header-brand",
        label: "Talend Component Kit Web Tester",
      }}
    />
  );

  return (
    <div className={theme.App}>
      <IconsProvider />
      <Layout mode={"TwoColumns"} header={header} one={<Menu />}>
        <Detail saga="Detail::start" />
      </Layout>
      <Notification
        leaveFn={props.removeNotification}
        notifications={props.notifications}
      />
    </div>
  );
}

export default App;
