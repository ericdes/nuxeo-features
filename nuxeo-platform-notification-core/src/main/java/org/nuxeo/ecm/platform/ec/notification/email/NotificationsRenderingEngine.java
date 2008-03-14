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
 *     narcis
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.ec.notification.email;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import org.nuxeo.ecm.platform.ec.notification.email.templates.NuxeoTemplatesLoader;
import org.nuxeo.ecm.platform.rendering.RenderingContext;
import org.nuxeo.ecm.platform.rendering.RenderingException;
import org.nuxeo.ecm.platform.rendering.RenderingResult;
import org.nuxeo.ecm.platform.rendering.impl.DefaultRenderingResult;
import org.nuxeo.ecm.platform.rendering.template.DocumentRenderingEngine;
import org.nuxeo.ecm.platform.rendering.template.FreemarkerRenderingJob;

import freemarker.template.Configuration;

/**
 * @author <a href="mailto:npaslaru@nuxeo.com">Narcis Paslaru</a>
 *
 */
public class NotificationsRenderingEngine extends DocumentRenderingEngine {

    private final String template;

    public NotificationsRenderingEngine(String template){
        this.template = template;
    }

    @Override
    protected FreemarkerRenderingJob createJob(RenderingContext ctx)
            throws RenderingException {
        return new NotifsRenderingJob("ftl");
    }

    public String getFormatName() {
        // TODO Auto-generated method stub
        return null;
    }

    class NotifsRenderingJob extends DefaultRenderingResult implements FreemarkerRenderingJob {

        private static final long serialVersionUID = -7133062841713259967L;

        NotifsRenderingJob(String formatName) {
            super(formatName);
        }

        final Writer strWriter = new StringWriter();

        @Override
        public Object getOutcome() {
            return strWriter.toString();
        }

        public RenderingResult getResult() {
            return this;
        }

        public String getTemplate() {
            return template;
        }

        public Writer getWriter() throws IOException {
            return strWriter;
        }

    }

}