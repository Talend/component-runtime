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

import static java.util.Objects.requireNonNull;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.RefreshQueue;

import org.talend.sdk.component.intellij.Configuration;

public class ProjectDownloader {

    private final TalendModuleBuilder builder;

    private final ProjectCreationRequest request;

    public ProjectDownloader(final TalendModuleBuilder builder, final ProjectCreationRequest request) {
        this.builder = builder;
        this.request = request;
    }

    private String userAgent() {
        return ApplicationNamesInfo.getInstance().getFullProductName() + "/"
                + ApplicationInfo.getInstance().getFullVersion();
    }

    public void download(final ProgressIndicator indicator) throws IOException {
        indicator.setText("Downloading files ...");
        final URL url = new URL(Configuration.getStarterHost() + request.getAction());
        final HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod(request.getRequestMethod());
        urlConnection.setRequestProperty("Accept", "application/zip");
        urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        urlConnection.setRequestProperty("User-Agent", userAgent());
        urlConnection.setDoOutput(true);
        try (final BufferedOutputStream outputStream = new BufferedOutputStream(urlConnection.getOutputStream())) {
            outputStream
                    .write(("project=" + URLEncoder.encode(request.getProject(), StandardCharsets.UTF_8))
                            .getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        }
        final int responseCode = urlConnection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            final String contentType = urlConnection.getHeaderField("content-type");
            if (!"application/zip".equals(contentType)) {
                throw new IOException("Invalid project format from starter server, content-type='" + contentType + "'");
            }
            try (final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                copy(urlConnection.getInputStream(), out);
                final File targetExtractionDir = new File(requireNonNull(builder.getContentEntryPath()));
                unzip(new ByteArrayInputStream(out.toByteArray()), targetExtractionDir, true, indicator);
                indicator.setText("Please wait ...");
                markAsExecutable(targetExtractionDir, "gradlew");
                markAsExecutable(targetExtractionDir, "gradlew.bat");
                markAsExecutable(targetExtractionDir, "mvnw");
                markAsExecutable(targetExtractionDir, "mvnw.cmd");
                final VirtualFile targetFile =
                        LocalFileSystem.getInstance().refreshAndFindFileByIoFile(targetExtractionDir);
                RefreshQueue.getInstance().refresh(false, true, null, targetFile);
            }
        } else {
            final byte[] error = slurp(urlConnection.getErrorStream());
            throw new IOException(new String(error, StandardCharsets.UTF_8));
        }
    }

    private static void copy(final InputStream from, final OutputStream to) throws IOException {
        final byte[] buffer = new byte[1024];
        int length = 0;
        while ((length = from.read(buffer)) != -1) {
            to.write(buffer, 0, length);
        }
        to.flush();
    }

    private static void unzip(final InputStream read, final File destination, final boolean noparent,
            final ProgressIndicator indicator) throws IOException {
        try (final ZipInputStream in = new ZipInputStream(read)) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                String path = entry.getName();
                if (noparent) {
                    path = path.replaceFirst("^[^/]+/", "");
                }
                final File file = new File(destination, path);
                if (!file.getCanonicalPath().startsWith(destination.getCanonicalPath() + File.separator)) {
                    throw new IOException("The output file is not contained in the destination directory");
                }

                if (entry.isDirectory()) {
                    indicator.setText("Creating directory " + file.getName());
                    file.mkdirs();
                    continue;
                }

                file.getParentFile().mkdirs();
                indicator.setText("Creating file " + file.getName());
                Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);

                final long lastModified = entry.getTime();
                if (lastModified > 0) {
                    file.setLastModified(lastModified);
                }
            }
        } catch (final IOException e) {
            throw new IOException("Unable to unzip " + read, e);
        }
    }

    private static void markAsExecutable(final File containingDir, final String relativePath) {
        final File toFix = new File(containingDir, relativePath);
        if (toFix.exists()) {
            toFix.setExecutable(true, false);
        }
    }

    private static byte[] slurp(final InputStream responseStream) {
        final byte[] buffer = new byte[8192];
        final ByteArrayOutputStream responseBuffer = new ByteArrayOutputStream(buffer.length);
        try (final InputStream inputStream = responseStream) {
            int count;
            while ((count = inputStream.read(buffer)) >= 0) {
                responseBuffer.write(buffer, 0, count);
            }
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
        return responseBuffer.toByteArray();
    }
}
