/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.maven;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.talend.sdk.component.maven.api.Audience.Type.TALEND_INTERNAL;

import java.util.Optional;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.talend.sdk.component.maven.api.Audience;

public abstract class AudienceAwareMojo extends AbstractMojo {

    @Parameter(readonly = true, defaultValue = "${project.groupId}")
    private String groupId;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        warnIfNotWrongAudience();
    }

    private void warnIfNotWrongAudience() {
        getAudience()
                .map(Audience::value)
                .filter(audience -> audience == TALEND_INTERNAL)
                .filter(ignored -> !groupId.startsWith("org.talend") && !groupId.startsWith("com.talend"))
                .ifPresent(ignored -> getLog()
                        .warn("You are using " + getClass().getSimpleName()
                                + " Mojo, this is not intended to be used outside Talend internal projects scope."));
    }

    private Optional<Audience> getAudience() {
        Class<?> current = getClass();
        while (current != null) {
            Audience annotation = current.getAnnotation(Audience.class);
            if (annotation != null) {
                return of(annotation);
            }
            current = current.getSuperclass();
        }
        return empty();
    }
}
