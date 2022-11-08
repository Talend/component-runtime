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
package org.talend.sdk.component.documentation.sample.updatable.components.processor;

import java.io.Serializable;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.action.Updatable;
import org.talend.sdk.component.api.configuration.condition.ActiveIf;
import org.talend.sdk.component.api.configuration.ui.DefaultValue;
import org.talend.sdk.component.api.configuration.ui.OptionsOrder;
import org.talend.sdk.component.api.meta.Documentation;

@OptionsOrder({"otherConfig", "updateMe"})
@Documentation("TODO fill the documentation for this configuration")
public class UpdateConfiguration implements Serializable {

    @Option
    @DefaultValue("Update button will appear after me, change me before clicking on the button")
    @Documentation("This other option will be before the 'update' button")
    private String otherConfig;

    @Option // it is important all parameters sent are JSON friendly so getters/setters for objects
    @Updatable(
            after = "complex", // add the button after the last field of IAmUpdatable
            parameters = { // callback parameter binding
                    "." , // send my own value
                    "../otherConfig" // the path here is relative to updateMe itself so go up to send also a sibling
            },
            value = "updateMe") // callback name/identifier in @Service classes
    private IAmUpdatable updateMe;

    public String getOtherConfig() {
        return otherConfig;
    }

    public IAmUpdatable getUpdateMe() {
        return updateMe;
    }

    public void setOtherConfig(final String otherConfig) {
        this.otherConfig = otherConfig;
    }

    public void setUpdateMe(final IAmUpdatable updateMe) {
        this.updateMe = updateMe;
    }

    @OptionsOrder({"fullUpdate", "simple", "complex"})
    @Documentation("TODO fill the documentation for this configuration")
    public static class IAmUpdatable implements Serializable {
        @Option
        @Documentation("The type of update to do when clicking on the button, " +
                "if selected it updates simple and complex")
        private boolean fullUpdate;

        @Option
        @Documentation("This value is always filled")
        private String simple;

        @Option
        @ActiveIf(target = "fullUpdate", value = "true")
        @Documentation("This value is set if full update is true only")
        private String complex;

        public boolean isFullUpdate() {
            return fullUpdate;
        }

        public void setFullUpdate(final boolean fullUpdate) {
            this.fullUpdate = fullUpdate;
        }

        public String getSimple() {
            return simple;
        }

        public void setSimple(final String simple) {
            this.simple = simple;
        }

        public String getComplex() {
            return complex;
        }

        public void setComplex(final String complex) {
            this.complex = complex;
        }
    }
}