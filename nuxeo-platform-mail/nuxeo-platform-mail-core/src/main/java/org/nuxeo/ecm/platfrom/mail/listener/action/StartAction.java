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

package org.nuxeo.ecm.platfrom.mail.listener.action;

import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.Flags.Flag;

import org.nuxeo.ecm.platform.mail.action.ExecutionContext;

/**
 *
 * @author <a href="mailto:cbaican@nuxeo.com">Catalin Baican</a>
 *
 */
public class StartAction extends AbstractMailAction {

    public boolean execute(ExecutionContext context) throws Exception {
        Message message = context.getMessage();
        if (message == null) {
            return false;
        }
        Flags flags = message.getFlags();
        if (flags != null && flags.contains(Flag.SEEN)) {
            return false;
        }
        // mark message as seen
        message.setFlag(Flag.SEEN, true);
        // flag it in case it is not treated correctly
        message.setFlag(Flag.FLAGGED, true);
        return true;
    }

    public void reset(ExecutionContext context) throws Exception {
        // do nothing
    }

}
