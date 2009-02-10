package org.nuxeo.ecm.platform.ec.notification;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.mail.MessagingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.ec.notification.email.EmailHelper;
import org.nuxeo.ecm.platform.ec.notification.service.NotificationService;
import org.nuxeo.ecm.platform.ec.notification.service.NotificationServiceHelper;
import org.nuxeo.ecm.platform.notification.api.Notification;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

public class NotificationEventListener implements PostCommitEventListener {

    private static final Log log = LogFactory.getLog(NotificationEventListener.class);

    private DocumentViewCodecManager docLocator;

    private UserManager userManager;

    public void handleEvent(EventBundle events) throws ClientException {

        NotificationService service = NotificationServiceHelper.getNotificationService();
        if (service == null) {
            log.error("Unable to get NotificationService, exiting");
            return;
        }

        for (Event event : events) {
            List<Notification> notifs = service.getNotificationsForEvents(event.getName());
            if (notifs != null && !notifs.isEmpty()) {
                try {
                    handleNotifications(event, notifs);
                } catch (Exception e) {
                    log.error("Error during Notification processing for event "
                            + event.getName(), e);
                }
            }
        }

    }

    protected void handleNotifications(Event event, List<Notification> notifs)
            throws Exception {

        String eventId = event.getName();
        EventContext ctx = event.getContext();
        DocumentEventContext docCtx = null;
        if (ctx instanceof DocumentEventContext) {
            docCtx = (DocumentEventContext) ctx;
        } else {
            log.warn("Can not handle notification on a event that is not bound to a DocumentEventContext");
            return;
        }

        CoreSession coreSession = event.getContext().getCoreSession();
        Map<String, Serializable> properties = event.getContext().getProperties();
        Map<Notification, List<String>> targetUsers = new HashMap<Notification, List<String>>();

        if (eventId.equals("documentPublicationApproved")
                || eventId.equals("documentPublished")) {
            DocumentModel publishedDoc = getDocFromPath(coreSession,
                    (String) properties.get("sectionPath"));
            if (publishedDoc == null) {
                log.error("unable to find published doc, exiting");
                return;
            }
            gatherConcernedUsersForDocument(coreSession, publishedDoc, notifs,
                    targetUsers);
        } else {
            gatherConcernedUsersForDocument(coreSession,
                    docCtx.getSourceDocument(), notifs, targetUsers);
        }

        for (Notification notif : targetUsers.keySet()) {
            if (!notif.getAutoSubscribed()) {
                for (String user : targetUsers.get(notif)) {
                    sendNotificationSignalForUser(notif, user, event, docCtx);
                }
            } else {
                String[] recipients = (String[]) properties.get(NotificationConstants.RECIPIENTS_KEY);
                if (recipients == null) {
                    return;
                }
                Set<String> users = new HashSet<String>();
                for (String recipient : recipients) {
                    if (recipient == null) {
                        continue;
                    }
                    if (recipient.contains(NuxeoPrincipal.PREFIX)) {
                        users.add(recipient.replace(NuxeoPrincipal.PREFIX, ""));
                    } else if (recipient.contains(NuxeoGroup.PREFIX)) {
                        List<String> groupMembers = getGroupMembers(recipient.replace(
                                NuxeoGroup.PREFIX, ""));
                        for(String member : groupMembers) {
                            users.add(member);
                        }
                    } else {
                        users.add(recipient);
                    }

                }
                for (String user : users) {
                    sendNotificationSignalForUser(notif, user, event, docCtx);
                }

            }
        }

    }

    protected UserManager getUserManager() {
        if(userManager == null) {
            try {
                userManager = Framework.getService(UserManager.class);
            } catch (Exception e) {
                throw new IllegalStateException("UserManager service not deployed.", e);
            }
        }
        return userManager;
    }
    protected List<String> getGroupMembers(String groupId) throws ClientException {
        return getUserManager().getUsersInGroup(groupId);
    }

    private void sendNotificationSignalForUser(Notification notification,
            String subscriptor, Event event, DocumentEventContext ctx)
            throws ClientException {

        log.debug("Producing notification message.");

        Map<String, Serializable> eventInfo = ctx.getProperties();

        eventInfo.put(NotificationConstants.DESTINATION_KEY, subscriptor);
        eventInfo.put(NotificationConstants.NOTIFICATION_KEY, notification);
        eventInfo.put(NotificationConstants.DOCUMENT_ID_KEY,
                ctx.getSourceDocument().getId());
        eventInfo.put(NotificationConstants.DATE_TIME_KEY, new Date(
                event.getTime()));
        eventInfo.put(NotificationConstants.AUTHOR_KEY,
                ctx.getPrincipal().getName());

        DocumentModel doc = ctx.getSourceDocument();

        if (!isDeleteEvent(event.getName())) {
            DocumentView docView = new DocumentViewImpl(doc);
            eventInfo.put(
                    NotificationConstants.DOCUMENT_URL_KEY,
                    getDocLocator().getUrlFromDocumentView(
                            docView,
                            true,
                            NotificationServiceHelper.getNotificationService().getServerUrlPrefix()));

            eventInfo.put(NotificationConstants.DOCUMENT_TITLE_KEY,
                    doc.getTitle());
        }

        if (isInterestedInNotification(notification)) {
            try {
                sendNotification(event, ctx);
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

    public void sendNotification(Event event, DocumentEventContext ctx)
            throws ClientException {

        String eventId = event.getName();
        log.debug("Recieved a message for notification sender with eventId : "
                + eventId);

        Map<String, Serializable> eventInfo = ctx.getProperties();
        String userDest = (String) eventInfo.get(NotificationConstants.DESTINATION_KEY);
        NotificationImpl notif = (NotificationImpl) eventInfo.get(NotificationConstants.NOTIFICATION_KEY);

        // send email
        NuxeoPrincipal recepient = NotificationServiceHelper.getUsersService().getPrincipal(
                userDest);
        if(recepient == null) {
            log.error("Couldn't find user: " + userDest + " to send her a mail.");
            return;
        }
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

        String authorUsername = (String) eventInfo.get(NotificationConstants.AUTHOR_KEY);

        if (authorUsername != null) {
            NuxeoPrincipal author = NotificationServiceHelper.getUsersService().getPrincipal(
                    authorUsername);
            mail.put(NotificationConstants.PRINCIPAL_AUTHOR_KEY, author);
        }

        mail.put(NotificationConstants.DOCUMENT_KEY, ctx.getSourceDocument());
        String subject = notif.getSubject() == null ? NotificationConstants.NOTIFICATION_KEY
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

        mail.put(NotificationConstants.EVENT_ID_KEY, eventId);

        try {
            EmailHelper.sendmail(mail);
        } catch (MessagingException e) {
            log.error("Failed to send notification email to '" + email + "': "
                    + e.getClass().getName() + ": " + e.getMessage());
        } catch (Exception e) {
            throw new ClientException("Failed to send notification email ", e);
        }
    }

    private DocumentModel getDocFromPath(CoreSession coreSession, String path)
            throws ClientException {
        if (path == null) {
            return null;
        }
        return coreSession.getDocument(new PathRef(path));
    }

    /**
     * Adds the concerned users to the list of targeted users for these
     * notifications.
     */
    private void gatherConcernedUsersForDocument(CoreSession coreSession,
            DocumentModel doc, List<Notification> notifs,
            Map<Notification, List<String>> targetUsers) throws Exception {
        if (doc.getPath().segmentCount() > 1) {
            log.debug("Searching document: " + doc.getName());
            getInterstedUsers(doc, notifs, targetUsers);
            DocumentModel parent = getDocumentParent(coreSession, doc);
            gatherConcernedUsersForDocument(coreSession, parent, notifs,
                    targetUsers);
        }
    }

    private DocumentModel getDocumentParent(CoreSession coreSession,
            DocumentModel doc) throws ClientException {
        if (doc == null) {
            return null;
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

    private boolean isDeleteEvent(String eventId) {
        List<String> deletionEvents = new ArrayList<String>();
        deletionEvents.add("aboutToRemove");
        deletionEvents.add("documentRemoved");
        return deletionEvents.contains(eventId);
    }

    private boolean isUser(String subscriptor) {
        return subscriptor != null && subscriptor.startsWith("user:");
    }

    public boolean isInterestedInNotification(Notification notif) {
        return notif != null && "email".equals(notif.getChannel());
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
