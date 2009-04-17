/*
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme.webwidgets.ui;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.theme.webwidgets.Manager;

public class Editor {

    /* Widget actions */
    public static void addWidget(String providerName, String widgetTypeName,
            String region, int order) throws WidgetEditorException {
        try {
            Manager.addWidget(providerName, widgetTypeName, region, order);
        } catch (Exception e) {
            throw new WidgetEditorException(e.getMessage(), e);
        }
    }

    public static String moveWidget(String srcProviderName,
            String destProviderName, String srcUid, String srcRegionName,
            String destRegionName, int destOrder) throws WidgetEditorException {
        try {
            return Manager.moveWidget(srcProviderName, destProviderName,
                    srcUid, srcRegionName, destRegionName, destOrder);
        } catch (Exception e) {
            throw new WidgetEditorException(e.getMessage(), e);
        }
    }

    public static void removeWidget(String providerName, String uid)
            throws WidgetEditorException {
        try {
            Manager.removeWidget(providerName, uid);
        } catch (Exception e) {
            throw new WidgetEditorException(e.getMessage(), e);
        }
    }

    /* Widget states */
    public static void setWidgetState(String providerName, String widgetUid,
            String state) throws WidgetEditorException {
        try {
            Manager.setWidgetState(providerName, widgetUid, state);
        } catch (Exception e) {
            throw new WidgetEditorException(e.getMessage(), e);
        }
    }

    /* Widget preferences */
    public static void setWidgetPreference(String providerName,
            String widgetUid, String dataName, String src)
            throws WidgetEditorException {
        try {
            Manager.setWidgetPreference(providerName, widgetUid, dataName, src);
        } catch (Exception e) {
            throw new WidgetEditorException(e.getMessage(), e);
        }
    }

    public static void updateWidgetPreferences(String providerName,
            String widgetUid, Map<String, String> preferencesMap)
            throws WidgetEditorException {
        try {
            Manager.updateWidgetPreferences(providerName, widgetUid,
                    preferencesMap);
        } catch (Exception e) {
            throw new WidgetEditorException(e.getMessage(), e);
        }
    }

    public static String uploadFile(HttpServletRequest req,
            String providerName, String widgetUid, String dataName)
            throws WidgetEditorException {
        try {
            return Manager.uploadFile(req, providerName, widgetUid, dataName);
        } catch (Exception e) {
            throw new WidgetEditorException(e.getMessage(), e);
        }
    }

}
