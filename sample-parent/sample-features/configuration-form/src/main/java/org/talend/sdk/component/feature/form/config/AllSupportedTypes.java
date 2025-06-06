/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.feature.form.config;

import java.io.File;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.meta.Documentation;

import lombok.Data;

@Data
@GridLayout(value = {
        @GridLayout.Row({ "aString" }),
        @GridLayout.Row({ "aBoolean" }),
        @GridLayout.Row({ "aBigDecimal" }),
        @GridLayout.Row({ "aFloat" }),
        @GridLayout.Row({ "aBigInteger" }),
        @GridLayout.Row({ "anInteger" }),
        @GridLayout.Row({ "aFile" }),
        @GridLayout.Row({ "aInetAddress" }),
        @GridLayout.Row({ "aURI" }),
        @GridLayout.Row({ "aURL" }),
        @GridLayout.Row({ "aLocalDateTime" }),
        @GridLayout.Row({ "aZonedDateTime" })
})
public class AllSupportedTypes implements Serializable {

    @Option
    @Documentation("A String value.")
    private String aString;

    @Option
    @Documentation("A boolean value.")
    private Boolean aBoolean;

    @Option
    @Documentation("A BigDecimal value.")
    private BigDecimal aBigDecimal;

    @Option
    @Documentation("A float value.")
    private float aFloat;

    @Option
    @Documentation("A BigInteger value.")
    private BigInteger aBigInteger;

    @Option
    @Documentation("An Integer value.")
    private Integer anInteger;

    @Option
    @Documentation("A File value.")
    private File aFile;

    @Option
    @Documentation("A InetAddress value.")
    private InetAddress aInetAddress;

    @Option
    @Documentation("A URI value.")
    private URI aURI;

    @Option
    @Documentation("A URL value.")
    private URL aURL;

    @Option
    @Documentation("A LocalDateTime value.")
    private LocalDateTime aLocalDateTime;

    @Option
    @Documentation("A ZonedDateTime value.")
    private ZonedDateTime aZonedDateTime;

}
