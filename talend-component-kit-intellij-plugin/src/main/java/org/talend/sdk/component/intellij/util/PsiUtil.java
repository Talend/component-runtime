package org.talend.sdk.component.intellij.util;

import static com.intellij.codeInsight.completion.CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED;
import static com.intellij.openapi.module.ModuleUtilCore.findModuleForPsiElement;

import javax.annotation.Nullable;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiElement;

import org.jetbrains.annotations.NotNull;

public class PsiUtil {

    @Nullable
    public static Module findModule(@NotNull PsiElement element) {
        return findModuleForPsiElement(element);
    }

    public static String truncateIdeaDummyIdentifier(@NotNull PsiElement element) {
        return truncateIdeaDummyIdentifier(element.getText());
    }

    public static String truncateIdeaDummyIdentifier(String text) {
        return text.replace(DUMMY_IDENTIFIER_TRIMMED, "");
    }
}
