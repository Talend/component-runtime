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
package org.talend.sdk.component.api.exception;

import javax.json.bind.annotation.JsonbCreator;

import lombok.Data;

/**
 * This class is dedicated to Studio's guess schema feature.
 * It has the same goal as ComponentException except that you can specify an action to execute in some cases.
 *
 * If you don't need such feature, just use ComponentException.
 *
 * See me TCOMP-2342 for more details.
 */
@Data
public class DiscoverSchemaException extends RuntimeException {

    public enum HandleErrorWith {
        EXCEPTION,
        SILENT,
        RETRY,
        EXECUTE_MOCK_JOB;
    }

    private HandleErrorWith possibleHandleErrorWith = HandleErrorWith.EXCEPTION;

    public DiscoverSchemaException(final ComponentException e) {
        super(e.getOriginalMessage(), e.getCause());
    }

    public DiscoverSchemaException(final ComponentException e, final HandleErrorWith handling) {
        super(e.getOriginalMessage(), e.getCause());
        setPossibleHandleErrorWith(handling);
    }

    public DiscoverSchemaException(final String message, final HandleErrorWith handling) {
        super(message);
        setPossibleHandleErrorWith(handling);
    }

    @JsonbCreator
    public DiscoverSchemaException(final String message, final StackTraceElement[] stackTrace,
            final HandleErrorWith handling) {
        super(message);
        setStackTrace(stackTrace);
        setPossibleHandleErrorWith(handling);
    }

}
