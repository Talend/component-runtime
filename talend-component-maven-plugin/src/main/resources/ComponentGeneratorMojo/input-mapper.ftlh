<#--
  Copyright (C) 2006-2017 Talend Inc. - www.talend.com
   Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
//
// This class was generated using Talend Component Generator Maven plugin
//
<#if configuration.packageName?has_content>
package ${configuration.packageName};
</#if>

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

import org.talend.sdk.component.api.component.Icon;
import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.input.Assessor;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.PartitionSize;
import org.talend.sdk.component.api.input.PartitionMapper;
import org.talend.sdk.component.api.input.Split;

@Version
@Icon(Icon.IconType.SEARCH)
@PartitionMapper(family = "${configuration.family}", name = "${configuration.className}${configuration.template}")
public class ${configuration.className}${configuration.template} implements Serializable {
    @Assessor
    public long estimateSize() {
        // TODO: try to estimate the dataset size
        return 0;
    }

    @Split
    public List<${configuration.className}${configuration.template}> split(@PartitionSize final long sizeByPartition) {
        final List<${configuration.className}${configuration.template}> partitions = new ArrayList<>();
        // TODO: split in a clever way the overall dataset recreating ${configuration.className}${configuration.template} instances
        partitions.add(this); // this is a workaround to ensure the component works but defeats the goal
        return partitions;
    }

    @Emitter
    public ${configuration.className}Emitter createInput() {
        // create the actual "reader", you can pass configuration here
        return new ${configuration.className}Emitter();
    }
}
