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
 *     Nuxeo - initial API and implementation
 *
 * $Id: DocumentIdCodec.java 22535 2007-07-13 14:57:58Z atchertchian $
 */

package org.nuxeo.ecm.platform.url.codec;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.platform.url.DocumentLocationImpl;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentLocation;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.codec.api.DocumentViewCodec;
import org.nuxeo.ecm.platform.url.service.AbstractDocumentViewCodec;

/**
 * Codec handling a document repository, path, view and additional request
 * parameters.
 *
 * @author Anahide Tchertchian
 */
public class DocumentPathCodec extends AbstractDocumentViewCodec {

    public static final String PREFIX = "nxpath";

    // nxpath/server/path/to/doc@view_id/?requestParams
    public static final String URLPattern
            = "/([\\w\\.]+)(/([\\w/\\-\\.]*))?(@([\\w\\-\\.]+))(/)?(\\?(.*)?)?";

    public DocumentPathCodec() {
    }

    public DocumentPathCodec(String prefix) {
    }

    @Override
    public String getPrefix() {
        if (prefix != null) {
            return prefix;
        }
        return PREFIX;
    }

    public String getUrlFromDocumentView(DocumentView docView) {
        DocumentLocation docLoc = docView.getDocumentLocation();
        if (docLoc != null) {
            List<String> items = new ArrayList<String>();
            items.add(getPrefix());
            items.add(docLoc.getServerName());
            PathRef docRef = docLoc.getPathRef();
            if (docRef == null) {
                return null;
            }
            // this is a path, get rid of leading slash
            String path = docRef.toString();
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            if (path.length() > 0) {
                items.add(path);
            }
            String uri = StringUtils.join(items, "/");
            String viewId = docView.getViewId();
            if (viewId != null) {
                uri += "@" + viewId;
            }
            return URIUtils.addParametersToURIQuery(uri,
                    docView.getParameters());
        }
        return null;
    }

    /**
     * Extracts document location from a Zope-like URL, eg:
     * server/path_or_docId/view_id/tab_id .
     */
    public DocumentView getDocumentViewFromUrl(String url) {
        final Pattern pattern = Pattern.compile(getPrefix() + URLPattern);
        Matcher m = pattern.matcher(url);
        if (m.matches()) {

            final String server = m.group(1);
            String path = m.group(3);
            if (path != null) {
                // add leading slash to make it absolute if it's not the root
                path = "/" + path;
            } else {
                path = "/";
            }
            final DocumentRef docRef = new PathRef(path);
            final String viewId = m.group(5);

            // get other parameters

            Map<String, String> params = null;
            if (m.groupCount() > 7) {
                String query = m.group(8);
                params = URIUtils.getRequestParameters(query);
            }

            final DocumentLocation docLoc = new DocumentLocationImpl(server,
                    docRef);

            return new DocumentViewImpl(docLoc, viewId, params);
        }

        return null;
    }

}
