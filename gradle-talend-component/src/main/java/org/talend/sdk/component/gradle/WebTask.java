/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.gradle;

import java.util.Collection;

import org.gradle.api.tasks.TaskAction;

public class WebTask extends TaCoKitTask {

    @Override
    protected boolean needsWeb() {
        return true;
    }

    @TaskAction
    public void web() {
        executeInContext(() -> {
            try {
                doWeb();
            } catch (final Exception e) {
                throw new IllegalStateException(e);
            }
        });
    }

    private void doWeb() throws Exception {
        final TaCoKitExtension extension =
                TaCoKitExtension.class.cast(getProject().getExtensions().findByName("talendComponentKit"));

        final String gav = mainGav();
        getLogger()
                .info("This task will rely on a provisionned maven repository with your dependencies and " + gav + ","
                        + "you can publish your artifact with `./gradlew publishToMavenLocal`");
        Runnable.class
                .cast(Thread
                        .currentThread()
                        .getContextClassLoader()
                        .loadClass("org.talend.sdk.component.tools.WebServer")
                        .getConstructor(Collection.class, Integer.class, Object.class, String.class)
                        .newInstance(extension.getServerArguments(), extension.getServerPort(), getLogger(), gav))
                .run();
    }
}
