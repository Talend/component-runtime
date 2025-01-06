/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.intellij.module.step;

import static org.talend.sdk.component.intellij.Configuration.getMessage;

import java.awt.BorderLayout;
import java.awt.Font;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.Disposable;

import org.jdesktop.swingx.HorizontalLayout;
import org.jdesktop.swingx.VerticalLayout;

public class WelcomeStep extends ModuleWizardStep {

    private final WelcomePanel panel;

    public WelcomeStep(final WizardContext context, final Disposable parentDisposable) {
        panel = new WelcomePanel();
    }

    @Override
    public JComponent getComponent() {
        return panel;
    }

    @Override
    public void updateDataModel() {

    }

    public static final class WelcomePanel extends JPanel {

        public WelcomePanel() {
            super(new BorderLayout());
            JPanel container = new JPanel(new VerticalLayout(10));
            JPanel headerPanel = new JPanel(new HorizontalLayout(10));
            try (InputStream is =
                    Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("talend_logo_.png"));) {
                headerPanel.add(new JLabel(new ImageIcon(ImageIO.read(is))));
            } catch (IOException e) {// no-op
            }

            JLabel titleLabel = new JLabel(getMessage("wizard.welcome.title"));
            titleLabel.setFont(new Font(titleLabel.getFont().getName(), Font.BOLD, 24));
            headerPanel.add(titleLabel);

            try (InputStream is =
                    Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("tacokit_logo.png"));) {
                headerPanel.add(new JLabel(new ImageIcon(ImageIO.read(is))));
            } catch (IOException e) {// no-op
            }

            container.add(headerPanel);
            container.add(new JSeparator(SwingConstants.HORIZONTAL));
            container.add(new JLabel(getMessage("wizard.welcome.description")));

            add(container, BorderLayout.CENTER);

            // JLabel footer = new JLabel(getMessage("wizard.welcome.description.footer"));
            // container.add(new JSeparator(SwingConstants.HORIZONTAL));
            // container.add(footer);
        }
    }

}
