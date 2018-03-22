package org.talend.sdk.component.intellij.completion.properties;

import static java.util.stream.Collectors.toList;
import static org.talend.sdk.component.intellij.util.PsiUtil.findModule;
import static org.talend.sdk.component.intellij.util.PsiUtil.truncateIdeaDummyIdentifier;

import java.util.List;
import java.util.Objects;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.psi.impl.PropertiesFileImpl;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;

import org.jetbrains.annotations.NotNull;
import org.talend.sdk.component.intellij.service.SuggestionService;

public class PropertiesCompletionProvider extends CompletionProvider<CompletionParameters> {

    @Override
    protected void addCompletions(@NotNull final CompletionParameters completionParameters,
            final ProcessingContext processingContext, @NotNull final CompletionResultSet resultSet) {

        PsiElement element = completionParameters.getPosition();
        if (element instanceof PsiComment) {
            return; // ignore comment
        }

        Project project = element.getProject();
        Module module = findModule(element);
        SuggestionService service = ServiceManager.getService(project, SuggestionService.class);
        if ((module == null || !service.isSupported(completionParameters))) { //limit suggestion to Messages
            return;
        }

        final List<String> containerElements = PropertiesFileImpl.class.cast(element.getContainingFile())
                .getProperties().stream()
                .filter(p -> !Objects.equals(p.getKey(), element.getText()))
                .map(IProperty::getKey)
                .collect(toList());

        service.computeSuggestions(project, module,
                getPropertiesPackage(module, completionParameters),
                containerElements,
                truncateIdeaDummyIdentifier(element))
                .forEach(resultSet::addElement);
    }

    private String getPropertiesPackage(final Module module, final CompletionParameters completionParameters) {
        final String moduleFilePath = module.getModuleFilePath();
        final String moduleName = module.getName();
        final String moduleDirPath = moduleFilePath.replace(moduleName + ".iml", "");
        final String propPath = completionParameters.getOriginalFile().getVirtualFile().getPath();
        return propPath.replace(moduleDirPath, "")
                .replace("/", ".").replace("\\", ".")
                .replace("src.main.resources.", "")
                .replace("." + completionParameters.getOriginalFile().getName(), "");
    }
}
