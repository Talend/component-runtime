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
import java.util.jar.JarFile

import static java.util.Locale.ENGLISH

def loader = Thread.currentThread().contextClassLoader
def jarMarker = 'META-INF/maven/org.talend.ui/talend-icon/pom.properties'
def talendIconUrl = loader.getResource(jarMarker)
if (talendIconUrl == null) {
    throw new IllegalStateException('No talend-icon in groovy classloader');
}
if ('jar' == talendIconUrl.protocol) {
    talendIconUrl = new URL(talendIconUrl.file)
}
def iconJar = new File(talendIconUrl.file.substring(0, talendIconUrl.file.length() - (jarMarker.length() + 2)))

def icons = []
def jar = new JarFile(iconJar)
try {
    icons.addAll(Collections.list(jar.entries())
            .findAll {
                it.name.endsWith('.svg') &&
                ((it.name.startsWith('icons/') && it.name.split('/').length == 2) ||
                ((it.name.startsWith('icons/svg/') || it.name.startsWith('icons/svg-deprecated/')) && it.name.split('/').length == 3))
            }
            .collect {
        def icon = it.name.substring(it.name.lastIndexOf('/') + 1, it.name.length() - ('.svg'.length()))
        "${icon.replace('-', '_').toUpperCase(ENGLISH)}(\"${icon}\")"
    })
} finally {
    jar.close()
}

icons.sort()

// add them at the end, always since they are not "dynamic"
icons.add('CUSTOM("custom")')
icons.add('DEFAULT("default")')

def iconJavaBuilder = new StringBuilder()
def skipUntilSemiColon = false
def iconJava = new File(project.basedir, 'src/main/java/org/talend/sdk/component/api/component/Icon.java')
def oldIcons = []
iconJava.eachLine {
    if (it.contains("enum IconType {")) {
        skipUntilSemiColon = true
        iconJavaBuilder.append(it).append('\n')
        icons.each { icon ->
            iconJavaBuilder.append("        ${icon},\n")
        }
    } else if (skipUntilSemiColon) {
        def trim = it.trim()
        if (it.trim().endsWith(';')) {
            skipUntilSemiColon = false
            if (!icons.contains(trim.substring(0, trim.length() - 1))) {
                iconJavaBuilder.append(it)
            } else {
                iconJavaBuilder.setLength(iconJavaBuilder.length() - 2)
            }
            iconJavaBuilder.append(';\n')
        } else if (trim.endsWith(',')) {
            oldIcons.add(trim.substring(0, trim.length() - 1))
        }
    } else if (!skipUntilSemiColon) {
        iconJavaBuilder.append(it).append('\n')
    }
}

// before rewriting the file ensure we didnt loose icons
def missingIcons = oldIcons - icons - [ /* to force an update with icon diff add them here */ ]
if (!missingIcons.isEmpty()) {
    throw new IllegalArgumentException(
            "These icons were here and are no more supported, either add an exception in CreateIcons.groovy or add them back:\n> ${missingIcons}")
}


iconJava.text = iconJavaBuilder.toString()
