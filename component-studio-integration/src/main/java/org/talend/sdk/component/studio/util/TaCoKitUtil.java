/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.studio.util;

import java.util.List;

import org.talend.core.model.general.Project;
import org.talend.core.model.properties.ConnectionItem;
import org.talend.core.model.repository.IRepositoryViewObject;
import org.talend.core.repository.model.ProxyRepositoryFactory;
import org.talend.repository.ProjectManager;
import org.talend.sdk.component.studio.metadata.model.TaCoKitConfigurationItemModel;
import org.talend.sdk.component.studio.metadata.model.TaCoKitConfigurationModel;

/**
 * DOC cmeng class global comment. Detailled comment
 */
public class TaCoKitUtil {

    /**
     * Get ConnectionItem from specified project
     * 
     * @param project {@link Project}
     * @param itemId
     * @return
     * @throws Exception
     */
    public static ConnectionItem getLatestTaCoKitConnectionItem(final Project project, final String itemId)
            throws Exception {
        IRepositoryViewObject lastVersion = ProxyRepositoryFactory.getInstance().getLastVersion(project, itemId, null,
                TaCoKitConst.METADATA_TACOKIT);
        if (lastVersion != null) {
            return (ConnectionItem) lastVersion.getProperty().getItem();
        }
        return null;
    }

    /**
     * Get ConnectionItem from main project or it's reference project
     * 
     * @param itemId
     * @return
     * @throws Exception
     */
    public static ConnectionItem getLatestTaCoKitConnectionItem(final String itemId) throws Exception {
        ConnectionItem item = getLatestTaCoKitConnectionItem(ProjectManager.getInstance().getCurrentProject(), itemId);
        if (item != null) {
            return item;
        }
        List<Project> allReferencedProjects = ProjectManager.getInstance().getAllReferencedProjects();
        if (allReferencedProjects != null && !allReferencedProjects.isEmpty()) {
            for (Project referenceProject : allReferencedProjects) {
                item = getLatestTaCoKitConnectionItem(referenceProject, itemId);
                if (item != null) {
                    return item;
                }
            }
        }
        return null;
    }

    public static TaCoKitConfigurationModel getTaCoKitConfigurationModel(final String itemId) throws Exception {
        ConnectionItem item = getLatestTaCoKitConnectionItem(itemId);
        if (item != null) {
            TaCoKitConfigurationItemModel itemModel = new TaCoKitConfigurationItemModel(item);
            return itemModel.getConfigurationModel();
        }
        return null;
    }

    public static boolean equals(final String str1, final String str2) {
        return str1 == null ? str2 == null : str1.equals(str2);
    }

    public static boolean isEmpty(final String str) {
        return str == null || str.length() == 0;
    }
}
