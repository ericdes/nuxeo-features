/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: DocumentViewCodecService.java 22535 2007-07-13 14:57:58Z atchertchian $
 */

package org.nuxeo.ecm.platform.url.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;
import org.nuxeo.ecm.platform.url.codec.api.DocumentViewCodec;
import org.nuxeo.ecm.platform.url.codec.descriptor.DocumentViewCodecDescriptor;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class DocumentViewCodecService extends DefaultComponent implements
        DocumentViewCodecManager {

    private static final long serialVersionUID = -4521897334653742494L;

    public static final String NAME = DocumentViewCodecService.class.getName();

    public static final String CODECS_EXTENSION_POINT = "codecs";

    protected String defaultCodecName;

    protected final Map<String, DocumentViewCodecDescriptor> descriptors;

    protected final Map<String, DocumentViewCodec> codecs;

    public DocumentViewCodecService() {
        descriptors = new HashMap<String, DocumentViewCodecDescriptor>();
        codecs = new HashMap<String, DocumentViewCodec>();
    }

    @Override
    public void deactivate(ComponentContext context) {
        descriptors.clear();
        codecs.clear();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(DocumentViewCodecManager.class)) {
            return (T) this;
        }
        return null;
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        if (CODECS_EXTENSION_POINT.equals(extensionPoint)) {
            DocumentViewCodecDescriptor codecDesc = (DocumentViewCodecDescriptor) contribution;
            String codecName = codecDesc.getName();
            descriptors.put(codecName, codecDesc);
            if (codecDesc.getDefaultCodec()) {
                defaultCodecName = codecName;
            }
            codecs.remove(codecName);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        if (CODECS_EXTENSION_POINT.equals(extensionPoint)) {
            DocumentViewCodecDescriptor codecDesc = (DocumentViewCodecDescriptor) contribution;
            String codecName = codecDesc.getName();
            descriptors.remove(codecName);
            codecs.remove(codecName);
        }
    }

    public List<String> getDocumentViewCodecDescriptorNames() {
        List<String> lst = new ArrayList<String>();
        for (String k : descriptors.keySet()) {
            if (descriptors.get(k).getEnabled()) {
                lst.add(k);
            }
        }
        return lst;
    }

    public String getDefaultCodecName() {
        String name = defaultCodecName;
        if (name == null) {
            // take first one
            List<String> descs = getDocumentViewCodecDescriptorNames();
            if (descs != null && !descs.isEmpty()) {
                Collections.sort(descs);
                name = descs.get(0);
            }
        }
        return name;
    }

    public DocumentViewCodec getCodec() {
        return getCodec(defaultCodecName);
    }

    public DocumentViewCodec getCodec(String codecName) {
        DocumentViewCodec codec = codecs.get(codecName);
        if (codec == null) {
            // try to instanciate it
            DocumentViewCodecDescriptor desc = descriptors.get(codecName);
            if (desc == null) {
                throw new IllegalArgumentException("Unknown codec " + codecName);
            }
            String className = desc.getClassName();
            if (className == null) {
                throw new IllegalArgumentException("Invalid class for codec "
                        + codecName);
            }
            try {
                // Thread context loader is not working in isolated EARs
                codec = (DocumentViewCodec) DocumentViewCodecManager.class.getClassLoader().loadClass(
                        className).newInstance();
            } catch (Exception e) {
                String msg = String.format(
                        "Caught error when instantiating codec %s with class %s ",
                        codecName, className);
                throw new IllegalArgumentException(msg, e);
            }
            String prefix = desc.getPrefix();
            if (prefix != null) {
                codec.setPrefix(prefix);
            }
            codecs.put(codecName, codec);
        }
        return codec;
    }

    public String getUrlFromDocumentView(DocumentView docView,
            boolean needBaseUrl, String baseUrl) {
        String codecName = getDefaultCodecName();
        String url = getUrlFromDocumentView(codecName, docView, needBaseUrl,
                baseUrl);
        if (url == null) {
            for (String name : descriptors.keySet()) {
                url = getUrlFromDocumentView(name, docView, needBaseUrl,
                        baseUrl);
                if (url != null) {
                    break;
                }
            }
        }
        return url;
    }

    public String getUrlFromDocumentView(String codecName,
            DocumentView docView, boolean needBaseUrl, String baseUrl) {
        DocumentViewCodec codec = getCodec(codecName);
        if (codec.handleDocumentView(docView)) {
            String partialUrl = codec.getUrlFromDocumentView(docView);
            if (needBaseUrl && baseUrl != null) {
                return baseUrl + partialUrl;
            } else {
                return partialUrl;
            }
        }
        return null;
    }

    public DocumentView getDocumentViewFromUrl(String url, boolean hasBaseUrl,
            String baseUrl) {
        String codecName = getDefaultCodecName();
        DocumentView docView = getDocumentViewFromUrl(codecName, url,
                hasBaseUrl, baseUrl);
        if (docView == null) {
            for (String name : descriptors.keySet()) {
                docView = getDocumentViewFromUrl(name, url, hasBaseUrl, baseUrl);
                if (docView != null) {
                    break;
                }
            }
        }
        return docView;
    }

    public DocumentView getDocumentViewFromUrl(String codecName, String url,
            boolean hasBaseUrl, String baseUrl) {
        if (hasBaseUrl && baseUrl != null) {
            if (url.startsWith(baseUrl)) {
                url = url.substring(baseUrl.length());
            }
        }
        DocumentViewCodec codec = getCodec(codecName);
        DocumentView docView = null;
        if (codec.handleUrl(url)) {
            docView = codec.getDocumentViewFromUrl(url);
        }
        return docView;
    }

}
