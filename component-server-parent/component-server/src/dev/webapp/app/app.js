/**
 *  Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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

//
// this code just grabs tcomp model,
// converts it to a simple metawidget/jsonschema one
//
// what is missing is a more advanced section handling, an exhaustive meta handling and a layout handling
//

axios.get('api/v1/component/details?identifiers=Y29tcG9uZW50LWpkYmMjamRiYyNpbnB1dA==')
  .then(function (response) { // demo code, use underscore in real code to do that (chain)
    var findProperties = function (props, prefix) {
        return props.filter(function (prop) {
            return prop.path.indexOf(prefix) == 0 && prop.path === (prefix + prop.name);
        });
    };

    var toMetawidget = function (properties, filter) {
        return properties.map(function (prop) {
            var model = { // convert to metawidget model
                name: prop.name,
                type: prop.type.toLowerCase(),
                masked: prop.metadata && prop.metadata['ui::credential']
            };

            var nested = findProperties(dataProperties, prop.path + '.');
            if (nested.length > 0) {
                model.properties = toMetawidget(nested);
            }

            return model;
            // reduce to a map instead of an array for metawigget
        }).reduce(function (map, prop) {map[prop.name] = prop;return map;}, {})
    };

    var dataProperties = response.data.details[0].properties;
    var props = toMetawidget(dataProperties
        .filter(function (prop) {
            // keep only first level properties
            return prop.path.indexOf('.') < 0;
        }));

    // if we have a single wrapper then unwrap it to avoid a level we don't care about
    var keys = Object.keys(props);
    if (keys.length == 1 && !!props[keys[0]].properties) {
        props = props[keys[0]].properties;
    }

    new metawidget.Metawidget(document.getElementById( 'metawidget' ), {
        inspector: new metawidget.inspector.JsonSchemaInspector({
            properties: props
        })
    }).buildWidgets();
  })
  .catch(function (error) {
    alert(JSON.stringify(error));
  });
