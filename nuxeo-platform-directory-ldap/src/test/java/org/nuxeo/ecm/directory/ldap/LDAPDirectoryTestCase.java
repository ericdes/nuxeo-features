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

package org.nuxeo.ecm.directory.ldap;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.protocol.shared.store.LdifFileLoader;
import org.apache.directory.server.protocol.shared.store.LdifLoadFilter;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author <a href="ogrisel@nuxeo.com">Olivier Grisel</a>
 *
 */
public abstract class LDAPDirectoryTestCase extends NXRuntimeTestCase {

    protected static final LdapTestServer SERVER = new LdapTestServer();

    // change this flag to use an external LDAP directory instead of the
    // non networked default ApacheDS implementation
    public boolean USE_EXTERNAL_TEST_LDAP_SERVER = false;

    // change this flag if your test server has support for dynamic groups
    // through the groupOfURLs objectclass, eg for OpenLDAP:
    // http://www.ldap.org.br/modules/ldap/files/files///dyngroup.schema
    public boolean HAS_DYNGROUP_SCHEMA = false;

    public String EXTERNAL_SERVER_SETUP = "TestDirectoriesWithExternalOpenLDAP.xml";

    public String INTERNAL_SERVER_SETUP = "TestDirectoriesWithInternalApacheDS.xml";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // setup the client environment
        deploy("ldap-test-setup/CoreService.xml");
        deploy("ldap-test-setup/TypeService.xml");

        deploy("ldap-test-setup/DirectoryTypes.xml");
        deploy("ldap-test-setup/DirectoryService.xml");
        deploy("ldap-test-setup/LDAPDirectoryFactory.xml");
        if (USE_EXTERNAL_TEST_LDAP_SERVER) {
            deployContrib(EXTERNAL_SERVER_SETUP);
        } else {
            deployContrib(INTERNAL_SERVER_SETUP);
            getLDAPDirectory("userDirectory").setTestServer(SERVER);
            getLDAPDirectory("groupDirectory").setTestServer(SERVER);
        }
        LDAPSession session = (LDAPSession) getLDAPDirectory("userDirectory").getSession();
        try {
            DirContext ctx = session.getContext();
            loadDataFromLdif("sample-users.ldif", ctx);
            loadDataFromLdif("sample-groups.ldif", ctx);
            if (HAS_DYNGROUP_SCHEMA) {
                loadDataFromLdif("sample-dynamic-groups.ldif", ctx);
            }
        } finally {
            session.close();
        }
    }

    @Override
    protected void tearDown() throws Exception {
        LDAPSession session = (LDAPSession) getLDAPDirectory("userDirectory").getSession();
        try {
            if (USE_EXTERNAL_TEST_LDAP_SERVER) {
                DirContext ctx = session.getContext();
                destroyRecursively("ou=people,dc=example,dc=com", ctx);
                destroyRecursively("ou=groups,dc=example,dc=com", ctx);
            } else {
                DirContext ctx = SERVER.getContext();
                destroyRecursively("ou=people", ctx);
                destroyRecursively("ou=groups", ctx);
            }
        } finally {
            session.close();
        }
        super.tearDown();
    }

    protected static void loadDataFromLdif(String ldif, DirContext ctx) {
        List<LdifLoadFilter> filters = new ArrayList<LdifLoadFilter>();
        LdifFileLoader loader = new LdifFileLoader(ctx, new File(ldif),
                filters, Thread.currentThread().getContextClassLoader());
        loader.execute();
    }

    protected void destroyRecursively(String dn, DirContext ctx)
            throws NamingException {
        SearchControls scts = new SearchControls();
        scts.setSearchScope(SearchControls.ONELEVEL_SCOPE);
        String providerUrl = (String) ctx.getEnvironment().get(
                Context.PROVIDER_URL);
        NamingEnumeration<SearchResult> children = ctx.search(dn,
                "(objectClass=*)", scts);
        while (children.hasMore()) {
            SearchResult child = children.next();
            String subDn = child.getName();
            if (!USE_EXTERNAL_TEST_LDAP_SERVER && subDn.endsWith(providerUrl)) {
                subDn = subDn.substring(0, subDn.length()
                        - providerUrl.length() - 1);
            } else {
                subDn = subDn + ',' + dn;
            }
            destroyRecursively(subDn, ctx);
        }
        ctx.destroySubcontext(dn);
    }

    public static void _setUpContextFactory() {
        Properties props = System.getProperties();
        props.put("java.naming.factory.initial",
                "org.nuxeo.ecm.directory.sql.LocalContextFactory");
    }

    public static LDAPDirectory getLDAPDirectory(String name)
            throws DirectoryException {
        LDAPDirectoryFactory factory = (LDAPDirectoryFactory) Framework.getRuntime().getComponent(
                LDAPDirectoryFactory.NAME);
        return ((LDAPDirectoryProxy) factory.getDirectory(name)).getDirectory();
    }

}
