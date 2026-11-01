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
package org.talend.sdk.component.tcketl.app.canvas;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

import org.talend.sdk.component.tcketl.domain.connector.Port;
import org.talend.sdk.component.tcketl.domain.pipeline.Endpoint;
import org.talend.sdk.component.tcketl.domain.pipeline.NodeId;

/**
 * One port of a node: a handle that straddles the edge of the node, and the name of its flow. A link starts from the
 * handle (or the name) of an output and ends on an input. Where the links meet the port is {@link #anchorIn}, which
 * is kept up to date through {@link #sceneAnchor()} whatever moves the port: the node, a layout, the zoom.
 */
final class PortView extends HBox {

    /** What the port shows while a link is being drawn. */
    enum Mode {

        IDLE(""),
        COMPATIBLE("compatible"),
        DIMMED("dimmed"),
        HOT("hot"),
        REFUSED("refused");

        private final String style;

        Mode(final String style) {
            this.style = style;
        }
    }

    static final double HANDLE_SIZE = 12;

    private static final double HALF_HANDLE = HANDLE_SIZE / 2;

    private static final double LABEL_GAP = 4;

    private final NodeId node;

    private final Port port;

    private final Region handle = new Region();

    private final Label label = new Label();

    private final Tooltip tooltip = new Tooltip();

    private final ReadOnlyObjectWrapper<Point2D> sceneAnchor = new ReadOnlyObjectWrapper<>();

    private boolean connected;

    private boolean dangling;

    private Mode mode = Mode.IDLE;

    /**
     * @param onLinkStart told when the user presses the primary button on an output, to start drawing a link
     */
    PortView(final NodeId node, final Port port, final BiConsumer<PortView, MouseEvent> onLinkStart) {
        this.node = node;
        this.port = port;
        handle.getStyleClass().add("handle");
        handle.setMinSize(HANDLE_SIZE, HANDLE_SIZE);
        handle.setPrefSize(HANDLE_SIZE, HANDLE_SIZE);
        handle.setMaxSize(HANDLE_SIZE, HANDLE_SIZE);
        label.getStyleClass().add("port-label");
        label.setText(port.label());
        // the handle is centred on the edge of the node, and the name takes the room it leaves
        if (port.direction() == Port.Direction.INPUT) {
            setAlignment(Pos.CENTER_LEFT);
            handle.setTranslateX(-HALF_HANDLE);
            HBox.setMargin(label, new Insets(0, 0, 0, LABEL_GAP - HALF_HANDLE));
            getChildren().addAll(handle, label);
        } else {
            setAlignment(Pos.CENTER_RIGHT);
            handle.setTranslateX(HALF_HANDLE);
            HBox.setMargin(label, new Insets(0, LABEL_GAP - HALF_HANDLE, 0, 0));
            getChildren().addAll(label, handle);
            setOnMousePressed(event -> {
                if (event.getButton() == MouseButton.PRIMARY) {
                    onLinkStart.accept(this, event);
                    event.consume();
                }
            });
        }
        Tooltip.install(this, tooltip);
        // the property is lazy: it only tells that it changed once it has been read since the previous change
        handle.localToSceneTransformProperty().addListener(observed -> {
            handle.getLocalToSceneTransform();
            sceneAnchor.set(center());
        });
        handle.getLocalToSceneTransform();
        show(true, false, false);
    }

    Port port() {
        return port;
    }

    Endpoint endpoint() {
        return new Endpoint(node, port.flow());
    }

    boolean isDangling() {
        return dangling;
    }

    Mode mode() {
        return mode;
    }

    /**
     * Changes each time the handle moves on the screen, which is also when the links have to follow: it moves with
     * the node, the layout and the zoom of the canvas.
     */
    ReadOnlyObjectProperty<Point2D> sceneAnchor() {
        return sceneAnchor.getReadOnlyProperty();
    }

    /** The centre of the handle in the coordinates of {@code space}, which must be an ancestor of this port. */
    Point2D anchorIn(final Parent space) {
        return space.sceneToLocal(center());
    }

    /** True when the point, in scene coordinates, is on the handle or on the name of the flow. */
    boolean hits(final double sceneX, final double sceneY) {
        return localToScene(getBoundsInLocal()).contains(sceneX, sceneY);
    }

    /**
     * @param declared false when the flow is only known through a link
     * @param resolved true when the connector of the node is loaded, so that an undeclared flow is a defect
     * @param connected true when a link uses the port
     */
    void show(final boolean declared, final boolean resolved, final boolean connected) {
        this.connected = connected;
        this.dangling = resolved && !declared;
        tooltip.setText(describe());
        restyle();
    }

    void setMode(final Mode mode) {
        if (this.mode != mode) {
            this.mode = mode;
            restyle();
        }
    }

    private Point2D center() {
        return handle.localToScene(HALF_HANDLE, HALF_HANDLE);
    }

    private String describe() {
        final String kind = port.direction() == Port.Direction.INPUT ? "Input" : "Output";
        final String text = kind + " \"" + port.label() + "\"" + (port.isReject() ? " (rejected records)" : "");
        return dangling ? text + "\nThe connector does not declare this flow (anymore)" : text;
    }

    private void restyle() {
        final List<String> styles = new ArrayList<>();
        styles.add("port");
        styles.add(port.direction() == Port.Direction.INPUT ? "input" : "output");
        if (port.isReject()) {
            styles.add("reject");
        }
        if (connected) {
            styles.add("connected");
        }
        if (dangling) {
            styles.add("dangling");
        }
        if (mode != Mode.IDLE) {
            styles.add(mode.style);
        }
        getStyleClass().setAll(styles);
    }
}
