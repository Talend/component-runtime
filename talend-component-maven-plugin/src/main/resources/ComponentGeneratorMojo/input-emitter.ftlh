//
// This class was generated using Talend Component Generator Maven plugin
//
<#if configuration.packageName?has_content>
package ${configuration.packageName};
</#if>

import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.talend.component.api.input.Producer;

public class ${configuration.className}${configuration.template} implements Serializable {
    @PostConstruct
    public void init() {
        // TODO: open any connection you can need for the life of this component
    }

    @Producer
    public ${configuration.className}Record next() {
        // TODO: use the opened connection to read next record (new ${configuration.className}Record())
        //       or return null when you are done and there is no more data to read
        return null;
    }

    @PreDestroy
    public void destroy() {
        // TODO: close any resource created in the @PostConstruct method
    }

    public static class ${configuration.className}Record implements Serializable {
        // TODO: add data

        /**
          * if you don't know your schema before the execution you can use to store the values.
          *
          *  @ObjectMap.Any
          *  private Map<String, Object> values;
          *
          */
    }
}
