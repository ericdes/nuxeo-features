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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.ui.web.restAPI.service.PluggableRestletService;
import org.nuxeo.ecm.platform.ui.web.restAPI.service.RestletPluginDescriptor;
import org.nuxeo.runtime.api.Framework;
import org.restlet.Filter;
import org.restlet.Restlet;
import org.restlet.Router;

import com.noelios.restlet.ext.servlet.ServletConverter;

/**
 * Servlet used to run Restlet inside Nuxeo.
 * Setup Seam Restlet filter if needed
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
public class RestletServlet extends HttpServlet {

    private static final Log log = LogFactory.getLog(RestletServlet.class);

    private static final long serialVersionUID = 1764653653643L;

    protected ServletConverter converter;

    protected PluggableRestletService service;

    @Override
    public void init() throws ServletException {
        super.init();
        converter = new ServletConverter(getServletContext());

        // init the router
        Router restletRouter = new Router();

        // get the service
        service = (PluggableRestletService) Framework.getRuntime().getComponent(
                PluggableRestletService.NAME);
        if (service == null) {
            log.error("Unable to get Service " + PluggableRestletService.NAME);
            throw new ServletException(
                    "Can't initialize Nuxeo Pluggable Restlet Service");
        }

        for (String restletName : service.getContributedRestletNames()) {
            RestletPluginDescriptor plugin = service.getContributedRestletDescriptor(restletName);

            Restlet restletToAdd;
            if (plugin.getUseSeam()) {

                Filter seamFilter;
                seamFilter = new SeamRestletFilter(plugin.getUseConversation());

                Restlet seamRestlet = service.getContributedRestletByName(
                        restletName);

                seamFilter.setNext(seamRestlet);

                restletToAdd = seamFilter;
            } else {
                if (plugin.isSingleton()) {
                    restletToAdd = service.getContributedRestletByName(restletName);
                } else {

                    Filter threadSafeRestletFilter;
                    threadSafeRestletFilter = new ThreadSafeRestletFilter();

                    Restlet restlet = service.getContributedRestletByName(restletName);

                    threadSafeRestletFilter.setNext(restlet);
                    restletToAdd = threadSafeRestletFilter;
                }
            }

            for (String urlPattern : plugin.getUrlPatterns()) {
                restletRouter.attach(urlPattern, restletToAdd);
            }

        }

        // init the filters and restlets
/*
        Restlet testRestlet = new SimpleRestlet();
        Restlet testRestlet2 = new SimpleRestlet2();

        Filter seamFilter = new SeamRestletFilter();
        Restlet testSeam = new SimpleResletWithSeam();
        seamFilter.setNext(testSeam);

        restletRouter.attach("/simple/{name}",testRestlet);
        restletRouter.attach("/simple2/{name}",testRestlet2);

        restletRouter.attach("/seam/{repo}/{docid}",seamFilter);
*/
        //this.converter.setTarget(testRestlet);
        converter.setTarget(restletRouter);
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        converter.service(req, res);
    }

}
