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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SvgRasterizerTest {

    @ParameterizedTest
    @ValueSource(strings = { "form-input.svg", "mapper.svg" })
    void drawsTheIconsTheServerSends(final String fixture) throws IOException {
        final BufferedImage image = SvgRasterizer.render(read(fixture), 32);

        assertEquals(32, image.getWidth());
        assertEquals(32, image.getHeight());
        assertTrue(visiblePixels(image) > 0, "nothing was drawn");
    }

    @Test
    void fitsADrawingThatIsNotSquareInTheMiddleOfTheImage() throws IOException {
        final String wide =
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"40\" height=\"10\" viewBox=\"0 0 40 10\">"
                        + "<rect width=\"40\" height=\"10\" fill=\"#336699\"/></svg>";

        final BufferedImage image = SvgRasterizer.render(wide.getBytes(StandardCharsets.UTF_8), 20);

        assertEquals(0, alpha(image, 10, 1));
        assertEquals(255, alpha(image, 10, 10));
        assertEquals(0, alpha(image, 10, 18));
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "not xml at all", "<html><body/></html>" })
    void refusesWhatIsNotAnSvgDocument(final String content) {
        assertThrows(IOException.class, () -> SvgRasterizer.render(content.getBytes(StandardCharsets.UTF_8), 16));
    }

    private static byte[] read(final String fixture) throws IOException {
        try (final InputStream stream = SvgRasterizerTest.class.getResourceAsStream(fixture)) {
            assertNotNull(stream, fixture);
            return stream.readAllBytes();
        }
    }

    private static int alpha(final BufferedImage image, final int x, final int y) {
        return image.getRGB(x, y) >>> 24;
    }

    private static int visiblePixels(final BufferedImage image) {
        int count = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (alpha(image, x, y) != 0) {
                    count++;
                }
            }
        }
        return count;
    }
}
