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

package org.nuxeo.ecm.platform.ui.web.auth.plugins;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.NXAuthContants;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;

public class FormAuthenticator implements NuxeoAuthenticationPlugin {

    private static final Log log = LogFactory.getLog(FormAuthenticator.class);

    protected String loginPage = "login.jsp";

    protected String usernameKey = NXAuthContants.USERNAME_KEY;

    protected String passwordKey = NXAuthContants.PASSORD_KEY;

    public Boolean handleLoginPrompt(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse, String baseURL) {
        try {
            log.debug("Forward to Login Screen");
            String loginError = (String) httpRequest.getAttribute(NXAuthContants.LOGIN_ERROR);
            if (loginError == null) {
                httpResponse.sendRedirect(baseURL + loginPage);
            } else {
                if (NXAuthContants.ERROR_USERNAME_MISSING.equals(loginError)) {
                    httpResponse.sendRedirect(baseURL + loginPage
                            + "?loginMissing=true");
                } else {
                    httpResponse.sendRedirect(baseURL + loginPage
                            + "?loginFailed=true");
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public UserIdentificationInfo handleRetrieveIdentity(
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        log.debug("Looking for user/password in the request");
        String userName = httpRequest.getParameter(usernameKey);
        String password = httpRequest.getParameter(passwordKey);
        // NXP-2650: ugly hack to check if form was submitted
        if (httpRequest.getParameter(NXAuthContants.FORM_SUBMITTED_MARKER) != null
                && (userName == null || userName.length() == 0)) {
            httpRequest.setAttribute(NXAuthContants.LOGIN_ERROR,
                    NXAuthContants.ERROR_USERNAME_MISSING);
        }

        return new UserIdentificationInfo(userName, password);
    }

    public Boolean needLoginPrompt(HttpServletRequest httpRequest) {
        return true;
    }

    public void initPlugin(Map<String, String> parameters) {
        if (parameters.get("LoginPage") != null) {
            loginPage = parameters.get("LoginPage");
        }
        if (parameters.get("UsernameKey") != null) {
            usernameKey = parameters.get("UsernameKey");
        }
        if (parameters.get("PasswordKey") != null) {
            passwordKey = parameters.get("PasswordKey");
        }
    }

    public List<String> getUnAuthenticatedURLPrefix() {
        // Login Page is unauthenticated !
        List<String> prefix = new ArrayList<String>();
        prefix.add(loginPage);
        return prefix;
    }

}
