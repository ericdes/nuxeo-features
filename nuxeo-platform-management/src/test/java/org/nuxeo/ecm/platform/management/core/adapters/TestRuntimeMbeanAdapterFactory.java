/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     matic
 */
package org.nuxeo.ecm.platform.management.core.adapters;

import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;
import org.nuxeo.ecm.platform.management.adapters.RepositorySessionMetricFactory;
import org.nuxeo.ecm.platform.management.adapters.RuntimeInventoryFactory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.ResourcePublisherService;
import org.nuxeo.runtime.management.ResourceFactoryDescriptor;

/**
 * @author Stephane Lacoin (Nuxeo EP Software Engineer)
 * 
 */
public class TestRuntimeMbeanAdapterFactory extends RepositoryOSGITestCase {

    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.runtime.management");

        managementService = (ResourcePublisherService) Framework.getRuntime().getComponent(
                ResourcePublisherService.NAME);
    }

    protected ResourcePublisherService managementService;

    @Override
    public void tearDown() throws Exception {
        Framework.getRuntime().stop();
        super.tearDown();
    }

    public void testRegisterFactories() throws Exception {
        doRegisterInventoryFactory();
        doRegisterRepositorySessionMetricFactory();
    }

    public void doRegisterInventoryFactory() throws Exception {
        ResourceFactoryDescriptor factoryDescriptor = new ResourceFactoryDescriptor(
                RuntimeInventoryFactory.class);
        managementService.registerContribution(factoryDescriptor, "factories",
                null);
    }
    
    public void doRegisterRepositorySessionMetricFactory() throws Exception {
        ResourceFactoryDescriptor factoryDescriptor = new ResourceFactoryDescriptor(
                RepositorySessionMetricFactory.class);
        managementService.registerContribution(factoryDescriptor, "factories",
                null);
    }

}
