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

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.parser.resources.ResourcePolicy;
import com.github.weisj.jsvg.view.FloatSize;
import com.github.weisj.jsvg.view.ViewBox;

/**
 * Draws an SVG document, which the toolkit cannot, into a bitmap. The icons come from a remote server, so the
 * document is never allowed to load anything else (no external image, no stylesheet).
 */
final class SvgRasterizer {

    private SvgRasterizer() {
        // static helper only
    }

    /**
     * @param svg the document
     * @param size the side in pixels of the square the image is fitted in, keeping its proportions and centered
     * @return an image of {@code size} by {@code size} pixels
     * @throws IOException when the bytes are not a drawable SVG document
     */
    public static BufferedImage render(final byte[] svg, final int size) throws IOException {
        final LoaderContext context =
                LoaderContext.builder().externalResourcePolicy(ResourcePolicy.DENY_ALL).build();
        final SVGDocument document = new SVGLoader().load(new ByteArrayInputStream(svg), null, context);
        if (document == null) {
            throw new IOException("Not an SVG document");
        }
        final FloatSize natural = document.size();
        if (!(natural.width > 0f) || !(natural.height > 0f)) {
            throw new IOException("The SVG document has no size");
        }
        final float scale = Math.min(size / natural.width, size / natural.height);
        final float width = natural.width * scale;
        final float height = natural.height * scale;
        final BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            document.render(null, graphics, new ViewBox((size - width) / 2f, (size - height) / 2f, width, height));
        } finally {
            graphics.dispose();
        }
        return image;
    }
}
