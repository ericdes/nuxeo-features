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

package org.nuxeo.ecm.platform.versioning;

/**
 * Exception class to wrap other services exceptions.
 *
 * @author DM
 *
 * @deprecated use {@link VersioningException} instead
 */
@Deprecated
public class VersioningException extends Exception {

    private static final long serialVersionUID = 5390675925763793228L;

    public VersioningException() {
    }

    public VersioningException(String message, Throwable cause) {
        super(message, cause);
    }

    public VersioningException(String message) {
        super(message);
    }

    public VersioningException(Throwable cause) {
        super(cause);
    }

}
