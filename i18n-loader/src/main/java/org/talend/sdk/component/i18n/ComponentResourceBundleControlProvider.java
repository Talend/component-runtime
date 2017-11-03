/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.i18n;

import java.util.ResourceBundle;
import java.util.spi.ResourceBundleControlProvider;

// don't forget to set it in Extension classloader
// -Djava.ext.dirs=${java.home}/jre/lib/ext:/folder/containing/this/jar/
//
// Note for Java9 migration:
// - check out http://bugs.java.com/bugdatabase/view_bug.do?bug_id=JDK-8190696 before reading next bullets
// - ResourceBundleControlProvider works for unamed modules but for named modules it is just ignored
// - the replacement is java.util.spi.ResourceBundleProvider which requires to implement:
//      - a class with the name <packagename>.spi.<classname>Provider implementing ResourceBundleProvider
//      - add in module-info
//        "uses <packagename>.spi.<classname>Provider;" and
//        "provides <packagename>.spi.<classname>Provider with <packagename>.spi.<classname>Provider;"
// -> we can do a custom loader to automatize it or use a javaagent to rewrite the ResourceBundle.getBundle
//    to call a ComponentResourceBundleControlProvider facade, it will also need to add a module-info the right import
//    (i18n.loader by default but we will want to name the module org.talend.sdk.component.i18n)
//
// For now the implementation only targets Java 8
public class ComponentResourceBundleControlProvider implements ResourceBundleControlProvider {

    @Override
    public ResourceBundle.Control getControl(final String baseName) {
        final ProviderLocator.Provider delegate = ProviderLocator.instance().current();
        return delegate == null ? null : delegate.getProvider().getControl(baseName);
    }
}
