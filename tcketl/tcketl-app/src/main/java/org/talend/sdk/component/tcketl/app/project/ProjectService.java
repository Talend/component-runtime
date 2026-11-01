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
package org.talend.sdk.component.tcketl.app.project;

import java.lang.System.Logger;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.function.Consumer;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;

import org.talend.sdk.component.tcketl.app.async.AsyncRunner;
import org.talend.sdk.component.tcketl.domain.pipeline.Pipeline;
import org.talend.sdk.component.tcketl.domain.pipeline.PipelineEditor;
import org.talend.sdk.component.tcketl.domain.pipeline.PipelineEvent;
import org.talend.sdk.component.tcketl.persistence.FileFormatException;
import org.talend.sdk.component.tcketl.persistence.PipelineRepository;

/**
 * The pipeline the user works on and the file it comes from: it opens and saves it without blocking the UI, and tells
 * whether it holds changes that were not saved yet. That is derived from the revision of the editor, so a change
 * that is undone by hand still counts, and a change made while a save is running is not lost from the flag.
 *
 * <p>
 * Everything MUST be used on the UI thread, which is also the thread the outcomes are delivered on.
 */
public final class ProjectService {

    private static final Logger LOGGER = System.getLogger(ProjectService.class.getName());

    private static final String TITLE_SUFFIX = " - tcketl";

    private final PipelineEditor editor;

    private final PipelineRepository repository;

    private final AsyncRunner async;

    private final ReadOnlyObjectWrapper<Path> file = new ReadOnlyObjectWrapper<>();

    private final ReadOnlyBooleanWrapper dirty = new ReadOnlyBooleanWrapper();

    private final ReadOnlyBooleanWrapper busy = new ReadOnlyBooleanWrapper();

    private final ReadOnlyStringWrapper name;

    private final StringBinding title;

    private long savedRevision;

    public ProjectService(final PipelineEditor editor, final PipelineRepository repository,
            final AsyncRunner async) {
        this.editor = editor;
        this.repository = repository;
        this.async = async;
        this.name = new ReadOnlyStringWrapper(editor.pipeline().name());
        this.title = Bindings
                .createStringBinding(() -> name.get() + (dirty.get() ? " *" : "") + TITLE_SUFFIX, name, dirty);
        this.savedRevision = editor.revision();
        editor.addListener(this::onEvent);
    }

    /** Starts again from an empty pipeline that is not bound to any file. */
    public void newPipeline() {
        editor.load(Pipeline.empty());
        file.set(null);
        markSaved(editor.revision());
    }

    /**
     * Replaces the pipeline by the one of {@code path}. Nothing changes when the file cannot be read.
     *
     * @param onOpened run once the pipeline is loaded
     * @param onFailed receives a sentence that tells the user why it could not be
     */
    public void open(final Path path, final Runnable onOpened, final Consumer<String> onFailed) {
        if (refuseWhenBusy(onFailed)) {
            return;
        }
        busy.set(true);
        async.<Pipeline> submit(() -> repository.load(path), pipeline -> {
            busy.set(false);
            editor.load(pipeline);
            file.set(path);
            markSaved(editor.revision());
            onOpened.run();
        }, failure -> {
            busy.set(false);
            LOGGER.log(Logger.Level.WARNING, "Cannot open " + path + ": " + failure);
            onFailed.accept("Cannot open " + path.getFileName() + ": " + describe(failure));
        });
    }

    /**
     * Writes the pipeline to the file it comes from.
     *
     * @throws IllegalStateException when it does not come from a file, see {@link #saveAs}
     */
    public void save(final Runnable onSaved, final Consumer<String> onFailed) {
        final Path current = file.get();
        if (current == null) {
            throw new IllegalStateException("The pipeline is not bound to a file");
        }
        write(current, onSaved, onFailed);
    }

    /**
     * Writes the pipeline to {@code path}, which becomes the file it comes from. A pipeline that still has the
     * default name is named after the file.
     */
    public void saveAs(final Path path, final Runnable onSaved, final Consumer<String> onFailed) {
        if (Pipeline.DEFAULT_NAME.equals(editor.pipeline().name())) {
            final String base = baseName(path);
            if (!base.isBlank()) {
                editor.rename(base);
            }
        }
        write(path, onSaved, onFailed);
    }

    /** The file the pipeline comes from, {@code null} for a pipeline that was never saved. */
    public ReadOnlyObjectProperty<Path> file() {
        return file.getReadOnlyProperty();
    }

    /** Whether the pipeline holds changes that are not in its file. */
    public ReadOnlyBooleanProperty dirty() {
        return dirty.getReadOnlyProperty();
    }

    /** Whether a file is being read or written. */
    public ReadOnlyBooleanProperty busy() {
        return busy.getReadOnlyProperty();
    }

    /** The name of the pipeline. */
    public ReadOnlyStringProperty name() {
        return name.getReadOnlyProperty();
    }

    /** What the window is titled: the name, a star while there are unsaved changes, and the application. */
    public StringBinding title() {
        return title;
    }

    /** The name a file proposes for a pipeline: its file name without {@code .tcketl.json} or other extension. */
    static String baseName(final Path path) {
        final Path fileName = path.getFileName();
        if (fileName == null) {
            return "";
        }
        final String text = fileName.toString();
        final String lower = text.toLowerCase(Locale.ROOT);
        if (lower.endsWith(PipelineRepository.FILE_EXTENSION)) {
            return text.substring(0, text.length() - PipelineRepository.FILE_EXTENSION.length());
        }
        final int dot = text.lastIndexOf('.');
        return dot > 0 ? text.substring(0, dot) : text;
    }

    private void write(final Path target, final Runnable onSaved, final Consumer<String> onFailed) {
        if (refuseWhenBusy(onFailed)) {
            return;
        }
        // what is written is what the pipeline is now: changes made while the file is written stay unsaved
        final Pipeline snapshot = editor.pipeline();
        final long revision = editor.revision();
        busy.set(true);
        async.<Path> submit(() -> {
            repository.save(target, snapshot);
            return target;
        }, saved -> {
            busy.set(false);
            file.set(saved);
            markSaved(revision);
            onSaved.run();
        }, failure -> {
            busy.set(false);
            LOGGER.log(Logger.Level.WARNING, "Cannot save " + target + ": " + failure);
            onFailed.accept("Cannot save " + target.getFileName() + ": " + describe(failure));
        });
    }

    private boolean refuseWhenBusy(final Consumer<String> onFailed) {
        if (busy.get()) {
            onFailed.accept("Another file operation is still running");
        }
        return busy.get();
    }

    private void markSaved(final long revision) {
        savedRevision = revision;
        refreshDirty();
    }

    private void onEvent(final PipelineEvent event) {
        if (event instanceof PipelineEvent.Renamed renamed) {
            name.set(renamed.name());
        } else if (event instanceof PipelineEvent.Reset reset) {
            name.set(reset.pipeline().name());
        }
        refreshDirty();
    }

    private void refreshDirty() {
        dirty.set(editor.revision() != savedRevision);
    }

    private static String describe(final Throwable failure) {
        if (failure instanceof FileFormatException) {
            return failure.getMessage();
        }
        if (failure instanceof NoSuchFileException) {
            return "the file does not exist";
        }
        if (failure instanceof AccessDeniedException) {
            return "access is denied";
        }
        final String message = failure.getMessage();
        return message == null || message.isBlank() ? failure.getClass().getSimpleName() : message;
    }
}
