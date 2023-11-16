/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.tools.validator;

import java.io.File;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Stream;

import org.talend.sdk.component.api.component.Icon;
import org.talend.sdk.component.runtime.manager.ParameterMeta;
import org.talend.sdk.component.runtime.manager.reflect.ParameterModelService;
import org.talend.sdk.component.tools.validator.Validators.ValidatorHelper;

/**
 * Default helper class for tests.
 */
public class FakeHelper implements ValidatorHelper {

    @Override
    public boolean isService(Parameter parameter) {
        return false;
    }

    @Override
    public ResourceBundle findResourceBundle(Class<?> component) {
        return null;
    }

    @Override
    public String validateFamilyI18nKey(Class<?> clazz, String... keys) {
        return null;
    }

    @Override
    public List<ParameterMeta> buildOrGetParameters(Class<?> c) {
        return null;
    }

    @Override
    public String validateIcon(Icon annotation, Collection<String> errors) {
        return null;
    }

    @Override
    public ParameterModelService getParameterModelService() {
        return null;
    }

    @Override
    public Stream<File> componentClassFiles() {
        return null;
    }
}
