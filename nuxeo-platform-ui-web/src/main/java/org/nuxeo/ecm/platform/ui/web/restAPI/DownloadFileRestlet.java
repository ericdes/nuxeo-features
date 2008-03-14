package org.nuxeo.ecm.platform.ui.web.restAPI;

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

import static org.jboss.seam.ScopeType.EVENT;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.http.HttpServletResponse;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.tag.fn.LiveEditConstants;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.OutputRepresentation;

/**
 * Restlet to help LiveEdit clients download the blob content of a document
 * 
 * @author Sun Tan <stan@nuxeo.com>
 * @author Olivier Grisel <ogrisel@nuxeo.com>
 */
@Name("downloadFileRestlet")
@Scope(EVENT)
public class DownloadFileRestlet extends BaseNuxeoRestlet implements
        LiveEditConstants {

    @In(create = true)
    protected NavigationContext navigationContext;

    protected CoreSession documentManager;

    @Override
    public void handle(Request req, Response res) {

        String repo = (String) req.getAttributes().get("repo");
        if (repo == null || repo.equals("*")) {
            handleError(res, "you must specify a repository");
            return;
        }

        DocumentModel dm = null;
        try {
            navigationContext.setCurrentServerLocation(new RepositoryLocation(
                    repo));
            documentManager = navigationContext.getOrCreateDocumentManager();
            String docid = (String) req.getAttributes().get("docid");
            if (docid != null) {
                dm = documentManager.getDocument(new IdRef(docid));
            } else {
                handleError(res, "you must specify a valid document IdRef");
                return;
            }
        } catch (ClientException e) {
            handleError(res, e);
            return;
        }
        String schemaName = getQueryParamValue(req, SCHEMA, DEFAULT_SCHEMA);
        String blobFieldName = getQueryParamValue(req, BLOB_FIELD,
                DEFAULT_BLOB_FIELD);
        String filenameFieldName = getQueryParamValue(req, FILENAME_FIELD,
                DEFAULT_FILENAME_FIELD);

        try {

            final String filename = (String) dm.getProperty(schemaName,
                    filenameFieldName);
            final Blob blob = (Blob) dm.getProperty(schemaName, blobFieldName);

            final File tempfile = File.createTempFile(
                    "nuxeo-downloadrestlet-tmp", "");
            blob.transferTo(tempfile);
            res.setEntity(new OutputRepresentation(null) {
                @Override
                public void write(OutputStream outputStream) throws IOException {
                    // the write call happens after the seam conversation is
                    // finished which will garbage collect the CoreSession
                    // instance, hence we store the blob content in a temporary
                    // file
                    FileInputStream instream = new FileInputStream(tempfile);
                    FileUtils.copy(instream, outputStream);
                    instream.close();
                    tempfile.delete();
                    blob.transferTo(outputStream);
                }
            });
            HttpServletResponse response = getHttpResponse(res);

            response.setHeader("Content-Disposition", "attachment; filename=\""
                    + filename + "\";");
            // TODO: add mimetype here too

        } catch (Exception e) {
            handleError(res, e);
        }

    }

}