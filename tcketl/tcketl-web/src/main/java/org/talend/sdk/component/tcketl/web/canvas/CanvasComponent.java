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
package org.talend.sdk.component.tcketl.web.canvas;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.EventData;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.shared.Registration;

/** Server side handle of the {@code tcketl-canvas} custom element (see {@code tcketl-canvas.js} for the protocol). */
@Tag("tcketl-canvas")
@JsModule("./tcketl-canvas.js")
public class CanvasComponent extends Component implements HasSize {

    private static final long serialVersionUID = 1L;

    /** A user intent reported by the browser: {@code detail} is a JSON document that MUST be validated. */
    @DomEvent("canvas-event")
    public static class CanvasEvent extends ComponentEvent<CanvasComponent> {

        private static final long serialVersionUID = 1L;

        private final String detail;

        public CanvasEvent(final CanvasComponent source, final boolean fromClient,
                @EventData("event.detail") final String detail) {
            super(source, fromClient);
            this.detail = detail;
        }

        public String detail() {
            return detail;
        }
    }

    public void setGraph(final String json) {
        getElement().callJsFunction("setGraph", json);
    }

    public void zoomBy(final double factor) {
        getElement().callJsFunction("zoomBy", factor);
    }

    public void resetView() {
        getElement().callJsFunction("resetView");
    }

    public Registration onEvent(final ComponentEventListener<CanvasEvent> listener) {
        return addListener(CanvasEvent.class, listener);
    }
}
