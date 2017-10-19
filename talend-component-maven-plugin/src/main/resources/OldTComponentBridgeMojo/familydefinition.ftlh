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
