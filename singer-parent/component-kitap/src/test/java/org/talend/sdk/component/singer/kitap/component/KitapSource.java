/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.singer.kitap.component;

import java.io.Serializable;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.slf4j.LoggerFactory;
import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.Producer;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Version
@Emitter(family = "kitaptest", name = "kitapsource")
public class KitapSource implements Serializable {

    private final RecordBuilderFactory factory;

    private final Configuration configuration;

    private int remaining;

    @PostConstruct
    public void init() {
        remaining = configuration.recordCount;
    }

    @Producer
    public Record next() {
        if (remaining > 0) {
            remaining--;
            switch (remaining) {
                case -1: // not managed anymore
                    final org.apache.logging.log4j.Logger log4j2 = LogManager.getLogger("log4j2");
                    log4j2.info("log4j2 info");
                    log4j2.error("log4j2 error");
                    break;
                case 8:
                    final Logger log4j = Logger.getLogger("log4j");
                    log4j.info("log4j info");
                    log4j.error("log4j error");
                    break;
                case 7:
                    final org.slf4j.Logger slf4jLogback = LoggerFactory.getLogger("logback");
                    slf4jLogback.info("logback info");
                    slf4jLogback.error("logback error");
                    break;
                case 6:
                    final java.util.logging.Logger jul = java.util.logging.Logger.getLogger("jul");
                    jul.info("jul info");
                    jul.severe("jul error");
                    break;
                default:
            }
            return factory.newRecordBuilder().withInt("record_number", configuration.recordCount - remaining).build();
        }
        return null;
    }

    public static class Configuration {

        @Option
        private int recordCount = 1;
    }
}