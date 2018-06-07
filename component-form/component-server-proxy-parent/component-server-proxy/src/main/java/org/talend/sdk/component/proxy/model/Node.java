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
package org.talend.sdk.component.proxy.model;

import java.util.Collection;

import io.swagger.annotations.ApiModelProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Node {

    @ApiModelProperty("The identifier of this configuration/node.")
    private String id;

    @ApiModelProperty("The display name of this configuration.")
    private String label;

    @ApiModelProperty("The identifier of the family of this configuration.")
    private String familyId;

    @ApiModelProperty("The display name of the family of this configuration.")
    private String familyLabel;

    @ApiModelProperty("The icon of this configuration. If you use an existing bundle (@talend/ui/icon), ensure it "
            + "is present by default and if not do a request using the family on the related endpoint.")
    private String icon;

    @ApiModelProperty("The list of configuration reusing this one as a reference (can be created \"next\").")
    private Collection<String> children;

    @ApiModelProperty("The version of this configuration for the migration management.")
    private Integer version;

    @ApiModelProperty("The technical name of this node (it is human readable but not i18n friendly), useful for debug purposes.")
    private String name;
}
