package {{package}};

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.talend.component.api.component.Icon;
import org.talend.component.api.component.Version;
import org.talend.component.api.configuration.Option;
import org.talend.component.api.processor.AfterGroup;
import org.talend.component.api.processor.BeforeGroup;
import org.talend.component.api.processor.Processor;{{#inputs.length}}
import org.talend.component.api.processor.Input;{{/inputs.length}}{{#outputs.length}}
import org.talend.component.api.processor.Output;
import org.talend.component.api.processor.OutputEmitter;{{/outputs.length}}{{#generic}}
import org.talend.component.api.processor.data.ObjectMap;{{/generic}}

import {{servicePackage}}.{{serviceName}};

@Version(1) // default version is 1, if some configuration changes happen between 2 versions you can add a migrationHandler
@Icon({{icon}}) // you can use a custom one using @Icon(value=CUSTOM, custom="filename") and adding icons/filename_icon32.png in resources
@Processor(name = "{{name}}")
public class {{className}} implements Serializable {
    private final {{configurationName}} configuration;
    private final {{serviceName}} service;

    public {{className}}(@Option("configuration") final {{configurationName}} configuration,
                         final {{serviceName}} service) {
        this.configuration = configuration;
        this.service = service;
    }

    @PostConstruct
    public void init() {
        // this method will be executed once for the whole component execution,
        // this is where you can establish a connection for instance
        // Note: if you don't need it you can delete it
    }

    @BeforeGroup
    public void beforeGroup() {
        // if the environment supports chunking this method is called at the beginning if a chunk
        // it can be used to start a local transaction specific to the backend you use
        // Note: if you don't need it you can delete it
    }

    @Producer
    public void onNext({{#inputs}}
        @Input("{{name}}") final {{type}} {{javaName}}Input{{^-last}},{{/-last}}{{/inputs}}{{#outputs}}
        @Output("{{name}}") final OutputEmitter<{{type}}> {{javaName}}Output{{^-last}},{{/-last}}{{/outputs}}) {
        // this is the method allowing you to handle the input(s) and emit the output(s)
        // after some custom logic you put here, to send a value to next element you can use an
        // output parameter and call emit(value).
    }

    @AfterGroup
    public void afterGroup() {
        // symmetric method of the beforeGroup() executed after the chunk processing
        // Note: if you don't need it you can delete it
    }

    @PreDestroy
    public void release() {
        // this is the symmetric method of the init() one,
        // release potential connections you created or data you cached
        // Note: if you don't need it you can delete it
    }
}
