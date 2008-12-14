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

package org.nuxeo.ecm.directory.ldap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * @author <a href="mailto:ogrisel@nuxeo.com">Olivier Grisel</a>
 *
 */
public class LDAPDirectoryActivator implements BundleActivator {

    private static final Log log = LogFactory.getLog(LDAPDirectoryActivator.class);

    // FIXME: should be named "org.nuxeo.ecm.directory.ldap"
    public static final String NAME = "org.nuxeo.ecm.directory.sql";

    public void start(BundleContext context) {
        log.info("bundle started: " + context.getBundle().getSymbolicName());
    }

    public void stop(BundleContext context) {
        log.info("bundle stopped: " + context.getBundle().getSymbolicName());
    }

}
