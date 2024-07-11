/**
 *  Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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

import React from "react";
import { CircularProgress, TreeView, Toggle } from "@talend/react-components";
import { Modal } from "@talend/design-system";

import theme from "./Menu.module.scss";

class Menu extends React.Component {
  constructor(props) {
    super(props);
    this.onSelect = this.onSelect.bind(this);
    this.onToggle = this.onToggle.bind(this);
    this.onSwitch = this.onSwitch.bind(this);
    this.noOp = () => {};
  }

  componentDidMount() {
    this.props.getComponentsList();
  }

  onToggle(event, node) {
    this.props.toggleComponent(node);
  }

  onSelect(event, node) {
    if (node.$$type === "component" && node.selected) {
      return;
    }
    this.props.selectComponent(node);
  }

  onSwitch() {
    this.props.getComponentsList({
      configuration: !this.props.configurationSelected,
    });
  }

  render() {
    if (this.props.isLoading) {
      return <CircularProgress light />;
    }
    return (
      <div className={theme.menu}>
        {this.props.error && <p>{this.props.error}</p>}
        <div className={theme.TreeViewHeader}>
          <div>Components</div>
          <Toggle
            id="index-switch"
            onChange={this.onSwitch}
            checked={this.props.configurationSelected}
          />
          <div>Configurations</div>
        </div>
        <TreeView
          id="menu"
          noHeader={true}
          className={theme.menu}
          structure={this.props.categories || []}
          selectedId={this.props.selectedId}
          onSelect={this.onSelect}
          onToggle={this.onToggle}
          onToggleAllSiblings={this.noOp}
        />
        {this.props.displayDocumentation && (
          <Modal
            header={{ title: "Documentation" }}
            onClose={this.props.onDocumentationModalClose}
          >
            <div
              dangerouslySetInnerHTML={{ __html: this.props.documentation }}
            />
          </Modal>
        )}
      </div>
    );
  }
}

export default Menu;
