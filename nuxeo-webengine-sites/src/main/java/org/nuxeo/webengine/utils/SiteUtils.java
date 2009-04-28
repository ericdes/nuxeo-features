/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     rdarlea
 */
package org.nuxeo.webengine.utils;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.webengine.sites.JsonAdapter;

/**
 * Utility class for sites implementation.
 * @author rux added web comments related
 */
public class SiteUtils {

    private static final Log log = LogFactory.getLog(SiteUtils.class);

    private SiteUtils() {
    }

    /**
     * Method used to return the list with the details about the <b>Contextual
     * Link</b>-s that have been created under a <b>Workspace</b> or
     * <b>Webpage</b> document type.
     *
     * @param documentModel-this can be either an <b>Workspace</b> or
     *            <b>Webpage</b> document type
     * @return the list with the details about the <b>Contextual Link</b>-s
     */
    public static List<Object> getContextualLinks(CoreSession session, 
            DocumentModel documentModel) throws Exception {
        List<Object> contextualLinks = new ArrayList<Object>();
        if (SiteConstants.WORKSPACE.equals(documentModel.getType())
                || SiteConstants.WEBPAGE.equals(documentModel.getType())) {
            for (DocumentModel document : session.getChildren(
                    documentModel.getRef(), SiteConstants.CONTEXTUAL_LINK)) {
                if (!document.getCurrentLifeCycleState().equals(
                        SiteConstants.DELETED)) {
                    try {
                        Map<String, String> contextualLink = new HashMap<String, String>();
                        contextualLink.put("title", getString(document, "dc:title"));
                        contextualLink.put("description", getString(document, "dc:description"));
                        contextualLink.put("link", getString(document, "clink:link"));
                        contextualLinks.add(contextualLink);
                    } catch (Exception e) {
                        log.debug("Problems retrieving the contextual links for "
                                + documentModel.getTitle(), e);
                    }
                }
            }
        }
        return contextualLinks;
    }

    public static Response getLogoResponse(DocumentModel document) throws Exception {
        Blob blob = getBlob(document, SiteConstants.WEBCONTAINER_LOGO);
        if (blob != null) {
            return Response.ok().entity(blob).type(blob.getMimeType()).build();
        }
        return null;
    }

    /**
     * Get the first mini-site parent
     * */
    public static DocumentModel getFirstWorkspaceParent(CoreSession session,
            DocumentModel doc) throws Exception {
        List<DocumentModel> parents = session.getParentDocuments(doc.getRef());
        Collections.reverse(parents);
        for (DocumentModel currentDocumentModel : parents) {
            if (SiteConstants.WORKSPACE.equals(currentDocumentModel.getType())
                    && currentDocumentModel.hasFacet(
                            SiteConstants.WEB_CONTAINER_FACET)) {
                return currentDocumentModel;
            }
        }
        return null;
    }

    /**
     * Gets the number of comment added on a page (published actually, if the 
     * moderation is on).
     * @param session
     * @param page
     * @return
     * @throws Exception
     */
    public static int getNumberCommentsForPage(CoreSession session,
            DocumentModel page) throws Exception {
        List<DocumentModel> comments = getCommentManager().getComments(page);
        if (isCurrentModerated(session, page)) {
            List<DocumentModel> publishedComments = new ArrayList<DocumentModel>();
            for (DocumentModel comment : comments) {
                if (CommentsConstants.PUBLISHED_STATE.equals(comment.getCurrentLifeCycleState())) {
                    publishedComments.add(comment);
                }

            }
            return publishedComments.size();
        }
        return comments.size();
    }

    /**
     * Retrieves user details for a certain username.
     *
     * @param username
     * @return user first name + user last name
     * @throws Exception
     */
    public static String getUserDetails(String username) throws Exception {
        UserManager userManager = getUserManager();
        NuxeoPrincipal principal = userManager.getPrincipal(username);
        if (principal == null) {
            return StringUtils.EMPTY;
        }
        if (StringUtils.isEmpty(principal.getFirstName())
                && StringUtils.isEmpty(principal.getLastName())) {
            return principal.toString();
        }
        return principal.getFirstName() + " " + principal.getLastName();
    }

    /**
     * Searches a certain webPage between all the pages under a <b>Workspace</b>
     * that contains in title, description , main content or attached files the
     * given searchParam.
     *
     * @param ws the workspace
     * @param searchParam the search parameter
     * @param nrWordsFromDescription the number of words from the page
     *            description
     * @return the <b>WebPage</b>-s found under a <b>Workspace</b> that match
     *         the corresponding criteria
     */
    public static List<Object> searchPagesInSite(CoreSession session, 
            DocumentModel ws, String searchParam, int nrWordsFromDescription)
            throws Exception {
        List<Object> webPages = new ArrayList<Object>();
        if (!StringUtils.isEmpty(searchParam)) {
            DocumentModelList results = SiteQueriesColection.querySearchPages(
                    session, searchParam, ws.getPathAsString());
            for (DocumentModel documentModel : results) {
                try {
                    Map<String, String> page = new HashMap<String, String>();
                    GregorianCalendar creationDate = getGregorianCalendar(
                            documentModel, "dc:created");
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
                            "dd MMMM yyyy",
                            WebEngine.getActiveContext().getLocale());
                    String formattedString = simpleDateFormat.format(creationDate.getTime());
                    page.put("created", formattedString);
                    GregorianCalendar modificationDate = getGregorianCalendar(
                            documentModel, "dc:modified");
                    formattedString = simpleDateFormat.format(modificationDate.getTime());
                    page.put("modified", formattedString);
                    page.put("author", getUserDetails(getString(documentModel, 
                            "dc:creator")));
                    page.put("path", getPagePath(ws, documentModel));
                    page.put("name", getString(documentModel, "dc:title"));
                    page.put("description", getFistNWordsFromString(
                            getString(documentModel, "dc:description"), nrWordsFromDescription));
                    webPages.add(page);
                } catch (Exception e) {
                    log.debug("Problems retrieving pages in site", e);
                }
            }
        }
        return webPages;
    }

    /**
     * Returns the path to all the existing web containers.
     *
     * @return the path to all the existing web containers
     */
    public static StringBuilder getWebContainersPath() {
        WebContext context = WebEngine.getActiveContext();
        StringBuilder initialPath = new StringBuilder(context.getBasePath())
                .append(context.getUriInfo().getMatchedURIs().get(
                            context.getUriInfo().getMatchedURIs().size() - 1));
        return initialPath;
    }

    /**
     * Returns the path for a webPage from a webSite.
     *
     * @param ws the web site
     * @param documentModel the webPage
     * @return the path
     */
    public static String getPagePath(DocumentModel ws,
            DocumentModel documentModel) {
        StringBuilder path = new StringBuilder(getWebContainersPath()).append("/");
        path.append(ws.getPath().segment(ws.getPath().segmentCount() - 1))
                .append("/");
        path.append(JsonAdapter.getRelativPath(ws, documentModel));
        return path.toString();
    }

    /**
     * Creates a web page as document model.
     * @param request
     * @param session
     * @param parentPath
     * @return
     * @throws Exception
     */
    public static DocumentModel createWebPageDocument(HttpServletRequest request, 
            CoreSession session, String parentPath) throws Exception {
        String title = request.getParameter("title");
        String description = request.getParameter("description");
        Boolean isRichtext = Boolean.parseBoolean(request.getParameter("isRichtext"));
        String wikitextEditor = request.getParameter("wikitextEditor");
        String richtextEditor = request.getParameter("richtextEditor");
        String pushToMenu = request.getParameter("pushToMenu");

        DocumentModel documentModel = session.createDocumentModel(parentPath,
                IdUtils.generateId(title + System.currentTimeMillis()),
                SiteConstants.WEBPAGE);
        documentModel.setPropertyValue("dc:title", title);
        documentModel.setPropertyValue("dc:description", description);
        documentModel.setPropertyValue(SiteConstants.WEBPAGE_EDITOR, isRichtext);
        if (isRichtext) {
            // Is rich text editor
            documentModel.setPropertyValue(SiteConstants.WEBPAGE_CONTENT, richtextEditor);
        } else {
            // Is wiki text editor
            documentModel.setPropertyValue(SiteConstants.WEBPAGE_CONTENT, wikitextEditor);
        }
        documentModel.setPropertyValue(SiteConstants.WEBPAGE_PUSHTOMENU, 
                Boolean.valueOf(pushToMenu));

        documentModel = session.createDocument(documentModel);
        documentModel = session.saveDocument(documentModel);
        session.save();

        return documentModel;
    }
    
    /**
     * @return true if the current user is an Administrator
     * @throws Exception
     * */
     public static boolean currentUserIsAdministaror(CoreSession session) {
         return ((NuxeoPrincipal) session.getPrincipal()).isAdministrator();
     }

     /**
      * @return  all users with a given permission for the corresponding workspace
      * @throws Exception
      */
     public static ArrayList<String> getUsersWithPermission(CoreSession session,
             DocumentModel doc, Set<String> permissions) throws Exception {
         DocumentModel parentWorkspace = getFirstWorkspaceParent(session, doc);
         if (parentWorkspace != null) {
             String[] moderators = 
                 parentWorkspace.getACP().listUsernamesForAnyPermission(permissions);
             return new ArrayList<String>(Arrays.asList(moderators));
         }
         return new ArrayList<String>();
     }

     /**
      * @return true if the corresponding workspace is moderated : there is at 
      * least one user with moderate permission on this workspace and the 
      * moderationType is a priori
      * @throws Exception
      */
     public static boolean isCurrentModerated(CoreSession session,
             DocumentModel doc) throws Exception {
         if (!getModerationType(session, doc).equals(
                 SiteConstants.MODERATION_APRIORI)) {
             //no moderation set
             return false;
         }
         //Nuxeo rule: there is at least one user / group with EVERYTHING on a
         //particular document, ergo Moderation permission granted
         //Rux: I leave the code just in case
//         Set<String> moderatePermissions = new HashSet<String>();
//         moderatePermissions.addAll(Arrays.asList(session.getPermissionsToCheck(
//                 WebCommentsConstants.PERMISSION_MODERATE)));
//         if (getUsersWithPermission(session, doc, moderatePermissions).size() <= 0) {
//             return false;
//         }
         return true;
     }

     /**
      * @return true if the current user is between moderators
      * @throws Exception
      */
     public static boolean isModeratedByCurrentUser(CoreSession session,
             DocumentModel doc) throws Exception {
         return session.hasPermission(doc.getRef(), 
                 SiteConstants.PERMISSION_MODERATE);
     }

     /**
      * @return true if the current user has comment permission on this document
      * @throws Exception
      */
     public static boolean currentUserHasCommentPermision(CoreSession session,
             DocumentModel doc) throws Exception {
         return session.hasPermission(doc.getRef(), 
                 SiteConstants.PERMISSION_COMMENT);
     }

     public static CommentManager getCommentManager() throws Exception {
         CommentManager commentManager = Framework.getService(CommentManager.class);
         if (commentManager == null) {
             throw new Exception("Unable to get commentManager");
         }
         return commentManager;
     }

     public static UserManager getUserManager() throws Exception {
         UserManager userManager = Framework.getService(UserManager.class);
         if (userManager == null) {
             throw new Exception("unable to get userManager");
         }
         return userManager;
     }

     /**
      * This method is used to retrieve the <b>WebPage</b> where this
      * <b>WebComment</b> was published
      *
      * @param comment
      * @return the <b>WebPage</b>
      * @throws Exception
      */
     public static DocumentModel getPageForComment(DocumentModel comment)
             throws Exception {
         List<DocumentModel> list = getCommentManager().getDocumentsForComment(comment);
         if (list.size() != 0) {
             DocumentModel page = list.get(0);
             if (!SiteConstants.DELETED.equals(page.getCurrentLifeCycleState())){
                 return page;
             }
         }
         return null;
     }
     
     /**
      * @return all the moderators for the corresponding workspace
      * @throws Exception
      * */
     public static ArrayList<String> getModerators(CoreSession session,
             DocumentModel doc) throws Exception {
         Set<String> moderatePermissions = new HashSet<String>();
         moderatePermissions.addAll(Arrays.asList(
                 session.getPermissionsToCheck(SiteConstants.PERMISSION_MODERATE)));
         return getUsersWithPermission(session, doc, moderatePermissions);
     }
     
     /**
      * @return the moderation type for the corresponding workspace ; 
      * default is aposteriori
      * @throws Exception
      * */
     public static String getModerationType(CoreSession session,
             DocumentModel doc) throws Exception {
         DocumentModel workspaceParent = getFirstWorkspaceParent(session, doc);
         if (workspaceParent != null) {
             return getString(workspaceParent, 
                     SiteConstants.WEBCONTAINER_MODERATION, 
                     SiteConstants.MODERATION_APOSTERIORI);
         }
         return SiteConstants.MODERATION_APOSTERIORI;
     }
     
     public static String getString(DocumentModel d, String xpath,
             String defaultValue) {
         try {
             return getString(d, xpath);
         } catch (ClientException e) {
             return defaultValue;
         }
     }

     public static String getString(DocumentModel d, String xpath)
             throws ClientException {
         Property p = d.getProperty(xpath);
         if (p != null) {
             Serializable v = p.getValue();
             if (v != null) {
                 return v.toString();
             }
         }
         return "";
     }

     public static GregorianCalendar getGregorianCalendar(DocumentModel d,
             String xpath) throws ClientException {
         Property p = d.getProperty(xpath);
         if (p != null) {
             Serializable v = p.getValue();
             if (v != null) {
                 return (GregorianCalendar) v;
             }
         }
         return null;
     }

     public static Blob getBlob(DocumentModel d, String xpath)
             throws ClientException {
         Property p = d.getProperty(xpath);
         if (p != null) {
             Serializable v = p.getValue();
             if (v != null) {
                 return (Blob) v;
             }
         }
         return null;
     }

     public static boolean getBoolean(DocumentModel d, String xpath,
             boolean defaultValue) {
         try {
             return getBoolean(d, xpath);
         } catch (ClientException e) {
             return defaultValue;
         }
     }

     public static boolean getBoolean(DocumentModel d, String xpath)
             throws ClientException {
         Property p = d.getProperty(xpath);
         if (p != null) {
             Serializable v = p.getValue();
             if (v != null) {
                 return (Boolean) v;
             }
         }
         throw new ClientException("value is null");
     }

     public static String getFistNWordsFromString(String string, int n) {
         String[] result = string.split(" ", n + 1);
         StringBuffer firstNwords = new StringBuffer();
         for (int i = 0; i < ((n <= result.length) ? n : result.length); i++) {
             firstNwords.append(result[i]);
             firstNwords.append(" ");

         }
         return new String(firstNwords);
     }


}
