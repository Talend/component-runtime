/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.talend.sdk.component.studio.ui.guessschema;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.ui.runtime.exception.ExceptionMessageDialog;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.ITDQRuleService;
import org.talend.core.model.metadata.IMetadataColumn;
import org.talend.core.model.metadata.IMetadataTable;
import org.talend.core.model.metadata.MetadataColumn;
import org.talend.core.model.metadata.MetadataTable;
import org.talend.core.model.metadata.MetadataToolHelper;
import org.talend.core.model.process.IContext;
import org.talend.core.model.process.IElement;
import org.talend.core.model.process.IElementParameter;
import org.talend.core.model.properties.Property;
import org.talend.core.ui.metadata.dialog.MetadataDialog;
import org.talend.core.utils.CsvArray;
import org.talend.core.utils.KeywordsValidator;
import org.talend.designer.core.model.components.EParameterName;
import org.talend.designer.core.ui.editor.cmd.ChangeMetadataCommand;
import org.talend.designer.core.ui.editor.nodes.Node;
import org.talend.designer.core.ui.editor.properties.controllers.AbstractGuessSchemaProcess;
import org.talend.designer.core.ui.editor.properties.controllers.uidialog.OpenContextChooseComboDialog;
import org.talend.sdk.component.studio.i18n.Messages;

/**
 * DOC cmeng class global comment. Detailled comment
 */
public class GuessSchema {

    private static final String CONTEXT_CHOOSE_DIALOG_TITLE = "Choose a context for query :";//$NON-NLS-1$

    private IElement elem;

    private IElementParameter elementParameter;

    private CommandStack commandStack;

    private Composite composite;

    private ChangeMetadataCommand changeMetadataCommand;

    public GuessSchema(final IElement elem, final IElementParameter elementParameter, final Composite composite,
            final CommandStack commandStack) {
        this.elem = elem;
        this.elementParameter = elementParameter;
        this.composite = composite;
        this.commandStack = commandStack;
    }

    public void guessSchema() throws Exception {
        changeMetadataCommand = null;
        try {
            /*
             * get the select node,it's the input node of the process. then transfer selected context varriable to
             * openContextChooseDialog, added by hyWang
             */
            final IElementParameter switchParam =
                    elem.getElementParameter(EParameterName.REPOSITORY_ALLOW_AUTO_SWITCH.getName());

            Node inputNode = (Node) elementParameter.getElement();
            Shell parentShell = composite.getShell();
            final Property property = AbstractGuessSchemaProcess.getNewmockProperty();
            List<IContext> allcontexts = inputNode.getProcess().getContextManager().getListContext();

            OpenContextChooseComboDialog dialog = new OpenContextChooseComboDialog(parentShell, allcontexts);
            dialog.create();
            dialog.getShell().setText(CONTEXT_CHOOSE_DIALOG_TITLE);
            IContext selectContext = null;
            // job only have defoult context,or the query isn't context mode
            if (allcontexts.size() == 1) {
                selectContext = inputNode.getProcess().getContextManager().getDefaultContext();
            } else if (Window.OK == dialog.open()) {
                selectContext = dialog.getSelectedContext();
            }
            final IContext context = selectContext;
            if (context != null) {
                //
                final ProgressMonitorDialog pmd = new ProgressMonitorDialog(this.composite.getShell());

                pmd.run(true, true, new IRunnableWithProgress() {

                    @Override
                    public void run(final IProgressMonitor monitor)
                            throws InvocationTargetException, InterruptedException {
                        runShadowProcess(property, inputNode, context, switchParam);
                    }

                });
            }
        } catch (Exception e) {
            throw e;
        }
    }

    private void runShadowProcess(final Property property, final Node inputNode, final IContext selectContext,
            final IElementParameter switchParam) {
        TaCoKitGuessSchemaProcess gsp = new TaCoKitGuessSchemaProcess(property, inputNode, selectContext);
        try {
            CsvArray csvArray = gsp.run();
            final Throwable ex[] = new Throwable[1];
            Display.getDefault().syncExec(new Runnable() {

                @Override
                public void run() {
                    try {
                        createSchema(csvArray, inputNode);
                    } catch (Throwable e) {
                        ex[0] = e;
                    }
                }
            });
            if (ex[0] != null) {
                throw ex[0];
            }
        } catch (Throwable e) {
            Display.getDefault().asyncExec(new Runnable() {

                @Override
                public void run() {
                    ExceptionMessageDialog.openError(composite.getShell(),
                            Messages.getString("guessSchema.dialog.error.title"), //$NON-NLS-1$
                            Messages.getString("guessSchema.dialog.error.msg.default"), e); //$NON-NLS-1$
                }
            });
        }
    }

    private void createSchema(final CsvArray csvArray, final Node inputNode) {
        List<String[]> schemaContent = csvArray.getRows();
        if (schemaContent == null || schemaContent.isEmpty()) {
            return;
        }
        List<Integer> indexsForSameNamedColumn = new ArrayList<Integer>();
        List<String> columnLabels = new ArrayList<String>();
        List<IMetadataColumn> columns = new ArrayList<IMetadataColumn>();

        int i = -1;
        for (String[] row : schemaContent) {
            i++;
            ColumnInfo ci = new ColumnInfo(row);
            indexsForSameNamedColumn.clear();
            IMetadataColumn oneColum = new MetadataColumn();
            String labelName = ci.getName();
            String name = labelName;
            String sub = ""; //$NON-NLS-1$
            String sub2 = ""; //$NON-NLS-1$
            if (labelName != null && labelName.length() > 0 && labelName.startsWith("_")) { //$NON-NLS-1$
                sub = labelName.substring(1);
                if (sub != null && sub.length() > 0) {
                    sub2 = sub.substring(1);
                }
            }
            if (KeywordsValidator.isKeyword(labelName) || KeywordsValidator.isKeyword(sub)
                    || KeywordsValidator.isKeyword(sub2)) {
                labelName = "_" + labelName; //$NON-NLS-1$
            }
            oneColum.setLabel(MetadataToolHelper.validateColumnName(labelName, i, columnLabels));
            oneColum.setOriginalDbColumnName(name);
            oneColum.setPrecision(ci.getPrecision());
            oneColum.setLength(ci.getScale());
            try {
                oneColum.setTalendType(ci.getType());

            } catch (Exception e) {
                /*
                 * the table have no data at all ,to do nothing
                 */
                ExceptionHandler.process(e);
            }
            // get if a column is nullable from the temp file genenrated by GuessSchemaProcess.java
            oneColum.setNullable(ci.isNullable());
            columns.add(oneColum);
            columnLabels.add(oneColum.getLabel());
        }

        if (columns.size() > 0) {
            IElementParameter dqRule = elem.getElementParameter("DQRULES_LIST");
            if (dqRule != null) {
                ITDQRuleService ruleService = null;
                try {
                    ruleService =
                            (ITDQRuleService) GlobalServiceRegister.getDefault().getService(ITDQRuleService.class);
                } catch (RuntimeException e) {
                    // nothing to do
                }
                IElementParameter queryParam = elem.getElementParameter("QUERY");
                if (ruleService != null && queryParam != null) {
                    ruleService.updateOriginalColumnNames(columns, queryParam);
                }
            }
        }

        IMetadataTable tempMetatable = new MetadataTable();
        /* for bug 20973 */
        if (tempMetatable.getTableName() == null) {
            tempMetatable.setTableName(inputNode.getUniqueName());
        }
        IMetadataTable outputMetaCopy;
        IMetadataTable originaleOutputTable;
        String propertyName = elementParameter.getName();
        IElementParameter param = inputNode.getElementParameter(propertyName);
        // not really obvious logic. doesn't work for several connectors/schemas available.
        // we already have context in parameter. why do we need to iterate through parameters?
        // for (IElementParameter eParam : elem.getElementParameters()) {
        // if (eParam.getContext() != null) {
        // param = eParam;
        // }
        // }
        originaleOutputTable = inputNode.getMetadataFromConnector(param.getContext());
        if (originaleOutputTable != null) {
            outputMetaCopy = originaleOutputTable.clone();
        }

        tempMetatable.setListColumns(columns);
        MetadataDialog metaDialog = new MetadataDialog(composite.getShell(), tempMetatable, inputNode, commandStack);
        if (metaDialog != null) {
            metaDialog.setText(Messages.getString("guessSchema.dialog.title", inputNode.getLabel())); //$NON-NLS-1$
        }

        // ok pressed
        if (metaDialog.open() == MetadataDialog.OK) {
            outputMetaCopy = metaDialog.getOutputMetaData();
            boolean modified = false;
            if (!outputMetaCopy.sameMetadataAs(originaleOutputTable, IMetadataColumn.OPTIONS_NONE)) {
                modified = true;
            }

            if (modified) {
                IElementParameter switchParam =
                        elem.getElementParameter(EParameterName.REPOSITORY_ALLOW_AUTO_SWITCH.getName());
                if (switchParam != null) {
                    switchParam.setValue(Boolean.FALSE);
                }

                changeMetadataCommand =
                        new ChangeMetadataCommand(inputNode, param, originaleOutputTable, outputMetaCopy);
                changeMetadataCommand.execute();
            }
        }

    }

    public ChangeMetadataCommand getChangeMetadataCommand() {
        return changeMetadataCommand;
    }

    class ColumnInfo {

        private String name;

        private String type;

        private int scale = 0;

        private int precision = 0;

        private boolean nullable = true;

        public ColumnInfo(final String... infos) {
            if (infos == null) {
                return;
            }
            int length = infos.length;
            if (0 < length) {
                name = infos[0];
            }
            if (1 < length) {
                type = infos[1];
            }
            if (2 < length) {
                try {
                    scale = Integer.valueOf(infos[2]);
                } catch (Exception e) {
                    ExceptionHandler.process(e);
                }
            }
            if (3 < length) {
                try {
                    precision = Integer.valueOf(infos[3]);
                } catch (Exception e) {
                    ExceptionHandler.process(e);
                }
            }
            if (4 < length) {
                nullable = Boolean.valueOf(infos[4]);
            }
        }

        public String getName() {
            return this.name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public String getType() {
            return this.type;
        }

        public void setType(final String type) {
            this.type = type;
        }

        public int getScale() {
            return this.scale;
        }

        public void setScale(final int scale) {
            this.scale = scale;
        }

        public int getPrecision() {
            return this.precision;
        }

        public void setPrecision(final int precision) {
            this.precision = precision;
        }

        public boolean isNullable() {
            return this.nullable;
        }

        public void setNullable(final boolean nullable) {
            this.nullable = nullable;
        }

    }

}
