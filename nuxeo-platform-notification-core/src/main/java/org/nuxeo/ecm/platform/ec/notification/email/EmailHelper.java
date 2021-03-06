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

package org.nuxeo.ecm.platform.ec.notification.email;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.naming.InitialContext;
import javax.security.auth.login.LoginContext;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ec.notification.NotificationConstants;
import org.nuxeo.ecm.platform.ec.notification.service.NotificationService;
import org.nuxeo.ecm.platform.ec.notification.service.NotificationServiceHelper;
import org.nuxeo.ecm.platform.rendering.RenderingResult;
import org.nuxeo.ecm.platform.rendering.RenderingService;
import org.nuxeo.ecm.platform.rendering.impl.DocumentRenderingContext;
import org.nuxeo.runtime.api.Framework;

import freemarker.template.Configuration;
import freemarker.template.Template;

/**
 * Class EmailHelper.
 * <p>
 * An email helper:
 * <p>
 *
 * <pre>
 * Hashtable mail = new Hashtable();
 * mail.put(&quot;from&quot;, &quot;dion@almaer.com&quot;);
 * mail.put(&quot;to&quot;, &quot;dion@almaer.com&quot;);
 * mail.put(&quot;subject&quot;, &quot;a subject&quot;);
 * mail.put(&quot;body&quot;, &quot;the body&quot;);
 * &lt;p&gt;
 * EmailHelper.sendmail(mail);
 * </pre>
 *
 * Currently only supports one email in to address
 *
 * @author <a href="mailto:npaslaru@nuxeo.com">Narcis Paslaru</a>
 * @author <a href="mailto:tmartins@nuxeo.com">Thierry Martins</a>
 */
public class EmailHelper {

    // used for loading templates from strings
    private final Configuration stringCfg = new Configuration();

    /* Only static methods here chaps */
    public EmailHelper() {
    }

    /**
     * Static Method: sendmail(Map mail).
     *
     * @param mail
     *            A map of the settings
     */
    public void sendmail(Map<String, Object> mail) throws Exception {

        Session session = getSession();
        // Construct a MimeMessage
        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(session.getProperty("mail.from")));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(
                (String) mail.get("mail.to"), false));

        RenderingService rs = Framework.getService(RenderingService.class);

        DocumentRenderingContext context = new DocumentRenderingContext();
        context.remove("doc");
        context.putAll(mail);
        context.setDocument((DocumentModel) mail.get("document"));

        String customSubjectTemplate = (String) mail
                .get(NotificationConstants.SUBJECT_TEMPLATE_KEY);
        if (customSubjectTemplate == null) {
            String subjTemplate = (String) mail
                    .get(NotificationConstants.SUBJECT_KEY);
            Template templ = new Template("name",
                    new StringReader(subjTemplate), stringCfg);

            Writer out = new StringWriter();
            templ.process(mail, out);
            out.flush();

            msg.setSubject(out.toString(), "UTF-8");
        } else {
            rs.registerEngine(new NotificationsRenderingEngine(
                    customSubjectTemplate));

            LoginContext lc = Framework.login();

            Collection<RenderingResult> results = rs.process(context);
            String subjectMail = "<HTML><P>No parsing Succeded !!!</P></HTML>";

            for (RenderingResult result : results) {
                subjectMail = (String) result.getOutcome();
            }
            subjectMail = NotificationServiceHelper.getNotificationService()
                    .getEMailSubjectPrefix()
                    + subjectMail;
            msg.setSubject(subjectMail, "UTF-8");

            lc.logout();
        }

        msg.setSentDate(new Date());

        rs.registerEngine(new NotificationsRenderingEngine((String) mail
                .get(NotificationConstants.TEMPLATE_KEY)));

        LoginContext lc = Framework.login();

        Collection<RenderingResult> results = rs.process(context);
        String bodyMail = "<HTML><P>No parsing Succeded !!!</P></HTML>";

        for (RenderingResult result : results) {
            bodyMail = (String) result.getOutcome();
        }

        lc.logout();

        rs.unregisterEngine("ftl");

        msg.setContent(bodyMail, "text/html; charset=utf-8");

        // Send the message.
        Transport.send(msg);
    }

    /**
     * Gets the session from the JNDI.
     */
    private static Session getSession() {
        Session session = null;
        // First, try to get the session from JNDI, as would be done under J2EE.
        try {
            NotificationService service = (NotificationService) Framework
                    .getRuntime().getComponent(NotificationService.NAME);

            InitialContext ic = new InitialContext();
            session = (Session) ic.lookup(service.getMailSessionJndiName());
        } catch (Exception ex) {
            // ignore it
            ex.printStackTrace();
        }

        return session;
    }

}
