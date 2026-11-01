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

import javafx.beans.value.ChangeListener;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.scene.shape.CubicCurve;
import javafx.scene.shape.Polygon;

import org.talend.sdk.component.tcketl.domain.pipeline.Link;

/**
 * A link between two ports: a curve that leaves the output to the right and enters the input from the left, ending
 * with an arrow. It follows its two ports by itself, whatever moves them, and is selected by a click on it.
 */
final class LinkView extends Group {

    private static final double HIT_WIDTH = 12;

    private static final double MIN_CONTROL = 40;

    // the arrow stops short of the handle of the input
    private static final double ARROW_GAP = 7;

    private static final double ARROW_LENGTH = 8;

    private static final double ARROW_HALF_WIDTH = 4;

    private final Link link;

    private final PortView from;

    private final PortView to;

    private final Parent space;

    private final CubicCurve hit = new CubicCurve();

    private final CubicCurve line = new CubicCurve();

    private final Polygon arrow = new Polygon();

    private final ChangeListener<Point2D> follow = (observed, before, after) -> update();

    /**
     * @param space the parent in whose coordinates the link is drawn, which is an ancestor of the two ports
     */
    LinkView(final CanvasViewModel model, final Link link, final PortView from, final PortView to,
            final Parent space) {
        this.link = link;
        this.from = from;
        this.to = to;
        this.space = space;
        hit.setFill(null);
        hit.setStroke(Color.TRANSPARENT);
        hit.setStrokeWidth(HIT_WIDTH);
        line.setFill(null);
        getChildren().addAll(hit, line, arrow);

        setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                model.select(link);
                event.consume();
            }
        });
        setOnContextMenuRequested(event -> {
            model.select(link);
            final MenuItem delete = new MenuItem("Delete link");
            delete.setOnAction(action -> model.deleteLink(link));
            new ContextMenu(delete).show(this, event.getScreenX(), event.getScreenY());
            event.consume();
        });

        from.sceneAnchor().addListener(follow);
        to.sceneAnchor().addListener(follow);
        update();
        refresh(false);
    }

    Link link() {
        return link;
    }

    /** Stops following the ports: to call when the link is removed. */
    void dispose() {
        from.sceneAnchor().removeListener(follow);
        to.sceneAnchor().removeListener(follow);
    }

    /** The look depends on what the link carries and on the state of the ports it joins. */
    void refresh(final boolean selected) {
        final List<String> styles = new ArrayList<>();
        if (selected) {
            styles.add("selected");
        }
        if (from.port().isReject()) {
            styles.add("reject");
        }
        if (from.isDangling() || to.isDangling()) {
            styles.add("dangling");
        }
        line.getStyleClass().setAll("link");
        line.getStyleClass().addAll(styles);
        arrow.getStyleClass().setAll("link-arrow");
        arrow.getStyleClass().addAll(styles);
    }

    /** Draws the curve of a link, also used for the one that follows the pointer while a link is being drawn. */
    static void shape(final CubicCurve curve, final Point2D start, final Point2D end) {
        final double control = Math.max(MIN_CONTROL, Math.abs(end.getX() - start.getX()) / 2);
        curve.setStartX(start.getX());
        curve.setStartY(start.getY());
        curve.setControlX1(start.getX() + control);
        curve.setControlY1(start.getY());
        curve.setControlX2(end.getX() - control);
        curve.setControlY2(end.getY());
        curve.setEndX(end.getX());
        curve.setEndY(end.getY());
    }

    private void update() {
        final Point2D start = from.anchorIn(space);
        final Point2D end = to.anchorIn(space);
        shape(hit, start, end);
        shape(line, start, end);
        final double tip = end.getX() - ARROW_GAP;
        arrow.getPoints()
                .setAll(tip, end.getY(), tip - ARROW_LENGTH, end.getY() - ARROW_HALF_WIDTH, tip - ARROW_LENGTH,
                        end.getY() + ARROW_HALF_WIDTH);
    }
}
