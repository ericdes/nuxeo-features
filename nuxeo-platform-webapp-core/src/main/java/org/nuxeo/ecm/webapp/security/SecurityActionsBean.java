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

package org.nuxeo.ecm.webapp.security;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.io.Serializable;

import javax.annotation.security.PermitAll;
import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.ejb.Remove;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.FacesMessages;
import org.nuxeo.common.utils.i18n.Labeler;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.PermissionProvider;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.UserEntry;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.platform.ejb.EJBExceptionHandler;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.util.ECInvalidParameterException;
import org.nuxeo.ecm.webapp.base.InputController;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.ecm.webapp.table.cell.AbstractTableCell;
import org.nuxeo.ecm.webapp.table.cell.IconTableCell;
import org.nuxeo.ecm.webapp.table.cell.PermissionsTableCell;
import org.nuxeo.ecm.webapp.table.cell.SelectionTableCell;
import org.nuxeo.ecm.webapp.table.cell.UserTableCell;
import org.nuxeo.ecm.webapp.table.header.CheckBoxColHeader;
import org.nuxeo.ecm.webapp.table.header.TableColHeader;
import org.nuxeo.ecm.webapp.table.model.UserPermissionsTableModel;
import org.nuxeo.ecm.webapp.table.row.UserPermissionsTableRow;
import org.nuxeo.runtime.api.Framework;

/**
 * Provides security related methods.
 *
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 */
@Name("securityActions")
@Scope(CONVERSATION)
public class SecurityActionsBean extends InputController implements
        SecurityActions, Serializable {

    private static final long serialVersionUID = -7190826911734958662L;

    // XXX temporary
    protected static final String ADMIN_GROUP = "administrators";

    protected static final String[] SEED_PERMISSIONS_TO_CHECK = {SecurityConstants.WRITE_SECURITY, SecurityConstants.READ_SECURITY};

    protected String[] CACHED_PERMISSION_TO_CHECK = null;

    private static final Log log = LogFactory.getLog(SecurityActionsBean.class);

    private static final Labeler labeler = new Labeler(
            "label.security.permission");

    protected SecurityData securityData;

    protected boolean obsoleteSecurityData = true;

    protected UserPermissionsTableModel tableModel;

    protected List<String> selectedUsers;

    protected transient List<String> cachedValidatedUserAndGroups;

    protected transient List<String> cachedDeletedUserAndGroups;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected PermissionActionListManager permissionActionListManager;

    @In(create = true)
    protected PermissionListManager permissionListManager;

    @In(create = true)
    protected PrincipalListManager principalListManager;

    @In(create = true)
    protected transient UserManager userManager;

    @In(create = true)
    protected NuxeoPrincipal currentUser;

    private Boolean blockRightInheritance;


    @Observer(value=EventNames.USER_ALL_DOCUMENT_TYPES_SELECTION_CHANGED, create=false, inject=false)
    public void resetSecurityData() throws ClientException {
        obsoleteSecurityData = true;
        blockRightInheritance = null;
    }

    public void rebuildSecurityData() throws ClientException {
        try {
            if (null != currentDocument) {
                if (null == securityData) {
                    securityData = new SecurityData();
                }
                ACP acp = documentManager.getACP(currentDocument.getRef());

                if (null != acp) {
                    new SecurityDataConverter().convertToSecurityData(acp,
                            securityData);
                } else {
                    securityData.clear();
                }

                reconstructTableModel();
                if (blockRightInheritance == null) {
                    blockRightInheritance = false;
                }
                obsoleteSecurityData = false;
            }
        } catch (Throwable t) {
            throw EJBExceptionHandler.wrapException(t);
        }
    }

    @Destroy
    @Remove
    @PermitAll
    public void destroy() {
        log.debug("Removing SEAM action listener...");
    }

    /**
     * @return update the dataTableModel from the current {@link SecurityData}
     *         this method is automatically called by rebuildSecurityData method
     *
     * @throws ClientException
     * @throws ECInvalidParameterException
     */
    protected UserPermissionsTableModel reconstructTableModel()
            throws ClientException {
        List<TableColHeader> headers = new ArrayList<TableColHeader>();

        TableColHeader header = new CheckBoxColHeader(
                "label.content.header.selection", "c0", false);
        headers.add(header);

        header = new TableColHeader("label.content.header.type", "c4");
        headers.add(header);

        header = new TableColHeader("label.username", "c1");
        headers.add(header);

        header = new TableColHeader("label.security.grantedPerms", "c2");
        headers.add(header);

        header = new TableColHeader("label.security.deniedPerms", "c3");
        headers.add(header);

        List<UserPermissionsTableRow> rows = new ArrayList<UserPermissionsTableRow>();

        /*
         * This is a fix for NXP-1122 issue (ghost users in access rights
         * lists): for the parent document users list and current document users
         * list are now used the new getters defined in this class, not the ones
         * defined in SecurityData. The best solution would be to remove de user
         * from the ACP/ACE entries too at the deletion moment. When this will
         * be done, the getters from SecurityData will have to be used again
         * (here and in documents_rights.xhtml)
         */
        // for (String user : securityData.getCurrentDocumentUsers()) {
        for (String user : getCurrentDocumentUsers()) {
            UserPermissionsTableRow row = createDataTableRow(user);
            if (row != null) {
                rows.add(row);
            }
        }

        tableModel = new UserPermissionsTableModel(headers, rows);

        return tableModel;
    }

    protected UserPermissionsTableRow createDataTableRow(String user)
            throws ClientException {

        List<AbstractTableCell> cells = new ArrayList<AbstractTableCell>();

        if (user.equals(SecurityConstants.EVERYONE)
                && securityData.getCurrentDocDeny().get(user).contains(
                        SecurityConstants.EVERYTHING)) {
            // remove DENY EveryThing to EveryOne from display list
            blockRightInheritance = true;
            return null;
        }

        cells.add(new SelectionTableCell(false));

        String principalType = principalListManager.getPrincipalType(user);
        IconTableCell iconTableCell = new IconTableCell(getIconPathMap().get(
                principalType));
        iconTableCell.setIconAlt(getIconAltMap().get(principalType));
        cells.add(iconTableCell);

        cells.add(new UserTableCell(user, principalType));

        cells.add(new PermissionsTableCell(user,
                securityData.getCurrentDocGrant().get(user)));

        cells.add(new PermissionsTableCell(user,
                securityData.getCurrentDocDeny().get(user)));

        return new UserPermissionsTableRow(user, cells);
    }

    public UserPermissionsTableModel getDataTableModel()
            throws ClientException, ECInvalidParameterException {
        if (obsoleteSecurityData) {
            // lazy initialization at first time access
            rebuildSecurityData();
        }

        return tableModel;
    }

    public SecurityData getSecurityData() throws ClientException {
        if (obsoleteSecurityData) {
            // lazy initialization at first time access
            rebuildSecurityData();
        }
        return securityData;
    }

    public String updateSecurityOnDocument() throws ClientException {
        try {
            List<UserEntry> modifiableEntries = new SecurityDataConverter().convertToUserEntries(securityData);
            ACP acp = currentDocument.getACP();

            if (null == acp) {
                acp = new ACPImpl();
            }

            acp.setRules(modifiableEntries.toArray(new UserEntry[0]));

            documentManager.setACP(currentDocument.getRef(), acp, true);
            documentManager.save();

            // Reread data from the backend to be sure the current bean
            // state is uptodate w.r.t. the real backend state
            rebuildSecurityData();

            // Type currentType =
            // typeManager.getType(currentDocument.getType());
            // return applicationController
            // .getPageOnEditedDocumentType(currentType);

            // Forward to default view, that's not what we want
            // return navigationContext.getActionResult(currentDocument,
            // UserAction.AFTER_EDIT);

            // Temporary fix, to avoid forward to default_view.
            // The same page is reloaded after submit.
            // May use UserAction, with new kind of action (AFTER_EDIT_RIGHTS) ?
            return null;

        } catch (Throwable t) {
            throw EJBExceptionHandler.wrapException(t);
        }
    }

    public String addPermission(String principalName, String permissionName,
            boolean grant) {
        if (securityData == null) {
            try {
                securityData = getSecurityData();
            } catch (ClientException e) {
                log.error(e); // TODO raise me instead
                return null;
            }
        }
        securityData.addModifiablePrivilege(principalName, permissionName,
                grant);
        // remove the opposite rule if any
        securityData.removeModifiablePrivilege(principalName, permissionName,
                !grant);
        return null;
    }

    public String addPermission() {
        String principalName = principalListManager.getSelectedPrincipal();
        String permissionName = permissionListManager.getSelectedPermission();
        boolean grant = permissionActionListManager.getSelectedGrant().equals(
                "Grant");
        return addPermission(principalName, permissionName, grant);
    }

    public String addPermissions() {
        List<String> principalsName = principalListManager.getSelectedUsers();
        String permissionName = permissionListManager.getSelectedPermission();
        boolean grant = permissionActionListManager.getSelectedGrant().equals(
                "Grant");

        for (String principalName : principalsName) {
            addPermission(principalName, permissionName, grant);
        }
        // principalListManager.resetSelectedUserList();
        return null;
    }

    public String addPermissionAndUpdate() throws ClientException {
        addPermission();
        updateSecurityOnDocument();
        // do not redirect to the default folder view
        return null;
    }

    public String addPermissionsAndUpdate() throws ClientException {
        if (principalListManager.getSelectedUserListEmpty()) {
            String message = ComponentUtils.translate(
                    FacesContext.getCurrentInstance(),
                    "error.rightsManager.noUsersSelected");
            FacesMessages.instance().add(message);
            return null;
        }
        addPermissions();
        updateSecurityOnDocument();
        // do not redirect to the default folder view
        principalListManager.resetSelectedUserList();

        facesMessages.add(FacesMessage.SEVERITY_INFO,
                resourcesAccessor.getMessages().get("message.updated.rights"));

        return null;
    }

    public String removePermission() {
        securityData.removeModifiablePrivilege(
                principalListManager.getSelectedPrincipal(),
                permissionListManager.getSelectedPermission(),
                permissionActionListManager.getSelectedGrant().equals("Grant"));

        return null;
    }

    public String removePermissionAndUpdate() throws ClientException {
        removePermission();

        if (!checkPermissions()) {
            facesMessages.add(FacesMessage.SEVERITY_ERROR,
                    resourcesAccessor.getMessages().get("message.updated.rights"));
            return null;
        }

        updateSecurityOnDocument();
        // do not redirect to the default folder view
        return null;
    }

    public String removePermissionsAndUpdate() throws ClientException,
            ECInvalidParameterException {
        for (String user : getDataTableModel().getSelectedUsers()) {
            securityData.removeModifiablePrivilege(user);
            if (!checkPermissions()) {
                facesMessages.add(FacesMessage.SEVERITY_ERROR,
                        resourcesAccessor.getMessages().get("message.error.removeRight"));
                return null;
            }
        }
        updateSecurityOnDocument();
        facesMessages.add(FacesMessage.SEVERITY_INFO,
                resourcesAccessor.getMessages().get("message.updated.rights"));
        // do not redirect to the default folder view
        return null;
    }

    public boolean getCanAddSecurityRules() throws ClientException {
        return documentManager.hasPermission(currentDocument.getRef(),
                "WriteSecurity");
    }

    public boolean getCanRemoveSecurityRules() throws ClientException {
        try {
            return documentManager.hasPermission(currentDocument.getRef(),
                    "WriteSecurity")
                    && !getDataTableModel().getSelectedRows().isEmpty();
        } catch (Exception e) {
            throw EJBExceptionHandler.wrapException(e);
        }
    }

    public List<SelectItem> getSettablePermissions() throws ClientException {
        String documentType = navigationContext.getCurrentDocument().getType();

        // BBB: use the platform service if it defines permissions (deprecated)
        UIPermissionService service = (UIPermissionService) Framework.getRuntime().getComponent(
                UIPermissionService.NAME);
        String[] settablePermissions = service.getUIPermissions(documentType);

        if (settablePermissions == null || settablePermissions.length == 0) {
            // new centralized permission provider at the core level
            try {
                PermissionProvider pservice = Framework.getService(PermissionProvider.class);
                settablePermissions = pservice.getUserVisiblePermissions(documentType);
            } catch (Exception e) {
                throw new ClientException(e);
            }
        }

        List<SelectItem> permissions = new ArrayList<SelectItem>();
        for (String perm : settablePermissions) {
            String label = labeler.makeLabel(perm);
            SelectItem it = new SelectItem(perm,
                    resourcesAccessor.getMessages().get(label));
            permissions.add(it);
        }
        return permissions;
    }

    @PrePassivate
    public void saveState() {
        log.info("PrePassivate");
    }

    @PostActivate
    public void readState() {
        log.info("PostActivate");
    }

    public Map<String, String> getIconAltMap() {
        return principalListManager.iconAlt;
    }

    public Map<String, String> getIconPathMap() {
        return principalListManager.iconPath;
    }

    public Boolean getBlockRightInheritance() {
        return blockRightInheritance;
    }

    public void setBlockRightInheritance(Boolean blockRightInheritance)
            throws ClientException {
        if (blockRightInheritance) {
            // Block
            securityData.addModifiablePrivilege(SecurityConstants.EVERYONE,
                    SecurityConstants.EVERYTHING, false);
            // add user to avoid lock up
            Principal currentUser = FacesContext.getCurrentInstance().getExternalContext().getUserPrincipal();
            if (securityData.getCurrentDocumentUsers() != null
                    && !securityData.getCurrentDocumentUsers().contains(
                            currentUser.getName())) {
                securityData.addModifiablePrivilege(currentUser.getName(),
                        SecurityConstants.EVERYTHING, true);
                // add administrators to avoid LockUp
                securityData.addModifiablePrivilege(ADMIN_GROUP,
                        SecurityConstants.EVERYTHING, true);
            }
        } else {
            securityData.removeModifiablePrivilege(SecurityConstants.EVERYONE,
                    SecurityConstants.EVERYTHING, false);
        }
        updateSecurityOnDocument();
        resetSecurityData();
    }

    public Boolean displayInheritedPermissions() throws ClientException {
        if (blockRightInheritance == null) {
            rebuildSecurityData();
        }
        if (blockRightInheritance) {
            return false;
        }
        return !securityData.getParentDocumentsUsers().isEmpty();
    }

    protected List<String> getSelectedUsers() {
        if (selectedUsers == null) {
            selectedUsers = new ArrayList<String>();
        }
        return selectedUsers;
    }

    public List<String> getCurrentDocumentUsers() throws ClientException {
        List<String> currentUsers = securityData.getCurrentDocumentUsers();
        return validateUserGroupList(currentUsers);
    }

    public List<String> getParentDocumentsUsers() throws ClientException {
        List<String> parentUsers = securityData.getParentDocumentsUsers();
        return validateUserGroupList(parentUsers);
    }

    /**
     * Validates user/group against userManager in order to remove obsolete
     * entries (ie: deleted groups/users).
     *
     * @param usersGroups2Validate
     * @return
     * @throws ClientException
     */
    private List<String> validateUserGroupList(List<String> usersGroups2Validate)
            throws ClientException {
        // TODO :
        // 1 -should add a clean cache system to avoid
        // calling the directory : this can be problematic for big ldaps
        // 2 - this filtering should at some point be applied to acp and saved
        // back in a batch?

        List<String> returnList = new ArrayList<String>();
        for (String entry : usersGroups2Validate) {
            if (entry.equals(SecurityConstants.EVERYONE)) {
                returnList.add(entry);
                continue;
            }
            if (isUserGroupInCache(entry)) {
                returnList.add(entry);
                continue;
            }
            if (isUserGroupInDeletedCache(entry)) {
                continue;
            }

            if (userManager.getPrincipal(entry) != null) {
                returnList.add(entry);
                addUserGroupInCache(entry);
                continue;
            } else if (userManager.getGroup(entry) != null) {
                returnList.add(entry);
                addUserGroupInCache(entry);
                continue;
            } else {
                addUserGroupInDeletedCache(entry);
            }
        }
        return returnList;
    }

    private Boolean isUserGroupInCache(String entry) {
        if (cachedValidatedUserAndGroups == null) {
            return false;
        }
        return cachedValidatedUserAndGroups.contains(entry);
    }

    private void addUserGroupInCache(String entry) {
        if (cachedValidatedUserAndGroups == null) {
            cachedValidatedUserAndGroups = new ArrayList<String>();
        }
        cachedValidatedUserAndGroups.add(entry);
    }

    private Boolean isUserGroupInDeletedCache(String entry) {
        if (cachedDeletedUserAndGroups == null) {
            return false;
        }
        return cachedDeletedUserAndGroups.contains(entry);
    }

    private void addUserGroupInDeletedCache(String entry) {
        if (cachedDeletedUserAndGroups == null) {
            cachedDeletedUserAndGroups = new ArrayList<String>();
        }

        cachedDeletedUserAndGroups.add(entry);
    }

    /**
     * Checks if the current user can still read and write access rights. If
     * he can't, then the security data are rebuilt.
     *
     * @return
     * @throws ClientException
     */
    private boolean checkPermissions() throws ClientException {
        List<String> principals = new ArrayList<String>();
        principals.add(currentUser.getName());
        principals.addAll(currentUser.getAllGroups());

        ACP acp = currentDocument.getACP();
        List<UserEntry> modifiableEntries = new SecurityDataConverter().convertToUserEntries(securityData);
        if (null == acp) {
            acp = new ACPImpl();
        }
        acp.setRules(modifiableEntries.toArray(new UserEntry[0]));

        final boolean access = acp.getAccess(principals.toArray(new String[0]), getPermissionsToCheck()).toBoolean();
        if (!access) {
            rebuildSecurityData();
        }
        return access;
    }

    protected String[] getPermissionsToCheck() throws ClientException {
        if (CACHED_PERMISSION_TO_CHECK == null) {
            try {
                PermissionProvider pprovider = Framework.getService(PermissionProvider.class);
                List<String> aggregatedPerms = new LinkedList<String>();
                for (String seedPerm : SEED_PERMISSIONS_TO_CHECK) {
                    aggregatedPerms.add(seedPerm);
                    String[] compoundPerms = pprovider.getPermissionGroups(seedPerm);
                    if (compoundPerms != null) {
                        aggregatedPerms.addAll(Arrays.asList(compoundPerms));
                    }
                }
                CACHED_PERMISSION_TO_CHECK = aggregatedPerms.toArray(new String[aggregatedPerms.size()]);
            } catch (Exception e) {
                throw new ClientException(e);
            }
        }
        return CACHED_PERMISSION_TO_CHECK;
    }

}
