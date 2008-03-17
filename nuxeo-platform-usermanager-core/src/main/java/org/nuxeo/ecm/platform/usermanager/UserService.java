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

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

public class UserService extends DefaultComponent {

    public static final ComponentName NAME = new ComponentName(
            UserService.class.getName());

    private static final Log log = LogFactory.getLog(UserService.class);

    private final List<UserManagerDescriptor> descriptors = new LinkedList<UserManagerDescriptor>();

    private UserManager userManager;

    public UserManager getUserManager() throws ClientException {
        if (userManager == null) {
            recomputeUserManager(false);
        }
        return userManager;
    }

    protected void recomputeUserManager(boolean lazy) throws ClientException {
        if (lazy && userManager == null) {
            return;
        }
        UserManagerDescriptor merged = new UserManagerDescriptor();
        merged.userListingMode = "search_only"; // SEARCH_ONLY
        // BBB backward compatibility defaults
        merged.userDirectoryName = "userDirectory";
        merged.userEmailField = "email";
        merged.userSearchFields = new HashSet<String>(Arrays.asList("username",
                "firstName", "lastName"));
        merged.groupDirectoryName = "groupDirectory";
        merged.groupMembersField = "members";
        merged.groupSubGroupsField = "subGroups";
        merged.groupParentGroupsField = "parentGroups";
        for (UserManagerDescriptor descriptor : descriptors) {
            merged.merge(descriptor);
        }
        if (userManager == null) {
            // which means we'll always keep the first registered class
            if (descriptors.isEmpty()) {
                throw new ClientException(
                        "No contributions registered for the userManager");
            }
            Class<?> klass = merged.userManagerClass;
            if (klass == null) {
                throw new ClientException(
                        "No class specified for the userManager");
            }
            try {
                userManager = (UserManager) klass.newInstance();
            } catch (InstantiationException e) {
                throw new ClientException("Failed to instantiate class "
                        + klass, e);
            } catch (IllegalAccessException e) {
                throw new ClientException("Failed to instantiate class "
                        + klass, e);
            }
        }
        // TODO just put the descriptor in the userManager
        userManager.setDefaultGroup(merged.defaultGroup);
        userManager.setRootLogin(merged.rootLogin);
        userManager.setUserSortField(merged.userSortField);
        userManager.setGroupSortField(merged.groupSortField);
        userManager.setUserListingMode(merged.userListingMode);
        userManager.setGroupListingMode(merged.groupListingMode);
        userManager.setUserDirectoryName(merged.userDirectoryName);
        userManager.setUserEmailField(merged.userEmailField);
        userManager.setUserSearchFields(merged.userSearchFields);
        userManager.setUserPasswordPattern(merged.userPasswordPattern);
        userManager.setGroupDirectoryName(merged.groupDirectoryName);
        userManager.setGroupMembersField(merged.groupMembersField);
        userManager.setGroupSubGroupsField(merged.groupSubGroupsField);
        userManager.setGroupParentGroupsField(merged.groupParentGroupsField);
        userManager.setAnonymousUser(merged.anonymousUser);
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.getName().equals(UserManager.class.getName())) {
            try {
                return adapter.cast(getUserManager());
            } catch (ClientException e) {
                log.error("error fetching UserManager: ", e);
            }
        }
        return null;
    }

    @Override
    public void activate(ComponentContext context) {
        log.info("UserService activated");
    }

    @Override
    public void deactivate(ComponentContext context) {
        log.info("UserService deactivated");
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws ClientException {
        descriptors.add((UserManagerDescriptor) contribution);
        recomputeUserManager(true);
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws ClientException {
        descriptors.remove(contribution);
        recomputeUserManager(true);
    }
}
