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
package org.talend.sdk.component.test.connectors.service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.asyncvalidation.AsyncValidation;
import org.talend.sdk.component.api.service.asyncvalidation.ValidationResult;
import org.talend.sdk.component.api.service.completion.SuggestionValues;
import org.talend.sdk.component.api.service.completion.SuggestionValues.Item;
import org.talend.sdk.component.api.service.completion.Suggestions;
import org.talend.sdk.component.api.service.healthcheck.HealthCheck;
import org.talend.sdk.component.api.service.healthcheck.HealthCheckStatus;
import org.talend.sdk.component.api.service.healthcheck.HealthCheckStatus.Status;
import org.talend.sdk.component.api.service.update.Update;
import org.talend.sdk.component.test.connectors.config.NestedConfig;
import org.talend.sdk.component.test.connectors.config.TheDatastore;

@Service
public class UIService {

    /**
     * In this service sample class we will implement every existing particular actions to check their API usages.
     * Services actions are listed here: https://talend.github.io/component-runtime/main/latest/services-actions.html
     *
     * Implemented:
     * - Suggestions https://talend.github.io/component-runtime/main/latest/services-actions.html#_suggestions
     * - Update https://talend.github.io/component-runtime/main/latest/services-actions.html#_update
     * - Validation https://talend.github.io/component-runtime/main/latest/services-actions.html#_validation
     *
     * Todo:
     * - Close Connection https://talend.github.io/component-runtime/main/latest/services-actions.html#_close_connection
     * - Create Connection
     * https://talend.github.io/component-runtime/main/latest/services-actions.html#_create_connection
     * - Discoverdataset https://talend.github.io/component-runtime/main/latest/services-actions.html#_discoverdataset
     * - Dynamic Values https://talend.github.io/component-runtime/main/latest/services-actions.html#_dynamic_values
     * - Healthcheck https://talend.github.io/component-runtime/main/latest/services-actions.html#_healthcheck
     * - User https://talend.github.io/component-runtime/main/latest/services-actions.html#_user
     * - built_in_suggestable
     * https://talend.github.io/component-runtime/main/latest/services-actions.html#_built_in_suggestable
     */

    public final static String LIST_ENTITIES = "action_LIST_ENTITIES";

    public final static String UPDATE_CONFIG = "action_UPDATE";

    public final static String VALIDATION = "action_VALIDATION";

    public final static String HEALTH_CHECK = "action_HEALTH_CHECK";

    @Service
    private I18n i18n;

    /**
     * Suggestions action
     *
     * https://talend.github.io/component-runtime/main/latest/services-actions.html#_suggestions
     *
     * Returned type: org.talend.sdk.component.api.service.completion.SuggestionValues
     * 
     * @return
     */
    @Suggestions(LIST_ENTITIES)
    public SuggestionValues getListEntities() {

        List<Item> entities = Arrays.asList(1, 2, 3, 4)
                .stream()
                .map(i -> String.valueOf(i))
                .map(i -> new Item(i, i18n.entityName(i)))
                .collect(Collectors.toList());

        return new SuggestionValues(true, entities);
    }

    /**
     * Update action
     *
     * https://talend.github.io/component-runtime/main/latest/services-actions.html#_update
     *
     * @return
     */
    @Update(UPDATE_CONFIG)
    public NestedConfig retrieveFeedback(final NestedConfig source) throws Exception {
        NestedConfig dest = new NestedConfig();
        dest.setStringOption1(i18n.setByService(source.getStringOption1()));
        dest.setStringOption2(i18n.setByService(source.getStringOption2()));
        return dest;
    }

    /**
     * Validation action
     *
     * https://talend.github.io/component-runtime/main/latest/services-actions.html#_update
     *
     * Returned type: org.talend.sdk.component.api.service.asyncvalidation.ValidationResult
     * 
     * @return
     */
    @AsyncValidation(VALIDATION)
    public ValidationResult retrieveValidation() throws Exception {
        ValidationResult result = new ValidationResult();

        result.setStatus(ValidationResult.Status.OK);
        result.setComment(i18n.validationComment());
        return result;
    }


    /**
     * Healthcheck action
     *
     * Documentation: https://talend.github.io/component-runtime/main/latest/services-actions.html#_healthcheck
     * Type: healthcheck
     * API: @org.talend.sdk.component.api.service.healthcheck.HealthCheck
     *
     * Returned type: org.talend.sdk.component.api.service.healthcheck.HealthCheckStatus
     */
    @HealthCheck(HEALTH_CHECK)
    public HealthCheckStatus discoverHealthcheck(@Option final TheDatastore datastore) {
        try {
            datastore.setUrl(i18n.setByService(datastore.getUrl()));
            return new HealthCheckStatus(Status.OK, "HealthCheck Success");
        } catch (Exception e) {
            return new HealthCheckStatus(Status.KO, "HealthCheck Failed");
        }
    }

}
