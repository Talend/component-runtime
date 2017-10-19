// ============================================================================
//
// Copyright (C) 2006-2017 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.components.form.api;

import static java.util.Optional.ofNullable;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.talend.components.form.model.UiActionResult;

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
                .map(String::valueOf).orElse(exception.getMessage()));
        // actionResult.setErrors(singletonMap("field", actionResult.getError()));
        actionResult.setType(UiActionResult.UpdateType.TF_SET_PARTIAL_ERROR);
        return actionResult;
    }
}
