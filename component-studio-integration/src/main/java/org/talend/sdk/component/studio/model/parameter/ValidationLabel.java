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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.swt.graphics.RGB;
import org.talend.core.model.process.EParameterFieldType;
import org.talend.core.model.process.IElement;

/**
 * Synthetic Element Parameter which shows validation message
 */
public class ValidationLabel extends TaCoKitElementParameter {

    /**
     * Delimiter between messages in resulting validation message
     */
    private static final String DELIMITER = "; ";

    private static final RGB RED = new RGB(255, 0, 0);

    /**
     * Constraint messages
     */
    private final Set<String> constraintMessages = new HashSet<>();

    /**
     * Validation message
     */
    private String validationMessage;

    public ValidationLabel(final IElement element) {
        super(element);
        setColor(RED);
        setDisplayName("Validation Label");
        setFieldType(EParameterFieldType.LABEL);
        setShow(false);
        setValue("");
    }

    public void showValidation(final String message) {
        validationMessage = message;
        setValue(buildValue());
        setShow(true);
    }

    public void hideValidation() {
        validationMessage = null;
        setValue(buildValue());
        setShow(false);
    }

    public void showConstraint(final String message) {
        constraintMessages.add(message);
        setValue(buildValue());
        setShow(true);
    }

    public void hideConstraint(final String message) {
        constraintMessages.remove(message);
        setValue(buildValue());
        setShow(!constraintMessages.isEmpty());
    }

    private String buildValue() {
        return Stream
                .concat(constraintMessages.stream(), Stream.of(validationMessage))
                .filter(Objects::nonNull)
                .collect(Collectors.joining(DELIMITER));
    }

}
