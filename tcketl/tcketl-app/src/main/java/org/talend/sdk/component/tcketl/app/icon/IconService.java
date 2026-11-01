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
package org.talend.sdk.component.tcketl.app.icon;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.System.Logger;
import java.util.Optional;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.scene.image.Image;

import org.talend.sdk.component.tcketl.app.async.AsyncRunner;
import org.talend.sdk.component.tcketl.app.async.LazyResources;
import org.talend.sdk.component.tcketl.app.session.ServerConnection;
import org.talend.sdk.component.tcketl.app.session.ServerSession;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorId;
import org.talend.sdk.component.tcketl.domain.server.IconData;
import org.talend.sdk.component.tcketl.domain.server.IconSource;
import org.talend.sdk.component.tcketl.domain.server.IconTheme;

/**
 * The icons of the connectors and of their families. They are asked of the server the first time they are needed,
 * and answered with a placeholder until they arrive (and for good when the server has none, or when it sends
 * something that cannot be drawn). Everything is forgotten whenever the session loads its connectors again, so a
 * new server never shows the icons of the previous one.
 *
 * <p>
 * Must be created and used on the UI thread, once the toolkit is running.
 */
public final class IconService {

    /** The side in pixels at which the icons are meant to be displayed. */
    public static final int DISPLAY_SIZE = 16;

    private static final Logger LOGGER = System.getLogger(IconService.class.getName());

    // the images are twice as big as they are shown, to stay sharp on scaled screens
    private static final int RENDER_SIZE = DISPLAY_SIZE * 2;

    private static final String SVG_MEDIA_TYPE = "image/svg+xml";

    private final LazyResources<IconKey, Image> resources;

    private volatile IconSource source;

    public IconService(final AsyncRunner async, final ServerSession session) {
        this.resources = new LazyResources<>(async, FxImages.placeholder(RENDER_SIZE), this::load);
        useSourceOf(session);
        session.revision().addListener((observed, before, after) -> {
            resources.clear();
            useSourceOf(session);
        });
    }

    /** The icon of a connector, a placeholder until loaded. */
    public ReadOnlyObjectProperty<Image> component(final ConnectorId id) {
        return resources.get(IconKey.component(id));
    }

    /** The icon of a family, a placeholder until loaded. */
    public ReadOnlyObjectProperty<Image> family(final String familyId) {
        return resources.get(IconKey.family(familyId));
    }

    private void useSourceOf(final ServerSession session) {
        final ServerConnection connection = session.connection().get();
        source = connection == null ? null : connection.icons();
    }

    private Optional<Image> load(final IconKey key) {
        final IconSource current = source;
        if (current == null) {
            return Optional.empty();
        }
        final Optional<IconData> data =
                key.kind() == IconKey.Kind.FAMILY ? current.familyIcon(key.id(), IconTheme.LIGHT)
                        : current.componentIcon(new ConnectorId(key.id()), IconTheme.LIGHT);
        return data.flatMap(content -> decode(key, content));
    }

    private Optional<Image> decode(final IconKey key, final IconData data) {
        try {
            if (SVG_MEDIA_TYPE.equals(data.mediaType()) || data.isSvg()) {
                return Optional.of(FxImages.fromAwt(SvgRasterizer.render(data.content(), RENDER_SIZE)));
            }
            final Image image =
                    new Image(new ByteArrayInputStream(data.content()), RENDER_SIZE, RENDER_SIZE, true, true);
            if (image.isError()) {
                throw new IOException("Not an image", image.getException());
            }
            return Optional.of(image);
        } catch (final IOException | RuntimeException e) {
            LOGGER.log(Logger.Level.WARNING, "Icon " + key + " cannot be drawn: " + e.getMessage());
            return Optional.empty();
        }
    }
}
