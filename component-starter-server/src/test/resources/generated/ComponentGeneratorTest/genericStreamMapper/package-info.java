// this tells the framework in which family (group of components) and categories (UI grouping)
// the components in the nested packages belong to
@Components(family = "superfamily", categories = "supercategory")
@Icon(value = CUSTOM, custom = "superfamily")
package com.foo;

import static org.talend.sdk.component.api.component.Icon.IconType.CUSTOM;

import org.talend.sdk.component.api.component.Components;
import org.talend.sdk.component.api.component.Icon;
