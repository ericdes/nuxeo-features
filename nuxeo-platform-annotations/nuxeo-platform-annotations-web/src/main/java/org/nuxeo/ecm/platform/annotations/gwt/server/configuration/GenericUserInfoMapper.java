/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.annotations.gwt.server.configuration;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;

public class GenericUserInfoMapper implements UserInfoMapper {

    public Map<String, String> getUserInfo(NuxeoPrincipal principal) {
        Map<String, String> userInfo = new HashMap<String, String>();
        userInfo.put("userName", principal.getName());
        return userInfo;
    }

}
