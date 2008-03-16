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
 * $Id$
 */

package org.nuxeo.ecm.webapp.filemanager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Remove;
import javax.ejb.Stateless;
import javax.faces.application.FacesMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.myfaces.trinidad.model.UploadedFile;
import org.jboss.annotation.ejb.SerializedConcurrentAccess;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.core.Events;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.platform.ejb.EJBExceptionHandler;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.ecm.platform.filemanager.api.FileManagerPermissionException;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.platform.publishing.api.PublishActions;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.ecm.platform.ui.web.api.UserAction;
import org.nuxeo.ecm.platform.ui.web.util.files.FileUtils;

import org.nuxeo.ecm.webapp.base.InputController;
import org.nuxeo.ecm.webapp.clipboard.ClipboardActions;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.runtime.api.Framework;

import sun.misc.BASE64Decoder;

/**
 * @author <a href="mailto:andreas.kalogeropoulos@nuxeo.com">Andreas
 *         Kalogeropoulos</a>
 *
 */
@Stateless
@Name("FileManageActions")
@SerializedConcurrentAccess
@Local(FileManageActionsLocal.class)
@Remote(FileManageActions.class)
public class FileManageActionsBean extends InputController implements
        FileManageActionsLocal {

    private static final Log log = LogFactory.getLog(FileManageActionsBean.class);

    private static final String TRANSF_ERROR = "TRANSF_ERROR";

    private static final String MOVE_ERROR = "MOVE_ERROR";

    private static final String COPY_ERROR = "COPY_ERROR";

    private static final String PASTE_ERROR = "PASTE_ERROR";

    private static final String MOVE_IMPOSSIBLE = "MOVE_IMPOSSIBLE";

    private static final String MOVE_PUBLISH = "MOVE_PUBLISH";

    private static final String MOVE_OK = "MOVE_OK";

    protected UploadedFile fileUpload;

    @In(create = true)
    protected CoreSession documentManager;

    @In(required = true)
    protected TypeManager typeManager;

    @In(create = true)
    protected ClipboardActions clipboardActions;

    @In(create = true)
    protected PublishActions publishActions;

    protected FileManager fileManager;

    protected FileManager getFileManagerService() throws ClientException {
        if (fileManager == null) {
            try {
                fileManager = Framework.getService(FileManager.class);
            } catch (Exception e) {
                log.error("Unable to get FileManager service " + e.getMessage());
                throw new ClientException("Unable to get FileManager service "
                        + e.getMessage());
            }
        }
        return fileManager;
    }

    @Remove
    public void destroy() {
        log.debug("Removing SEAM action listener...");
    }

    public String display() {
        return "view_documents";
    }

    public String addFile() throws ClientException {
        try {
            if (fileUpload == null) {
                facesMessages.add(FacesMessage.SEVERITY_ERROR,
                        resourcesAccessor.getMessages().get(
                                "fileImporter.error.nullUploadedFile"));
                return navigationContext.getActionResult(
                        navigationContext.getCurrentDocument(),
                        UserAction.AFTER_CREATE);
            }
            DocumentModel currentDocument = navigationContext.getCurrentDocument();
            String path = currentDocument.getPathAsString();
            Blob blob = FileUtils.fetchContent(fileUpload);

            DocumentModel createdDoc = getFileManagerService().createDocumentFromBlob(
                    documentManager, blob, path, true, fileUpload.getFilename());
            eventManager.raiseEventsOnDocumentSelected(createdDoc);

            facesMessages.add(FacesMessage.SEVERITY_INFO,
                    resourcesAccessor.getMessages().get("document_saved"),
                    resourcesAccessor.getMessages().get(createdDoc.getType()));
            return navigationContext.getActionResult(createdDoc,
                    UserAction.AFTER_CREATE);

        } catch (Throwable t) {
            throw EJBExceptionHandler.wrapException(t);
        }
    }

    @Deprecated
    // TODO: update the Seam remoting-based desktop plugins to stop calling this
    // method
    public boolean canWrite() throws ClientException {
        // let the FolderImporter and FileImporter plugin handle the security
        // checks to avoid hardcoded behavior
        return true;
    }

    public String addFileFromPlugin(String content, String mimetype,
            String fullName, String morePath, Boolean UseBase64)
            throws ClientException {
        try {
            byte[] bcontent;
            if (UseBase64.booleanValue()) {
                BASE64Decoder decoder = new BASE64Decoder();
                bcontent = decoder.decodeBuffer(content);
            } else {
                bcontent = content.getBytes();
            }
            return addBinaryFileFromPlugin(bcontent, mimetype, fullName,
                    morePath);
        } catch (Throwable t) {
            log.error(t);
            //Rux INA-224 simple patch for the moment, until full i18n into Exceptions
            return TRANSF_ERROR + " |(" + fullName + ")| "
                    + resourcesAccessor.getMessages().get("message.operation.fails.generic");
        }
    }

    public String addBinaryFileFromPlugin(byte[] content, String mimetype,
            String fullName, String morePath) throws ClientException {
        try {
            DocumentModel currentDocument = navigationContext.getCurrentDocument();
            String curPath = currentDocument.getPathAsString();

            // compute the path of the target container
            if (!currentDocument.isFolder()) {
                curPath = curPath.substring(0, curPath.lastIndexOf("/"));
            }
            String path = curPath + morePath;

            // wrap the content into a streaming blob and use the mimetype
            // service to update the right
            Blob blob = StreamingBlob.createFromByteArray(content, null);
            MimetypeRegistry mimeService = Framework.getService(MimetypeRegistry.class);
            mimetype = mimeService.getMimetypeFromFilenameAndBlobWithDefault(
                    fullName, blob, mimetype);
            blob.setMimeType(mimetype);
            DocumentModel createdDoc;
            try {
                createdDoc = getFileManagerService().createDocumentFromBlob(
                        documentManager, blob, path, true, fullName);
            } catch (FileManagerPermissionException e) {
                // security check failed
                //Rux INA-224 simple patch for the moment, until full i18n into Exceptions
                log.error("No permissions creating " + fullName);
                return TRANSF_ERROR + " |(" + fullName + ")| "
                        + resourcesAccessor.getMessages().get("message.operation.fails.generic");
            }

            if (createdDoc == null) {
                //Rux INA-224 simple patch for the moment, until full i18n into Exceptions
                log.error("Couldn't create the document " + fullName);
                return TRANSF_ERROR + " |(" + fullName + ")| "
                        + resourcesAccessor.getMessages().get("message.operation.fails.generic");
            }

            // update the context, raise events and return the next page
            if (currentDocument.getRef().equals(createdDoc.getRef())) {
                // contextManager.updateContext(createdDoc);
                navigationContext.updateDocumentContext(createdDoc);
            }
            Events.instance().raiseEvent(EventNames.DOCUMENT_CHILDREN_CHANGED,
                    currentDocument);
            eventManager.raiseEventsOnDocumentSelected(createdDoc);
            return createdDoc.getName();
        } catch (Throwable t) {
            //Rux INA-224 simple patch for the moment, until full i18n into Exceptions
            log.error(t);
            return TRANSF_ERROR + " |(" + fullName + ")| "
                    + resourcesAccessor.getMessages().get("message.operation.fails.generic");
        }
    }

    public String addBinaryFile(byte[] content, String mimetype,
            String fullName, DocumentRef docRef) throws ClientException {
        try {
            DocumentModel targetContainer = documentManager.getDocument(docRef);

            String path = targetContainer.getPathAsString();
            Blob blob = StreamingBlob.createFromByteArray(content, mimetype);

            MimetypeRegistry mimeService = Framework.getService(MimetypeRegistry.class);
            mimetype = mimeService.getMimetypeFromFilenameAndBlobWithDefault(
                    fullName, blob, mimetype);
            blob.setMimeType(mimetype);

            DocumentModel createdDoc = getFileManagerService().createDocumentFromBlob(
                    documentManager, blob, path, true, fullName);
            return createdDoc.getName();
        } catch (Throwable t) {
            //Rux INA-224 simple patch for the moment, until full i18n into Exceptions
            log.error(t);
            return TRANSF_ERROR + " |(" + fullName + ")| "
                    + resourcesAccessor.getMessages().get("message.operation.fails.generic");
        }
    }

    public String addFolderFromPlugin(String fullName, String morePath)
            throws ClientException {
        try {
            DocumentModel currentDocument = navigationContext.getCurrentDocument();

            String curPath = currentDocument.getPathAsString();
            if (!currentDocument.isFolder()) {
                curPath = curPath.substring(0, curPath.lastIndexOf("/"));
            }
            String path = curPath + morePath;

            DocumentModel createdDoc;
            try {
                createdDoc = getFileManagerService().createFolder(
                        documentManager, fullName, path);
            } catch (FileManagerPermissionException e) {
                //Rux INA-224 simple patch for the moment, until full i18n into Exceptions
                log.error("No permissions creating folder " + fullName);
                return TRANSF_ERROR + " |(" + fullName + ")| "
                        + resourcesAccessor.getMessages().get("message.operation.fails.generic");
            }

            if (createdDoc == null) {
                //Rux INA-224 simple patch for the moment, until full i18n into Exceptions
                log.error("Couldn't create the folder " + fullName);
                return TRANSF_ERROR + " |(" + fullName + ")| "
                        + resourcesAccessor.getMessages().get("message.operation.fails.generic");
            }

            eventManager.raiseEventsOnDocumentSelected(createdDoc);
            Events.instance().raiseEvent(EventNames.DOCUMENT_CHILDREN_CHANGED,
                    currentDocument);
            return createdDoc.getName();
        } catch (Throwable t) {
            //Rux INA-224 simple patch for the moment, until full i18n into Exceptions
            log.error(t);
            return TRANSF_ERROR + " |(" + fullName + ")| "
                    + resourcesAccessor.getMessages().get("message.operation.fails.generic");
        }
    }

    // TODO: this method is weird! What is it doing?
    public String delCopyWithId(String docId) throws ClientException {
        try {
            String debug = "deleting copyId " + docId;
            if (docId.startsWith("pasteRef_")) {
                docId = docId.split("pasteRef_")[1];
            }
            // XXX - TD : fix that, is it used ????
            // DocumentModel srcDoc = documentManager.getDocument(new
            // IdRef(docId));
            // removeDocumentFromList(clipboard.getClipboardDocuments(),
            // srcDoc);
            log.debug(debug);
            return debug;
        } catch (Throwable t) {
            //Rux INA-224 simple patch for the moment, until full i18n into Exceptions
            log.error(t);
            return COPY_ERROR + " |(" + docId + ")| "
                    + resourcesAccessor.getMessages().get("message.operation.fails.generic");
        }
    }

    private String checkMoveAllowed(DocumentRef docRef, DocumentRef containerRef)
            throws ClientException {

        DocumentModel doc = documentManager.getDocument(docRef);
        DocumentModel container = documentManager.getDocument(containerRef);

        if (container.getType().equals("Section")
                && !doc.getType().equals("Section")) {
            // We try to do a publication
            // check read in sections
            if (!documentManager.hasPermission(containerRef,
                    SecurityConstants.READ)) {
                // This should never append since user can only drop in visible
                // sections
                facesMessages.add(FacesMessage.SEVERITY_WARN,
                        resourcesAccessor.getMessages().get(
                                "move_insuffisant_rights"));
                return MOVE_IMPOSSIBLE;
            }

            // check write in source folder :
            if (!documentManager.hasPermission(doc.getParentRef(),
                    SecurityConstants.WRITE)) {
                // This should never append since user can only drop in visible
                // sections
                facesMessages.add(FacesMessage.SEVERITY_WARN,
                        resourcesAccessor.getMessages().get(
                                "move_insuffisant_rights"));
                return MOVE_IMPOSSIBLE;
            }

            if (doc.isProxy()) {
                // if this is a proxy, simply move it
                return MOVE_OK;
            }

            if (doc.hasFacet(FacetNames.PUBLISHABLE)) {
                return MOVE_PUBLISH;
            } else {
                facesMessages.add(FacesMessage.SEVERITY_WARN,
                        resourcesAccessor.getMessages().get(
                                "publish_impossible"));
                return MOVE_IMPOSSIBLE;
            }

        }

        // do not allow to move a published document back in a workspace
        if (doc.isProxy() && !container.getType().equals("Section")) {
            facesMessages.add(FacesMessage.SEVERITY_WARN,
                    resourcesAccessor.getMessages().get("move_impossible"));
            return MOVE_IMPOSSIBLE;
        }

        if (!documentManager.hasPermission(containerRef,
                SecurityConstants.ADD_CHILDREN)) {
            facesMessages.add(FacesMessage.SEVERITY_WARN,
                    resourcesAccessor.getMessages().get(
                            "move_insuffisant_rights"));
            return MOVE_IMPOSSIBLE;
        }

        List<String> allowedTypes = Arrays.asList(typeManager.getType(
                container.getType()).getAllowedSubTypes());
        if (!allowedTypes.contains(doc.getType())) {
            facesMessages.add(FacesMessage.SEVERITY_WARN,
                    resourcesAccessor.getMessages().get("move_impossible"));
            return MOVE_IMPOSSIBLE;
        }

        return MOVE_OK;
    }

    public String moveWithId(String docId, String containerId)
            throws ClientException {
        try {
            String debug = "move " + docId + " into " + containerId;
            log.debug(debug);
            if (docId.startsWith("docRef:")) {
                docId = docId.split("docRef:")[1];
            }
            if (docId.startsWith("docClipboardRef:")) {
                docId = docId.split("docClipboardRef:")[1];
            }
            DocumentRef srcRef = new IdRef(docId);
            String dst = containerId;
            if (dst.startsWith("docRef:")) {
                dst = dst.split("docRef:")[1];
            }
            if (dst.startsWith("nodeRef:")) {
                dst = dst.split("nodeRef:")[1];
            }
            DocumentRef dstRef = new IdRef(dst);

            String moveStatus = checkMoveAllowed(srcRef, dstRef);

            if (moveStatus.equals(MOVE_IMPOSSIBLE)) {
                return debug;
            }

            if (moveStatus.equals(MOVE_PUBLISH)) {
                DocumentModel doc = documentManager.getDocument(srcRef);
                DocumentModel container = documentManager.getDocument(dstRef);
                if (publishActions==null) {
                    return debug;
                }
                publishActions.publishDocument(doc, container);
                facesMessages.add(FacesMessage.SEVERITY_INFO,
                        resourcesAccessor.getMessages().get(
                                "document_published"),
                        resourcesAccessor.getMessages().get(doc.getType()));

                documentManager.save();
                return debug;
            }

            documentManager.move(srcRef, dstRef, null);
            // delCopyWithId(docId);
            documentManager.save();
            DocumentModel currentDocument = navigationContext.getCurrentDocument();
            eventManager.raiseEventsOnDocumentChildrenChange(currentDocument);

            // notify current container
            Events.instance().raiseEvent(EventNames.DOCUMENT_CHILDREN_CHANGED,
                    currentDocument);
            // notify the other container
            DocumentModel otherContainer = documentManager.getDocument(dstRef);
            Events.instance().raiseEvent(EventNames.DOCUMENT_CHILDREN_CHANGED,
                    otherContainer);

            facesMessages.add(FacesMessage.SEVERITY_INFO,
                    resourcesAccessor.getMessages().get("document_moved"),
                    resourcesAccessor.getMessages().get(
                            documentManager.getDocument(srcRef).getType()));

            return debug;
        } catch (Throwable t) {
            //Rux INA-224 simple patch for the moment, until full i18n into Exceptions
            log.error(t);
            return MOVE_ERROR + " |(" + docId + ")| "
                    + resourcesAccessor.getMessages().get("message.operation.fails.generic");
        }
    }

    public String copyWithId(String docId) throws ClientException {
        try {
            String debug = "copying " + docId;
            log.debug(debug);
            if (docId.startsWith("docRef:")) {
                docId = docId.split("docRef:")[1];
            }
            if (docId.startsWith("docClipboardRef:")) {
                docId = docId.split("docClipboardRef:")[1];
            }
            DocumentRef srcRef = new IdRef(docId);
            DocumentModel srcDoc = documentManager.getDocument(srcRef);
            List<DocumentModel> docsToAdd = new ArrayList<DocumentModel>();
            docsToAdd.add(srcDoc);
            clipboardActions.putSelectionInWorkList(docsToAdd, true);
            return debug;
        } catch (Throwable t) {
            //Rux INA-224 simple patch for the moment, until full i18n into Exceptions
            log.error(t);
            return COPY_ERROR + " |(" + docId + ")| "
                    + resourcesAccessor.getMessages().get("message.operation.fails.generic");
        }
    }

    public String pasteWithId(String docId) throws ClientException {
        try {
            String debug = "pasting " + docId;
            log.debug(debug);
            if (docId.startsWith("pasteRef_")) {
                docId = docId.split("pasteRef_")[1];
            }
            if (docId.startsWith("docClipboardRef:")) {
                docId = docId.split("docClipboardRef:")[1];
            }
            DocumentRef srcRef = new IdRef(docId);
            DocumentModel srcDoc = documentManager.getDocument(srcRef);
            List<DocumentModel> pasteDocs = new ArrayList<DocumentModel>();
            pasteDocs.add(srcDoc);
            clipboardActions.pasteDocumentList(pasteDocs);
            return debug;
        } catch (Throwable t) {
            //Rux INA-224 simple patch for the moment, until full i18n into Exceptions
            log.error(t);
            return PASTE_ERROR + " |(" + docId + ")| "
                    + resourcesAccessor.getMessages().get("message.operation.fails.generic");
        }
    }

    public void initialize() {
        log.info("Initializing...");
    }

    public UploadedFile getFileUpload() {
        return fileUpload;
    }

    public void setFileUpload(UploadedFile fileUpload) {
        this.fileUpload = fileUpload;
    }

    public DocumentModel getChangeableDocument() {
        return navigationContext.getChangeableDocument();
    }

    public void setChangeableDocument(DocumentModel changeableDocument) {
        navigationContext.setChangeableDocument(changeableDocument);
    }

}
