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


import java.lang.reflect.ParameterizedType
import javax.json.bind.annotation.JsonbTransient

import static java.util.Locale.ROOT
import static java.util.Optional.ofNullable

static def toPropType(type) {
    if (type == String.class) {
        return 'PropType.string'
    }
    if (type.isEnum()) {
        return "PropTypes.oneOf([\"${type.enumConstants.collect {it.name()}.join('", "')}\"])" as String
    }
    if (type == Boolean.class) {
        return 'PropType.boolean'
    }
    if (type == boolean.class) {
        return 'PropType.boolean.isRequired'
    }
    if (Number.class.isAssignableFrom(type) || type.isPrimitive()) {
        return 'PropType.number'
    }
    if (type == Object.class) {
        return 'PropType.object'
    }
    return "PropTypes.shape(${type.simpleName})" as String
}

static def isNotPrimitive(it) {
    it != Object.class &&
    it != String.class &&
    it != Boolean.class &&
    !(Class.class.isInstance(it) && (it.isEnum() || Number.class.isAssignableFrom(it) || it.isPrimitive())) &&
    !(ParameterizedType.class.isInstance(it) && it.actualTypeArguments[0] == String.class)
}

class GenerationAggregator {
    Collection<String> filenames
    String namespace
}

GenerationAggregator createPropType(parent, clazz, aggregate) {
    def namespace = parent == null ? clazz.simpleName.toLowerCase(ROOT) : parent
    def output = new File(project.build.directory, "prop-types/src/${namespace}/${clazz.simpleName}.js")
    if (output.exists()) {
        return
    }
    output.parentFile.mkdirs()
    aggregate.filenames.add(clazz.simpleName)
    if (!aggregate.namespace) {
        aggregate.namespace = namespace
    }

    def nested = clazz.declaredFields
        .findAll { !it.isAnnotationPresent(JsonbTransient.class) }
        .findAll { isNotPrimitive(it.genericType) }
        .collect {
            def type = it.genericType
            if (ParameterizedType.class.isInstance(type)) {
                if (Collection.class.isAssignableFrom(type.rawType)) {
                    if (type.actualTypeArguments[0].class.name == "sun.reflect.generics.reflectiveObjects.WildcardTypeImpl") {
                        return org.talend.sdk.component.form.model.uischema.UiSchema.NameValue.class
                    }
                    return type.actualTypeArguments[0]
                }
                if (Map.class.isAssignableFrom(type.rawType)) { // no need to impl a custom validator for this case
                    return type.actualTypeArguments[1]
                }
                throw new IllegalArgumentException("Unsupported: ${it}")
            }
            type
        }.findAll { isNotPrimitive(it) }.unique()

    output.withWriter('UTF-8') { writer ->
        def appendedAttributes = new StringBuilder()

        writer.write('import PropTypes from \'prop-types\';\n')
        nested.findAll { it != Object.class && it != clazz }.collect { it.simpleName }.sort().each {
            writer.write("import ${it} from './${it}';\n")
        }
        writer.write('\n')
        writer.write('const definition = {\n')
        clazz.declaredFields.findAll { !it.isAnnotationPresent(JsonbTransient.class) }.sort { it.name }.each {
            def type = it.genericType
            def name = ofNullable(it.getAnnotation(javax.json.bind.annotation.JsonbProperty.class))
                .map { t -> t.value() }.orElse(it.name)
            if (ParameterizedType.class.isInstance(it.genericType)) {
                if (Collection.class.isAssignableFrom(type.rawType)) {
                    def nestedType = type.actualTypeArguments[0]
                    if (nestedType.class.name == "sun.reflect.generics.reflectiveObjects.WildcardTypeImpl") {
                        nestedType = org.talend.sdk.component.form.model.uischema.UiSchema.NameValue.class
                    }
                    if (nestedType == clazz) {
                        appendedAttributes.append("definition.${name} = PropType.arrayOf(${toPropType(nestedType)});\n")
                    } else {
                        writer.write("  ${name}: PropType.arrayOf(${toPropType(nestedType)}),\n")
                    }
                } else if (Map.class.isAssignableFrom(type.rawType)) { // no need to impl a custom validator for this case
                    writer.write("  ${name}: PropTypes.object,\n")
                } else {
                    throw new IllegalArgumentException("Unsupported: ${it}")
                }
            } else if (clazz == type) {
                appendedAttributes.append("definition.${name} = PropType.arrayOf(${toPropType(type)});\n")
            } else {
                writer.write("  ${name}: ${toPropType(type)},\n")
            }
        }
        writer.write('};\n')
        if (appendedAttributes.length() > 0) {
            writer.append(appendedAttributes.toString())
        }
        writer.append("\nexport default definition;\n\n")
        writer.flush()
    }

    nested.each {
        createPropType(namespace, it, aggregate)
    }

    return aggregate
}

static def writeIndex(file, list) {
    file.withWriter('UTF-8') { writer ->
        list.each {
            writer.append("import ${it} from './${it}';\n")
        }
        writer.append('\n')
        list.each {
            writer.append("export { ${it} };\n")
        }
    }
}

def namespaces = types.split(',').collect {
    def result = createPropType(null, Thread.currentThread().contextClassLoader.loadClass(it.trim()), new GenerationAggregator(filenames: []))
    if (!result) {
        return null
    }
    def index = new File(project.build.directory, "prop-types/src/${result.namespace}/index.js")
    writeIndex(index, result.filenames)
    return index.parentFile.name
}
if (namespaces.count { it != null } > 0) {
    writeIndex(new File(project.build.directory, "prop-types/src/index.js"), namespaces)
}

new File(project.build.directory, "prop-types/package.json").withWriter('UTF-8') { writer ->
    writer.write("""{
  "name": "@talend/component-form-core",
  "description": "Generate prop-types from java JsonSchema/UiSchema model.",
  "main": "lib/index.js",
  "license": "Apache-2.0",
  "scripts": {
    "prepublish": "rimraf lib && babel -d lib ./src/"
  },
  "keywords": [
    "talend",
    "forms",
    "json",
    "schema"
  ],
  "author": "Talend <contact@talend.com>",
  "homepage": "https://talend.github.io/component-runtime/",
  "bugs": {
    "url": "https://jira.talendforge.org/projects/TCOMP/issues"
  },
  "repository": {
    "type": "git",
    "url": "${project.scm.connection.replace('scm:git:', '')}"
  },
  "devDependencies": {
    "babel-cli": "^6.26.0",
    "babel-core": "^6.26.0",
    "babel-preset-env": "^1.6.0",
    "prop-types": "^15.5.10"
  },
  "version": "${project.version}"
}""")
}
new File(project.build.directory, "prop-types/.babelrc").withWriter('UTF-8') { writer ->
    writer.write("""{
  "presets": [
    [
      "env",
      {
        "targets": {
          "browsers": [
            ">0.25%",
            "not op_mini all",
            "IE 11"
          ]
        }
      }
    ]
  ]
}""")
}
