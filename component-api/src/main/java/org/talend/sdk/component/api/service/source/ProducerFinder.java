/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.api.service.source;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;

import org.talend.sdk.component.api.record.Record;

/**
 * This service aims to retrieve a record iterator based on a configured dataset of a connector.
 * It's expected that Producer has no extra-configuration on dataset and is a finite producer (not a queue for example).
 */
public interface ProducerFinder extends Serializable {

    /**
     * Initialize the ProducerFinder
     *
     * @param plugin plugin id
     * @param builder component instantiate builder
     * @param converter function to convert to Record
     *
     * @return initialized ProducerFinder
     */
    ProducerFinder init(final String plugin, final Object builder, final Function<Object, Record> converter);

    /**
     * Retrieve iterator.
     *
     * @param familyName : connector family name.
     * @param inputName : dataset name.
     * @param version : version of configuration.
     * @param configuration : dataset configuration.
     *
     * @return the Record iterator
     */
    Iterator<Record> find(final String familyName, //
            final String inputName, //
            final int version, //
            final Map<String, String> configuration);
}
