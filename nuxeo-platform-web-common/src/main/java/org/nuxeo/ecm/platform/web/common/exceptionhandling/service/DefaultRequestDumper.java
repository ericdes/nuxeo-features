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
 *     arussel
 */
package org.nuxeo.ecm.platform.web.common.exceptionhandling.service;

import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

/**
 * @author arussel
 *
 */
public class DefaultRequestDumper implements RequestDumper {

    @SuppressWarnings("unchecked")
    public String getDump(HttpServletRequest request) {
        StringBuilder builder = new StringBuilder();
        builder.append("\nRequest Attributes:\n\n");
        Enumeration<String> e = request.getAttributeNames();
        while(e.hasMoreElements()) {
            String name = e.nextElement();
            Object obj = request.getAttribute(name);
            builder.append(name);
            builder.append(" : ");
            builder.append(obj.toString());
            builder.append("\n");
        }
        return builder.toString();
    }

}
