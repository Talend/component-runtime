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
package org.talend.sdk.component.studio.metadata.handler;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.emf.ecore.resource.Resource;
import org.talend.commons.exception.PersistenceException;
import org.talend.commons.ui.runtime.image.IImage;
import org.talend.core.model.properties.Item;
import org.talend.core.model.repository.AbstractRepositoryContentHandler;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.sdk.component.studio.util.ETaCoKitImage;
import org.talend.sdk.component.studio.util.TaCoKitConst;

public class TaCoKitRepositoryContentHandler extends AbstractRepositoryContentHandler {

    @Override
    public Resource create(final IProject project, final Item item, final int classifierID, final IPath path)
            throws PersistenceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Resource save(final Item item) throws PersistenceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Item createNewItem(final ERepositoryObjectType type) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isRepObjType(final ERepositoryObjectType type) {
        return type == TaCoKitConst.METADATA_TACOKIT;
    }

    @Override
    public ERepositoryObjectType getRepositoryObjectType(final Item item) {
        // TODO
        // return ERepositoryObjectType.METADATA_TACOKIT_CONNECTION;
        return null;
    }

    @Override
    public IImage getIcon(final ERepositoryObjectType type) {
        if (TaCoKitConst.METADATA_TACOKIT == type) {
            return ETaCoKitImage.TACOKIT_REPOSITORY_ICON;
        }
        return null;
    }

    @Override
    public ERepositoryObjectType getHandleType() {
        return TaCoKitConst.METADATA_TACOKIT;
    }

    @Override
    public boolean hasSchemas() {
        return true;
    }

}
