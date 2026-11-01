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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.talend.sdk.component.tcketl.domain.form.Props.array;
import static org.talend.sdk.component.tcketl.domain.form.Props.layout;
import static org.talend.sdk.component.tcketl.domain.form.Props.number;
import static org.talend.sdk.component.tcketl.domain.form.Props.object;
import static org.talend.sdk.component.tcketl.domain.form.Props.string;
import static org.talend.sdk.component.tcketl.domain.form.Props.tree;

import java.util.List;

import org.junit.jupiter.api.Test;

/** How a flat list of definitions becomes a tree. */
class PropertyTreeTest {

    @Test
    void aPropertyIsTheChildOfItsPathPrefixInServerOrder() {
        final PropertyTree tree =
                tree(object("cfg"), string("cfg.a"), object("cfg.sub"), number("cfg.sub.b"), string("cfg.z"));

        final PropertyNode root = tree.roots().get(0);
        final PropertyNode sub = root.child("sub").orElseThrow();

        assertEquals(1, tree.roots().size());
        assertEquals(List.of("a", "sub", "z"), root.children().stream().map(PropertyNode::name).toList());
        assertEquals(PropertyKind.OBJECT, sub.kind());
        assertEquals(PropertyKind.NUMBER, sub.child("b").orElseThrow().kind());
        assertSame(sub, sub.child("b").orElseThrow().parent().orElseThrow());
        assertTrue(root.parent().isEmpty());
    }

    @Test
    void levelsTheServerSkippedAreMadeUp() {
        final PropertyTree tree = tree(string("cfg.sub.leaf"));

        final PropertyNode sub = tree.find("cfg.sub").orElseThrow();

        assertTrue(tree.find("cfg").orElseThrow().isImplicit());
        assertTrue(sub.isImplicit());
        assertEquals(PropertyKind.OBJECT, sub.kind());
        assertFalse(tree.find("cfg.sub.leaf").orElseThrow().isImplicit());
    }

    @Test
    void aListOfValuesKeepsItsElementApart() {
        final PropertyTree tree = tree(object("cfg"), array("cfg.tags"), number("cfg.tags[]"));

        final PropertyNode tags = tree.find("cfg.tags").orElseThrow();
        final PropertyNode element = tags.element().orElseThrow();

        assertEquals(PropertyKind.ARRAY_OF_PRIMITIVE, tags.kind());
        assertEquals(PropertyKind.NUMBER, element.kind());
        assertTrue(element.isElement());
        assertTrue(tags.children().isEmpty());
    }

    @Test
    void aListWithoutElementDefinitionHoldsText() {
        final PropertyNode tags = tree(object("cfg"), array("cfg.tags")).find("cfg.tags").orElseThrow();

        assertEquals(PropertyKind.ARRAY_OF_PRIMITIVE, tags.kind());
        assertEquals(PropertyKind.STRING, tags.element().orElseThrow().kind());
    }

    @Test
    void aListOfObjectsGetsAnImplicitElementHoldingTheFields() {
        final PropertyTree tree = tree(object("cfg"), array("cfg.items").layout(layout("Main:qty|name")),
                string("cfg.items[].name"), number("cfg.items[].qty"));

        final PropertyNode items = tree.find("cfg.items").orElseThrow();
        final PropertyNode element = items.element().orElseThrow();

        assertEquals(PropertyKind.ARRAY_OF_OBJECT, items.kind());
        assertTrue(element.isImplicit());
        assertEquals(List.of("name", "qty"), element.children().stream().map(PropertyNode::name).toList());
        assertEquals(items.definition().layout(), element.layout());
    }

    @Test
    void aMapIsAnObjectMadeOfAKeyListAndAValueList() {
        final PropertyTree tree = tree(object("cfg"), object("cfg.headers"), string("cfg.headers.key[]"),
                string("cfg.headers.value[]"), object("cfg.pojos"), string("cfg.pojos.key[]"),
                string("cfg.pojos.value[].host"));

        final PropertyNode headers = tree.find("cfg.headers").orElseThrow();
        final PropertyNode pojos = tree.find("cfg.pojos").orElseThrow();

        assertEquals(PropertyKind.MAP, headers.kind());
        assertTrue(headers.child("key[]").orElseThrow().repeats());
        assertEquals(PropertyKind.MAP, pojos.kind());
        assertEquals(PropertyKind.OBJECT, pojos.child("value[]").orElseThrow().kind());
        assertTrue(pojos.child("value[]").orElseThrow().isImplicit());
    }

    @Test
    void concretePathsCarryTheIndexOfTheEntryAndFindAcceptsThem() {
        final PropertyTree tree = tree(object("cfg"), array("cfg.items"), string("cfg.items[].name"),
                object("cfg.headers"), string("cfg.headers.key[]"), string("cfg.headers.value[]"));
        final PropertyNode element = tree.find("cfg.items").orElseThrow().element().orElseThrow();
        final PropertyNode name = element.child("name").orElseThrow();
        final PropertyNode key = tree.find("cfg.headers").orElseThrow().child("key[]").orElseThrow();

        assertEquals("cfg.items[2]", element.concretePath("cfg.items", 2));
        assertEquals("cfg.items[2].name", name.concretePath("cfg.items[2]", 0));
        assertEquals("cfg.headers.key[1]", key.concretePath("cfg.headers", 1));
        assertEquals("cfg", tree.roots().get(0).concretePath("", 0));
        assertSame(name, tree.find("cfg.items[7].name").orElseThrow());
        assertEquals("a.list[].b[]", PropertyTree.definitionPath("a.list[3].b[12]"));
    }

    @Test
    void duplicateAndMalformedDefinitionsAreIgnoredWithAWarning() {
        final PropertyTree tree = tree(object("cfg"), string("cfg.a").defaultValue("first"),
                string("cfg.a").defaultValue("second"), string("cfg..b"), string(".c"));

        assertEquals("first", tree.find("cfg.a").orElseThrow().definition().defaultValue());
        assertTrue(tree.find("cfg..b").isEmpty());
        assertEquals(3, tree.warnings().size());
    }
}
