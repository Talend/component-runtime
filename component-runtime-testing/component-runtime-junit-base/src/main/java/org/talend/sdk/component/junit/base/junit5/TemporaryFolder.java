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
package org.talend.sdk.component.junit.base.junit5;

import static java.util.Optional.ofNullable;

import java.io.File;
import java.io.IOException;
import java.util.stream.Stream;

// backport the junit4 rule of the same name to junit 5
@Deprecated // part of jupiter 5.4 now
public class TemporaryFolder {

    private File folder;

    public void create() throws IOException {
        folder = createTemporaryFolderIn(null);
    }

    public File newFile(final String fileName) throws IOException {
        final File file = new File(getRoot(), fileName);
        if (!file.createNewFile()) {
            throw new IOException("a file with the name \'" + fileName + "\' already exists in the test folder");
        }
        return file;
    }

    public File newFile() throws IOException {
        return File.createTempFile("talendjunit", null, getRoot());
    }

    public File newFolder(final String... folderNames) throws IOException {
        File file = getRoot();
        for (int i = 0; i < folderNames.length; i++) {
            String folderName = folderNames[i];
            validateFolderName(folderName);
            file = new File(file, folderName);
            if (!file.mkdir() && isLastElementInArray(i, folderNames)) {
                throw new IOException("a folder with the name \'" + folderName + "\' already exists");
            }
        }
        return file;
    }

    private void validateFolderName(final String folderName) throws IOException {
        File tempFile = new File(folderName);
        if (tempFile.getParent() != null) {
            String errorMsg = "Folder name cannot consist of multiple path components separated by a file separator."
                    + " Please use newFolder('MyParentFolder','MyFolder') to create hierarchies of folders";
            throw new IOException(errorMsg);
        }
    }

    private boolean isLastElementInArray(final int index, final String[] array) {
        return index == array.length - 1;
    }

    public File newFolder() throws IOException {
        return createTemporaryFolderIn(getRoot());
    }

    private File createTemporaryFolderIn(final File parentFolder) throws IOException {
        final File createdFolder = File.createTempFile("junit", "", parentFolder);
        createdFolder.delete();
        createdFolder.mkdir();
        return createdFolder;
    }

    public File getRoot() {
        if (folder == null) {
            throw new IllegalStateException("the temporary folder has not yet been created");
        }
        return folder;
    }

    public void delete() {
        ofNullable(folder).ifPresent(this::recursiveDelete);
    }

    private void recursiveDelete(final File file) {
        final File[] files = file.listFiles();
        if (files != null) {
            Stream.of(files).forEach(this::recursiveDelete);
        }
        file.delete();
    }
}
