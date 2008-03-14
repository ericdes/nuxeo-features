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

package org.nuxeo.ecm.webapp.contentbrowser;

import static org.jboss.seam.ScopeType.SESSION;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.ejb.Remove;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.RequestParameter;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.datamodel.DataModel;
import org.jboss.seam.contexts.Context;
import org.nuxeo.ecm.core.api.AlreadyConnectedException;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.ejb.EJBExceptionHandler;
import org.nuxeo.ecm.platform.interfaces.ejb.ECContentRoot;
import org.nuxeo.ecm.platform.ui.web.api.UserAction;
import org.nuxeo.ecm.platform.util.ECInvalidParameterException;
import org.nuxeo.ecm.webapp.base.InputController;
import org.nuxeo.ecm.webapp.table.cell.AbstractTableCell;
import org.nuxeo.ecm.webapp.table.cell.DateTableCell;
import org.nuxeo.ecm.webapp.table.cell.DocModelTableCell;
import org.nuxeo.ecm.webapp.table.cell.IconTableCell;
import org.nuxeo.ecm.webapp.table.cell.SelectionTableCell;
import org.nuxeo.ecm.webapp.table.cell.TableCell;
import org.nuxeo.ecm.webapp.table.header.CheckBoxColHeader;
import org.nuxeo.ecm.webapp.table.header.DocModelColHeader;
import org.nuxeo.ecm.webapp.table.header.IconColHeader;
import org.nuxeo.ecm.webapp.table.header.TableColHeader;
import org.nuxeo.ecm.webapp.table.model.DocModelTableModel;
import org.nuxeo.ecm.webapp.table.row.DocModelTableRow;
import org.nuxeo.ecm.webapp.table.row.TableRow;

/**
 * Action listener that deals with operations with the content roots of a
 * domain.
 *
 * @author <a href="mailto:npaslaru@nuxeo.com">Narcis Paslaru</a>
 *
 */

@Name("contentRootsActions")
@Scope(SESSION)
@Deprecated
public class ContentRootsActionsBean extends InputController implements
        ContentRootsActions {
    private static final Log log = LogFactory.getLog(ContentRootsActionsBean.class);

    @In(create = true)
    CoreSession documentManager;

    @RequestParameter("tabParam")
    String tabParam;

    @In(required = false)
    DocumentModel currentDomain;

    @In(required = false)
    DocumentModel changeableDocument;

    @In
    protected transient Context sessionContext;

    @In(required = true, create = true)
    protected ECContentRoot ecContentRoot;

    DocumentModel currentItem;

    // @DataModel("contentRootChildrenList")
    List<DocumentModel> workspacesChildren = null;

    // @DataModel("contentRootChildrenList")
    List<DocumentModel> sectionsChildren = null;

    protected DocModelTableModel workspacesTableModel;

    protected DocModelTableModel sectionsTableModel;

    protected Blob logo;

    protected DocumentModel logoHolder;

    @Create
    public void initialize() {
        sessionContext.set("useTemplateFlag", true);
        logo = null;
    }


    @Destroy
    @Remove
    public void destroy() {
        log.debug("Removing Seam action listener...");
    }

    public String display() {
        try {
            // return onFinishSelectingItem(getContentRootWithType(tabParam));
            final DocumentModel doc = getContentRootWithType(tabParam);
            return navigationContext.getActionResult(doc, UserAction.VIEW);
        } catch (Throwable t) {
            // TODO throw exceptionHandler.wrapException(t);
            return null;
        }
    }

    public String selectWorkspace() throws ClientException {
        try {
            // return onFinishSelectingItem(getWorkspacesTableModel()
            // .getSelectedDocModel());
            final DocumentModel doc = getWorkspacesTableModel()
                    .getSelectedDocModel();
            return navigationContext.getActionResult(doc, UserAction.VIEW);
        } catch (Throwable t) {
            throw EJBExceptionHandler.wrapException(t);
        }
    }

    public String selectSection() throws ClientException {
        try {
            // return onFinishSelectingItem(getWorkspacesTableModel()
            // .getSelectedDocModel());
            final DocumentModel doc = getWorkspacesTableModel()
                    .getSelectedDocModel();
            return navigationContext.getActionResult(doc, UserAction.VIEW);
        } catch (Throwable t) {
            throw EJBExceptionHandler.wrapException(t);
        }
    }

    // TODO a editSections method is alse necessary
    public String editWorkspace() throws AlreadyConnectedException,
            ClientException, ECInvalidParameterException {
        sessionContext.set("changeableDocument",
                getWorkspacesTableModel().getSelectedDocModel());

        return "edit_workspace";
    }

    public String updateWorkspace() throws ClientException {
        try {
            documentManager.saveDocument(changeableDocument);

            documentManager.save();

            // return super.onFinishEditingItem(changeableDocument);
            final DocumentModel doc = changeableDocument;
            return navigationContext
                    .getActionResult(doc, UserAction.AFTER_EDIT);
        } catch (Throwable t) {
            throw EJBExceptionHandler.wrapException(t);
        }
    }

    // @Observer( { EventNames.DOMAIN_SELECTION_CHANGED,
    //         EventNames.CONTENT_ROOT_SELECTION_CHANGED })
    public void resetTableModel() {
        workspacesTableModel = null;
        sectionsTableModel = null;
    }

    /**
     * Lazy getter to return the list of workspace type documents.
     *
     * @return
     * @throws AlreadyConnectedException
     * @throws ClientException
     * @throws ECInvalidParameterException
     */
    protected List<DocumentModel> getWorkspacesChildren()
            throws AlreadyConnectedException, ClientException,
            ECInvalidParameterException {
        if (null == workspacesChildren) {
            getWorkspaces();
        }

        return workspacesChildren;
    }

    /**
     * Lazy getter to return the list of section type documents.
     *
     * @return
     * @throws AlreadyConnectedException
     * @throws ClientException
     * @throws ECInvalidParameterException
     */
    protected List<DocumentModel> getSectionsChildren()
            throws AlreadyConnectedException, ClientException,
            ECInvalidParameterException {
        if (null == sectionsChildren) {
            getSections();
        }

        return sectionsChildren;
    }

    // @Observer( { EventNames.DOMAIN_SELECTION_CHANGED })
    // @Factory("contentRootChildrenList")
    public void getWorkspaces() throws ClientException,
            AlreadyConnectedException, ECInvalidParameterException {
        try {
            String selectedTab = "";
            // we display workspaces first
            DocumentModel dc = (DocumentModel) sessionContext
                    .get("contentRootDocument");
            if (null == dc) {
                selectedTab = "Workspace";
            } else {
                selectedTab = (String) dc.getProperty("dublincore",
                        "description");
            }

            workspacesChildren = ecContentRoot.getContentRootChildren(
                    selectedTab, currentDomain.getRef(), documentManager);

            log.debug("Retrieved workspace type children for domain: "
                    + currentDomain + " " + workspacesChildren);
        } catch (Throwable t) {
            throw EJBExceptionHandler.wrapException(t);
        }
    }

    // @Observer( { EventNames.DOMAIN_SELECTION_CHANGED })
    // @Factory("contentRootChildrenList")
    public void getSections() throws ClientException,
            AlreadyConnectedException, ECInvalidParameterException {
        try {
            String selectedTab = "";
            // we display workspaces first
            DocumentModel dc = (DocumentModel) sessionContext
                    .get("contentRootDocument");
            if (null == dc) {
                selectedTab = "Section";
            } else {
                selectedTab = (String) dc.getProperty("dublincore",
                        "description");
            }

            sectionsChildren = ecContentRoot.getContentRootChildren(
                    selectedTab, currentDomain.getRef(), documentManager);

            log.debug("Retrieved workspace type children for domain: "
                    + currentDomain + " " + sectionsChildren);
        } catch (Throwable t) {
            throw EJBExceptionHandler.wrapException(t);
        }
    }

    @DataModel("contentRootDocumentList")
    protected List<DocumentModel> contentRootDocuments;

    // @Observer( { EventNames.DOMAIN_SELECTION_CHANGED })
    public void resetContentRootDocuments() {
        contentRootDocuments = null;
    }

    protected List<DocumentModel> getContentRoots()
            throws AlreadyConnectedException, ClientException {
        if (null == contentRootDocuments) {
            getContentRootDocuments();
        }

        return contentRootDocuments;
    }

    // @Observer( { EventNames.DOMAIN_SELECTION_CHANGED })
    // @Factory("contentRootDocumentList")
    public List<DocumentModel> getContentRootDocuments()
            throws ClientException, AlreadyConnectedException {
        try {
            contentRootDocuments = ecContentRoot.getContentRootDocuments(
                    currentDomain.getRef(), documentManager);

            logDocumentWithTitle("Retrieving content roots for domain: ",
                    currentDomain);

            return contentRootDocuments;
        } catch (Throwable t) {
            throw EJBExceptionHandler.wrapException(t);
        }
    }

    /**
     * Returns the type of documents that should be under this content root doc.
     *
     * @param type
     * @return
     * @throws ClientException
     * @throws AlreadyConnectedException
     */
    protected DocumentModel getContentRootWithType(String type)
            throws AlreadyConnectedException, ClientException {
        for (DocumentModel rightDocModel : getContentRoots()) {
            if (type.equalsIgnoreCase((String) rightDocModel.getProperty(
                    "dublincore", "description"))) {
                return rightDocModel;
            }
        }
        return null;
    }

    @Deprecated
    public Boolean getCanAddWorkspaces() throws ClientException {
        return documentManager.hasPermission(currentDomain.getRef(),
                SecurityConstants.ADD_CHILDREN);
    }

    public String cancel() {
        return "view_workspaces";
    }

    @PrePassivate
    public void saveState() {
        log.info("PrePassivate");
    }

    @PostActivate
    public void readState() {
        log.info("PostActivate");
    }

    // Clipboardable interface

    public boolean getCanCopy() throws ClientException {
        try {
            return (!getWorkspacesTableModel().getSelectedRows().isEmpty()
                        || !getSectionsTableModel().getSelectedRows().isEmpty())
                    && currentItem.isFolder();
        } catch (Exception e) {
            throw EJBExceptionHandler.wrapException(e);
        }
    }

    public List<DocumentModel> copy() throws ClientException {
        try {
            List<DocumentModel> selectedDocs = new ArrayList<DocumentModel>();
            selectedDocs.addAll(getWorkspacesTableModel().getSelectedDocs());
            selectedDocs.addAll(getSectionsTableModel().getSelectedDocs());
            return selectedDocs;
        } catch (ECInvalidParameterException e) {
            throw EJBExceptionHandler.wrapException(e);
        }
    }

    public void removeDocumentFromList(DocumentModel doc) {
        removeDocumentFromList(workspacesChildren, doc);
    }

    public DocModelTableModel reconstructWorkspacesTableModel()
            throws ClientException, ECInvalidParameterException {
        List<TableColHeader> headers = new ArrayList<TableColHeader>();

        TableColHeader header = new CheckBoxColHeader(
                "label.content.header.selection", "c0", false);
        headers.add(header);

        header = new IconColHeader("", "", "c1");
        headers.add(header);

        header = new DocModelColHeader("label.content.header.title", "c2");
        headers.add(header);

        header = new TableColHeader("label.content.header.modified", "c4");
        headers.add(header);

        header = new TableColHeader("label.content.header.author", "c5");
        headers.add(header);

        getWorkspaces();

        List<TableRow> rows = new ArrayList<TableRow>();

        for (DocumentModel doc : workspacesChildren) {
            rows.add(createDataTableRow(doc));
        }

        if (workspacesTableModel != null) {
            cacheUpdateNotifier.removeCacheListener(workspacesTableModel);
        }

        workspacesTableModel = new DocModelTableModel(headers, rows);

        workspacesTableModel.setSort("label.content.header.title");
        cacheUpdateNotifier.addCacheListener(workspacesTableModel);

        return workspacesTableModel;
    }

    public DocModelTableModel reconstructSectionsTableModel()
            throws ClientException, ECInvalidParameterException {
        List<TableColHeader> headers = new ArrayList<TableColHeader>();

        TableColHeader header = new CheckBoxColHeader(
                "label.content.header.selection", "c0", false);
        headers.add(header);

        header = new IconColHeader("", "", "c1");
        headers.add(header);

        header = new DocModelColHeader("label.content.header.title", "c2");
        headers.add(header);

        header = new TableColHeader("label.content.header.modified", "c4");
        headers.add(header);

        header = new TableColHeader("label.content.header.author", "c5");
        headers.add(header);

        getSections();

        List<TableRow> rows = new ArrayList<TableRow>();

        for (DocumentModel doc : sectionsChildren) {
            rows.add(createDataTableRow(doc));
        }

        if (workspacesTableModel != null) {
            cacheUpdateNotifier.removeCacheListener(sectionsTableModel);
        }

        sectionsTableModel = new DocModelTableModel(headers, rows);

        sectionsTableModel.setSort("label.content.header.title");
        cacheUpdateNotifier.addCacheListener(sectionsTableModel);

        return sectionsTableModel;
    }

    public DocModelTableModel getWorkspacesTableModel()
            throws AlreadyConnectedException, ClientException,
            ECInvalidParameterException {
        if (null == workspacesTableModel) {
            workspacesTableModel = reconstructWorkspacesTableModel();
        }

        return workspacesTableModel;
    }

    public DocModelTableModel getSectionsTableModel()
            throws AlreadyConnectedException, ClientException,
            ECInvalidParameterException {
        if (null == sectionsTableModel) {
            sectionsTableModel = reconstructSectionsTableModel();
        }

        return sectionsTableModel;
    }

    /**
     * Creates a custom table row based on a {@link DocumentModel}.
     *
     * @param doc
     * @return
     * @throws ClientException
     */
    protected DocModelTableRow createDataTableRow(DocumentModel doc)
            throws ClientException {

        List<AbstractTableCell> cells = new ArrayList<AbstractTableCell>();

        // selection
        cells.add(new SelectionTableCell(false));

        // icon
        String iconPath = (String) doc.getProperty("common", "icon");
        if (iconPath == null || iconPath.length() == 0) {
            iconPath = typeManager.getType(doc.getType()).getIcon();
        }
        cells.add(new IconTableCell(iconPath, "", "docRef:" + doc.getRef(),
                doc.isFolder()));

        // title
        cells.add(new DocModelTableCell(doc, "dublincore", "title"));

        // modification date
        Calendar modified = (Calendar) doc.getProperty("dublincore", "modified");
        DateFormat df = new SimpleDateFormat("dd/MM/yyyy (HH:mm)");
        if (modified != null) {
            Date modifiedDate = modified.getTime();
            String modifiedStr = df.format(modifiedDate);
            cells.add(new DateTableCell(modifiedStr, modifiedDate));
        } else {
            cells.add(new TableCell(""));
        }

        // author
        String[] contributors = (String[]) doc.getProperty("dublincore",
                "contributors");
        String author = "";

        if (null != contributors) {
            author = contributors[0];
        }
        cells.add(new TableCell(author));

        DocModelTableRow row = new DocModelTableRow(doc, cells);

        return row;
    }

    public void selectAllRows(boolean checked)
            throws AlreadyConnectedException, ClientException,
            ECInvalidParameterException {
        getWorkspacesTableModel().selectAllRows(checked);
        getSectionsTableModel().selectAllRows(checked);
    }
}