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
package org.talend.sdk.component.tcketl.app.testing;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.AccessControlContext;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.input.DataFormat;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.util.Pair;

import javax.imageio.ImageIO;

import com.sun.javafx.scene.SceneHelper;
import com.sun.javafx.tk.TKClipboard;
import com.sun.javafx.tk.TKDropTargetListener;

/**
 * Runs code on the JavaFX toolkit, drives a scene with the pointer, the keyboard and drag and drop, and renders it to
 * PNG files. The scenes are never shown: the toolkit renders them off screen and takes the events through the same
 * code as the ones of a window, picking what is under the pointer as it does on the screen. So this works on a
 * machine whose desktop is locked or that has no display attached, which a screen capture does not.
 */
public final class FxHarness {

    /** The system property that turns the snapshot tests on: the directory where they write their pictures. */
    public static final String SNAPSHOTS = "tcketl.ui.snapshots";

    private static final long TOOLKIT_TIMEOUT_SECONDS = 30;

    private static final long POLL_MILLIS = 50;

    private static boolean started;

    private FxHarness() {
        // static helpers only
    }

    /** Where the snapshot tests write their pictures. */
    public static Path snapshotDirectory() {
        return Path.of(System.getProperty(SNAPSHOTS, "target/ui-snapshots"));
    }

    /** Starts the toolkit unless it already runs. */
    public static synchronized void start() throws InterruptedException {
        if (started) {
            return;
        }
        final CountDownLatch ready = new CountDownLatch(1);
        try {
            Platform.startup(ready::countDown);
        } catch (final IllegalStateException alreadyRunning) {
            ready.countDown();
        }
        if (!ready.await(TOOLKIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            throw new IllegalStateException("The JavaFX toolkit did not start");
        }
        Platform.setImplicitExit(false);
        started = true;
    }

    /** Runs {@code action} on the toolkit thread, waits for it and gives back what it returned or threw. */
    public static <T> T call(final Callable<T> action) throws Exception {
        final FutureTask<T> task = new FutureTask<>(action);
        Platform.runLater(task);
        try {
            return task.get(TOOLKIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (final ExecutionException e) {
            if (e.getCause() instanceof Exception) {
                throw (Exception) e.getCause();
            }
            throw e;
        }
    }

    /** Waits until everything that was already queued on the toolkit thread has run. */
    public static void settle() throws Exception {
        call(() -> null);
    }

    /** Evaluates {@code condition} on the toolkit thread until it holds, and tells whether it did in time. */
    public static boolean await(final Duration timeout, final BooleanSupplier condition) throws Exception {
        final long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (call(condition::getAsBoolean)) {
                return true;
            }
            Thread.sleep(POLL_MILLIS);
        }
        return call(condition::getAsBoolean);
    }

    /**
     * Like {@link #await(Duration, BooleanSupplier)}, but applies the CSS and lays {@code scene} out before each
     * check, as the pulses of a shown window do. The scenes of the tests are never shown, so nothing else does it:
     * until then the cells of a list or tree do not exist, and neither does anything they would load.
     */
    public static boolean await(final Scene scene, final Duration timeout, final BooleanSupplier condition)
            throws Exception {
        return await(timeout, () -> {
            scene.getRoot().applyCss();
            scene.getRoot().layout();
            return condition.getAsBoolean();
        });
    }

    /**
     * Writes what {@code scene} shows to {@code file}.
     *
     * @return the number of distinct colors in the picture, 1 when nothing was drawn
     */
    public static int snapshot(final Scene scene, final Path file) throws Exception {
        return call(() -> write(scene.snapshot(null), file));
    }

    /** Every node of {@code type} below {@code root}, in the order they are drawn: the ones behind come first. */
    public static <T extends Node> List<T> all(final Parent root, final Class<T> type) {
        final List<T> found = new ArrayList<>();
        for (final Node child : root.getChildrenUnmodifiable()) {
            if (type.isInstance(child)) {
                found.add(type.cast(child));
            }
            if (child instanceof Parent parent) {
                found.addAll(all(parent, type));
            }
        }
        return found;
    }

    /**
     * Moves the pointer to a point of the scene, with no button down: the nodes it enters and leaves are told, as
     * they are by a mouse.
     */
    public static void move(final Scene scene, final Point2D at) throws Exception {
        call(() -> {
            pointer(scene, MouseEvent.MOUSE_MOVED, at);
            return null;
        });
    }

    /** Presses the primary button at {@code at}. */
    public static void press(final Scene scene, final Point2D at) throws Exception {
        call(() -> {
            pointer(scene, MouseEvent.MOUSE_PRESSED, at);
            return null;
        });
    }

    /** Moves the pointer to {@code at} with the primary button held down. */
    public static void dragTo(final Scene scene, final Point2D at) throws Exception {
        call(() -> {
            pointer(scene, MouseEvent.MOUSE_DRAGGED, at);
            return null;
        });
    }

    /** Releases the primary button at {@code at}. */
    public static void release(final Scene scene, final Point2D at) throws Exception {
        call(() -> {
            pointer(scene, MouseEvent.MOUSE_RELEASED, at);
            return null;
        });
    }

    /** Moves to {@code at}, presses and releases the primary button there. */
    public static void click(final Scene scene, final Point2D at) throws Exception {
        call(() -> {
            clickAt(scene, at);
            return null;
        });
    }

    /** Clicks twice in a row at {@code at}, quickly enough for the scene to count a click of two. */
    public static void doubleClick(final Scene scene, final Point2D at) throws Exception {
        call(() -> {
            clickAt(scene, at);
            clickAt(scene, at);
            return null;
        });
    }

    /**
     * The node the pointer picks at a point of the scene, which is the one that gets the events there, whatever its
     * look: shadows, clips and the nodes that let the mouse through are accounted for by the toolkit. Moves the
     * pointer to the point.
     *
     * @return {@code null} when nothing but the scene itself is there
     */
    public static Node under(final Scene scene, final Point2D at) throws Exception {
        return call(() -> {
            final AtomicReference<Object> target = new AtomicReference<>();
            final EventHandler<MouseEvent> spy = event -> target.set(event.getTarget());
            scene.addEventFilter(MouseEvent.MOUSE_MOVED, spy);
            try {
                pointer(scene, MouseEvent.MOUSE_MOVED, at);
            } finally {
                scene.removeEventFilter(MouseEvent.MOUSE_MOVED, spy);
            }
            return target.get() instanceof Node node ? node : null;
        });
    }

    /** Presses and releases a key, which goes to what has the focus in the scene, and to its shortcuts. */
    public static void key(final Scene scene, final KeyCode code) throws Exception {
        call(() -> {
            keyPress(scene, code, false);
            return null;
        });
    }

    /** Presses a key with the key of the shortcuts of the platform held down. */
    public static void shortcut(final Scene scene, final KeyCode code) throws Exception {
        call(() -> {
            keyPress(scene, code, true);
            return null;
        });
    }

    /**
     * Starts dragging {@code content} over {@code scene}, as another application would: the toolkit asks the nodes
     * under the pointer whether they take it.
     */
    public static Drag drag(final Scene scene, final DataFormat format, final Object content) throws Exception {
        return call(() -> new Drag(scene, format, content));
    }

    private static void clickAt(final Scene scene, final Point2D at) {
        pointer(scene, MouseEvent.MOUSE_MOVED, at);
        pointer(scene, MouseEvent.MOUSE_PRESSED, at);
        pointer(scene, MouseEvent.MOUSE_RELEASED, at);
    }

    // built as the toolkit does when the system reports the mouse: the scene picks the target and counts the clicks
    private static void pointer(final Scene scene, final EventType<MouseEvent> type, final Point2D at) {
        final boolean held = type == MouseEvent.MOUSE_PRESSED || type == MouseEvent.MOUSE_DRAGGED;
        final MouseButton button = type == MouseEvent.MOUSE_MOVED ? MouseButton.NONE : MouseButton.PRIMARY;
        SceneHelper.processMouseEvent(scene, new MouseEvent(type, at.getX(), at.getY(), at.getX(), at.getY(), button,
                0, false, false, false, false, held, false, false, false, false, false, false, false, null));
    }

    private static void keyPress(final Scene scene, final KeyCode code, final boolean shortcut) {
        final boolean mac = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac");
        SceneHelper.processKeyEvent(scene, new KeyEvent(KeyEvent.KEY_PRESSED, KeyEvent.CHAR_UNDEFINED, "", code, false,
                shortcut && !mac, false, shortcut && mac));
    }

    /**
     * A drag that comes from outside the scene, as one from another window or application does: the scene is told it
     * entered, where it moves, then that it was dropped or left.
     */
    public static final class Drag {

        private final Scene scene;

        private final TKDropTargetListener listener;

        private final Payload payload = new Payload();

        private boolean inside;

        private Drag(final Scene scene, final DataFormat format, final Object content)
                throws ReflectiveOperationException {
            this.scene = scene;
            payload.content.put(format, content);
            // the scene keeps the listener of the drops to itself, and a window would be the one to call it
            final Constructor<?> constructor =
                    Class.forName("javafx.scene.Scene$DropTargetListener").getDeclaredConstructor(Scene.class);
            constructor.setAccessible(true);
            listener = (TKDropTargetListener) constructor.newInstance(scene);
        }

        /**
         * Moves the drag over {@code at}, which is where the scene is asked whether it takes the content.
         *
         * @return what the scene accepts there, {@code null} when it does not take the content
         */
        public TransferMode over(final Point2D at) throws Exception {
            return call(() -> hover(at));
        }

        /**
         * Lets go of the content at {@code at}, after moving there as a real drag does.
         *
         * @return true when the scene took the content, which only the handler of the drop decides
         */
        public boolean drop(final Point2D at) throws Exception {
            return call(() -> {
                if (hover(at) == null) {
                    exit(at);
                    return false;
                }
                inside = false;
                return listener.drop(at.getX(), at.getY(), at.getX(), at.getY(), TransferMode.COPY) != null;
            });
        }

        /** Takes the drag away from the scene, without dropping it. */
        public void leave(final Point2D at) throws Exception {
            call(() -> {
                exit(at);
                return null;
            });
        }

        private TransferMode hover(final Point2D at) {
            if (!inside) {
                inside = true;
                return listener.dragEnter(at.getX(), at.getY(), at.getX(), at.getY(), TransferMode.COPY, payload);
            }
            return listener.dragOver(at.getX(), at.getY(), at.getX(), at.getY(), TransferMode.COPY);
        }

        private void exit(final Point2D at) {
            if (inside) {
                inside = false;
                listener.dragExit(at.getX(), at.getY(), at.getX(), at.getY());
            }
        }

        @Override
        public String toString() {
            return "Drag over " + scene;
        }
    }

    /** What the toolkit hands to a scene when something is dragged into it: the content, which can only be copied. */
    private static final class Payload implements TKClipboard {

        private final Map<DataFormat, Object> content = new HashMap<>();

        @Override
        @SuppressWarnings("removal")
        public void setSecurityContext(final AccessControlContext context) {
            // nothing to restrict
        }

        @Override
        public Set<DataFormat> getContentTypes() {
            return content.keySet();
        }

        @Override
        @SafeVarargs
        public final boolean putContent(final Pair<DataFormat, Object>... pairs) {
            for (final Pair<DataFormat, Object> pair : pairs) {
                content.put(pair.getKey(), pair.getValue());
            }
            return true;
        }

        @Override
        public Object getContent(final DataFormat format) {
            return content.get(format);
        }

        @Override
        public boolean hasContent(final DataFormat format) {
            return content.containsKey(format);
        }

        @Override
        public Set<TransferMode> getTransferModes() {
            return EnumSet.of(TransferMode.COPY);
        }

        @Override
        public void setDragView(final Image image) {
            // no image follows the pointer
        }

        @Override
        public void setDragViewOffsetX(final double offsetX) {
            // no image follows the pointer
        }

        @Override
        public void setDragViewOffsetY(final double offsetY) {
            // no image follows the pointer
        }

        @Override
        public Image getDragView() {
            return null;
        }

        @Override
        public double getDragViewOffsetX() {
            return 0;
        }

        @Override
        public double getDragViewOffsetY() {
            return 0;
        }
    }

    private static int write(final Image image, final Path file) throws IOException {
        final int width = (int) image.getWidth();
        final int height = (int) image.getHeight();
        final PixelReader pixels = image.getPixelReader();
        final BufferedImage png = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        final Set<Integer> colors = new HashSet<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final int argb = pixels.getArgb(x, y);
                png.setRGB(x, y, argb);
                colors.add(argb);
            }
        }
        Files.createDirectories(file.toAbsolutePath().getParent());
        ImageIO.write(png, "png", file.toFile());
        return colors.size();
    }
}
