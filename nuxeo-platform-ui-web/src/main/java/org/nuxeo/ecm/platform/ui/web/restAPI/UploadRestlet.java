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

package org.nuxeo.ecm.platform.ui.web.restAPI;

import static org.jboss.seam.ScopeType.EVENT;

import java.util.List;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;
import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMDocumentFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.SimpleFileManager;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.ext.fileupload.RestletFileUpload;
import org.restlet.resource.Representation;

/**
 * Restlet to import files as nuxeo documents using the pluggable FileManager
 * service. This restlet is mainly used for desktop integration with drag and
 * drop browser plugins.
 *
 * @author tdelprat
 */
@Name("uploadRestlet")
@Scope(EVENT)
public class UploadRestlet extends BaseNuxeoRestlet {

    private static final Log log = LogFactory.getLog(UploadRestlet.class);

    @In(create = true)
    protected NavigationContext navigationContext;

    protected CoreSession documentManager;

    @In(create = true)
    protected SimpleFileManager FileManageActions;

    @Override
    public void handle(Request req, Response res) {
        String repo = (String) req.getAttributes().get("repo");
        String docid = (String) req.getAttributes().get("docid");
        String fileName = (String) req.getAttributes().get("filename");

        DOMDocumentFactory domfactory = new DOMDocumentFactory();
        DOMDocument result = (DOMDocument) domfactory.createDocument();

        DocumentModel targetContainer;
        try {
            navigationContext.setCurrentServerLocation(new RepositoryLocation(
                    repo));
            documentManager = navigationContext.getOrCreateDocumentManager();
            targetContainer = documentManager.getDocument(new IdRef(docid));
        } catch (Exception e) {
            handleError(res, e);
            return;
        }

        if (targetContainer != null) {
            Representation repr = req.getEntity();
            RestletFileUpload fu = new RestletFileUpload();
            List<FileItem> fiList = null;
            try {
                fiList = fu.parseRequest(req);
            } catch (FileUploadException e) {
                // handleError(res, e);
                // return;
                // XXX : this fails for requests not sent via browser
            }
            if (fiList == null) {
                // mono import
                String outcome;
                try {
                    Blob inputBlob = StreamingBlob.createFromStream(
                            repr.getStream()).persist();
                    inputBlob.setFilename(fileName);
                    outcome = FileManageActions.addBinaryFileFromPlugin(
                            inputBlob, fileName, targetContainer);
                } catch (Exception e) {
                    outcome = "ERROR : " + e.getMessage();
                }
                result.addElement("upload").setText(outcome);
            } else {
                // multiple file upload
                Element uploads = result.addElement("uploads");
                for (FileItem fileItem : fiList) {
                    String outcome;
                    try {
                        Blob inputBlob = StreamingBlob.createFromStream(
                                fileItem.getInputStream()).persist();
                        inputBlob.setFilename(fileItem.getName());
                        outcome = FileManageActions.addBinaryFileFromPlugin(
                                inputBlob, fileItem.getName(), targetContainer);
                    } catch (Exception e) {
                        log.error("error importing " + fileItem.getName()
                                + ": " + e.getMessage(), e);
                        outcome = "ERROR : " + e.getMessage();
                    }
                    uploads.addElement("upload").setText(outcome);
                }
            }
        }
        res.setEntity(result.asXML(), MediaType.TEXT_XML);
    }
}
