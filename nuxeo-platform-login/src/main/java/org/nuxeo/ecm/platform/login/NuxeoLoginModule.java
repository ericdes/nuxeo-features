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

package org.nuxeo.ecm.platform.login;

import java.io.IOException;
import java.security.Principal;
import java.security.acl.Group;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfoCallback;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.login.LoginComponent;
import sun.security.acl.GroupImpl;
import sun.security.acl.PrincipalImpl;

public class NuxeoLoginModule extends NuxeoAbstractServerLoginModule {

    private static final Log log = LogFactory.getLog(NuxeoLoginModule.class);

    private UserManager manager;

    private Random random;

    private NuxeoPrincipal identity;

    private LoginPluginRegistry loginPluginManager;

    private boolean useUserIdentificationInfoCB = false;

    @Override
    @SuppressWarnings("unchecked")
    public void initialize(Subject subject, CallbackHandler callbackHandler,
            Map sharedState, Map options) {
        // explicit cast to match the direct superclass method declaration
        // (JBoss implementation)
        // rather than the newer (jdk1.5) LoginModule (... Map<String,?>...)
        // This is needed to avoid compilation errors when the linker wants to
        // bind
        // with the (interface) LoginModule method (which is abstract of course
        // and cannot be called)
        String useUIICB = (String) options.get("useUserIdentificationInfoCB");
        if (useUIICB != null && useUIICB.equalsIgnoreCase("true")) {
            useUserIdentificationInfoCB = true;
        }

        super.initialize(subject, callbackHandler, sharedState,
                options);
        random = new Random(System.currentTimeMillis());

        try {
            manager = Framework.getService(UserManager.class);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            log.error("UserManager implementation not found", e);
        }
        if (manager != null) {
            log.debug("NXLoginModule initialized");
        } else {
            log.error("UserManager implementation not found");
        }

        try {
            loginPluginManager = (LoginPluginRegistry) Framework.getRuntime()
                    .getComponent(LoginPluginRegistry.NAME);
        } catch (Throwable t) {
            log.error("Unable to load Plugin Registry : " + t.getMessage());
        }
    }

    /**
     * Gets the roles the user belongs to.
     */
    @Override
    protected Group[] getRoleSets() throws LoginException {
        log.debug("getRoleSets");
        if (manager == null) {
            throw new LoginException("UserManager implementation not found");
        }
        String username = identity.getName();
        List<String> roles = identity.getRoles();

        Group roleSet = new GroupImpl("Roles");
        log.debug("Getting roles for user=" + username);
        for (String roleName : roles) {
            Principal role = new PrincipalImpl(roleName);
            log.debug("Found role=" + roleName);
            roleSet.addMember(role);
        }
        Group callerPrincipal = new GroupImpl("CallerPrincipal");
        callerPrincipal.addMember(identity);

        return new Group[] { roleSet, callerPrincipal };
    }

    private NuxeoPrincipal getPrincipal() throws LoginException {
        UserIdentificationInfo userIdent = null;

        // **** init the callbacks
        // Std login/password callbacks
        NameCallback nc = new NameCallback("Username: ",
                SecurityConstants.ANONYMOUS);
        PasswordCallback pc = new PasswordCallback("Password: ", false);

        // Nuxeo specific cb : handle LoginPlugin initialization
        UserIdentificationInfoCallback uic = new UserIdentificationInfoCallback();

        // JBoss specific cb : handle web=>ejb propagation
        // SecurityAssociationCallback ac = new SecurityAssociationCallback();
        // ObjectCallback oc = new ObjectCallback("UserInfo:");

        // **** handle callbacks
        // We can't check the callback handler class to know what will be
        // supported
        // because the cbh is wrapped by JAAS
        // => just try and swalow exceptions
        // => will be externalised to plugins via EP to avoid JBoss dependency
        boolean cb_handled = false;

        try {
            // only try this cbh when called from the web layer
            if (useUserIdentificationInfoCB) {
                callbackHandler.handle(new Callback[] { uic });
                // First check UserInfo CB return
                userIdent = uic.getUserInfo();
                cb_handled = true;
            }
        } catch (UnsupportedCallbackException e) {
            log.debug("UserIdentificationInfoCallback is not supported");
        } catch (IOException e) {
            log.warn("Error calling callback handler with UserIdentificationInfoCallback : "
                    + e.getMessage());
        }

        Principal principal = null;
        Object credential = null;

        if (!cb_handled) {
            CallbackResult result = loginPluginManager.handleSpecifcCallbacks(callbackHandler);

            if (result != null && result.cb_handled) {
                if (result.userIdent != null
                        && result.userIdent.containsValidIdentity()) {
                    userIdent = result.userIdent;
                    cb_handled = true;
                } else {
                    principal = result.principal;
                    credential = result.credential;
                    if (principal != null) {
                        cb_handled = true;
                    }
                }
            }
        }

        if (!cb_handled) {
            try {
                // Std CBH : will only works for L/P
                callbackHandler.handle(new Callback[] { nc, pc });
                cb_handled = true;
            } catch (UnsupportedCallbackException e) {
                LoginException le = new LoginException(
                        "Authentications Failure - " + e.getMessage());
                le.initCause(e);
            } catch (IOException e) {
                LoginException le = new LoginException(
                        "Authentications Failure - " + e.getMessage());
                le.initCause(e);
            }
        }

        try {
            // Login via the Web Interface : may be using a plugin
            if (userIdent != null && userIdent.containsValidIdentity()) {
                NuxeoPrincipal nxp = validateUserIdentity(userIdent);

                if (nxp != null) {
                    sharedState.put("javax.security.auth.login.name", nxp.getName());
                    sharedState.put("javax.security.auth.login.password", userIdent);
                }
                return nxp;
            }

            if (LoginComponent.isSystemLogin(principal)) {
                return new SystemPrincipal(principal.getName());
            }
            // if (principal instanceof NuxeoPrincipal) { // a nuxeo principal
            // return validatePrincipal((NuxeoPrincipal) principal);
            // } else
            if (principal != null) { // a non null principal
                String password = null;
                if (credential instanceof char[]) {
                    password = new String((char[]) credential);
                } else if (credential != null) {
                    password = credential.toString();
                }
                return validateUsernamePassword(principal.getName(), password);
            } else { // we don't have a principal - try the username &
                // password
                String username = nc.getName();
                if (username == null) {
                    return null;
                }
                char[] password = pc.getPassword();
                return validateUsernamePassword(username, password != null ? new String(
                        password)
                        : null);
            }
        } catch (LoginException e) {
            throw e;
        } catch (Exception e) {
            // jboss catches LoginException, so show it at least in the logs
            log.error(e);
            LoginException le = new LoginException("Authentication Failure - "
                    + e.getMessage());
            le.initCause(e);
            throw le;
        }
    }

    public boolean login() throws LoginException {
        if (manager == null) {
            throw new LoginException("UserManager implementation not found");
        }

        super.loginOk = false;

        identity = getPrincipal();
        if (identity == null) { // auth failed
            throw new LoginException("Authentication Failed");
        }

        super.loginOk = true;
        log.trace("User '" + identity + "' authenticated");

        /*if( getUseFirstPass() == true )
        {    // Add the username and password to the shared state map
            // not sure it's needed
           sharedState.put("javax.security.auth.login.name", identity.getName());
           sharedState.put("javax.security.auth.login.password", identity.getPassword());
        }*/

        return true;
    }

    @Override
    public Principal getIdentity() {
        return identity;
    }

    @Override
    public Principal createIdentity(String name) throws LoginException {
        log.debug("createIdentity: " + name);
        try {
            NuxeoPrincipal principal = null;
            if (manager == null) {
                principal = new NuxeoPrincipalImpl(name);
            } else {
                principal = manager.getPrincipal(name);
                if (principal == null) {
                    throw new LoginException(String.format(
                            "principal %s does not exist", name));
                }
            }

            String principalId = String.valueOf(random.nextLong());
            principal.setPrincipalId(principalId);
            return principal;
        } catch (Exception e) {
            log.error("createIdentity failed", e);
            LoginException le = new LoginException("createIdentity failed for user " + name);
            le.initCause(e);
            throw le;
        }
    }

    private NuxeoPrincipal validateUserIdentity(UserIdentificationInfo userIdent)
            throws Exception {
        String loginPluginName = userIdent.getLoginPluginName();
        if (loginPluginName == null) {
            // we don't use a specific plugin
            if (manager.checkUsernamePassword(userIdent.getUserName(),
                    userIdent.getPassword())) {
                return (NuxeoPrincipal) createIdentity(userIdent.getUserName());
            } else {
                return null;
            }
        } else {
            LoginPlugin lp = loginPluginManager.getPlugin(loginPluginName);
            if (lp == null) {
                log.error("Can't authenticate against a null loginModul plugin");
                return null;
            }
            // set the parameters and reinit if needed
            LoginPluginDescriptor lpd = loginPluginManager.getPluginDescriptor(loginPluginName);
            if (!lpd.getInitialized()) {
                Map<String, String> existingParams = lp.getParameters();
                if (existingParams == null) {
                    existingParams = new HashMap<String, String>();
                }
                Map<String, String> loginParams = userIdent.getLoginParameters();
                if (loginParams != null) {
                    existingParams.putAll(loginParams);
                }
                boolean init = lp.initLoginModule();
                if (init) {
                    lpd.setInitialized(true);
                } else {
                    log.error("Unable to initialize LoginModulePlugin "
                            + lp.getName());
                    return null;
                }
            }

            String username = lp.validatedUserIdentity(userIdent);
            if (username == null) {
                return null;
            } else {
                return (NuxeoPrincipal) createIdentity(username);
            }
        }
    }

    private NuxeoPrincipal validateUsernamePassword(String username, String password)
            throws Exception {
        if (!manager.checkUsernamePassword(username, password)) {
            return null;
        }
        return (NuxeoPrincipal) createIdentity(username);
    }

    // Not used. Remove ?
    private NuxeoPrincipal validatePrincipal(NuxeoPrincipal principal) throws Exception {
        if (!manager.checkUsernamePassword(principal.getName(), principal.getPassword())) {
            return null;
        }
        return principal;
    }

}
