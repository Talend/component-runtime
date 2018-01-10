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
package org.talend.sdk.component.form.api;

import static java.util.Optional.ofNullable;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.talend.sdk.component.form.model.UiActionResult;

// TODO: for now it is a passthrough but we'll need to comply to ui formats
public class ActionService {

    public UiActionResult map(final String actionType, final Map<String, Object> action) {
        final UiActionResult actionResult = new UiActionResult();
        actionResult.setRawData(action);
        switch (actionType) {
        case "healthcheck":
            // comment and status keys
            break;
        default:
        }
        return actionResult;
    }

    public UiActionResult map(final WebException exception) {
        final UiActionResult actionResult = new UiActionResult();
        actionResult.setRawData(exception.getData());
        // default error will be mapped to the calling option
        actionResult.setError(ofNullable(exception.getData())
                .flatMap(d -> Stream.of("description", "comment").map(d::get).filter(Objects::nonNull).findFirst())
                .map(String::valueOf)
                .orElse(exception.getMessage()));
        return actionResult;
    }
}
