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

package org.nuxeo.ecm.platform.ui.web.auth.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfoCallbackHandler;
import org.nuxeo.ecm.platform.ui.web.auth.CachableUserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPropagator;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationSessionManager;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoCallbackHandlerFactory;
import org.nuxeo.ecm.platform.ui.web.auth.plugins.DefaultSessionManager;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class PluggableAuthenticationService extends DefaultComponent {

    public static final String NAME = "org.nuxeo.ecm.platform.ui.web.auth.service.PluggableAuthenticationService";

    public static final String EP_AUTHENTICATOR = "authenticators";

    public static final String EP_SESSIONMANAGER = "sessionManager";

    public static final String EP_CHAIN = "chain";

    public static final String EP_PROPAGATOR = "propagator";

    public static final String EP_CBFACTORY = "JbossCallbackfactory";

    public static final String EP_STARTURL = "startURL";

    private static final Log log = LogFactory.getLog(PluggableAuthenticationService.class);

    private Map<String, AuthenticationPluginDescriptor> authenticatorsDescriptors;

    private Map<String, NuxeoAuthenticationPlugin> authenticators;

    private Map<String, NuxeoAuthenticationSessionManager> sessionManagers;

    private NuxeoAuthenticationSessionManager defaultSessionManager;

    private NuxeoAuthenticationPropagator propagator = null;

    private NuxeoCallbackHandlerFactory cbhFactory = null;

    private List<String> authChain;

    private final List<String> startupURLs = new ArrayList<String>();

    @Override
    public void activate(ComponentContext context) {
        authenticatorsDescriptors = new HashMap<String, AuthenticationPluginDescriptor>();
        authChain = new ArrayList<String>();
        authenticators = new HashMap<String, NuxeoAuthenticationPlugin>();
        sessionManagers = new HashMap<String, NuxeoAuthenticationSessionManager>();
        defaultSessionManager = new DefaultSessionManager();
    }

    @Override
    public void deactivate(ComponentContext context) {
        authenticatorsDescriptors = null;
        authenticators = null;
        authChain = null;
        sessionManagers = null;
        defaultSessionManager = null;
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {

        if (extensionPoint.equals(EP_AUTHENTICATOR)) {
            AuthenticationPluginDescriptor descriptor = (AuthenticationPluginDescriptor) contribution;
            if (authenticatorsDescriptors.containsKey(descriptor.getName())) {
                mergeDescriptors(descriptor);
                log.debug("merged AuthenticationPluginDescriptor: "
                        + descriptor.getName());
            } else {
                authenticatorsDescriptors.put(descriptor.getName(), descriptor);
                log.debug("registered AuthenticationPluginDescriptor: "
                        + descriptor.getName());
            }

            // create the new instance
            AuthenticationPluginDescriptor actualDescriptor = authenticatorsDescriptors.get(descriptor.getName());
            try {
                NuxeoAuthenticationPlugin authPlugin = actualDescriptor.getClassName().newInstance();
                authPlugin.initPlugin(actualDescriptor.getParameters());
                authenticators.put(actualDescriptor.getName(), authPlugin);
            } catch (InstantiationException e) {
                log.error("Unable to create AuthPlugin for : "
                        + actualDescriptor.getName() + "Error : "
                        + e.getMessage(), e);
            } catch (IllegalAccessException e) {
                log.error("Unable to create AuthPlugin for : "
                        + actualDescriptor.getName() + "Error : "
                        + e.getMessage(), e);
            }

        } else if (extensionPoint.equals(EP_CHAIN)) {
            AuthenticationChainDescriptor chainContrib = (AuthenticationChainDescriptor) contribution;
            authChain.clear();
            authChain.addAll(chainContrib.getPluginsNames());
        } else if (extensionPoint.equals(EP_STARTURL)) {
            StartURLPatternDescriptor startupURLContrib = (StartURLPatternDescriptor) contribution;
            startupURLs.addAll(startupURLContrib.getStartURLPatterns());
        } else if (extensionPoint.equals(EP_PROPAGATOR)) {
            AuthenticationPropagatorDescriptor propagationContrib = (AuthenticationPropagatorDescriptor) contribution;

            // create the new instance
            try {
                propagator = propagationContrib.getClassName().newInstance();
            } catch (InstantiationException e) {
                log.error("Unable to create propagator", e);
            } catch (IllegalAccessException e) {
                log.error("Unable to create propagator", e);
            }
        } else if (extensionPoint.equals(EP_CBFACTORY)) {
            CallbackHandlerFactoryDescriptor cbhfContrib = (CallbackHandlerFactoryDescriptor) contribution;

            // create the new instance
            try {
                cbhFactory = cbhfContrib.getClassName().newInstance();
            } catch (InstantiationException e) {
                log.error("Unable to create callback handler factory", e);
            } catch (IllegalAccessException e) {
                log.error("Unable to create callback handler factory", e);
            }
        } else if (extensionPoint.equals(EP_SESSIONMANAGER)) {
            SessionManagerDescriptor smContrib = (SessionManagerDescriptor) contribution;
            if (smContrib.enabled) {
                try {
                    NuxeoAuthenticationSessionManager sm = smContrib.getClassName().newInstance();
                    sessionManagers.put(smContrib.getName(), sm);
                } catch (Exception e) {
                    log.error("Unable to create session manager", e);
                }
            } else {
                sessionManagers.remove(smContrib.getName());
            }
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {

        if (extensionPoint.equals(EP_AUTHENTICATOR)) {
            AuthenticationPluginDescriptor descriptor = (AuthenticationPluginDescriptor) contribution;
            authenticatorsDescriptors.remove(descriptor.getName());
            log.debug("unregistered AuthenticationPlugin: "
                    + descriptor.getName());
        }
    }

    private void mergeDescriptors(AuthenticationPluginDescriptor newContrib) {
        AuthenticationPluginDescriptor oldDescriptor = authenticatorsDescriptors.get(newContrib.getName());

        // Enable/Disable
        if (newContrib.getEnabled() != null) {
            oldDescriptor.setEnabled(newContrib.getEnabled());
        }

        // Merge parameters
        Map<String, String> oldParameters = oldDescriptor.getParameters();
        oldParameters.putAll(newContrib.getParameters());
        oldDescriptor.setParameters(oldParameters);

        // override LoginLModule
        if (newContrib.getLoginModulePlugin() != null
                && newContrib.getLoginModulePlugin().length() > 0) {
            oldDescriptor.setLoginModulePlugin(newContrib.getLoginModulePlugin());
        }
    }

    // Service API

    public List<String> getStartURLPatterns() {
        return startupURLs;
    }

    public List<String> getAuthChain() {
        return authChain;
    }

    public UserIdentificationInfoCallbackHandler getCallbackHandler(
            UserIdentificationInfo userIdent) {
        if (cbhFactory == null) {
            return new UserIdentificationInfoCallbackHandler(userIdent);
        }
        return cbhFactory.createCallbackHandler(userIdent);
    }

    public void propagateUserIdentificationInformation(
            CachableUserIdentificationInfo cachableUserIdent) {
        if (propagator != null) {
            propagator.propagateUserIdentificationInformation(cachableUserIdent);
        }
    }

    public List<NuxeoAuthenticationPlugin> getPluginChain() {
        List<NuxeoAuthenticationPlugin> result = new ArrayList<NuxeoAuthenticationPlugin>();

        for (String pluginName : authChain) {
            if (authenticatorsDescriptors.containsKey(pluginName)
                    && authenticatorsDescriptors.get(pluginName).getEnabled()) {
                if (authenticators.containsKey(pluginName)) {
                    result.add(authenticators.get(pluginName));
                }
            }
        }
        return result;
    }

    public NuxeoAuthenticationPlugin getPlugin(String pluginName) {
        if (authenticatorsDescriptors.containsKey(pluginName)
                && authenticatorsDescriptors.get(pluginName).getEnabled()) {
            if (authenticators.containsKey(pluginName)) {
                return authenticators.get(pluginName);
            }
        }
        return null;
    }

    public AuthenticationPluginDescriptor getDescriptor(String pluginName) {
        if (authenticatorsDescriptors.containsKey(pluginName)) {
            return authenticatorsDescriptors.get(pluginName);
        } else {
            log.error("Plugin " + pluginName + " not registered or not created");
            return null;
        }
    }

    public void invalidateSession(ServletRequest request) {
        if (!sessionManagers.isEmpty()) {
            for (String smName : sessionManagers.keySet()) {
                NuxeoAuthenticationSessionManager sm = sessionManagers.get(smName);
                sm.onBeforeSessionInvalidate(request);
            }
        }
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    public HttpSession reinitSession(HttpServletRequest httpRequest) {
        if (!sessionManagers.isEmpty()) {
            for (String smName : sessionManagers.keySet()) {
                NuxeoAuthenticationSessionManager sm = sessionManagers.get(smName);
                sm.onBeforeSessionReinit(httpRequest);
            }
        }

        HttpSession session = httpRequest.getSession(true);

        if (!sessionManagers.isEmpty()) {
            for (String smName : sessionManagers.keySet()) {
                NuxeoAuthenticationSessionManager sm = sessionManagers.get(smName);
                sm.onAfterSessionReinit(httpRequest);
            }
        }
        return session;
    }

    public boolean canBypassRequest(ServletRequest request) {
        if (!sessionManagers.isEmpty()) {
            for (String smName : sessionManagers.keySet()) {
                NuxeoAuthenticationSessionManager sm = sessionManagers.get(smName);
                if (sm.canBypassRequest(request)){
                    return true;
                }
            }
        }
        return false;
    }

    public boolean needResetLogin(ServletRequest request) {
        if (!sessionManagers.isEmpty()) {
            for (NuxeoAuthenticationSessionManager sm : sessionManagers.values()) {
                if (sm.needResetLogin(request)) {
                    return true;
                }
            }
        }
        return false;
    }

    public String getBaseURL(ServletRequest request) {
        return VirtualHostHelper.getBaseURL(request);
    }

    public void onAuthenticatedSessionCreated(ServletRequest request, HttpSession session, CachableUserIdentificationInfo cachebleUserInfo) {
        if (!sessionManagers.isEmpty()) {
            for (String smName : sessionManagers.keySet()) {
                NuxeoAuthenticationSessionManager sm = sessionManagers.get(smName);
                sm.onAuthenticatedSessionCreated(request, session, cachebleUserInfo);
            }
        }
    }

}
