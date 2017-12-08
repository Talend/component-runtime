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
package org.talend.sdk.component.studio.model.parameter;

import static org.talend.sdk.component.studio.model.parameter.Metadatas.UI_GRIDLAYOUT_ADVANCED;
import static org.talend.sdk.component.studio.model.parameter.Metadatas.UI_GRIDLAYOUT_MAIN;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.talend.sdk.component.server.front.model.PropertyValidation;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

/**
 * Extends functionality of {@link SimplePropertyDefinition}
 * It doesn't allow to change <code>delegate</code> state
 */
class PropertyDefinitionDecorator extends SimplePropertyDefinition {

    /**
     * Separator in property path
     */
    private static final String PATH_SEPARATOR = ".";

    /**
     * Denotes that some property has no parent property
     */
    private static final String NO_PARENT_ID = "";

    /**
     * Suffix used in id ({@link SimplePropertyDefinition#getPath()}), which denotes Array typed property
     * (which is Table property in Studio)
     */
    private static final String ARRAY_PATH = "[]";

    private final SimplePropertyDefinition delegate;

    /**
     * Creates instance by wrapping existing {@link SimplePropertyDefinition} instance
     * All calls to {@link SimplePropertyDefinition} API will be delegated to wrapped instance
     * 
     * @param property {@link SimplePropertyDefinition} to wrap
     */
    PropertyDefinitionDecorator(final SimplePropertyDefinition property) {
        this.delegate = property;
    }

    /**
     * Wraps {@link Collection} or {@link SimplePropertyDefinition} to {@link Collection} of
     * {@link PropertyDefinitionDecorator}
     * 
     * @param properties original properties collection
     * @return wrapped properties
     */
    static Collection<PropertyDefinitionDecorator> wrap(final Collection<SimplePropertyDefinition> properties) {
        return properties.stream().map(property -> new PropertyDefinitionDecorator(property)).collect(
                Collectors.toList());
    }

    /**
     * Returns path for parent {@link SimplePropertyDefinition}
     * 
     * @return parent path
     */
    String getParentPath() {
        String path = delegate.getPath();
        if (!path.contains(PATH_SEPARATOR)) {
            return NO_PARENT_ID;
        }
        String parentPath = path.substring(0, path.lastIndexOf("."));
        // following is true, when parent has type=ARRAY
        if (parentPath.endsWith(ARRAY_PATH)) {
            parentPath = parentPath.substring(0, parentPath.lastIndexOf("[]"));
        }
        return parentPath;
    }

    /**
     * Checks whether it has {@link Metadatas#UI_GRIDLAYOUT_MAIN} metadata
     * 
     * @return true, if it contains main gridlayout metadata
     */
    boolean hasMainGridLayout() {
        return delegate.getMetadata().containsKey(UI_GRIDLAYOUT_MAIN);
    }

    /**
     * Checks whether it has {@link Metadatas#UI_GRIDLAYOUT_ADVANCED} metadata
     * 
     * @return true, is contains advanced gridlayout metadata
     */
    boolean hasAdvancedGridLayout() {
        return delegate.getMetadata().containsKey(UI_GRIDLAYOUT_ADVANCED);
    }

    @Override
    public boolean equals(final Object o) {
        return delegate.equals(o);
    }

    @Override
    public String getDefaultValue() {
        return delegate.getDefaultValue();
    }

    @Override
    public String getDisplayName() {
        return delegate.getDisplayName();
    }

    @Override
    public Map<String, String> getMetadata() {
        return delegate.getMetadata();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public String getPath() {
        return delegate.getPath();
    }

    @Override
    public String getType() {
        return delegate.getType();
    }

    @Override
    public PropertyValidation getValidation() {
        return delegate.getValidation();
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public void setDefaultValue(final String defaultValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDisplayName(final String displayName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setMetadata(final Map<String, String> metadata) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setName(final String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setPath(final String path) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setType(final String type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setValidation(final PropertyValidation validation) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

}
