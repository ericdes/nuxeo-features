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

package org.nuxeo.ecm.platform.picture.web;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.ejb.Remove;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.annotations.remoting.WebRemote;
import org.jboss.seam.annotations.web.RequestParameter;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.platform.picture.api.adapters.PictureResourceAdapter;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.UserAction;
import org.nuxeo.ecm.platform.ui.web.rest.RestHelper;
import org.nuxeo.ecm.platform.ui.web.tag.fn.DocumentModelFunctions;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.codec.DocumentFileCodec;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.nuxeo.ecm.webapp.base.InputController;
import org.nuxeo.ecm.webapp.helpers.EventNames;

/**
 * @author <a href="mailto:ldoguin@nuxeo.com">Laurent Doguin</a>
 *
 */
@Name("pictureManager")
@Scope(CONVERSATION)
@Transactional
public class PictureManagerBean extends InputController implements
        PictureManager, Serializable {

    private static final long serialVersionUID = -7323791279190937921L;

    private static final Log log = LogFactory.getLog(PictureManagerBean.class);

    @In(create = true, required = false)
    private transient CoreSession documentManager;

    @RequestParameter
    private String fileFieldFullName;

    @In(required = true, create = true)
    private transient NavigationContext navigationContext;

    String fileurlPicture;

    String filename;

    Blob fileContent;

    Integer index;

    String cropCoords;

    ArrayList<Map<String, Object>> selectItems;

    @Create
    public void initialize() throws Exception {
        log.debug("Initializing...");
        index = 0;
    }

    protected DocumentModel getCurrentDocument() {
        return navigationContext.getCurrentDocument();
    }

    public String getFileurlPicture() throws ClientException {
        ArrayList<Map<String, Object>> views = (ArrayList) getCurrentDocument().getProperty(
                "picture", "views");
        return views.get(index).get("title") + ":content";
    }

    public void setFileurlPicture(String fileurlPicture) {
        this.fileurlPicture = fileurlPicture;
    }

    private void initSelectItems() throws ClientException {
        selectItems = new ArrayList<Map<String, Object>>();
        DocumentModel doc = getCurrentDocument();
        ArrayList<Map<String, Object>> views = (ArrayList) doc.getProperty(
                "picture", "views");
        for (int i = 0; i < views.size(); i++) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("title", views.get(i).get("title"));
            map.put("idx", i);
            selectItems.add(map);
        }
    }

    public ArrayList getSelectItems() throws ClientException {
        if (selectItems == null) {
            initSelectItems();
            return selectItems;
        } else {
            return selectItems;
        }
    }

    public void setSelectItems(ArrayList selectItems) {
        this.selectItems = selectItems;
    }

    public String addPicture() throws Exception {
        DocumentModel doc = navigationContext.getChangeableDocument();

        String parentPath;
        if (getCurrentDocument() == null) {
            // creating item at the root
            parentPath = documentManager.getRootDocument().getPathAsString();
        } else {
            parentPath = navigationContext.getCurrentDocument().getPathAsString();
        }

        String title = (String) doc.getProperty("dublincore", "title");
        if (title == null) {
            title = "";
        }
        String name = IdUtils.generateId(title);
        // set parent path and name for document model
        doc.setPathInfo(parentPath, name);
        try {
            DocumentModel parent = getCurrentDocument();
            ArrayList<Map<String, Object>> pictureTemplates = null;
            if (parent.getType().equals("PictureBook")) {
                // Use PictureBook Properties
                pictureTemplates = (ArrayList<Map<String, Object>>) parent.getProperty(
                        "picturebook", "picturetemplates");
            }
            PictureResourceAdapter picture = doc.getAdapter(PictureResourceAdapter.class);
            Boolean status = picture.createPicture(fileContent, filename,
                    title, pictureTemplates);
            if (!status) {
                documentManager.cancel();
                log.info("Picture type unsupported.");
                FacesMessage message = FacesMessages.createFacesMessage(
                        FacesMessage.SEVERITY_ERROR,
                        resourcesAccessor.getMessages().get(
                                "label.picture.upload.error"));
                FacesMessages.instance().add(message);

                return navigationContext.getActionResult(
                        navigationContext.getCurrentDocument(), UserAction.VIEW);
            } else {
                doc = documentManager.createDocument(doc);
                documentManager.saveDocument(doc);
                documentManager.save();
            }
        } catch (Exception e) {
            log.error("Picture Creation failed", e);
            documentManager.cancel();
            FacesMessage message = FacesMessages.createFacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    resourcesAccessor.getMessages().get(
                            "label.picture.upload.error"));
            FacesMessages.instance().add(message);
            return navigationContext.getActionResult(
                    navigationContext.getCurrentDocument(), UserAction.VIEW);
        }
        return navigationContext.getActionResult(doc, UserAction.AFTER_CREATE);
    }

    public String rotate90left() throws ClientException, IOException {
        DocumentModel doc = getCurrentDocument();
        PictureResourceAdapter picture = doc.getAdapter(PictureResourceAdapter.class);
        picture.doRotate(-90);
        documentManager.saveDocument(doc);
        documentManager.save();
        navigationContext.setCurrentDocument(doc);
        return null;
    }

    public String rotate90right() throws ClientException, IOException {
        DocumentModel doc = getCurrentDocument();
        PictureResourceAdapter picture = doc.getAdapter(PictureResourceAdapter.class);
        picture.doRotate(90);
        documentManager.saveDocument(doc);
        documentManager.save();
        navigationContext.setCurrentDocument(doc);
        return null;
    }

    public String crop() throws ClientException, IOException {
        if (cropCoords != null && !cropCoords.equals("")) {
            DocumentModel doc = getCurrentDocument();
            PictureResourceAdapter picture = doc.getAdapter(PictureResourceAdapter.class);
            picture.doCrop(cropCoords);
            documentManager.saveDocument(doc);
            documentManager.save();
            navigationContext.setCurrentDocument(doc);
        }
        return null;
    }

    @Observer(value = { EventNames.DOCUMENT_SELECTION_CHANGED,
            EventNames.DOCUMENT_CHANGED })
    public void resetFields() {
        filename = "";
        fileContent = null;
        selectItems = null;
    }

    @WebRemote
    public String remoteDownload(String patternName, String docID,
            String blobPropertyName, String filename) throws ClientException {
        IdRef docref = new IdRef(docID);
        DocumentModel doc = documentManager.getDocument(docref);
        return DocumentModelFunctions.fileUrl(patternName, doc,
                blobPropertyName, filename);
    }

    @WebRemote
    public static String urlPopup(String url) {
        return RestHelper.addCurrentConversationParameters(url);
    }

    public void download(DocumentView docView) throws ClientException {
        if (docView != null) {
            DocumentLocation docLoc = docView.getDocumentLocation();
            // fix for NXP-1799
            if (documentManager == null) {
                RepositoryLocation loc = new RepositoryLocation(
                        docLoc.getServerName());
                navigationContext.setCurrentServerLocation(loc);
                documentManager = navigationContext.getOrCreateDocumentManager();
            }
            DocumentModel doc = documentManager.getDocument(docLoc.getDocRef());
            if (doc != null) {
                String[] propertyPath = docView.getParameter(
                        DocumentFileCodec.FILE_PROPERTY_PATH_KEY).split(":");
                String title = null;
                String field = null;
                Property datamodel = null;
                if (propertyPath.length == 2) {
                    title = propertyPath[0];
                    field = propertyPath[1];
                    datamodel = doc.getProperty("picture:views");
                } else if (propertyPath.length == 3) {
                    String schema = propertyPath[0];
                    title = propertyPath[1];
                    field = propertyPath[2];
                    datamodel = doc.getProperty(schema + ":" + "views");
                }
                Property view = null;
                for (Property property : datamodel) {
                    if (property.get("title").getValue().equals(title)) {
                        view = property;
                    }
                }

                if (view == null) {
                    for (Property property : datamodel) {
                        if (property.get("title").getValue().equals("Thumbnail")) {
                            view = property;
                        }
                    }
                }
                Blob blob = (Blob) view.getValue(field);
                String filename = (String) view.getValue("filename");
                // download
                FacesContext context = FacesContext.getCurrentInstance();

                ComponentUtils.download(context, blob, filename);
            }
        }
    }

    @Destroy
    @Remove
    public void destroy() {
        log.debug("Removing Seam action listener...");
        fileurlPicture = null;
        filename = null;
        fileContent = null;
        index = null;
        selectItems = null;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Blob getFileContent() {
        return fileContent;
    }

    public void setFileContent(Blob fileContent) {
        this.fileContent = fileContent;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public String getCropCoords() {
        return cropCoords;
    }

    public void setCropCoords(String cropCoords) {
        this.cropCoords = cropCoords;
    }
}
