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
import { Link } from 'react-router-dom';
import { TreeView, IconsProvider } from '@talend/react-components';

import { COMPONENT_SELECTED } from './reducers';
import Api from './Api';

export default class Menu extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      icons: {},
      remainingIcons: 0
    };
    [ 'onTreeClick', 'onTreeSelect' ].forEach(a => this[a] = this[a].bind(this));
  }

  componentWillMount() {
    Api.loadComponents()
      .then(payload => this.setState(state => {
        let icons = {};
        state.tree = payload.components.reduce((agg, component) => {
          const node = {
            $$id: component.id.id,
            $$detail: component.links[0].path,
            icon: component.icon.customIconType ? `app_component-${component.id.id}` : component.icon,
            name: component.displayName
          };
          if (!icons[node.icon]) {
            icons[node.icon] = { path: `/component/icon/${component.id.id}`, config: component.icon };
          }

          if (!component.categories || component.categories.length === 0) {
            component.categories = ['Others'];
          }

          component.categories.forEach(category => {
            if (!agg.categoriesIndex[category]) {
              const categoryNode = {
                name: category,
                children: [],
                toggle: false,
                $$childrenIndex: {}
              };
              agg.categoriesIndex[category] = categoryNode;
              agg.result.push(categoryNode);
            }
            if (!agg.categoriesIndex[category].$$childrenIndex[component.id.family]) {
              const familyNode = {
                icon: component.iconFamily.customIconType ? `app_family-${component.id.familyId}` : component.iconFamily.icon,
                name: component.familyDisplayName,
                children: [],
                toggle: false,
                $$childrenIndex: {}
              };
              agg.categoriesIndex[category].$$childrenIndex[component.id.family] = familyNode;
              agg.categoriesIndex[category].children.push(familyNode);
              if (!icons[familyNode.icon]) {
                icons[familyNode.icon] = { path: `/component/icon/family/${component.id.familyId}`, config: component.iconFamily };
              }
            }

            const parent = agg.categoriesIndex[category].$$childrenIndex[component.id.family];
            parent.$$childrenIndex = node;
            parent.children.push(node);
          });

          // now open the first part of the tree
          let children = agg.result;
          while (children && children.length > 0) {
            children[0].toggled = true;
            children = children[0].children;
          }

          return agg;
        }, {result: [], categoriesIndex: {}}).result;

        let svgIcons = {};
        const iconToLoad = Object.keys(icons);
        state.remainingIcons = iconToLoad.length;
        Object.keys(icons).filter(id => icons[id].config.customIconType).forEach(id => {
          Api.getIcon(icons[id].path)
            .then(icon => {
              // todo: better handling of svg when we'll support svg as a first citizen image format
              const content = (
                <svg xmlns="http://www.w3.org/2000/svg">
                  <image href={`data:${icons[id].config.customIconType};base64,${icon}`} height="20px" width="20px" />
                </svg>
              );

              svgIcons[id] = content;
              svgIcons[`${id}-closed`] = content;

              state.remainingIcons--;
              if (state.remainingIcons === 0) {
                this.setState({ icons: {...svgIcons} });
              }
            });
        });
      }));
  }

  onTreeClick(item) {
    // no-op, mandatory but handled in select for this impl
  }

  onTreeSelect(item) {
    let oldComponent = this.state.lastSelectedNode;
    if (oldComponent) {
      oldComponent.selected = false;
    }
    item.selected = true;

    if (item.children) {
      item.toggled = !item.toggled;
    } // else it is always true

    const eventData = { component: item.children ? undefined : item, lastSelectedNode: item };
    this.props.store.dispatch({ type: 'COMPONENT_SELECTED', data: eventData });
    this.setState(eventData);
  }

  render() {
    if (!this.state.tree) {
      return (<div>Loading component list...</div>);
    }
    return (
      <span>
        {
          this.state.remainingIcons === 0 && <IconsProvider defaultIcons={{}} icons={this.state.icons} />
        }
        <TreeView
          headerText="Components"
          className={this.props.className}
          structure={this.state.tree}
          onClick={this.onTreeClick}
          onSelect={this.onTreeSelect}
        />
      </span>
    );
  }
}
