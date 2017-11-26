/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.studio.metadata;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.talend.core.model.properties.ConnectionItem;
import org.talend.core.model.properties.Property;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.core.model.repository.IRepositoryViewObject;
import org.talend.core.model.repository.RepositoryObject;
import org.talend.core.repository.model.ProxyRepositoryFactory;
import org.talend.core.repository.ui.actions.metadata.AbstractCreateAction;
import org.talend.metadata.managment.ui.utils.ConnectionContextHelper;
import org.talend.metadata.managment.ui.wizard.CheckLastVersionRepositoryWizard;
import org.talend.repository.ProjectManager;
import org.talend.repository.model.IProxyRepositoryFactory;
import org.talend.repository.model.IRepositoryNode;
import org.talend.repository.model.RepositoryNode;
import org.talend.repository.model.RepositoryNodeUtilities;
import org.talend.repository.view.di.metadata.action.MetedataNodeActionProvier;
import org.talend.sdk.component.server.front.model.ComponentDetail;
import org.talend.sdk.component.server.front.model.ComponentIndex;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;
import org.talend.sdk.component.studio.Lookups;
import org.talend.sdk.component.studio.lang.Pair;
import org.talend.sdk.component.studio.service.ComponentService;
import org.talend.sdk.component.studio.websocket.WebSocketClient;

import lombok.Data;

public class NodeActionProvider extends MetedataNodeActionProvier {

    private final WebSocketClient.V1Component client;

    private final ComponentService service;

    private final ConcurrentMap<String, ConfigAction> actions = new ConcurrentHashMap<>();

    public NodeActionProvider() {
        client = Lookups.client().v1().component();
        service = Lookups.service();
    }

    @Override
    public void fillContextMenu(final IMenuManager manager) {
        final IStructuredSelection sel = IStructuredSelection.class.cast(getContext().getSelection());
        final Object selObj = sel.getFirstElement();
        if (RepositoryNode.class.isInstance(selObj)) {
            final RepositoryNode rn = RepositoryNode.class.cast(selObj);
            final ERepositoryObjectType nodeType =
                    ERepositoryObjectType.class.cast(rn.getProperties(IRepositoryNode.EProperties.CONTENT_TYPE));
            if (!ERepositoryObjectType.METADATA_CON_TABLE.equals(nodeType)
                    && !ERepositoryObjectType.METADATA_CON_COLUMN.equals(nodeType)) {
                final IRepositoryViewObject repObj = rn.getObject();
                if (repObj == null) {
                    createAction(null, sel);
                } else {
                    client
                            .details(Locale.getDefault().getLanguage())
                            .flatMap(detail -> detail
                                    .getSecond()
                                    .getProperties()
                                    .stream()
                                    .filter(service::isConfiguration)
                                    .map(p -> new Pair<>(detail, p)))
                            .forEach(pair -> createAction(pair, sel).update(pair, sel));
                }
                manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
            }
        }
        super.fillContextMenu(manager);
    }

    private ConfigAction createAction(final Pair<Pair<ComponentIndex, ComponentDetail>, SimplePropertyDefinition> data,
            final IStructuredSelection sel) {
        return actions.computeIfAbsent(data.getFirst().getSecond().getId().getId() + "//" + data.getSecond().getPath(),
                k -> {
                    ConfigAction action = new ConfigAction(new AtomicReference<>(), service);
                    action.init(TreeViewer.class.cast(getActionSite().getStructuredViewer()), sel);
                    return action;
                });
    }

    @Data
    private static class ConfigAction extends AbstractCreateAction {

        private final AtomicReference<Pair<Pair<ComponentIndex, ComponentDetail>, SimplePropertyDefinition>> data;

        private final ComponentService service;

        private ERepositoryObjectType repObjType;

        private boolean creation = true;

        @Override
        protected void doRun() {
            final Pair<Pair<ComponentIndex, ComponentDetail>, SimplePropertyDefinition> current = data.get();
            final IWizard wizard = new ConfigWizard(PlatformUI.getWorkbench(), creation, current, repositoryNode,
                    getExistingNames(), service);
            final WizardDialog wizardDialog = new ConfigDialog(
                    PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), wizard, true, true /* todo */);

            if (service.isLinux()) { // why???? for the +80? isn't it a linux theme issue more than a stdio issue?
                wizardDialog.setPageSize(700, 480);
            }
            wizardDialog.create();
            wizardDialog.open();
        }

        @Override
        protected void init(final RepositoryNode node) {
            final Pair<Pair<ComponentIndex, ComponentDetail>, SimplePropertyDefinition> current = data.get();

            repositoryNode = getCurrentRepositoryNode();
            repObjType = repositoryNode.getObjectType();
            if (repObjType == null || repositoryNode.getType() != IRepositoryNode.ENodeType.REPOSITORY_ELEMENT) {
                repObjType = ERepositoryObjectType.class
                        .cast(repositoryNode.getProperties(IRepositoryNode.EProperties.CONTENT_TYPE));
            }

            setText(current.getSecond().getDisplayName());
            setImageDescriptor(ImageDescriptor
                    .createFromImage(service.toEclipseIcon(current.getFirst().getFirst().getIcon()).createImage()));

            final IProxyRepositoryFactory factory = ProxyRepositoryFactory.getInstance();
            switch (node.getType()) {
            case SIMPLE_FOLDER:
            case SYSTEM_FOLDER:
                if (factory.isUserReadOnlyOnCurrentProject()
                        || !ProjectManager.getInstance().isInCurrentMainProject(node)) {
                    setEnabled(false);
                    return;
                }
                if (node.getObject() != null && node.getObject().getProperty().getItem().getState().isDeleted()) {
                    setEnabled(false);
                    return;
                }
                collectChildNames(node);
                creation = true;
                break;
            case REPOSITORY_ELEMENT:
                if (factory.isPotentiallyEditable(node.getObject()) && isLastVersion(node)) {
                    collectSiblingNames(node);
                }
                creation = false;
                break;
            default:
                return;
            }

            setEnabled(true);
        }

        private void update(final Pair<Pair<ComponentIndex, ComponentDetail>, SimplePropertyDefinition> data,
                final IStructuredSelection sel) {
            this.data.set(data);
            final Object o = sel.getFirstElement();
            if (sel.size() == 1 && RepositoryNode.class.isInstance(o)) {
                this.repositoryNode = RepositoryNode.class.cast(o);
            }
        }
    }

    private static class ConfigWizard extends CheckLastVersionRepositoryWizard {

        private final Pair<Pair<ComponentIndex, ComponentDetail>, SimplePropertyDefinition> data;

        private final RepositoryNode repository;

        private final String[] existing;

        private final ComponentService service;

        private SavedItem original;

        private ConfigWizard(final IWorkbench workbench, final boolean creation,
                final Pair<Pair<ComponentIndex, ComponentDetail>, SimplePropertyDefinition> current,
                final RepositoryNode repositoryNode, final String[] existingNames, final ComponentService service) {
            super(workbench, creation);
            this.data = current;
            this.repository = repositoryNode;
            this.existing = existingNames;
            this.service = service;

            init();
        }

        private void init() {
            /*
             * ERepositoryObjectType repObjType = repository.getObjectType(); if (repObjType
             * == null || repository.getType() !=
             * IRepositoryNode.ENodeType.REPOSITORY_ELEMENT) { repObjType =
             * ERepositoryObjectType.class.cast(repository.getProperties(IRepositoryNode.
             * EProperties.CONTENT_TYPE)); }
             */

            final IRepositoryNode.ENodeType nodeType = repository.getType();
            switch (nodeType) {
            case SIMPLE_FOLDER:
            case REPOSITORY_ELEMENT:
                pathToSave = RepositoryNodeUtilities.getPath(repository);
                break;
            case SYSTEM_FOLDER:
                pathToSave = new Path("");
                break;
            default:
            }

            switch (nodeType) {
            case SIMPLE_FOLDER:
            case SYSTEM_FOLDER:
                /*
                 * connection = GenericMetadataFactory.eINSTANCE.createGenericConnection();
                 * connectionProperty = PropertiesFactory.eINSTANCE.createProperty();
                 * 
                 * connectionProperty.setId(ProxyRepositoryFactory.getInstance().getNextId());
                 * connectionProperty.setAuthor(((RepositoryContext)
                 * CoreRuntimePlugin.getInstance().getContext()
                 * .getProperty(Context.REPOSITORY_CONTEXT_KEY)).getUser());
                 * connectionProperty.setVersion(VersionUtils.DEFAULT_VERSION);
                 * connectionProperty.setStatusCode(""); //$NON-NLS-1$
                 */

                /*
                 * connectionItem =
                 * GenericMetadataFactory.eINSTANCE.createGenericConnectionItem();
                 * 
                 * connectionItem.setProperty(connectionProperty);
                 * connectionItem.setConnection(connection);
                 */
                break;
            case REPOSITORY_ELEMENT:
                final RepositoryObject object = new RepositoryObject(repository.getObject().getProperty());
                setRepositoryObject(object);
                /*
                 * connection = (GenericConnection) ((ConnectionItem)
                 * object.getProperty().getItem()).getConnection(); // Set context name to null
                 * so as to open context select dialog once if there are more than one context
                 * // group when opening a connection. connection.setContextName(null);
                 * connectionProperty = object.getProperty();
                 */
                connectionItem = (ConnectionItem) object.getProperty().getItem();
                // set the repositoryObject, lock and set isRepositoryObjectEditable
                setRepositoryObject(repository.getObject());
                initLockStrategy();
                break;
            default:
            }
            if (!creation) {
                final Property property = connectionItem.getProperty();
                original = new SavedItem(property.getLabel(), property.getVersion(), property.getDescription(),
                        property.getPurpose(), property.getStatusCode());
            }
            /*
             * oldMetadataTable =
             * GenericUpdateManager.getConversionMetadataTables(connectionItem.getConnection
             * ()); compService = new GenericWizardInternalService().getComponentService();
             * compService.setRepository(new GenericRepository());
             */

            ConnectionContextHelper.checkContextMode(connectionItem);
            setHelpAvailable(false);
            // setRepositoryLocation(wizard, location,
            // connectionItem.getProperty().getId());
        }

        @Override
        public void addPages() {
            setWindowTitle(data.getSecond().getDisplayName());
            setDefaultPageImageDescriptor(service.toEclipseIcon(data.getFirst().getFirst().getIcon()));

            // todo: do the equivalent than GenericConnWizardPage based on the meta of the
            // properties in data
            // -> this task depends the form integration into the studio
            // -> entry point = AbstractNamedWizardPage
            //
            // we will build one page per "tab", ie by default a single page and if MAIN and
            // ADVANCED are defined 2 pages
            // (main then advanced)
            //
            // todo: for (page in pages) addPage(page);
        }

        @Override
        public boolean performFinish() {
            /*
             * todo if (page.isPageComplete()) { try { createOrUpdateConnectionItem(); }
             * catch (final Throwable e) { ExceptionHandler.process(e); return false; }
             * return true; } else { return false; }
             */
            return true;
        }

        @Override
        public boolean performCancel() {
            if (!creation) {
                connectionItem.getProperty().setVersion(original.originalVersion);
                connectionItem.getProperty().setDisplayName(original.originalLabel);
                connectionItem.getProperty().setDescription(original.originalDescription);
                connectionItem.getProperty().setPurpose(original.originalPurpose);
                connectionItem.getProperty().setStatusCode(original.originalStatus);
            }
            return super.performCancel();
        }

        @Override
        public ConnectionItem getConnectionItem() {
            return this.connectionItem;
        }
    }

    private static class ConfigDialog extends WizardDialog {

        private final boolean first;

        private final boolean last;

        private ConfigDialog(final Shell parentShell, final IWizard newWizard, final boolean first,
                final boolean last) {
            super(parentShell, newWizard);
            this.first = first;
            this.last = last;
        }

        @Override
        public void updateButtons() {
            super.updateButtons();
            {
                Button nextButton = getButton(IDialogConstants.NEXT_ID);
                if (nextButton != null && nextButton.isEnabled()) {
                    nextButton.setEnabled(!last);
                }
            }
            {
                Button backButton = getButton(IDialogConstants.BACK_ID);
                if (backButton != null && backButton.isEnabled()) {
                    backButton.setEnabled(!first);
                }
            }
            {
                Button finishButton = getButton(IDialogConstants.FINISH_ID);
                if (finishButton != null && finishButton.isEnabled()) {
                    finishButton.setEnabled(last);
                }
            }
        }
    }

    @Data
    private static class SavedItem {

        private final String originalLabel;

        private final String originalVersion;

        private final String originalDescription;

        private final String originalPurpose;

        private final String originalStatus;
    }
}
