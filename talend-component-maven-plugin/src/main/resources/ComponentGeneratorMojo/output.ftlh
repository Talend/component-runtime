
//
// This class was generated using Talend Component Generator Maven plugin
//
<#if configuration.packageName?has_content>
package ${configuration.packageName};
</#if>

import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.talend.component.api.component.Icon;
import org.talend.component.api.component.Version;
import org.talend.component.api.processor.AfterGroup;
import org.talend.component.api.processor.BeforeGroup;
import org.talend.component.api.processor.ElementListener;
import org.talend.component.api.processor.Processor;
import org.talend.component.api.processor.data.ObjectMap;

@Version
@Icon(Icon.IconType.FILTER)
@Processor(family = "${configuration.family}", name = "${configuration.className}${configuration.template}")
public class ${configuration.className}${configuration.template} implements Serializable {
    @PostConstruct
    public void init() {
        // TODO: if needed open a connection here
    }

    @BeforeGroup
    public void beforeGroup() {
        // TODO: execute some code before a chunk
    }

    @ElementListener
    public void onNext(final ObjectMap data) {
        // TODO: store data
    }

    @AfterGroup
    public void afterGroup() {
        // TODO: execute some code after a chunk (batch)
    }

    @PreDestroy
    public void destroy() {
        // TODO: if needed close the opened connection here
    }
}
