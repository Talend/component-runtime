package org.talend.sdk.component.intellij.module;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProjectCreationRequest {
    private String action;
    private String requestMethod;
    private String project;
}
