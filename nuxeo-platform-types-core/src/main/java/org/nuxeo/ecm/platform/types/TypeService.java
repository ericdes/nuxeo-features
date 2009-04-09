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

package org.nuxeo.ecm.platform.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.ArrayUtils;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

public class TypeService extends DefaultComponent implements TypeManager {

    public static final ComponentName ID = new ComponentName(
            "org.nuxeo.ecm.platform.types.TypeService");

    private static final Log log = LogFactory.getLog(TypeService.class);

    private TypeRegistry typeRegistry;

    private TypeWidgetRegistry typeWidgetRegistry;

    @Override
    public void activate(ComponentContext context) {
        typeRegistry = new TypeRegistry();
        typeWidgetRegistry = new TypeWidgetRegistry();
    }

    @Override
    public void deactivate(ComponentContext context) {
        typeRegistry = null;
        typeWidgetRegistry = null;
    }

    @Override
    public void registerExtension(Extension extension) {
        String xp = extension.getExtensionPoint();
        if (xp.equals("types")) {
            registerTypeExtension(extension);
        } else if (xp.equals("default_layout")) {
            registerTypeWidgetExtension(extension);
        }
    }

    @Override
    public void unregisterExtension(Extension extension) {
        String xp = extension.getExtensionPoint();
        if (xp.equals("types")) {
            unregisterTypeExtension(extension);
        }
        if (xp.equals("default_layout")) {
            unregisterTypeWidgetExtension(extension);
        }
    }

    public void registerTypeExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        for (Object contrib : contribs) {
            Type type = (Type) contrib;
            String typeId = type.getId();
            if (type.getRemove()) {
                log.debug("Removing type with id " + typeId);
                typeRegistry.removeType(typeId);
            } else {
                if (typeRegistry.hasType(typeId)) {
                    type = mergeTypes(typeRegistry.getType(typeId), type);
                    typeRegistry.removeType(typeId);
                    log.debug("Merging type with id " + typeId);
                }
                typeRegistry.addType(type);
                log.info("Registered platform document type: " + typeId);
            }
        }
    }

    public void unregisterTypeExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        for (Object contrib : contribs) {
            Type type = (Type) contrib;
            typeRegistry.removeType(type.getId());
        }
    }

    public static Type mergeTypes(Type oldType, Type newType) {
        String icon = newType.getIcon();
        if (icon != null) {
            oldType.setIcon(icon);
        }
        String iconExpanded = newType.getIconExpanded();
        if (icon != null) {
            oldType.setIconExpanded(iconExpanded);
        }
        String label = newType.getLabel();
        if (label != null) {
            oldType.setLabel(label);
        }
        String[] allowedsubTypes = newType.getAllowedSubTypes();
        if (allowedsubTypes != null) {
            oldType.setAllowedSubTypes(ArrayUtils.arrayMerge(
                    oldType.getAllowedSubTypes(), allowedsubTypes));
        }

        // Code added to delete the denied SubType from allowed subtype

        List<String> result = new ArrayList<String>();
        String[] deniedSubTypes = newType.getDeniedSubTypes();
        String[] allowedSubTypes = oldType.getAllowedSubTypes();
        boolean toAdd = true;

        for (String allowedSubType : allowedSubTypes) {
            for (String deniedSubType : deniedSubTypes) {
                if (deniedSubType.equals(allowedSubType)) {
                    toAdd = false;
                    break;
                }
            }
            if (toAdd) {
                result.add(allowedSubType);
            }
            toAdd = true;
        }

        String[] arrayResult = new String[result.size()];
        for (int i = 0; i < result.size(); i++) {
            arrayResult[i] = result.get(i);
        }

        oldType.setAllowedSubTypes(arrayResult);

        // end of added code

        String defaultView = newType.getDefaultView();
        if (defaultView != null) {
            oldType.setDefaultView(defaultView);
        }
        String createView = newType.getCreateView();
        if (createView != null) {
            oldType.setCreateView(createView);
        }
        String editView = newType.getEditView();
        if (editView != null) {
            oldType.setEditView(editView);
        }

        for (TypeView view : newType.getViews()) {
            oldType.setView(view);
        }

        // overwrite old layout
        FieldWidget[] layout = newType.getLayout();
        if (layout != null && layout.length != 0) {
            oldType.setLayout(layout);
        }

        Map<String, Layouts> layouts = newType.getLayouts();
        if (layouts != null && !layouts.isEmpty()) {
            oldType.setLayouts(layouts);
        }

        // TODO: actions

        return oldType;
    }

    public void registerTypeWidgetExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        log.warn("The type widget contribution system is deprecated, "
                + "use the new layout system instead");
        for (Object contrib : contribs) {
            TypeWidget typeWidget = (TypeWidget) contrib;
            typeWidgetRegistry.addTypeWidget(typeWidget);
        }
    }

    public void unregisterTypeWidgetExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        for (Object contrib : contribs) {
            TypeWidget typeWidget = (TypeWidget) contrib;
            typeWidgetRegistry.removeTypeWidget(typeWidget.getFieldtype());
        }
    }

    public TypeRegistry getTypeRegistry() {
        return typeRegistry;
    }

    public TypeWidgetRegistry getTypeWidgetRegistry() {
        return typeWidgetRegistry;
    }

    // Service implementation for TypeManager interface

    public String[] getSuperTypes(String typeName) {
        try {
            SchemaManager schemaMgr = Framework.getService(SchemaManager.class);
            DocumentType type = schemaMgr.getDocumentType(typeName);
            if (type == null) {
                return null;
            }
            type = (DocumentType) type.getSuperType();
            List<String> superTypes = new ArrayList<String>();
            while (type != null) {
                superTypes.add(type.getName());
                type = (DocumentType) type.getSuperType();
            }
            return superTypes.toArray(new String[superTypes.size()]);
        } catch (Exception e) {
            log.error("Failed to lookup the SchemaManager service", e);
            return new String[0];
        }
    }

    public Type getType(String typeName) {
        return typeRegistry.getType(typeName);
    }

    public boolean hasType(String typeName) {
        return typeRegistry.hasType(typeName);
    }

    public Collection<Type> getTypes() {
        Collection<Type> types = new ArrayList<Type>();
        types.addAll(typeRegistry.getTypes());
        return types;
    }

    public Collection<Type> getAllowedSubTypes(String typeName) {
        Collection<Type> allowed = new ArrayList<Type>();
        Type type = getType(typeName);
        if (type != null) {
            for (String subTypeName : type.getAllowedSubTypes()) {
                Type subType = getType(subTypeName);
                if (subType != null) {
                    allowed.add(subType);
                }
            }
        }
        return allowed;
    }

}
