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
package org.talend.sdk.component.studio.model.command;

import static org.talend.sdk.component.studio.model.action.Action.HEALTH_CHECK;
import static org.talend.sdk.component.studio.model.action.Action.MESSAGE;
import static org.talend.sdk.component.studio.model.action.Action.OK;
import static org.talend.sdk.component.studio.model.action.Action.STATUS;

import java.util.Map;

import org.eclipse.gef.commands.Command;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.talend.sdk.component.studio.i18n.Messages;
import org.talend.sdk.component.studio.model.action.Action;
import org.talend.sdk.component.studio.model.action.ActionParameter;

/**
 * Command which is executed when "Test Connection" button is pushed
 */
public class HealthCheckCommand extends Command {

    private final Action healthCheckAction;

    public HealthCheckCommand(final String name, final String family) {
        this.healthCheckAction = new Action(name, family, HEALTH_CHECK);
    }

    @Override
    public void execute() {
        final Map<String, String> result = healthCheckAction.callback();
        if (OK.equals(result.get(STATUS))) {
            MessageDialog.openInformation(new Shell(), Messages.getString("healthCheck.connection.ok.title"),
                    result.get(MESSAGE));
        } else {
            MessageDialog.openError(new Shell(), Messages.getString("healthCheck.connection.fail.title"),
                    result.get(MESSAGE));
        }
    }

    public void addParameter(final ActionParameter parameter) {
        healthCheckAction.addParameter(parameter);
    }

}
