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
package org.talend.sdk.component.tcketl.forms;

/** The control a property is edited with. */
public enum WidgetKind {

    /** A nested object: a titled group of rows. */
    GROUP,

    CHECKBOX,

    /** One of the constants of an enumeration, shown with its display name. */
    CHOICE,

    NUMBER,

    /** A list of objects: one group of rows per entry. */
    LIST_OF_OBJECTS,

    /** A list of plain values: one control per entry. */
    LIST_OF_VALUES,

    /** A list of keys and values. */
    MAP,

    PASSWORD,

    /** Source text, in a monospaced font. */
    CODE,

    /** A text with suggestions the user may ignore. */
    SUGGESTION,

    /** A text taken from a list the server computes. */
    DYNAMIC_CHOICE,

    TEXT_AREA,

    DATE_TIME,

    /** A file or directory path. */
    PATH,

    TEXT
}
