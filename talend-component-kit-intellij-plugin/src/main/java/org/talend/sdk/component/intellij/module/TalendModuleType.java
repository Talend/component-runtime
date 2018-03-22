package org.talend.sdk.component.intellij.module;

import static org.talend.sdk.component.intellij.Configuration.getMessage;

import javax.swing.Icon;

import org.jetbrains.annotations.NotNull;
import org.talend.sdk.component.intellij.Icons;
import com.intellij.openapi.module.ModuleType;

public class TalendModuleType extends ModuleType<TalendModuleBuilder> {

    public TalendModuleType() {
        super("TALEND_COMPONENT_MODULE");
    }

    @NotNull
    @Override
    public TalendModuleBuilder createModuleBuilder() {
        return new TalendModuleBuilder(this);
    }

    @NotNull
    @Override
    public String getName() {
        return getMessage("plugin.name");
    }


    @NotNull
    @Override
    public String getDescription() {
        return getMessage("plugin.description");
    }

    @Override
    public Icon getNodeIcon(final boolean b) {
        return Icons.Tacokit;
    }

    @Override
    public Icon getIcon() {
        return Icons.Tacokit;
    }
}
