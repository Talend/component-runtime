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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.jetbrains.annotations.NotNull;
import org.talend.sdk.component.intellij.Configuration;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.RefreshQueue;

public class ProjectDownloader {
    private final TalendModuleBuilder builder;
    private final ProjectCreationRequest request;

    public ProjectDownloader(final TalendModuleBuilder builder, final ProjectCreationRequest request) {
        this.builder = builder;
        this.request = request;
    }

    @NotNull
    private String userAgent() {
        return ApplicationNamesInfo.getInstance()
                                   .getFullProductName() + "/" + ApplicationInfo.getInstance()
                                                                                .getFullVersion();
    }

    public void download(ProgressIndicator indicator) throws IOException {
        indicator.setText("Downloading files ...");
        HttpURLConnection urlConnection = null;
        URL url = new URL(Configuration.getStarterHost() + request.getAction());
        urlConnection = HttpURLConnection.class.cast(url.openConnection());
        urlConnection.setRequestMethod(request.getRequestMethod());
        urlConnection.setRequestProperty("Accept", "application/zip");
        urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        urlConnection.setRequestProperty("User-Agent", userAgent());
        urlConnection.setDoOutput(true);
        try (final BufferedOutputStream outputStream = new BufferedOutputStream(urlConnection.getOutputStream())) {
            outputStream.write(("project=" + request.getProject()).getBytes("utf-8"));
            outputStream.flush();
        }
        final int responseCode = urlConnection.getResponseCode();
        if (responseCode == 200) {
            final String contentType = urlConnection.getHeaderField("content-type");
            if (!"application/zip".equals(contentType)) {
                throw new IOException("Invalid project format from starter server.");
            }
            try (final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                copy(urlConnection.getInputStream(), out);
                File targetExtractionDir = new File(requireNonNull(builder.getContentEntryPath()));
                unzip(new ByteArrayInputStream(out.toByteArray()), targetExtractionDir, true, indicator);
                indicator.setText("Please wait ...");
                markAsExecutable(targetExtractionDir, "gradlew");
                markAsExecutable(targetExtractionDir, "gradlew.bat");
                markAsExecutable(targetExtractionDir, "mvnw");
                markAsExecutable(targetExtractionDir, "mvnw.cmd");
                VirtualFile targetFile = LocalFileSystem.getInstance()
                                                        .refreshAndFindFileByIoFile(targetExtractionDir);
                RefreshQueue.getInstance()
                            .refresh(false, true, null, targetFile);
            }
        } else {
            byte[] error = slurp(urlConnection.getErrorStream());
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
                              ProgressIndicator indicator)
            throws IOException {
        try {
            final ZipInputStream in = new ZipInputStream(read);
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                String path = entry.getName();
                if (noparent) {
                    path = path.replaceFirst("^[^/]+/", "");
                }
                final File file = new File(destination, path);

                if (entry.isDirectory()) {
                    indicator.setText("Creating directory " + file.getName());
                    file.mkdirs();
                    continue;
                }

                file.getParentFile()
                    .mkdirs();
                indicator.setText("Creating file " + file.getName());
                Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);

                final long lastModified = entry.getTime();
                if (lastModified > 0) {
                    file.setLastModified(lastModified);
                }
            }
            in.close();

        } catch (final IOException e) {
            throw new IOException("Unable to unzip " + read, e);
        }
    }

    private static void markAsExecutable(File containingDir, String relativePath) {
        File toFix = new File(containingDir, relativePath);
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
