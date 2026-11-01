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
package org.talend.sdk.component.tcketl.domain.form;

/**
 * Choices that change what a form shows, not what it means.
 *
 * @param appendUnlistedToFirstPane whether a child that no layout of its object mentions is shown at the end of the
 * object's first pane (the default) or not shown at all, which is what the reference form renderer does.
 */
public record FormOptions(boolean appendUnlistedToFirstPane) {

    public static final FormOptions DEFAULT = new FormOptions(true);
}
