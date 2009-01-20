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

package org.nuxeo.ecm.platform.usermanager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelComparator;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.impl.NuxeoGroupImpl;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.directory.BaseSession;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.usermanager.exceptions.GroupAlreadyExistsException;
import org.nuxeo.ecm.platform.usermanager.exceptions.UserAlreadyExistsException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.event.Event;
import org.nuxeo.runtime.services.event.EventService;

/**
 * @author George Lefter
 * @author Florent Guillaume
 * @author Anahide Tchertchian
 */
public class UserManagerImpl implements UserManager {

    private static final Log log = LogFactory.getLog(UserManagerImpl.class);

    public static final String USERMANAGER_TOPIC = "usermanager";

    public static final String USERCHANGED_EVENT_ID = "user_changed";

    public static final String GROUPCHANGED_EVENT_ID = "group_changed";

    public static final String DEFAULT_ANONYMOUS_USER_ID = "Anonymous";

    private final DirectoryService dirService;

    private String userDirectoryName;

    private String userSchemaName;

    private String userIdField;

    private String userEmailField;

    private Map<String, MatchType> userSearchFields;

    private String groupDirectoryName;

    private String groupSchemaName;

    private String groupIdField;

    private String groupMembersField;

    private String groupSubGroupsField;

    private String groupParentGroupsField;

    private String groupSortField;

    private String defaultGroup;

    private String defaultRootLogin;

    private String userSortField;

    private String userListingMode;

    private String groupListingMode;

    private Pattern userPasswordPattern;

    private VirtualUser anonymousUser;

    private final Map<String, VirtualUserDescriptor> virtualUsers;

    public UserManagerImpl() {
        dirService = Framework.getLocalService(DirectoryService.class);
        virtualUsers = new HashMap<String, VirtualUserDescriptor>();
    }

    public void setConfiguration(UserManagerDescriptor descriptor) {
        defaultGroup = descriptor.defaultGroup;
        defaultRootLogin = descriptor.rootLogin;
        userSortField = descriptor.userSortField;
        groupSortField = descriptor.groupSortField;
        userListingMode = descriptor.userListingMode;
        groupListingMode = descriptor.groupListingMode;
        userEmailField = descriptor.userEmailField;
        userSearchFields = descriptor.userSearchFields;
        userPasswordPattern = descriptor.userPasswordPattern;
        groupMembersField = descriptor.groupMembersField;
        groupSubGroupsField = descriptor.groupSubGroupsField;
        groupParentGroupsField = descriptor.groupParentGroupsField;
        anonymousUser = descriptor.anonymousUser;

        setUserDirectoryName(descriptor.userDirectoryName);
        setGroupDirectoryName(descriptor.groupDirectoryName);
        setVirtualUsers(descriptor.virtualUsers);
    }

    protected void setUserDirectoryName(String userDirectoryName) {
        this.userDirectoryName = userDirectoryName;
        try {
            userSchemaName = dirService.getDirectorySchema(userDirectoryName);
            userIdField = dirService.getDirectoryIdField(userDirectoryName);
        } catch (ClientException e) {
            throw new RuntimeException("Unkown user directory "
                    + userDirectoryName, e);
        }
    }

    public String getUserDirectoryName() {
        return userDirectoryName;
    }

    public String getUserIdField() throws ClientException {
        return userIdField;
    }

    public String getUserSchemaName() throws ClientException {
        return userSchemaName;
    }

    public String getUserEmailField() {
        return userEmailField;
    }

    public Set<String> getUserSearchFields() {
        return Collections.unmodifiableSet(userSearchFields.keySet());
    }

    protected void setGroupDirectoryName(String groupDirectoryName) {
        this.groupDirectoryName = groupDirectoryName;
        try {
            groupSchemaName = dirService.getDirectorySchema(groupDirectoryName);
            groupIdField = dirService.getDirectoryIdField(groupDirectoryName);
        } catch (ClientException e) {
            throw new RuntimeException("Unkown group directory "
                    + groupDirectoryName, e);
        }
    }

    public String getGroupDirectoryName() {
        return groupDirectoryName;
    }

    public String getGroupIdField() throws ClientException {
        return groupIdField;
    }

    public String getGroupSchemaName() throws ClientException {
        return groupSchemaName;
    }

    public String getGroupMembersField() {
        return groupMembersField;
    }

    public String getGroupSubGroupsField() {
        return groupSubGroupsField;
    }

    public String getGroupParentGroupsField() {
        return groupParentGroupsField;
    }

    public String getUserListingMode() {
        return userListingMode;
    }

    public String getGroupListingMode() {
        return groupListingMode;
    }

    public String getDefaultGroup() {
        return defaultGroup;
    }

    public Pattern getUserPasswordPattern() {
        return userPasswordPattern;
    }

    public String getAnonymousUserId() {
        if (anonymousUser == null) {
            return null;
        }
        String anonymousUserId = anonymousUser.getId();
        if (anonymousUserId == null) {
            return DEFAULT_ANONYMOUS_USER_ID;
        }
        return anonymousUserId;
    }

    protected void setVirtualUsers(
            Map<String, VirtualUserDescriptor> virtualUsers) {
        this.virtualUsers.clear();
        if (virtualUsers != null) {
            this.virtualUsers.putAll(virtualUsers);
        }
    }

    public boolean checkUsernamePassword(String username, String password)
            throws ClientException {

        if (username == null || password == null) {
            log.warn("Trying to authenticate against null username or password");
            return false;
        }

        // deal with anonymous user
        String anonymousUserId = getAnonymousUserId();
        if (username.equals(anonymousUserId)) {
            log.warn(String.format(
                    "Trying to authenticate anonymous user (%s)",
                    anonymousUserId));
            return false;
        }

        // deal with virtual users
        if (virtualUsers.containsKey(username)) {
            VirtualUser user = virtualUsers.get(username);
            String expected = user.getPassword();
            if (expected == null) {
                return false;
            }
            return expected.equals(password);
        }

        Session userDir = null;
        try {
            String userDirName;
            // BBB backward compat for userDirectory + userAuthentication
            if ("userDirectory".equals(userDirectoryName)
                    && dirService.getDirectory("userAuthentication") != null) {
                userDirName = "userAuthentication";
            } else {
                userDirName = userDirectoryName;
            }

            userDir = dirService.open(userDirName);
            if (!userDir.isAuthenticating()) {
                log.error("Trying to authenticate against a non authenticating "
                        + "directory: " + userDirName);
                return false;
            }

            return userDir.authenticate(username, password);
        } finally {
            if (userDir != null) {
                userDir.close();
            }
        }
    }

    public boolean validatePassword(String password) {
        if (userPasswordPattern == null) {
            return true;
        } else {
            Matcher userPasswordMatcher = userPasswordPattern.matcher(password);
            return userPasswordMatcher.find();
        }
    }

    protected NuxeoPrincipal makeAnonymousPrincipal() throws ClientException {
        DocumentModel userEntry = makeVirtualUserEntry(getAnonymousUserId(),
                anonymousUser);
        // XXX: pass anonymous user groups, but they will be ignored
        return makePrincipal(userEntry, true, anonymousUser.getGroups());
    }

    protected NuxeoPrincipal makeVirtualPrincipal(VirtualUser user)
            throws ClientException {
        DocumentModel userEntry = makeVirtualUserEntry(user.getId(), user);
        return makePrincipal(userEntry, false, user.getGroups());
    }

    protected DocumentModel makeVirtualUserEntry(String id, VirtualUser user)
            throws ClientException {
        final DocumentModel userEntry = BaseSession.createEntryModel(null,
                userSchemaName, id, null);
        // at least fill id field
        userEntry.setProperty(userSchemaName, userIdField, id);
        for (Entry<String, Serializable> prop : user.getProperties().entrySet()) {
            try {
                userEntry.setProperty(userSchemaName, prop.getKey(),
                        prop.getValue());
            } catch (ClientException ce) {
                log.error("Property: " + prop.getKey()
                        + " does not exists. Check your "
                        + "UserService configuration.", ce);
            }
        }
        return userEntry;
    }

    protected NuxeoPrincipal makePrincipal(DocumentModel userEntry)
            throws ClientException {
        return makePrincipal(userEntry, false, null);
    }

    protected NuxeoPrincipal makePrincipal(DocumentModel userEntry,
            boolean anonymous, List<String> groups) throws ClientException {
        NuxeoPrincipalImpl principal = new NuxeoPrincipalImpl(
                userEntry.getId(), anonymous);

        principal.setModel(userEntry);

        // XXX why not set groups to anonymous user?
        if (!anonymous) {
            List<String> virtualGroups = new LinkedList<String>();
            // Add preconfigured groups: useful for LDAP
            if (defaultGroup != null) {
                virtualGroups.add(defaultGroup);
            }
            // Add additional groups: useful for virtual users
            if (groups != null) {
                virtualGroups.addAll(groups);
            }
            // Create a default admin if needed
            if (defaultRootLogin != null
                    && defaultRootLogin.equals(principal.getName())) {
                virtualGroups.add(SecurityConstants.ADMINISTRATORS);
            }
            principal.setVirtualGroups(virtualGroups);
        }

        // TODO: reenable roles initialization once we have a use case for
        // a role directory. In the mean time we only set the JBOSS role
        // that is required to login
        List<String> roles = Arrays.asList("regular");
        principal.setRoles(roles);

        return principal;
    }

    public NuxeoPrincipal getPrincipal(String username) throws ClientException {
        if (username == null) {
            return null;
        }
        String anonymousUserId = getAnonymousUserId();
        if (username.equals(anonymousUserId)) {
            return makeAnonymousPrincipal();
        }
        if (virtualUsers.containsKey(username)) {
            return makeVirtualPrincipal(virtualUsers.get(username));
        }
        DocumentModel userModel = getUserModel(username);
        if (userModel != null) {
            return makePrincipal(userModel);
        }
        return null;
    }

    public DocumentModel getUserModel(String userName) throws ClientException {
        if (userName == null) {
            return null;
        }
        Session userDir = null;
        try {
            userDir = dirService.open(userDirectoryName);
            return userDir.getEntry(userName);
        } finally {
            if (userDir != null) {
                userDir.close();
            }
        }
    }

    public DocumentModel getBareUserModel() throws ClientException {
        String schema = dirService.getDirectorySchema(userDirectoryName);
        return BaseSession.createEntryModel(null, schema, null, null);
    }

    public NuxeoGroup getGroup(String groupName) throws ClientException {
        DocumentModel groupEntry = getGroupModel(groupName);
        if (groupEntry != null) {
            return makeGroup(groupEntry);
        }
        return null;
    }

    public DocumentModel getGroupModel(String groupName) throws ClientException {
        Session groupDir = null;
        try {
            groupDir = dirService.open(groupDirectoryName);
            return groupDir.getEntry(groupName);
        } finally {
            if (groupDir != null) {
                groupDir.close();
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected NuxeoGroup makeGroup(DocumentModel groupEntry) {
        NuxeoGroup group = new NuxeoGroupImpl(groupEntry.getId());
        List<String> list;
        try {
            list = (List<String>) groupEntry.getProperty(groupSchemaName,
                    groupMembersField);
        } catch (ClientException e) {
            list = null;
        }
        if (list != null) {
            group.setMemberUsers(list);
        }
        try {
            list = (List<String>) groupEntry.getProperty(groupSchemaName,
                    groupSubGroupsField);
        } catch (ClientException e) {
            list = null;
        }
        if (list != null) {
            group.setMemberGroups(list);
        }
        try {
            list = (List<String>) groupEntry.getProperty(groupSchemaName,
                    groupParentGroupsField);
        } catch (ClientException e) {
            list = null;
        }
        if (list != null) {
            group.setParentGroups(list);
        }
        return group;
    }

    @SuppressWarnings("unchecked")
    public List<String> getTopLevelGroups() throws ClientException {
        Session groupDir = null;
        try {
            List<String> topLevelGroups = new LinkedList<String>();
            groupDir = dirService.open(groupDirectoryName);
            // XXX retrieve all entries with references, can be costly.
            DocumentModelList groups = groupDir.query(
                    Collections.<String, Object> emptyMap(), null, null, true);
            for (DocumentModel group : groups) {
                List<String> parents = (List<String>) group.getProperty(
                        groupSchemaName, groupParentGroupsField);

                if (parents == null || parents.isEmpty()) {
                    topLevelGroups.add(group.getId());
                }
            }
            return topLevelGroups;
        } finally {
            if (groupDir != null) {
                groupDir.close();
            }
        }
    }

    public List<String> getGroupsInGroup(String parentId)
            throws ClientException {
        return getGroup(parentId).getMemberGroups();
    }

    public List<String> getUsersInGroup(String groupId) throws ClientException {
        return getGroup(groupId).getMemberUsers();
    }

    protected boolean isAnonymousMatching(Map<String, Object> filter,
            Set<String> fulltext) throws DirectoryException {
        String anonymousUserId = getAnonymousUserId();
        if (anonymousUserId == null) {
            return false;
        }
        if (filter == null || filter.isEmpty()) {
            return true;
        }
        Map<String, Serializable> anonymousUserMap = anonymousUser.getProperties();
        anonymousUserMap.put(userIdField, anonymousUserId);
        for (Entry<String, Object> e : filter.entrySet()) {
            String fieldName = e.getKey();
            Object expected = e.getValue();
            Object value = anonymousUserMap.get(fieldName);
            if (value == null) {
                if (expected != null) {
                    return false;
                }
            } else {
                if (fulltext != null && fulltext.contains(fieldName)) {
                    if (!value.toString().toLowerCase().startsWith(
                            expected.toString().toLowerCase())) {
                        return false;
                    }
                } else {
                    if (!value.equals(expected)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public List<NuxeoPrincipal> searchPrincipals(String pattern)
            throws ClientException {
        DocumentModelList entries = searchUsers(pattern);
        List<NuxeoPrincipal> principals = new ArrayList<NuxeoPrincipal>(
                entries.size());
        for (DocumentModel entry : entries) {
            principals.add(makePrincipal(entry));
        }
        return principals;
    }

    public List<NuxeoGroup> searchGroups(String pattern) throws ClientException {
        Session groupDir = null;
        try {
            groupDir = dirService.open(groupDirectoryName);
            Map<String, Object> filter = new HashMap<String, Object>();
            if (pattern != null && pattern != "") {
                filter.put(groupDir.getIdField(), pattern);
            }
            DocumentModelList groupEntries = searchGroups(filter,
                    filter.keySet());

            List<NuxeoGroup> groups = new ArrayList<NuxeoGroup>(
                    groupEntries.size());
            for (DocumentModel groupEntry : groupEntries) {
                groups.add(makeGroup(groupEntry));
            }
            return groups;

        } finally {
            if (groupDir != null) {
                groupDir.close();
            }
        }
    }

    public String getUserSortField() {
        return userSortField;
    }

    protected Map<String, String> getUserSortMap() throws DirectoryException {
        String sortField = userSortField != null ? userSortField : userIdField;
        Map<String, String> orderBy = new HashMap<String, String>();
        orderBy.put(sortField, DocumentModelComparator.ORDER_ASC);
        return orderBy;
    }

    /**
     * Notifies user has changed so that the JaasCacheFlusher listener can make
     * sure principals cache is reset.
     */
    protected void notifyUserChanged(String userName) throws ClientException {
        try {
            EventService eventService = Framework.getService(EventService.class);
            eventService.sendEvent(new Event(USERMANAGER_TOPIC,
                    USERCHANGED_EVENT_ID, this, userName));
        } catch (Exception e) {
            throw new ClientException(e);
        }
    }

    /**
     * Notifies group has changed so that the JaasCacheFlusher listener can make
     * sure principals cache is reset.
     */
    protected void notifyGroupChanged(String groupName) throws ClientException {
        try {
            EventService eventService = Framework.getService(EventService.class);
            eventService.sendEvent(new Event(USERMANAGER_TOPIC,
                    GROUPCHANGED_EVENT_ID, this, groupName));
        } catch (Exception e) {
            throw new ClientException(e);
        }
    }

    public Boolean areGroupsReadOnly() throws ClientException {
        Session groupDir = null;
        try {
            groupDir = dirService.open(groupDirectoryName);
            return groupDir.isReadOnly();
        } catch (DirectoryException e) {
            log.error(e);
            return false;
        } finally {
            try {
                if (groupDir != null) {
                    groupDir.close();
                }
            } catch (Exception e) {
            }
        }
    }

    public Boolean areUsersReadOnly() throws ClientException {
        Session userDir = null;
        try {
            userDir = dirService.open(userDirectoryName);
            return userDir.isReadOnly();
        } catch (DirectoryException e) {
            log.error(e);
            return false;
        } finally {
            try {
                if (userDir != null) {
                    userDir.close();
                }
            } catch (Exception e) {
            }
        }
    }

    protected String getGroupId(DocumentModel groupModel)
            throws ClientException {
        Object groupIdValue = groupModel.getProperty(groupSchemaName,
                groupIdField);
        if (groupIdValue != null && !(groupIdValue instanceof String)) {
            throw new ClientException("Invalid group id " + groupIdValue);
        }
        return (String) groupIdValue;
    }

    protected String getUserId(DocumentModel userModel) throws ClientException {
        Object userIdValue = userModel.getProperty(userSchemaName, userIdField);
        if (userIdValue != null && !(userIdValue instanceof String)) {
            throw new ClientException("Invalid user id " + userIdValue);
        }
        return (String) userIdValue;
    }

    public DocumentModel createGroup(DocumentModel groupModel)
            throws ClientException {
        Session groupDir = null;
        try {
            groupDir = dirService.open(groupDirectoryName);
            String groupId = getGroupId(groupModel);

            // check the group does not exist
            if (groupDir.hasEntry(groupId)) {
                throw new GroupAlreadyExistsException();
            }
            groupModel = groupDir.createEntry(groupModel);
            groupDir.commit();
            notifyGroupChanged(groupId);
            return groupModel;

        } finally {
            if (groupDir != null) {
                groupDir.close();
            }
        }
    }

    public DocumentModel createUser(DocumentModel userModel)
            throws ClientException {
        Session userDir = null;
        try {
            userDir = dirService.open(userDirectoryName);
            String userId = getUserId(userModel);

            // check the user does not exist
            if (userDir.hasEntry(userId)) {
                throw new UserAlreadyExistsException();
            }

            userModel = userDir.createEntry(userModel);
            userDir.commit();
            notifyUserChanged(userId);
            return userModel;

        } finally {
            if (userDir != null) {
                userDir.close();
            }
        }
    }

    public void deleteGroup(String groupId) throws ClientException {
        Session groupDir = null;
        try {
            groupDir = dirService.open(groupDirectoryName);
            if (!groupDir.hasEntry(groupId)) {
                throw new DirectoryException("Group does not exist: " + groupId);
            }
            groupDir.deleteEntry(groupId);
            groupDir.commit();
            notifyGroupChanged(groupId);

        } finally {
            if (groupDir != null) {
                groupDir.close();
            }
        }
    }

    public void deleteGroup(DocumentModel groupModel) throws ClientException {
        String groupId = getGroupId(groupModel);
        deleteGroup(groupId);
    }

    public void deleteUser(String userId) throws ClientException {
        Session userDir = null;
        try {
            userDir = dirService.open(userDirectoryName);
            if (!userDir.hasEntry(userId)) {
                throw new DirectoryException("User does not exist: " + userId);
            }
            userDir.deleteEntry(userId);
            userDir.commit();
            notifyUserChanged(userId);

        } finally {
            if (userDir != null) {
                userDir.close();
            }
        }
    }

    public void deleteUser(DocumentModel userModel) throws ClientException {
        String userId = getUserId(userModel);
        deleteUser(userId);
    }

    public List<String> getGroupIds() throws ClientException {
        Session groupDir = null;
        try {
            groupDir = dirService.open(groupDirectoryName);
            List<String> groupIds = groupDir.getProjection(
                    Collections.<String, Object> emptyMap(),
                    groupDir.getIdField());
            Collections.sort(groupIds);
            return groupIds;
        } finally {
            if (groupDir != null) {
                groupDir.close();
            }
        }
    }

    public List<String> getUserIds() throws ClientException {
        Session userDir = null;
        try {
            userDir = dirService.open(userDirectoryName);
            List<String> userIds = userDir.getProjection(
                    Collections.<String, Object> emptyMap(),
                    userDir.getIdField());
            Collections.sort(userIds);
            return userIds;
        } finally {
            if (userDir != null) {
                userDir.close();
            }
        }
    }

    public DocumentModelList searchGroups(Map<String, Object> filter,
            Set<String> fulltext) throws ClientException {
        Session groupDir = null;
        try {
            groupDir = dirService.open(groupDirectoryName);

            String sortField = groupSortField != null ? groupSortField
                    : groupDir.getIdField();
            Map<String, String> orderBy = new HashMap<String, String>();
            orderBy.put(sortField, DocumentModelComparator.ORDER_ASC);
            // XXX: do not fetch references, can be costly
            DocumentModelList entries = groupDir.query(filter, fulltext,
                    orderBy, false);

            return entries;
        } finally {
            if (groupDir != null) {
                groupDir.close();
            }
        }
    }

    protected DocumentModelList searchUsers(Map<String, Object> filter,
            Set<String> fulltext, Map<String, String> orderBy)
            throws ClientException {
        Session userDir = null;
        try {
            userDir = dirService.open(userDirectoryName);

            // XXX: do not fetch references, can be costly
            DocumentModelList entries = userDir.query(filter, fulltext, null,
                    false);
            if (isAnonymousMatching(filter, fulltext)) {
                entries.add(makeVirtualUserEntry(getAnonymousUserId(),
                        anonymousUser));
            }

            // TODO: match searchable virtual users

            if (orderBy != null && !orderBy.isEmpty()) {
                // sort: cannot sort before virtual users are added
                Collections.sort(entries, new DocumentModelComparator(
                        userSchemaName, orderBy));
            }

            return entries;
        } finally {
            if (userDir != null) {
                userDir.close();
            }
        }
    }

    public DocumentModelList searchUsers(String pattern) throws ClientException {
        DocumentModelList entries = new DocumentModelListImpl();
        if (pattern == null || pattern.equals("")) {
            entries = searchUsers(Collections.<String, Object> emptyMap(), null);
        } else {
            Map<String, DocumentModel> uniqueEntries = new HashMap<String, DocumentModel>();

            for (Map.Entry<String, MatchType> fieldEntry : userSearchFields.entrySet()) {
                Map<String, Object> filter = new HashMap<String, Object>();
                filter.put(fieldEntry.getKey(), pattern);
                DocumentModelList fetchedEntries;
                if (fieldEntry.getValue() == MatchType.SUBSTRING) {
                    fetchedEntries = searchUsers(filter, filter.keySet(), null);
                } else {
                    fetchedEntries = searchUsers(filter, null, null);
                }
                for (DocumentModel entry : fetchedEntries) {
                    uniqueEntries.put(entry.getId(), entry);
                }
            }
            log.debug(String.format("found %d unique entries",
                    uniqueEntries.size()));
            entries.addAll(uniqueEntries.values());
        }
        // sort
        Collections.sort(entries, new DocumentModelComparator(userSchemaName,
                getUserSortMap()));

        return entries;
    }

    public DocumentModelList searchUsers(Map<String, Object> filter,
            Set<String> fulltext) throws ClientException {
        return searchUsers(filter, fulltext, getUserSortMap());
    }

    public void updateGroup(DocumentModel groupModel) throws ClientException {
        Session groupDir = null;
        try {
            groupDir = dirService.open(groupDirectoryName);
            String groupId = getGroupId(groupModel);

            if (!groupDir.hasEntry(groupId)) {
                throw new DirectoryException("group does not exist: " + groupId);
            }
            groupDir.updateEntry(groupModel);
            groupDir.commit();
            notifyGroupChanged(groupId);
        } finally {
            if (groupDir != null) {
                groupDir.close();
            }
        }
    }

    public void updateUser(DocumentModel userModel) throws ClientException {
        Session userDir = null;
        try {
            userDir = dirService.open(userDirectoryName);
            String userId = getUserId(userModel);

            if (!userDir.hasEntry(userId)) {
                throw new DirectoryException("user does not exist: " + userId);
            }
            userDir.updateEntry(userModel);
            userDir.commit();
            notifyUserChanged(userId);
        } finally {
            if (userDir != null) {
                userDir.close();
            }
        }
    }

    public DocumentModel getBareGroupModel() throws ClientException {
        String schema = dirService.getDirectorySchema(groupDirectoryName);
        return BaseSession.createEntryModel(null, schema, null, null);
    }

    public void createGroup(NuxeoGroup group) throws ClientException {
        DocumentModel newGroupModel = getBareGroupModel();
        newGroupModel.setProperty(groupSchemaName, groupIdField,
                group.getName());
        newGroupModel.setProperty(groupSchemaName, groupMembersField,
                group.getMemberUsers());
        newGroupModel.setProperty(groupSchemaName, groupSubGroupsField,
                group.getMemberGroups());
        createGroup(newGroupModel);
    }

    public void createPrincipal(NuxeoPrincipal principal)
            throws ClientException {
        createUser(principal.getModel());
    }

    public void deleteGroup(NuxeoGroup group) throws ClientException {
        deleteGroup(group.getName());
    }

    public void deletePrincipal(NuxeoPrincipal principal)
            throws ClientException {
        deleteUser(principal.getName());
    }

    public List<NuxeoGroup> getAvailableGroups() throws ClientException {
        DocumentModelList groupModels = searchGroups(
                Collections.<String, Object> emptyMap(), null);
        List<NuxeoGroup> groups = new ArrayList<NuxeoGroup>(groupModels.size());
        for (DocumentModel groupModel : groupModels) {
            groups.add(makeGroup(groupModel));
        }
        return groups;
    }

    public List<NuxeoPrincipal> getAvailablePrincipals() throws ClientException {
        DocumentModelList userModels = searchUsers(
                Collections.<String, Object> emptyMap(), null);
        List<NuxeoPrincipal> users = new ArrayList<NuxeoPrincipal>(
                userModels.size());
        for (DocumentModel userModel : userModels) {
            users.add(makePrincipal(userModel));
        }
        return users;
    }

    public DocumentModel getModelForUser(String name) throws ClientException {
        return getUserModel(name);
    }

    public List<NuxeoPrincipal> searchByMap(Map<String, Object> filter,
            Set<String> pattern) throws ClientException {
        Session userDir = null;
        try {
            userDir = dirService.open(userDirectoryName);
            DocumentModelList entries = userDir.query(filter, pattern);
            List<NuxeoPrincipal> principals = new ArrayList<NuxeoPrincipal>(
                    entries.size());
            for (DocumentModel entry : entries) {
                principals.add(makePrincipal(entry));
            }
            if (isAnonymousMatching(filter, pattern)) {
                principals.add(makeAnonymousPrincipal());
            }
            return principals;
        } finally {
            if (userDir != null) {
                userDir.close();
            }
        }
    }

    public void updateGroup(NuxeoGroup group) throws ClientException {
        // XXX: need to refetch it for tests to pass, i don't get why (session
        // id is used maybe?)
        DocumentModel newGroupModel = getGroupModel(group.getName());
        newGroupModel.setProperty(groupSchemaName, groupIdField,
                group.getName());
        newGroupModel.setProperty(groupSchemaName, groupMembersField,
                group.getMemberUsers());
        newGroupModel.setProperty(groupSchemaName, groupSubGroupsField,
                group.getMemberGroups());
        updateGroup(newGroupModel);
    }

    public void updatePrincipal(NuxeoPrincipal principal)
            throws ClientException {
        updateUser(principal.getModel());
    }

}
