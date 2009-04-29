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

package org.nuxeo.ecm.platform.annotations.gwt.client.util;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.IFrameElement;
import com.google.gwt.dom.client.Node;

/**
 * @author Alexandre Russel
 *
 */
public class Utils {
    public native static String getSelectedText(IFrameElement iframe)/*-{
        if(iframe.contentDocument.defaultView.getSelection()) {
          return iframe.contentDocument.defaultView.getSelection().toString();
        } else if(iframe.getSelection) {
          return iframe.getSelection();
        } else if(iframe.selection) {
          return iframe.selection.createRange().innerHTML;
        }
    }-*/;
    public native static void removeSelection(IFrameElement iframe)/*-{
        if(iframe.contentDocument.defaultView.getSelection()) {
          iframe.contentDocument.defaultView.getSelection().removeAllRanges();
        } else if(iframe.selection) {
          iframe.selection.empty();
        }
    }-*/;

    /**
     * offset for FF is the number of character from point click to previous
     * node.
     *
     * @param iframe
     * @return
     */
    public native static int getStartOffset(IFrameElement iframe)/*-{
        // to add
        //if (!window.getSelection) window.getSelection = function() { return document.selection.createRange().text; };
        if( iframe &&
            iframe.contentDocument &&
            iframe.contentDocument.defaultView &&
            iframe.contentDocument.defaultView.getSelection() &&
            iframe.contentDocument.defaultView.getSelection().getRangeAt(0) &&
            iframe.contentDocument.defaultView.getSelection().getRangeAt(0).startOffset) {
            return iframe.contentDocument.defaultView.getSelection().getRangeAt(0).startOffset;
        } else if(iframe.getSelection && iframe.getSelection()) {
            return iframe.getSelection();
        } else if(iframe.selection) {
            return iframe.selection.createRange().startOffset;
        }
        return 0;
    }-*/;

    public native static Node getStartContainer(IFrameElement iframe)/*-{
        if( iframe &&
            iframe.contentDocument &&
            iframe.contentDocument.defaultView &&
            iframe.contentDocument.defaultView.getSelection() &&
            iframe.contentDocument.defaultView.getSelection().getRangeAt(0) &&
            iframe.contentDocument.defaultView.getSelection().getRangeAt(0).startContainer) {
            return iframe.contentDocument.defaultView.getSelection().getRangeAt(0).startContainer;
        }
        return null;
    }-*/;

    public native static Node getEndContainer(IFrameElement iframe)/*-{
        if( iframe &&
            iframe.contentDocument &&
            iframe.contentDocument.defaultView &&
            iframe.contentDocument.defaultView.getSelection() &&
            iframe.contentDocument.defaultView.getSelection().getRangeAt(0) &&
            iframe.contentDocument.defaultView.getSelection().getRangeAt(0).endContainer) {
             return iframe.contentDocument.defaultView.getSelection().getRangeAt(0).endContainer;
            }
        return null;
    }-*/;

    public native static int getEndOffset(IFrameElement iframe)/*-{
        if( iframe &&
            iframe.contentDocument &&
            iframe.contentDocument.defaultView &&
            iframe.contentDocument.defaultView.getSelection() &&
            iframe.contentDocument.defaultView.getSelection().getRangeAt(0) &&
            iframe.contentDocument.defaultView.getSelection().getRangeAt(0).endOffset) {
            return iframe.contentDocument.defaultView.getSelection().getRangeAt(0).endOffset;
        } else if(iframe.getSelection && iframe.getSelection()) {
            return iframe.getSelection();
        } else if(iframe.selection) {
            return iframe.selection.createRange().endOffset;
        }
        return 0;
    }-*/;

    public native static Document setDocument(Document document) /*-{
        $temp = $doc;
        $doc = document;
        return $temp
    }-*/;

    public static int[] getAbsoluteTopLeft(Element element, Document document) {
        int[] result = new int[2];
        Document doc = Utils.setDocument(document);
        result[0] = element.getAbsoluteTop();
        result[1] = element.getAbsoluteLeft();
        Utils.setDocument(doc);
        return result;
    }

    public native static String getBaseHref() /*-{
        return top['baseHref'];
    }-*/;


    public native static Range getCurrentRange(Document document) /*-{
        if( document &&
            document.defaultView &&
            document.defaultView.getSelection() &&
            document.defaultView.getSelection().getRangeAt(0)) {
            // W3C Range
            var userSelection = document.defaultView.getSelection().getRangeAt(0);
            var range = @org.nuxeo.ecm.platform.annotations.gwt.client.util.Range::new(Ljava/lang/String;Lcom/google/gwt/dom/client/Node;ILcom/google/gwt/dom/client/Node;I)(userSelection.toString(), userSelection.startContainer, userSelection.startOffset, userSelection.endContainer, userSelection.endOffset);
            return range;
        } else if(document.selection) {
            // IE TextRange
            var ieSelection = document.selection.createRange();
            var ieRange = new $wnd.InternetExplorerRange(ieSelection);
            ieRange._init();
            var range = @org.nuxeo.ecm.platform.annotations.gwt.client.util.Range::new(Ljava/lang/String;Lcom/google/gwt/dom/client/Node;ILcom/google/gwt/dom/client/Node;I)(ieSelection.text, ieRange.startContainer, ieRange.startOffset, ieRange.endContainer, ieRange.endOffset);
            return range;
        }
        return null;
    }-*/;
}