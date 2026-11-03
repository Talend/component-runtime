/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.tcketl.web;

import static com.vaadin.flow.component.Unit.PIXELS;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.server.StreamResource;

import org.talend.sdk.component.tcketl.app.CompositionRoot;
import org.talend.sdk.component.tcketl.persistence.FileSystemPipelineRepository;
import org.talend.sdk.component.tcketl.persistence.PipelineRepository;

/**
 * New, upload and download of the pipeline. The pipeline lives in the browser tab's session; it only leaves it as a
 * {@code .tcketl.json} download, and only enters it as an upload that is read with the same validation as a file of
 * the desktop application. Nothing is stored on the server, so that tabs and users never see each other's work.
 */
final class ProjectBar extends HorizontalLayout {

    private static final long serialVersionUID = 1L;

    private static final long MAX_UPLOAD_BYTES = 5L * 1024 * 1024;

    private final transient CompositionRoot root;

    private final transient PipelineRepository repository = new FileSystemPipelineRepository();

    private final TextField name = new TextField();

    private final Span dirty = new Span();

    ProjectBar(final CompositionRoot root) {
        this.root = root;
        setAlignItems(FlexComponent.Alignment.CENTER);
        name.setAriaLabel("Pipeline name");
        name.setWidth(220, PIXELS);
        name.setValue(root.project().name().get());
        name.addValueChangeListener(event -> {
            if (event.isFromClient() && !event.getValue().isBlank()) {
                root.editor().rename(event.getValue().trim());
            }
        });
        final Button fresh = new Button("New", event -> root.project().newPipeline());
        fresh.addThemeVariants(ButtonVariant.LUMO_SMALL);
        final Anchor download = new Anchor(new StreamResource("pipeline" + PipelineRepository.FILE_EXTENSION,
                this::serialize), "Download");
        download.getElement().setAttribute("download", true);
        add(name, dirty, fresh, upload(), download);
        root.project().name().addListener((value, before, after) -> {
            if (!after.equals(name.getValue())) {
                name.setValue(after);
            }
        });
        root.project().dirty().addListener((value, before, after) -> dirty.setText(after ? "\u25CF unsaved" : ""));
    }

    private Upload upload() {
        final MemoryBuffer buffer = new MemoryBuffer();
        final Upload upload = new Upload(buffer);
        upload.setMaxFiles(1);
        upload.setMaxFileSize((int) MAX_UPLOAD_BYTES);
        upload.setDropAllowed(false);
        upload.setUploadButton(new Button("Open\u2026"));
        upload.addSucceededListener(event -> open(buffer));
        upload.addFileRejectedListener(event -> Notification.show(event.getErrorMessage(), 4000,
                Notification.Position.MIDDLE));
        return upload;
    }

    private void open(final MemoryBuffer buffer) {
        try {
            final Path temp = Files.createTempFile("tcketl-upload-", PipelineRepository.FILE_EXTENSION);
            try {
                Files.write(temp, buffer.getInputStream().readAllBytes());
                root.project().open(temp, () -> delete(temp), message -> {
                    delete(temp);
                    Notification.show(message, 5000, Notification.Position.MIDDLE);
                });
            } catch (final IOException | RuntimeException e) {
                delete(temp);
                throw e;
            }
        } catch (final IOException e) {
            Notification.show("Cannot read the file: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        }
    }

    private ByteArrayInputStream serialize() {
        try {
            final Path temp = Files.createTempFile("tcketl-download-", PipelineRepository.FILE_EXTENSION);
            try {
                repository.save(temp, root.editor().pipeline());
                return new ByteArrayInputStream(Files.readAllBytes(temp));
            } finally {
                delete(temp);
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void delete(final Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (final IOException e) {
            path.toFile().deleteOnExit();
        }
    }
}
