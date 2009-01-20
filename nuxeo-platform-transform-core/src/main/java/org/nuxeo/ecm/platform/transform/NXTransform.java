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
 * $Id: NXTransform.java 24475 2007-09-03 06:03:57Z janguenot $
 */
package org.nuxeo.ecm.platform.transform;

import org.nuxeo.ecm.platform.transform.service.TransformService;
import org.nuxeo.runtime.api.Framework;

/**
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 * @deprecated remove in 5.2 - not used anymore in Nuxeo Platform
 */
@Deprecated
public final class NXTransform {

    private NXTransform() {
    }

    public static TransformService getTransformService() {
        return (TransformService) Framework.getRuntime().getComponent(TransformService.NAME);
    }

}
