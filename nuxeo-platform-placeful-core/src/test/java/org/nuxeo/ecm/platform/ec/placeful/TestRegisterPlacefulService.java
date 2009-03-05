/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: TestRegisterPlacefulService.java 19071 2007-05-21 16:20:16Z sfermigier $
 */
package org.nuxeo.ecm.platform.ec.placeful;

import java.util.Map;

import org.nuxeo.ecm.platform.ec.placeful.interfaces.PlacefulService;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 *
 * @author <a href="mailto:rspivak@nuxeo.com">Ruslan Spivak</a>
 *
 */
public class TestRegisterPlacefulService extends NXRuntimeTestCase {

    PlacefulService placefulService;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.platform.placeful.core.tests",
                "nxplacefulservice-framework.xml");
        deployContrib("org.nuxeo.ecm.platform.placeful.core.tests",
                "nxplacefulservice-configs-contrib.xml");
        placefulService = (PlacefulService) runtime.getComponent(PlacefulService.ID);
    }

    public void testRegistration() {
        Map<String, String> registry = placefulService.getAnnotationRegistry();

        assertEquals(1, registry.size());

        assertTrue(registry.containsKey("SubscriptionConfig"));

        assertEquals("org.nuxeo.ecm.platform.ec.placeful.SubscriptionConfig",
                registry.get("SubscriptionConfig"));


//        assertTrue(registry.containsKey("ACPAnnotation"));
//
//        assertEquals("org.nuxeo.ecm.platform.ec.placeful.ACPAnnotation",
//                registry.get("ACPAnnotation"));
    }
}
