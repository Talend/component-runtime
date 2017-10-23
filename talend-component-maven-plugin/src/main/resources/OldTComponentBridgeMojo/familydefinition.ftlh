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
<#if definition.packageName?has_content>
package ${definition.packageName};
</#if>

import org.osgi.service.component.annotations.Component;
import org.talend.components.api.AbstractComponentFamilyDefinition;
import org.talend.components.api.ComponentInstaller;
import org.talend.components.api.Constants;

<#list definitions as def><#if definition.packageName != def.packageName>
import ${def.packageName}.${def.className};</#if></#list>

@Component(name = Constants.COMPONENT_INSTALLER_PREFIX + "${definition.name}", service = ComponentInstaller.class)
public class ${definition.className} extends AbstractComponentFamilyDefinition implements ComponentInstaller {
    public ${definition.className}() {
        super(
            "${definition.name}",<#list definitions as def>
            new ${def.className}()<#sep>,</#sep></#list>);
    }

    @Override
    public void install(final ComponentFrameworkContext componentFrameworkContext) {
        componentFrameworkContext.registerComponentFamilyDefinition(this);
    }
}
