/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.manager.service;

import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * m2 discovery process is used for plugins/connectors loading.
 */
public interface MavenRepositoryResolver {
    // a set of system properties available in the framework

    /**
     * System property used to enforce maven repository location.
     */
    String TALEND_COMPONENT_MANAGER_M2_REPOSITORY = "talend.component.manager.m2.repository";

    /**
     * System property used to enforce the location of settings.xml.
     */
    String TALEND_COMPONENT_MANAGER_M2_SETTINGS = "talend.component.manager.m2.settings";

    /**
     * Studio's property used to specify maven repository location. If set {@code global}, the Studio uses user's
     * settings. Otherwise, Studio's internal repository is used.
     */
    String STUDIO_MVN_REPOSITORY = "maven.repository";

    // a set of environment variables available in the framework or in maven builtin...

    String M2_HOME = "M2_HOME";

    String MAVEN_HOME = "MAVEN_HOME";

    // a set of constants

    String M2_REPOSITORY = ".m2/repository";

    String M2_SETTINGS = ".m2/settings.xml";

    String CONF_SETTINGS = "conf/settings.xml";

    String USER_HOME = System.getProperty("user.home", "");

    // some settings.xml regexp patterns
    Pattern XML_COMMENTS_PATTERN = Pattern.compile("(<!--.*?-->)", Pattern.DOTALL);

    Pattern XML_EMPTY_LINES_PATTERN = Pattern.compile("^\\s*$|\\n|\\r\\n");

    Pattern XML_LOCAL_REPO_PATTERN =
            Pattern.compile(".*<localRepository>(.+)</localRepository>.*", Pattern.CASE_INSENSITIVE);

    /**
     * Main entry point for the discovery process.
     * It allows to priorize how we may find the local maven repository path according the context.
     *
     * @return local maven repository path discovered.
     */
    Path discover();

    /**
     * Make sure that we provide a fallback if discovery fails.
     * such like {@code return PathHandler.get(USER_HOME).resolve(M2_REPOSITORY);}
     *
     * @return a fallback path to local maven repository
     */
    Path fallback();

}
