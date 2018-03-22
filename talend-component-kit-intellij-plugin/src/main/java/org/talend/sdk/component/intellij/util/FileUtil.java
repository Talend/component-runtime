package org.talend.sdk.component.intellij.util;

import static com.intellij.openapi.roots.ModuleRootManager.getInstance;

import javax.annotation.Nullable;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;

public class FileUtil {

    @Nullable
    public static VirtualFile findFileUnderRootInModule(Module module, String targetFileName) {
        VirtualFile[] contentRoots = getInstance(module).getContentRoots();
        for (VirtualFile contentRoot : contentRoots) {
            VirtualFile childFile = findFileUnderRootInModule(contentRoot, targetFileName);
            if (childFile != null) {
                return childFile;
            }
        }
        return null;
    }

    @Nullable
    public static VirtualFile findFileUnderRootInModule(@NotNull VirtualFile contentRoot, String targetFileName) {
        VirtualFile childFile = contentRoot.findChild(targetFileName);
        if (childFile != null) {
            return childFile;
        }
        return null;
    }

}
