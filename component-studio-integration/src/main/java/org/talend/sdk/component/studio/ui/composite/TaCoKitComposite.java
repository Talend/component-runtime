/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.studio.ui.composite;

import java.beans.PropertyChangeListener;
import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.talend.commons.ui.gmf.util.DisplayUtils;
import org.talend.core.model.process.EComponentCategory;
import org.talend.core.model.process.EParameterFieldType;
import org.talend.core.model.process.Element;
import org.talend.core.model.process.IElementParameter;
import org.talend.designer.core.model.FakeElement;
import org.talend.designer.core.ui.editor.properties.controllers.AbstractElementPropertySectionController;
import org.talend.designer.core.ui.views.properties.MultipleThreadDynamicComposite;
import org.talend.designer.core.ui.views.properties.composites.MissingSettingsMultiThreadDynamicComposite;
import org.talend.sdk.component.studio.model.parameter.Layout;
import org.talend.sdk.component.studio.model.parameter.LayoutParameter;
import org.talend.sdk.component.studio.model.parameter.Level;
import org.talend.sdk.component.studio.model.parameter.TaCoKitElementParameter;

/**
 * Registers PropertyChangeListener for each IElementParameter during instantiation
 * PropertyChangeListener refreshes layout after each IElementParameter value update
 */
public class TaCoKitComposite extends MissingSettingsMultiThreadDynamicComposite {

    /**
     * Refresher {@link PropertyChangeListener}. It is created and registered during {@link this#init()}.
     * It is unregistered during {@link this#dispose()}
     */
    private PropertyChangeListener refresher;

    private List<? extends IElementParameter> parameters;

    public TaCoKitComposite(final Composite parentComposite, final int styles, final EComponentCategory section,
            final Element element, final boolean isCompactView) {
        super(parentComposite, styles, section, element, isCompactView);
        init();
    }

    public TaCoKitComposite(final Composite parentComposite, final int styles, final EComponentCategory section,
            final Element element, final boolean isCompactView, final Color backgroundColor) {
        super(parentComposite, styles, section, element, isCompactView, backgroundColor);
        init();
    }

    /**
     * For each {@link TaCoKitElementParameter} registers PropertyChangeListener, which calls
     * {@link this#refresh()} on each {@link TaCoKitElementParameter} value change event
     * 
     * Note, component has special parameter UPDATE_COMPONENTS, which is checked to know whether it is required to
     * refresh layout.
     * So, it should be true to force refresh
     */
    private void init() {
        createRefresherListener();
        registerRefresherListener();
    }

    /**
     * Creates {@link PropertyChangeListener}, which refreshes Composite each time {@link TaCoKitElementParameter} is
     * changed
     */
    private void createRefresherListener() {
        refresher = event -> refresh();
    }

    private void registerRefresherListener() {
        elem
                .getElementParameters()
                .stream()
                .filter(p -> p instanceof TaCoKitElementParameter)
                .map(p -> (TaCoKitElementParameter) p)
                .filter(TaCoKitElementParameter::isRedrawable)
                .forEach(p -> p.registerListener(p.getName(), refresher));
    }

    @Override
    public void refresh() {
        if (elem instanceof FakeElement) { // sync exec
            DisplayUtils.getDisplay().syncExec(new Runnable() {

                @Override
                public void run() {
                    operationInThread();
                }
            });
        } else { // async exec
            super.refresh();
        }
    }

    /**
     * Specifies minimal height of current UI element
     * 
     * @return minimal height
     */
    @Override
    public int getMinHeight() {
        if (minHeight < 200) {
            return 200;
        } else if (minHeight > 700) {
            return 700;
        }
        return minHeight;
    }

    /**
     * Unregisters Refresher {@link PropertyChangeListener} from every {@link TaCoKitElementParameter}, where it was
     * registered
     */
    @Override
    public synchronized void dispose() {
        elem
                .getElementParameters()
                .stream()
                .filter(p -> p instanceof TaCoKitElementParameter)
                .map(p -> (TaCoKitElementParameter) p)
                .filter(TaCoKitElementParameter::isRedrawable)
                .forEach(p -> p.unregisterListener(p.getName(), refresher));
        super.dispose();
    }

    // TODO Reuse it from parent class
    private void resizeScrolledComposite() {
        lastCompositeSize = getParent().getClientArea().height;
        // propertyResized = true;
        final Class<MultipleThreadDynamicComposite> clazz = MultipleThreadDynamicComposite.class;
        try {
            final Field field = clazz.getDeclaredField("propertyResized");
            field.setAccessible(true);
            field.set(this, true);
        } catch (NoSuchFieldException | SecurityException e) {
            e.printStackTrace();
        } catch (final IllegalArgumentException e) {
            e.printStackTrace();
        } catch (final IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getLastCompositeSize() {
        return this.lastCompositeSize;
    }

    private int lastCompositeSize = 0;

    /**
     * Initialize all components for the defined section for this node.
     * Note, the method was copied from MultipleThreadDynamicComposite
     *
     * @param forceRedraw defines whether to force redraw or not
     * @param reInitialize defines whether Composite is re-initialized. If yes, then children are disposed
     * @param height not used, but it is here, because the method is overridden
     */
    @Override
    public synchronized void addComponents(final boolean forceRedraw, final boolean reInitialize, final int height) {
        // achen modifed to fix feature 0005991 if composite.isDisposed return
        if (elem == null || composite.isDisposed()) {
            return;
        }
        if (!forceRedraw) {
            final boolean needRedraw = isNeedRedraw();
            if (!needRedraw) {
                return;
            }
        }
        if (reInitialize) {
            if (currentComponent != null) {
                disposeChildren();
            }
        }
        parameters = elem.getElementParametersWithChildrens();
        generator.initController(this);
        final Composite previousComposite = addCommonWidgets(composite);
        final LayoutParameter layoutParameter =
                (LayoutParameter) elem.getElementParameter(LayoutParameter.name(section));
        final Layout layout = layoutParameter.getLayout();
        fillComposite(composite, layout, previousComposite);
        resizeScrolledComposite();
    }

    /**
     * Adds common widgets on specified {@code parent} Composite.
     * These widgets will shown in the top of parent Composite.
     * The method may be overridden.
     * 
     * @param parent parent Composite
     * @return last Composite added
     */
    protected Composite addCommonWidgets(final Composite parent) {
        final Composite propertyComposite = addPropertyType(composite);
        final Composite lastSchemaComposite = addSchemas(composite, propertyComposite);
        return lastSchemaComposite;
    }

    protected Composite addPropertyType(final Composite parent) {
        final Composite propertyComposite = new Composite(parent, SWT.NONE);
        propertyComposite.setBackground(parent.getBackground());
        propertyComposite.setLayout(new FormLayout());
        propertyComposite.setLayoutData(levelLayoutData(null));
        IElementParameter propertyType = elem.getElementParameter("PROPERTY");
        addWidgetIfActive(propertyComposite, propertyType);
        return propertyComposite;
    }

    protected Composite addSchemas(final Composite parent, final Composite previous) {
        Composite previousComposite = previous;
        List<IElementParameter> activeSchemas = parameters
                .stream()
                .filter(p -> p.getFieldType() == EParameterFieldType.SCHEMA_TYPE)
                .filter(this::doShow)
                .collect(Collectors.toList());
        for (IElementParameter schema : activeSchemas) {
            final Composite schemaComposite = new Composite(parent, SWT.NONE);
            schemaComposite.setBackground(parent.getBackground());
            schemaComposite.setLayout(new FormLayout());
            schemaComposite.setLayoutData(levelLayoutData(previousComposite));
            previousComposite = schemaComposite;
            addWidgetIfActive(schemaComposite, schema);
        }
        return previousComposite;
    }

    /**
     * Fills composite according specified layout
     * 
     * @param composite composite to fill
     * @param layout composite layout
     */
    private void fillComposite(final Composite composite, final Layout layout, final Composite previous) {
        if (layout.isLeaf()) {
            final String path = layout.getPath();
            final IElementParameter current = elem.getElementParameter(path);
            addWidgetIfActive(composite, current);
        } else {
            Composite previousLevel = previous;
            for (final Level level : layout.getLevels()) {
                final Composite levelComposite = new Composite(composite, SWT.NONE);
                levelComposite.setBackground(composite.getBackground());
                levelComposite.setLayout(new FormLayout());
                levelComposite.setLayoutData(levelLayoutData(previousLevel));
                previousLevel = levelComposite;

                final int columnSize = level.getColumns().size();
                for (int i = 0; i < columnSize; i++) {
                    final Layout column = level.getColumns().get(i);
                    final Composite columnComposite = new Composite(levelComposite, SWT.NONE);
                    columnComposite.setLayout(new FormLayout());
                    columnComposite.setBackground(levelComposite.getBackground());
                    final FormData columnLayoutData = new FormData();
                    columnLayoutData.top = new FormAttachment(0, 0);
                    columnLayoutData.left = new FormAttachment((100 / columnSize) * i, 0);
                    columnLayoutData.right = new FormAttachment((100 / columnSize) * (i + 1), 0);
                    columnLayoutData.bottom = new FormAttachment(100, 0);
                    columnComposite.setLayoutData(columnLayoutData);
                    fillComposite(columnComposite, column, null);
                }
            }
        }
    }

    private void addWidgetIfActive(final Composite parent, final IElementParameter parameter) {
        if (doShow(parameter)) {
            final AbstractElementPropertySectionController controller =
                    generator.getController(parameter.getFieldType(), this);
            controller.createControl(parent, parameter, 1, 1, 0, null);
        }
    }

    private FormData levelLayoutData(final Composite previousLevel) {
        final FormData layoutData = new FormData();
        if (previousLevel == null) {
            layoutData.top = new FormAttachment(0, 0);
        } else {
            layoutData.top = new FormAttachment(previousLevel, 0);
        }
        layoutData.left = new FormAttachment(0, 0);
        layoutData.right = new FormAttachment(100, 0);
        return layoutData;
    }

    private boolean doShow(final IElementParameter parameter) {
        return parameter.getCategory() == section && parameter.getFieldType() != EParameterFieldType.TECHNICAL
                && parameter.isShow(parameters);
    }

}
