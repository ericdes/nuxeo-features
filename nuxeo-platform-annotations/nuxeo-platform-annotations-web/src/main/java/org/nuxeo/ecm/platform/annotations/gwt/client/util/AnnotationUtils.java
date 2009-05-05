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
 *     troger
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.gwt.client.util;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 *
 */
public class AnnotationUtils {
    private AnnotationUtils() {
        // Helper class
    }

    public static String escapeHtml(String maybeHtml) {
        final Element div = DOM.createDiv();
        DOM.setInnerText(div, maybeHtml);
        String escapedHtml = DOM.getInnerHTML(div).replaceAll("<BR>", "\n")
            // IE keep '&nbsp;' which will raise an error in the RDF backend
            .replaceAll("&nbsp;", " ");
        return escapedHtml;
    }

    public static String replaceCarriageReturns(String text) {
        return text.replaceAll("\n", "<br/>");
    }

}
