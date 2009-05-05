/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.gwt.client.view;

import org.nuxeo.ecm.platform.annotations.gwt.client.AbstractDocumentGWTTest;
import org.nuxeo.ecm.platform.annotations.gwt.client.util.Visitor;
import org.nuxeo.ecm.platform.annotations.gwt.client.view.decorator.AnnoteaDecoratorVisitor;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 *
 */
public class GwtTestDecoratorVisitor extends AbstractDocumentGWTTest {

    @SuppressWarnings("unchecked")
    public void testStraight() {
        createDocument();
        Element bNode = RootPanel.getBodyElement().getElementsByTagName("b").getItem(
                0);
        assertNotNull(bNode);
        AnnoteaDecoratorVisitor processor = new AnnoteaDecoratorVisitor(bNode, 8, 13,
                getDefaultAnnotation(), null);
        Visitor visitor = new Visitor(processor);
        visitor.process(bNode);
        assertNotNull(bNode);
        NodeList list = bNode.getChildNodes();
        SpanElement span = SpanElement.as(bNode.getParentElement());
        assertEquals(3, list.getLength());
        assertEquals(span.getInnerHTML(),
                "<b>Nuxeo EP 5 - <span class=\"decorate decorate0\">Nuxeo An</span>notation</b>");
        processor = new AnnoteaDecoratorVisitor(bNode, 5, 23, getDefaultAnnotation(), null);
        visitor = new Visitor(processor);
        visitor.process(bNode);
        assertEquals(5, bNode.getChildNodes().getLength());
        assertEquals(
                span.getInnerHTML(),
                "<b>Nuxeo EP 5 - <span class=\"decorate decorate0\">Nuxeo An</span>no<span class=\"decorate decorate0\">tatio</span>n</b>");

    }

    @SuppressWarnings("unchecked")
    public void testReverse() {
        createDocument();
        Element bNode = RootPanel.getBodyElement().getElementsByTagName("b").getItem(
                0);
        assertNotNull(bNode);
        AnnoteaDecoratorVisitor processor = new AnnoteaDecoratorVisitor(bNode, 5, 23, getDefaultAnnotation(),
                null);
        Visitor visitor = new Visitor(processor);
        visitor.process(bNode);
        assertNotNull(bNode);
        NodeList list = bNode.getChildNodes();
        SpanElement span = SpanElement.as(bNode.getParentElement());
        assertEquals(3, list.getLength());
        assertEquals(span.getInnerHTML(),
                "<b>Nuxeo EP 5 - Nuxeo Anno<span class=\"decorate decorate0\">tatio</span>n</b>");
        processor = new AnnoteaDecoratorVisitor(bNode, 8, 13, getDefaultAnnotation(), null);
        visitor = new Visitor(processor);
        visitor.process(bNode);
        assertEquals(5, bNode.getChildNodes().getLength());
        assertEquals(
                span.getInnerHTML(),
                "<b>Nuxeo EP 5 - <span class=\"decorate decorate0\">Nuxeo An</span>no<span class=\"decorate decorate0\">tatio</span>n</b>");
    }

    @SuppressWarnings("unchecked")
    public void testMultiLine() {
        createDocument();
        Element bNode = RootPanel.getBodyElement().getElementsByTagName("b").getItem(
                0);
        assertNotNull(bNode);
        AnnoteaDecoratorVisitor processor = new AnnoteaDecoratorVisitor(bNode, 48, 13, getDefaultAnnotation(),
                null);
        Visitor visitor = new Visitor(processor);
        visitor.process(bNode.getOwnerDocument());
        NodeList list = bNode.getChildNodes();
        assertEquals(2, list.getLength());
        DivElement div = DivElement.as(RootPanel.getBodyElement().getElementsByTagName(
                "div").getItem(0));
        @SuppressWarnings("unused")
        String testString = div.getInnerHTML();
        assertTrue(div.getInnerHTML().contains(parsedResult));
    }
    public void testRemoveWhiteSpace() {
        AnnoteaDecoratorVisitor v = new AnnoteaDecoratorVisitor(null, 0, 0, null, null);
        v.setLastCharIsSpace(true);
        String result = v.removeWhiteSpace(" b   c d      d     ");
        assertEquals("b c d d", result);
    }
    private String parsedResult = "<span class=\"decorate decorate0\">Nuxeo Annotation</span>"
            + "</b></span></nobr></div><div id=\"thediv\" style=\"position: absolute; top: 329px; left: 242px;\">"
            + "<span class=\"decorate decorate0\">The </span><nobr><span class=\"ft0\"><span class=\"decorate decorate0\">Da </span>"
            + "<b><span class=\"decorate decorate0\">Service</span></b></span></nobr><span class=\"decorate decorate0\"> and other stuff</span>"
            + "</div><div style=\"position: absolute; top: 691px; left: 199px;\"><nobr><span class=\"ft1\"><span class=\"decorate decorate0\">Co</span>";
}
