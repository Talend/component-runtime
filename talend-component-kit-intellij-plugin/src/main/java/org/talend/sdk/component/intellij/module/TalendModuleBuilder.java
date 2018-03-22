package org.talend.sdk.component.intellij.module;

import static com.intellij.openapi.application.ApplicationManager.getApplication;
import static com.intellij.openapi.components.ServiceManager.getService;
import static com.intellij.openapi.module.StdModuleTypes.JAVA;
import static java.util.Collections.singletonList;
import static org.talend.sdk.component.intellij.Configuration.getMessage;
import static org.talend.sdk.component.intellij.util.FileUtil.findFileUnderRootInModule;

import java.io.IOException;
import java.util.Base64;
import javax.swing.Icon;
import javax.swing.JTextField;

import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.plugins.gradle.service.project.wizard.GradleProjectImportBuilder;
import org.jetbrains.plugins.gradle.service.project.wizard.GradleProjectImportProvider;
import org.talend.sdk.component.intellij.module.step.StarterStep;
import org.talend.sdk.component.intellij.module.step.WelcomeStep;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.intellij.ide.util.newProjectWizard.AddModuleWizard;
import com.intellij.ide.util.projectWizard.JavaModuleBuilder;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.SettingsStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.Disposable;
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

public class TalendModuleBuilder extends JavaModuleBuilder {

    private final ModuleType moduleType;

    private ProjectCreationRequest request = null;

    private Gson gson = new Gson();

    private JsonObject jsonProject;


    public TalendModuleBuilder(final ModuleType moduleType) {
        this.moduleType = moduleType;
    }

    @Override
    public ModuleWizardStep[] createWizardSteps(
            @NotNull final WizardContext wizardContext, @NotNull final ModulesProvider modulesProvider) {

        return new ModuleWizardStep[]{new StarterStep(this)};
    }

    @Nullable
    @Override
    public ModuleWizardStep getCustomOptionsStep(WizardContext context, Disposable parentDisposable) {
        return new WelcomeStep(context, parentDisposable);
    }

    @Override
    public ModuleType getModuleType() {
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

    @Nullable
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
    public Module createModule(@NotNull ModifiableModuleModel moduleModel)
            throws InvalidDataException, IOException, ModuleWithNameAlreadyExists, JDOMException,
            ConfigurationException {
        Module module = super.createModule(moduleModel);
        getApplication().invokeLater(() -> {
            ProgressManager.getInstance()
                           .runProcessWithProgressSynchronously(() -> {
                               ProjectDownloader downloader = new ProjectDownloader(this, request);
                               try {
                                   downloader.download(ProgressManager.getInstance()
                                                                      .getProgressIndicator());
                               } catch (IOException e) {
                                   getApplication().invokeLater(() -> MessagesEx.showErrorDialog(e.getMessage(),
                                           getMessage("download.project.file.error")));
                               }
                           }, getMessage("download.project.file"), true, null);

            Project moduleProject = module.getProject();
            switch (jsonProject.get("buildType")
                               .getAsString()) {
                case "Maven":
                    VirtualFile pomFile = findFileUnderRootInModule(module, "pom.xml");
                    if (pomFile != null) {
                        MavenProjectsManager mavenProjectsManager = MavenProjectsManager.getInstance(moduleProject);
                        mavenProjectsManager.addManagedFiles(singletonList(pomFile));
                    }
                    break;
                case "Gradle":
                    VirtualFile gradleFile = findFileUnderRootInModule(module, "build.gradle");
                    if (gradleFile != null) {
                        ProjectDataManager projectDataManager = getService(ProjectDataManager.class);
                        GradleProjectImportBuilder importBuilder = new GradleProjectImportBuilder(projectDataManager);
                        GradleProjectImportProvider importProvider = new GradleProjectImportProvider(importBuilder);
                        AddModuleWizard addModuleWizard = new AddModuleWizard(moduleProject, gradleFile.getPath(),
                                importProvider);
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
        JTextField namedFile = settingsStep.getModuleNameField();
        if (namedFile != null) {
            String projectJsonString = new String(Base64.getDecoder()
                                                        .decode(request.getProject()));
            jsonProject = gson.fromJson(projectJsonString, JsonObject.class);
            namedFile.setText(jsonProject.get("artifact")
                                         .getAsString());
            namedFile.setEditable(false);
        }
        return super.modifySettingsStep(settingsStep);
    }

    public void updateQuery(ProjectCreationRequest q) {
        this.request = q;
    }

}
