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

import javafx.collections.ListChangeListener;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;

import org.talend.sdk.component.tcketl.domain.pipeline.PipelineProblem;

/**
 * Findings of the pipeline. Only present while there is something to report and collapsed until the user asks for it,
 * so a half-built pipeline does not eat the canvas.
 */
public final class ProblemsView extends TitledPane {

    private static final double ROW_HEIGHT = 24;

    private static final int MAX_VISIBLE_ROWS = 5;

    private final CanvasViewModel model;

    private final ListView<PipelineProblem> list;

    public ProblemsView(final CanvasViewModel model) {
        this.model = model;
        getStyleClass().add("problems");
        setAnimated(false);

        list = new ListView<>(model.problems());
        list.getStyleClass().add("problems-list");
        list.setFixedCellSize(ROW_HEIGHT);
        list.setFocusTraversable(false);
        list.setCellFactory(view -> new ProblemCell());
        setContent(list);

        model.problems().addListener((ListChangeListener<PipelineProblem>) change -> refresh());
        refresh();
    }

    private void refresh() {
        final int count = model.problems().size();
        final boolean any = count > 0;
        setVisible(any);
        setManaged(any);
        if (!any) {
            setExpanded(false);
        }
        setText(any ? "Problems (" + count + ")" : "Problems");
        // two pixels of border above and below the rows
        list.setPrefHeight(Math.min(count, MAX_VISIBLE_ROWS) * ROW_HEIGHT + 4);
    }

    private final class ProblemCell extends ListCell<PipelineProblem> {

        private final Label glyph = new Label();

        private final Tooltip tooltip = new Tooltip();

        ProblemCell() {
            tooltip.setWrapText(true);
            tooltip.setMaxWidth(420);
            setOnMouseClicked(event -> {
                final PipelineProblem problem = getItem();
                if (!isEmpty() && problem != null && problem.hasNode() && event.getButton() == MouseButton.PRIMARY) {
                    model.select(problem.node());
                }
            });
        }

        @Override
        protected void updateItem(final PipelineProblem problem, final boolean empty) {
            super.updateItem(problem, empty);
            if (empty || problem == null) {
                setText(null);
                setGraphic(null);
                setTooltip(null);
                return;
            }
            final boolean error = problem.severity() == PipelineProblem.Severity.ERROR;
            glyph.setText(error ? "\u2716" : "\u26A0");
            glyph.getStyleClass().setAll("problem-glyph", error ? "error" : "warning");
            tooltip.setText(problem.message());
            setText(problem.message());
            setGraphic(glyph);
            setTooltip(tooltip);
        }
    }
}
