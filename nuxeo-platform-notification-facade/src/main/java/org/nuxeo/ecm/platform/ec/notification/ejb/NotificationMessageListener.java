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
 * $Id:NXAuditMessageListener.java 1583 2006-08-04 10:26:40Z janguenot $
 */

package org.nuxeo.ecm.platform.ec.notification.ejb;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJBException;
import javax.ejb.MessageDriven;
import javax.ejb.SessionContext;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.mail.MessagingException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.platform.ec.notification.NotificationImpl;
import org.nuxeo.ecm.platform.ec.notification.email.EmailHelper;
import org.nuxeo.ecm.platform.ec.notification.service.NotificationService;
import org.nuxeo.ecm.platform.ec.notification.service.NotificationServiceHelper;
import org.nuxeo.ecm.platform.events.api.DocumentMessage;
import org.nuxeo.ecm.platform.events.api.JMSConstant;
import org.nuxeo.ecm.platform.notification.api.Notification;
import org.nuxeo.ecm.platform.types.adapter.TypeInfo;
import org.nuxeo.ecm.platform.url.DocumentLocationImpl;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentLocation;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Message Driven Bean listening for events, responsible for notifications.
 * <p/>
 * It does:
 * <ul>
 * <li>(1) Get the message from the topic/NXCoreMessages</li>
 * <li>(2) Gets the notifications that contain the event</li>
 * <li>(3) Navigates upwards in document tree and gets the users that
 * subscribed to the subscriptions</li>
 * <li>(4) Removes the duplicates so that a user would not receive the same
 * notification twice</li>
 * <li>(5) Send notifications according to the channel (email, jabber, etc)</li>
 * </ul>
 *
 * @author <a mailto="npaslaru@nuxeo.com">Narcis Paslaru</a>
 * @author <a href="mailto:tmartins@nuxeo.com">Thierry Martins</a>
 */

@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "topic/NXPMessages"),
        @ActivationConfigProperty(propertyName = "providerAdapterJNDI", propertyValue = "java:/NXCoreEventsProvider"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge"),
        @ActivationConfigProperty(propertyName = "messageSelector", propertyValue = JMSConstant.NUXEO_MESSAGE_TYPE
                + " IN ('"
                + JMSConstant.DOCUMENT_MESSAGE
                + "','"
                + JMSConstant.EVENT_MESSAGE + "')")})
@TransactionManagement(TransactionManagementType.BEAN)
public class NotificationMessageListener implements MessageListener {

    private static final Log log = LogFactory.getLog(NotificationMessageListener.class);

    private DocumentViewCodecManager docLocator;

    @Resource
    private SessionContext sessionContext;

    private LoginContext loginCtx;

    private void login() throws Exception {
        loginCtx = Framework.login();
    }

    private void logout() throws Exception {
        if (loginCtx != null) {
            loginCtx.logout();
            loginCtx = null;
        }
    }

    public void onMessage(Message message) {
        final UserTransaction transaction = sessionContext.getUserTransaction();

        CoreSession session = null;
        DocumentMessage docMsg;
        String eventId;

        try {
            Object obj = ((ObjectMessage) message).getObject();
            if (!(obj instanceof DocumentMessage)) {
                return;
            }
            docMsg = (DocumentMessage) obj;

            eventId = docMsg.getEventId();
            log.debug("Recieved a message for notification with eventId : " + eventId);
        }
        catch (JMSException e) {
            log.error("Error getting message from topic", e);
            return;
        }

        NotificationService service = NotificationServiceHelper.getNotificationService();
        if (service == null) {
            log.error("Unable to get NotificationService, exiting");
            return;
        }

        List<Notification> notifs = service.getNotificationsForEvents(eventId);
        if (notifs == null || notifs.isEmpty()) {
            log.debug("No notification bound to " + eventId);
            return;
        }

        Map<Notification, List<String>> targetUsers = new HashMap<Notification, List<String>>();

        try {
            transaction.begin();
        } catch (Exception e) {
            log.error("Unable to start transaction, existing", e);
            return;
        }

        try {
            login();
        } catch (Exception e) {
            log.error("Unable to do Framework.login", e);
            return;
        }

        String repoName = docMsg.getRepositoryName();
        Boolean errDuringProcess = false;
        CoreSession coreSession = null;

        try {
            RepositoryManager mgr = Framework.getService(RepositoryManager.class);
            Repository repo;
            if (repoName != null) {
                repo = mgr.getRepository(repoName);
            } else {
                repo = mgr.getDefaultRepository();
            }

            if (repo == null) {
                log.error("can not find repository, existing");
                errDuringProcess = true;
                return;
            }

            coreSession = repo.open();
            if (eventId.equals("documentPublicationApproved") || eventId.equals("documentPublished")) {
                DocumentModel publishedDoc = getDocFromPath(coreSession, (String) docMsg.getEventInfo().get("sectionPath"), docMsg);
                if (publishedDoc == null) {
                    log.error("unable to find published doc, existing");
                    return;
                }
                gatherConcernedUsersForDocument(coreSession, publishedDoc, notifs, targetUsers);
            } else {
                gatherConcernedUsersForDocument(coreSession, docMsg, notifs, targetUsers);
            }


            for (Notification notif : targetUsers.keySet()) {
                if (!notif.getAutoSubscribed()) {
                    for (String user : targetUsers.get(notif)) {
                        sendNotificationSignalForUser(notif, user, docMsg);
                    }
                } else {
                    Map<String, Serializable> info = docMsg.getEventInfo();
                    String recipient = (String) info.get("recipients");
                    if (recipient == null || recipient.trim().length() <= 0) {
                        // nobody to send notification to
                        log.warn("An autosubscribed notification [" + eventId
                                + "] was requested but found no target adress");
                        continue;
                    }
                    List<String> users = getUsersForMultiRecipients(recipient);
                    for (String user : users) {
                        sendNotificationSignalForUser(notif, user, docMsg);
                    }

                }
            }
        }
        catch (Exception e) {
            log.error("Error during message processing", e);
            errDuringProcess = true;
        }
        finally {
            if (errDuringProcess) {
                try {
                    transaction.rollback();
                } catch (Exception te) {
                    log.error("Error during transaction rollback", te);
                }
            } else {
                try {
                    transaction.commit();
                }
                catch (Exception te) {
                    log.error("Error during transaction commit", te);
                }
            }

            if (coreSession != null) {
                CoreInstance.getInstance().close(coreSession);
                coreSession = null;
            }
            try {
                logout();
            }
            catch (Exception le) {
                log.error("Error during logout", le);
            }
        }

    }

    private DocumentModel getDocFromPath(CoreSession coreSession, String path, DocumentMessage doc) throws ClientException {
        DocumentModel sectionDoc;
        if (path == null) {
            return null;
        }
        sectionDoc = coreSession.getDocument(new PathRef(path));
        return sectionDoc;
    }

    private List<String> getUsersForMultiRecipients(String recipient)
            throws ClientException {
        String[] recipients = recipient.split("\\|");
        List<String> users = new ArrayList<String>();

        for (String user : recipients) {
            users.addAll(getUsersForRecipient(user));
        }
        return users;
    }

    private List<String> getUsersForRecipient(String recipient)
            throws ClientException {
        List<String> users = new ArrayList<String>();
        if (isUser(recipient)) {
            users.add(recipient.substring(5));
        } else {
            // it is a group - get all users and send
            // notifications to them
            List<String> usersOfGroup = NotificationServiceHelper.getUsersService().getUsersInGroup(
                    recipient.substring(6));
            if (usersOfGroup != null && !usersOfGroup.isEmpty()) {
                users.addAll(usersOfGroup);
            }
        }
        return users;
    }

    /**
     * Adds the concerned users to the list of targeted users for these
     * notifications.
     *
     * @param doc
     * @param notifs
     * @param targetUsers
     * @throws Exception
     */
    private void gatherConcernedUsersForDocument(CoreSession coreSession, DocumentModel doc,
            List<Notification> notifs,
            Map<Notification, List<String>> targetUsers) throws Exception {
        if (doc.getPath().segmentCount() > 1) {
            log.debug("Searching document.... : " + doc.getName());
            getInterstedUsers(doc, notifs, targetUsers);
            DocumentModel parent = getDocumentParent(coreSession, doc);
            gatherConcernedUsersForDocument(coreSession, parent, notifs, targetUsers);
        }
    }

    private DocumentModel getDocumentParent(CoreSession coreSession, DocumentModel doc)
            throws ClientException {
        DocumentModel parentDoc = null;
        if (doc == null) {
            return parentDoc;
        }
        return coreSession.getDocument(doc.getParentRef());
    }

    private void getInterstedUsers(DocumentModel doc,
            List<Notification> notifs,
            Map<Notification, List<String>> targetUsers) throws Exception {
        for (Notification notification : notifs) {
            if (!notification.getAutoSubscribed()) {
                List<String> userGroup = NotificationServiceHelper.getNotificationService().getSubscribers(
                        notification.getName(), doc.getId());
                for (String subscriptor : userGroup) {
                    if (subscriptor != null) {
                        if (isUser(subscriptor)) {
                            storeUserForNotification(notification,
                                    subscriptor.substring(5), targetUsers);
                        } else {
                            // it is a group - get all users and send
                            // notifications to them
                            List<String> usersOfGroup = NotificationServiceHelper.getUsersService().getUsersInGroup(
                                    subscriptor.substring(6));
                            if (usersOfGroup != null && !usersOfGroup.isEmpty()) {
                                for (String usr : usersOfGroup) {
                                    storeUserForNotification(notification, usr,
                                            targetUsers);
                                }
                            }
                        }
                    }
                }
            } else {
                // An automatic notification happens
                // should be sent to interested users
                targetUsers.put(notification, new ArrayList<String>());
            }
        }
    }

    private void storeUserForNotification(Notification notification,
            String user, Map<Notification, List<String>> targetUsers) {
        List<String> subscribedUsers = targetUsers.get(notification);
        if (subscribedUsers == null) {
            targetUsers.put(notification, new ArrayList<String>());
        }
        if (!targetUsers.get(notification).contains(user)) {
            targetUsers.get(notification).add(user);
        }
    }

    private void sendNotificationSignalForUser(Notification notification,
            String subscriptor, DocumentMessage message) throws ClientException {

        log.debug("Producing notification message...........");

        Map<String, Serializable> eventInfo = message.getEventInfo();

        eventInfo.put("destination", subscriptor);
        eventInfo.put("notification", notification);
        eventInfo.put("docId", message.getId());
        eventInfo.put("dateTime", message.getEventDate());
        eventInfo.put("author", message.getPrincipalName());

        if (!documentHasBeenDeleted(message)) {
            DocumentLocation docLoc = new DocumentLocationImpl(message);
            TypeInfo typeInfo = message.getAdapter(TypeInfo.class);
            DocumentView docView = new DocumentViewImpl(docLoc, typeInfo.getDefaultView());
            eventInfo.put(
                    "docUrl",
                    getDocLocator().getUrlFromDocumentView(
                            docView,
                            true,
                            NotificationServiceHelper.getNotificationService().getServerUrlPrefix()));

            try {
                LoginContext lc = Framework.login();
                eventInfo.put("docTitle", message.getTitle());
                // eventInfo.put("document", message);
                lc.logout();
            } catch (LoginException le) {
                log.debug("Exception at reconnect !!! - no document data will be available in the template");
            }
        }
        if (isInterestedInNotification(notification)) {

            try {
                sendNotification(message);
                if (log.isDebugEnabled()) {
                    log.debug("notification " + notification.getName()
                            + " sent to " + notification.getSubject());
                }
            } catch (ClientException e) {
                log.error(
                        "An error occurred while trying to send user notification",
                        e);
            }

        }
    }

    private boolean documentHasBeenDeleted(DocumentMessage message) {
        List<String> deletionEvents = new ArrayList<String>();
        deletionEvents.add("aboutToRemove");
        deletionEvents.add("documentRemoved");
        return deletionEvents.contains(message.getEventId());
    }

    private boolean isUser(String subscriptor) {
        return subscriptor != null && subscriptor.startsWith("user:");
    }

    public boolean isInterestedInNotification(Notification notif) {
        return notif != null && "email".equals(notif.getChannel());
    }

    public void sendNotification(DocumentMessage docMessage)
            throws ClientException {

        String eventId = docMessage.getEventId();
        log.debug("Recieved a message for notification sender with eventId : "
                + eventId);

        Map<String, Serializable> eventInfo = docMessage.getEventInfo();
        String userDest = (String) eventInfo.get("destination");
        NotificationImpl notif = (NotificationImpl) eventInfo.get("notification");

        // send email
        NuxeoPrincipal recepient = NotificationServiceHelper.getUsersService().getPrincipal(
                userDest);
        // XXX hack, principals have only one model
        DataModel model = recepient.getModel().getDataModels().values().iterator().next();
        String email = (String) model.getData("email");
        String subjectTemplate = notif.getSubjectTemplate();
        String mailTemplate = notif.getTemplate();

        log.debug("email: " + email);
        log.debug("mail template: " + mailTemplate);
        log.debug("subject template: " + subjectTemplate);

        Map<String, Object> mail = new HashMap<String, Object>();
        mail.put("mail.to", email);

        String authorUsername = (String) eventInfo.get("author");

        if (authorUsername != null) {
            NuxeoPrincipal author = NotificationServiceHelper.getUsersService().getPrincipal(
                    authorUsername);
            mail.put("principalAuthor", author);
        }

        mail.put("document", docMessage);
        String subject = notif.getSubject() == null ? "Notification"
                : notif.getSubject();
        subject = NotificationServiceHelper.getNotificationService().getEMailSubjectPrefix()
                + subject;
        mail.put("subject", subject);
        mail.put("template", mailTemplate);
        mail.put("subjectTemplate", subjectTemplate);

        // Transferring all data from event to email
        for (String key : eventInfo.keySet()) {
            mail.put(key, eventInfo.get(key));
            log.debug("Mail prop: " + key);
        }

        mail.put("eventId", eventId);

        try {
            EmailHelper.sendmail(mail);
        } catch (MessagingException e) {
            log.error("Failed to send notification email to '" + email + "': " +
                    e.getClass().getName() + ": " + e.getMessage());
        } catch (Exception e) {
            throw new ClientException("Failed to send notification email ", e);
        }
    }

    public DocumentViewCodecManager getDocLocator() {
        if (docLocator == null) {
            try {
                docLocator = Framework.getService(DocumentViewCodecManager.class);
            } catch (Exception e) {
                log.info("Could not get service for document view manager");
            }
        }
        return docLocator;
    }

}
