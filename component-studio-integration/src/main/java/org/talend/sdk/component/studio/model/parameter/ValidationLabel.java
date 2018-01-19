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
package org.talend.sdk.component.studio.model.parameter;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.swt.graphics.RGB;
import org.talend.core.model.process.EParameterFieldType;
import org.talend.core.model.process.IElement;

/**
 * Synthetic Element Parameter which shows validation message
 */
public class ValidationLabel extends TaCoKitElementParameter {

    private static final String DELIMITER = "; ";

    private static final RGB RED = new RGB(255, 0, 0);

    private final Set<String> validationMessages = new HashSet<>();

    public ValidationLabel(final IElement element) {
        super(element);
        setColor(RED);
        setDisplayName("Validation Label");
        setFieldType(EParameterFieldType.LABEL);
        setShow(false);
        setValue("");
    }

    public void show(final String message) {
        validationMessages.add(message);
        setValue(buildValue());
        setShow(true);
    }

    public void hide(final String message) {
        validationMessages.remove(message);
        setValue(buildValue());
        setShow(!validationMessages.isEmpty());
    }

    private String buildValue() {
        return String.join(DELIMITER, validationMessages);
    }

}
