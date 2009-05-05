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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.syndication.serializer;

import java.util.*;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;
import org.nuxeo.runtime.api.Framework;
import org.restlet.data.MediaType;
import org.restlet.data.Response;

public abstract class AbstractDocumentModelSerializer implements
        DocumentModelListSerializer {

    public String serialize(DocumentModelList docList,
            List<String> columnsDefinition, HttpServletRequest req) throws ClientException {
        return serialize(null, docList, columnsDefinition, req);
    }

    public String serialize(ResultSummary summary, DocumentModelList docList,
            List<String> columnsDefinition, HttpServletRequest req) throws ClientException {
        return null;
    }

    public String serialize(DocumentModelList docList, String columnsDefinition, HttpServletRequest req) throws ClientException {
        if (columnsDefinition == null) {
            return serialize(docList, new ArrayList<String>(), req);
        } else {
            String[] cols = columnsDefinition.split(colDefinitonDelimiter);
            return serialize(docList, Arrays.asList(cols), req);
        }
    }

    public String serialize(ResultSummary summary, DocumentModelList docList,
            String columnsDefinition, HttpServletRequest req) throws ClientException {
        if (columnsDefinition == null) {
            return serialize(summary, docList, new ArrayList<String>(), req);
        } else {
            String[] cols = columnsDefinition.split(colDefinitonDelimiter);
            return serialize(summary, docList, Arrays.asList(cols), req);
        }
    }

    public void serialize(DocumentModelList docList, String columnsDefinition,
            Response res, HttpServletRequest req) throws ClientException {
        String xml = serialize(docList, columnsDefinition, req);
        res.setEntity(xml, MediaType.TEXT_XML);
    }

    public void serialize(ResultSummary summary, DocumentModelList docList,
            String columnsDefinition, Response res, HttpServletRequest req) throws ClientException {
        String xml = serialize(summary, docList, columnsDefinition, req);
        res.setEntity(xml, MediaType.TEXT_XML);
    }

    @SuppressWarnings("unchecked")
    protected ResultField getDocumentProperty(DocumentModel doc, String colDef) throws ClientException {
        ResultField res = null;
        if (colDef.equals(urlField)) {
            String url = getDocumentURL(doc);
            return new ResultField(urlField, url);
        } else if (colDef.contains(pathField)) {
            String path = doc.getPath().toString();
            return new ResultField(pathField, path);
        } else if (colDef.contains(authorField)) {
            List<Object> list = Arrays.asList((Object[]) doc.getProperty(
                    "dublincore", "contributors"));
            return new ResultField(authorField, (String) list.get(0));
        } else if (colDef.contains(SchemaDelimiter)) {
            String[] params = colDef.split("\\" + SchemaDelimiter);

            if (params.length == 2) {
                String schemaName = params[0];
                String fieldName = params[1];
                // We want to get an item from a list
                // Should not be used, generates malformed XML caused by
                // mylist[X]=...
                if (fieldName.contains(listIndex)) {
                    int pos = fieldName.indexOf(listIndex);
                    String sIndex = fieldName.substring(pos + 1, pos + 2);
                    String realFieldName = fieldName.substring(0, pos);
                    List<Object> list = Arrays.asList((Object[]) doc.getProperty(
                            schemaName, realFieldName));
                    if (list != null && !list.isEmpty()) {
                        return new ResultField(fieldName,
                                (String) list.get(Integer.parseInt(sIndex)));
                    }
                    return null;
                } else {

                    Object property = doc.getProperty(schemaName, fieldName);
                    if (property instanceof String) {
                        res = new ResultField(fieldName,
                                (String) doc.getProperty(schemaName, fieldName));
                    } else if (property instanceof Calendar) {
                        String date = DATE_PARSER.format(((Calendar) doc.getProperty(
                                schemaName, fieldName)).getTime());
                        res = new ResultField(fieldName, date);
                    } else if (property instanceof Object[]) {
                        List<Object> list = Arrays.asList((Object[]) doc.getProperty(
                                schemaName, fieldName));
                        res = new ResultField(fieldName, list.toString());
                    } else if (property instanceof List) {
                        List<Object> list = (List) doc.getProperty(schemaName,
                                fieldName);
                        res = new ResultField(fieldName, list.toString());
                    } else {
                        res = new ResultField(fieldName, null);
                    }

                }

            } else {
                res = new ResultField(colDef, null);
            }
        } else {
            String result = null;
            for (String schemaName : doc.getDeclaredSchemas()) {
                result = (String) doc.getProperty(schemaName, colDef);
                if (result != null) {
                    continue;
                }
            }
            res = new ResultField(colDef, result);
        }
        return res;
    }

    protected static String getDocumentURL(DocumentModel doc) {
        DocumentViewCodecManager dvcm;
        try {
            dvcm = Framework.getService(DocumentViewCodecManager.class);
        } catch (Exception e) {
            return null;
        }
        return dvcm.getUrlFromDocumentView(new DocumentViewImpl(doc),
                false, null);
    }

}
