/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.intellij.util;

import static com.intellij.openapi.roots.ModuleRootManager.getInstance;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;

public class FileUtil {

    public static VirtualFile findFileUnderRootInModule(final Module module, final String targetFileName) {
        final VirtualFile[] contentRoots = getInstance(module).getContentRoots();
        for (final VirtualFile contentRoot : contentRoots) {
            final VirtualFile childFile = findFileUnderRootInModule(contentRoot, targetFileName);
            if (childFile != null) {
                return childFile;
            }
        }
        return null;
    }

    public static VirtualFile findFileUnderRootInModule(final VirtualFile contentRoot, final String targetFileName) {
        return contentRoot.findChild(targetFileName);
    }
}
