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

package org.nuxeo.ecm.platform.annotations.gwt.client.view.annotater;

import org.nuxeo.ecm.platform.annotations.gwt.client.controler.AnnotationController;
import org.nuxeo.ecm.platform.annotations.gwt.client.model.Annotation;
import org.nuxeo.ecm.platform.annotations.gwt.client.model.Container;
import org.nuxeo.ecm.platform.annotations.gwt.client.util.Range;
import org.nuxeo.ecm.platform.annotations.gwt.client.util.Utils;
import org.nuxeo.ecm.platform.annotations.gwt.client.util.XPathUtil;
import org.nuxeo.ecm.platform.annotations.gwt.client.view.NewAnnotationPopup;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.user.client.Event;

/**
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 *
 */
public class TextAnnotater extends AbstractAnnotater {

    private final XPathUtil xpathUtil = new XPathUtil();

    public TextAnnotater(AnnotationController controller) {
        super(controller, false);
    }

    @Override
    public void onMouseUp(Event event) {
        Log.debug("TextAnnotater#onMouseUp; eventId= " + event.getType()
                + "; source: " + event.getCurrentTarget());
        Range currentRange = Utils.getCurrentRange(Document.get());
        if (currentRange != null && currentRange.getSelectedText().length() != 0) {
            Element startElement = Element.as(currentRange.getStartContainer());
            String pointer = xpathUtil.getSelectionXPointer(currentRange);
            controller.createNewAnnotation(pointer);

            Container startContainer = getStartContainer(currentRange);
            Container endContainer = getEndContainer(currentRange);

            Annotation annotation = controller.getNewAnnotation();
            annotation.setStartContainer(startContainer);
            annotation.setEndContainer(endContainer);

            NewAnnotationPopup popup = new NewAnnotationPopup(startElement,
                    controller, false, "local");
            controller.setNewAnnotationPopup(popup);
            addAnnotationPopup();
        } else {
            controller.setNewAnnotationPopup(null);
        }

        super.onMouseUp(event);
    }

    private Container getStartContainer(Range range) {
        Node startNode = range.getStartContainer();
        int startOffset = range.getStartOffset();
        startOffset = computeNewOffset(startNode, startOffset);
        return new Container(xpathUtil.getXPath(startNode), startOffset);
    }

    private int computeNewOffset(Node node, int currentOffset) {
        if (currentOffset <= 0) {
            return currentOffset;
        }
        String text = node.getNodeValue().substring(0, currentOffset);
        String processedText = text.replaceAll("\\s+", " ");
        int difference = text.length() - processedText.length();
        return currentOffset - difference;
    }

    private Container getEndContainer(Range range) {
        Node endNode = range.getEndContainer();
        int endOffset = range.getEndOffset();
        endOffset = computeNewOffset(endNode, endOffset);
        return new Container(xpathUtil.getXPath(endNode), endOffset);
    }

}