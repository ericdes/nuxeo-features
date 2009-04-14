/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: ComponentUtils.java 28924 2008-01-10 14:04:05Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.util;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import javax.el.ValueExpression;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.RFC2231;
import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.core.api.Blob;

/**
 * Generic component helper methods.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */
public final class ComponentUtils {

    public static final String WHITE_SPACE_CHARACTER = "&#x0020;";

    private static final Log log = LogFactory.getLog(ComponentUtils.class);

    // Utility class.
    private ComponentUtils() {
    }

    /**
     * Calls a component encodeBegin/encodeChildren/encodeEnd methods.
     *
     * @param context
     * @param component
     * @throws IOException
     */
    public static void encodeComponent(FacesContext context,
            UIComponent component) throws IOException {
        component.encodeBegin(context);
        component.encodeChildren(context);
        component.encodeEnd(context);
    }

    /**
     * Helper method meant to be called in the component constructor.
     * <p>
     * When adding sub components dynamically, the tree fetching could be a
     * problem so all possible sub components must be added.
     * <p>
     * By default initiated component are marked as not rendered.
     *
     * @param parent
     * @param child
     * @param facetName facet name to put the child in.
     */
    public static void initiateSubComponent(UIComponent parent,
            String facetName, UIComponent child) {
        parent.getFacets().put(facetName, child);
        child.setRendered(false);
    }

    /**
     * Add a sub component to a UI component, marking is as rendered.
     *
     * @param context
     * @param parent
     * @param child
     * @param defaultChildId
     * @return child comp
     */
    public static UIComponent hookSubComponent(FacesContext context,
            UIComponent parent, UIComponent child, String defaultChildId) {
        // build a valid id using the parent id so that it's found everytime.
        String childId = child.getId();
        if (defaultChildId != null) {
            // override with default
            childId = defaultChildId;
        }
        // make sure it's set
        if (childId == null) {
            childId = context.getViewRoot().createUniqueId();
        }
        // reset client id
        child.setId(childId);
        child.setParent(parent);
        child.setRendered(true);
        return child;
    }

    /**
     * Copies attributes and value expressions with given name from parent
     * component to child component.
     *
     * @param parent
     * @param child
     * @param valueNames
     */
    public static void copyValues(UIComponent parent, UIComponent child,
            String[] valueNames) {
        Map<String, Object> parentAttributes = parent.getAttributes();
        Map<String, Object> childAttributes = child.getAttributes();
        for (String name : valueNames) {
            // attributes
            if (parentAttributes.containsKey(name)) {
                childAttributes.put(name, parentAttributes.get(name));
            }
            // value expressions
            ValueExpression ve = parent.getValueExpression(name);
            if (ve != null) {
                child.setValueExpression(name, ve);
            }
        }
    }

    public static void copyLinkValues(UIComponent parent, UIComponent child) {
        String[] valueNames = { "accesskey", "charset", "coords", "dir",
                "disabled", "hreflang", "lang", "onblur", "onclick",
                "ondblclick", "onfocus", "onkeydown", "onkeypress", "onkeyup",
                "onmousedown", "onmousemove", "onmouseout", "onmouseover",
                "onmouseup", "rel", "rev", "shape", "style", "styleClass",
                "tabindex", "target", "title", "type" };
        copyValues(parent, child, valueNames);
    }

    public static Object getAttributeValue(UIComponent component,
            String attributeName, Object defaultValue) {
        Object value = component.getAttributes().get(attributeName);
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }

    public static String download(FacesContext faces, Blob blob, String filename) {
        if (!faces.getResponseComplete()) {
            ExternalContext econtext = faces.getExternalContext();
            Map<String, String> map = econtext.getRequestParameterMap();
            // do not perform download in an ajax request
            if (map != null && map.containsKey("AJAXREQUEST")) {
                return null;
            }
            if (blob == null) {
                log.error("No bytes available for the file: " + filename);
            } else {
                HttpServletResponse response = (HttpServletResponse) econtext.getResponse();
                if (filename == null || filename.length() == 0) {
                    filename = "file";
                }
                HttpServletRequest request = (HttpServletRequest) econtext.getRequest();
                boolean inline = request.getParameter("inline") != null;
                String userAgent = request.getHeader("User-Agent");
                String contentDisposition = RFC2231.encodeContentDisposition(
                        filename, inline, userAgent);
                response.setHeader("Content-Disposition", contentDisposition);
                log.debug("Downloading with mime/type : " + blob.getMimeType());
                response.setContentType(blob.getMimeType());
                try {
                    blob.transferTo(response.getOutputStream());
                    response.flushBuffer();
                } catch (IOException e) {
                    // XXX: better throw the exception instead of hiding the
                    // root cause of a potential error
                    log.error("Error while downloading the file: " + filename);
                }
                faces.responseComplete();
            }
        }
        return null;
    }

    // hook translation passing faces context

    public static String translate(FacesContext context, String messageId) {
        return translate(context, messageId, (Object[]) null);
    }

    public static String translate(FacesContext context, String messageId,
            Object... params) {
        String bundleName = context.getApplication().getMessageBundle();
        Locale locale = context.getViewRoot().getLocale();
        return I18NUtils.getMessageString(bundleName, messageId, params, locale);
    }

    public static void addErrorMessage(FacesContext context,
            UIComponent component, String message) {
        addErrorMessage(context, component, message, null);
    }

    public static void addErrorMessage(FacesContext context,
            UIComponent component, String message, Object[] params) {
        String bundleName = context.getApplication().getMessageBundle();
        Locale locale = context.getViewRoot().getLocale();
        message = I18NUtils.getMessageString(bundleName, message, params,
                locale);
        FacesMessage msg = new FacesMessage(message);
        msg.setSeverity(FacesMessage.SEVERITY_ERROR);
        context.addMessage(component.getClientId(context), msg);
    }

}
