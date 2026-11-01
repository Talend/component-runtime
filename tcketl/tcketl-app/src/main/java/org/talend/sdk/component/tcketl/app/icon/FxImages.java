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

import java.awt.image.BufferedImage;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

/** Conversions to the toolkit's images that do not need the {@code javafx.swing} module. */
final class FxImages {

    private static final int OPAQUE_GRAY = 0xFFC8CDD3;

    private FxImages() {
        // static helpers only
    }

    static Image fromAwt(final BufferedImage source) {
        final int width = source.getWidth();
        final int height = source.getHeight();
        final int[] argb = source.getRGB(0, 0, width, height, null, 0, width);
        final WritableImage image = new WritableImage(width, height);
        image.getPixelWriter().setPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), argb, 0, width);
        return image;
    }

    /** A neutral rounded square, shown while an icon is loading or when there is none. */
    static Image placeholder(final int size) {
        final WritableImage image = new WritableImage(size, size);
        final PixelWriter writer = image.getPixelWriter();
        final int margin = Math.max(1, size / 8);
        final int radius = Math.max(2, size / 4);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                writer.setArgb(x, y, inside(x, y, size, margin, radius) ? OPAQUE_GRAY : 0);
            }
        }
        return image;
    }

    private static boolean inside(final int x, final int y, final int size, final int margin, final int radius) {
        final int min = margin;
        final int max = size - margin - 1;
        if (x < min || x > max || y < min || y > max) {
            return false;
        }
        final int cornerX = x < min + radius ? min + radius : (x > max - radius ? max - radius : x);
        final int cornerY = y < min + radius ? min + radius : (y > max - radius ? max - radius : y);
        final int dx = x - cornerX;
        final int dy = y - cornerY;
        return dx * dx + dy * dy <= radius * radius;
    }
}
