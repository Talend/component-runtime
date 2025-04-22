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
package org.talend.sdk.component.intellij.module;

import static com.intellij.openapi.application.ApplicationManager.getApplication;
import static com.intellij.openapi.module.StdModuleTypes.JAVA;
import static java.util.Collections.singletonList;
import static org.talend.sdk.component.intellij.Configuration.getMessage;
import static org.talend.sdk.component.intellij.util.FileUtil.findFileUnderRootInModule;

import java.io.IOException;
import java.util.Base64;

import javax.swing.Icon;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.intellij.ide.util.newProjectWizard.AddModuleWizard;
import com.intellij.ide.util.projectWizard.JavaModuleBuilder;
import com.intellij.ide.util.projectWizard.ModuleNameLocationSettings;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.SettingsStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ModuleWithNameAlreadyExists;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.ui.ex.MessagesEx;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.vfs.VirtualFile;

import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.plugins.gradle.service.project.wizard.GradleProjectImportBuilder;
import org.jetbrains.plugins.gradle.service.project.wizard.GradleProjectImportProvider;
import org.talend.sdk.component.intellij.module.step.StarterStep;
import org.talend.sdk.component.intellij.module.step.WelcomeStep;

public class TalendModuleBuilder extends JavaModuleBuilder {

    private final ModuleType<TalendModuleBuilder> moduleType;

    private ProjectCreationRequest request = null;

    private final Gson gson = new Gson();

    private JsonObject jsonProject;

    public TalendModuleBuilder(final ModuleType<TalendModuleBuilder> moduleType) {
        this.moduleType = moduleType;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public ModuleWizardStep[] createWizardSteps(@NotNull final WizardContext wizardContext,
                                                @NotNull final ModulesProvider modulesProvider) {

        return new ModuleWizardStep[]{new StarterStep(this)};
    }

    @Override
    public ModuleWizardStep getCustomOptionsStep(final WizardContext context, final Disposable parentDisposable) {
        return new WelcomeStep(context, parentDisposable);
    }

    @Override
    public ModuleType<JavaModuleBuilder> getModuleType() {
        return JAVA;
    }

    @Override
    public Icon getNodeIcon() {
        return moduleType.getIcon();
    }

    @Override
    public String getPresentableName() {
        return moduleType.getName();
    }

    @Override
    public String getParentGroup() {
        return "Build Tools";
    }

    @Override
    public String getBuilderId() {
        return "talend/component";
    }

    @Override
    public String getDescription() {
        return moduleType.getDescription();
    }

    @NotNull
    @Override
    public Module createModule(@NotNull final ModifiableModuleModel moduleModel) throws InvalidDataException, IOException,
            ModuleWithNameAlreadyExists, ConfigurationException, JDOMException {
        final Module module = super.createModule(moduleModel);
        getApplication().invokeLater(() -> {
            ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
                final ProjectDownloader downloader = new ProjectDownloader(this, request);
                try {
                    downloader.download(ProgressManager.getInstance().getProgressIndicator());
                } catch (IOException e) {
                    getApplication()
                            .invokeLater(() -> MessagesEx
                                    .showErrorDialog(e.getMessage(), getMessage("download.project.file.error")));
                }
            }, getMessage("download.project.file"), true, null);

            final Project moduleProject = module.getProject();
            switch (jsonProject.get("buildType").getAsString()) {
                case "Maven":
                    final VirtualFile pomFile = findFileUnderRootInModule(module, "pom.xml");
                    if (pomFile != null) {
                        final MavenProjectsManager mavenProjectsManager =
                                MavenProjectsManager.getInstance(moduleProject);
                        mavenProjectsManager.addManagedFiles(singletonList(pomFile));
                    }
                    break;
                case "Gradle":
                    final VirtualFile gradleFile = findFileUnderRootInModule(module, "build.gradle");
                    if (gradleFile != null) {
                        final ProjectDataManager projectDataManager = ApplicationManager.getApplication().getService(ProjectDataManager.class);

                        // todo: move to JavaGradleProjectImportBuilder
                        final GradleProjectImportBuilder importBuilder =
                                new GradleProjectImportBuilder(projectDataManager);
                        final GradleProjectImportProvider importProvider =
                                new GradleProjectImportProvider(importBuilder);
                        final AddModuleWizard addModuleWizard =
                                new AddModuleWizard(moduleProject, gradleFile.getPath(), importProvider);
                        if (addModuleWizard.getStepCount() == 0 && addModuleWizard.showAndGet()) {
                            // user chose to import via the gradle import prompt
                            importBuilder.commit(moduleProject, null, null);
                        }
                    }
                    break;
                default:
                    break;
            }

        }, ModalityState.current());
        return module;
    }

    @Override
    public ModuleWizardStep modifySettingsStep(@NotNull final SettingsStep settingsStep) {
        final String projectJsonString = new String(Base64.getUrlDecoder().decode(request.getProject()));
        jsonProject = gson.fromJson(projectJsonString, JsonObject.class);

        try {
            final Object moduleNameLocationSettings =
                    settingsStep.getClass().getMethod("getModuleNameLocationSettings").invoke(settingsStep);
            if (moduleNameLocationSettings != null) {
                moduleNameLocationSettings
                        .getClass()
                        .getMethod("setModuleName", String.class)
                        .invoke(moduleNameLocationSettings, jsonProject.get("artifact").getAsString());
            }
        } catch (final Error | Exception e) {
            try {
                final ModuleNameLocationSettings settings = settingsStep.getModuleNameLocationSettings();
                if (settings != null) {
                    settings.setModuleName(jsonProject.get("artifact").getAsString());
                }
            } catch (final RuntimeException ex) {
                final IllegalStateException exception = new IllegalStateException(e);
                exception.addSuppressed(ex);
                throw exception;
            }
        }
        return super.modifySettingsStep(settingsStep);
    }

    public void updateQuery(final ProjectCreationRequest q) {
        this.request = q;
    }

}
