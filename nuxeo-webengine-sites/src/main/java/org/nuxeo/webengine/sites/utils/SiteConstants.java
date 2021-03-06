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
package org.nuxeo.webengine.sites.utils;

/**
 * Utility class used for registering constants.
 */
public final class SiteConstants {

    private SiteConstants() {
    }

    /**
     * Nuxeo document type names
     */
    public static final String CONTEXTUAL_LINK = "ContextualLink";
    public static final String WEBSITE = "WebSite";
    public static final String WORKSPACE = "Workspace";
    public static final String WEBPAGE = "WebPage";
    public static final String WEB_VIEW_FACET = "WebView";

    /**
     * Schemes and fields.
     */
    public static final String WEBCONTAINER_SCHEMA = "webcontainer";
    public static final String WEBCONTAINER_URL = "webc:url";
    public static final String WEBCONTAINER_NAME = "webc:name";
    public static final String WEBCONTAINER_WELCOMETEXT = "webc:welcomeText";
    public static final String WEBCONTAINER_ISWEBCONTAINER = "webc:isWebContainer";
    public static final String WEBCONTAINER_WELCOMEMEDIA = "webc:welcomeMedia";
    public static final String WEBCONTAINER_LOGO = "webc:logo";
    public static final String WEBCONTAINER_MODERATION = "webcontainer:moderationType";
    public static final String WEBCONTAINER_BASELINE = "webcontainer:baseline";
    public static final String WEBPAGE_THEME = "webp:theme";
    public static final String WEBPAGE_THEMEPAGE = "webp:themePage";
    public static final String WEBPAGE_EDITOR = "webp:isRichtext";
    public static final String WEBPAGE_PUSHTOMENU = "webp:pushtomenu";
    public static final String WEBPAGE_CONTENT = "webp:content";

    /**
     * Arguments in FreeMarker
     */
    public static final String PAGE_NAME = "name";
    public static final String SITE_DESCRIPTION = "siteDescription";
    public static final String SEARCH_PARAM = "searchParam";

    /**
     * LifeCycle
     */
    public static final String DELETED = "deleted";

    /**
     * Constants used for Comments
     */
    public static final String PERMISSION_COMMENT = "Comment";
    public static final String PERMISSION_MODERATE = "Moderate";
    public static final String MODERATION_APRIORI = "apriori";
    public static final String MODERATION_APOSTERIORI = "aposteriori";

    /**
     * Themes&Perspectives
     */
    public static final String THEME_BUNDLE = "org.nuxeo.theme.theme";
    public static final String THEME_PERSPECTIVE = "org.nuxeo.theme.perspective";
    public static final String VIEW_PERSPECTIVE = "view";
    public static final String CREATE_PERSPECTIVE = "create";
    public static final String EDIT_PERSPECTIVE = "edit";
    public static final String PAGE_THEME_PAGE = "sites/page";
    public static final String SEARCH_THEME_PAGE = "sites/search";
    public static final String SITES_THEME_PAGE = "sites/default";

}
