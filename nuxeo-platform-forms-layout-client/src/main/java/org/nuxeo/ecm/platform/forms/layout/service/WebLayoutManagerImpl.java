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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: WebLayoutManagerImpl.java 28510 2008-01-06 10:21:44Z sfermigier $
 */

package org.nuxeo.ecm.platform.forms.layout.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.el.VariableMapper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinWidgetModes;
import org.nuxeo.ecm.platform.forms.layout.api.Layout;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutRow;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutRowDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetType;
import org.nuxeo.ecm.platform.forms.layout.api.exceptions.LayoutException;
import org.nuxeo.ecm.platform.forms.layout.api.exceptions.WidgetException;
import org.nuxeo.ecm.platform.forms.layout.api.impl.LayoutImpl;
import org.nuxeo.ecm.platform.forms.layout.api.impl.LayoutRowImpl;
import org.nuxeo.ecm.platform.forms.layout.api.impl.WidgetImpl;
import org.nuxeo.ecm.platform.forms.layout.api.impl.WidgetTypeImpl;
import org.nuxeo.ecm.platform.forms.layout.descriptors.WidgetTypeDescriptor;
import org.nuxeo.ecm.platform.forms.layout.facelets.RenderVariables;
import org.nuxeo.ecm.platform.forms.layout.facelets.WidgetTypeHandler;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

import com.sun.facelets.FaceletContext;
import com.sun.facelets.FaceletHandler;
import com.sun.facelets.el.VariableMapperWrapper;
import com.sun.facelets.tag.TagConfig;

/**
 * Layout service implementation.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */
public class WebLayoutManagerImpl extends DefaultComponent implements
        WebLayoutManager {

    public static final ComponentName NAME = new ComponentName(
            WebLayoutManagerImpl.class.getName());

    public static final String WIDGET_TYPES_EP_NAME = "widgettypes";

    public static final String WIDGETS_EP_NAME = "widgets";

    public static final String LAYOUTS_EP_NAME = "layouts";

    private static final long serialVersionUID = -4778456059717447736L;

    private static final Log log = LogFactory.getLog(WebLayoutManagerImpl.class);

    private final Map<String, WidgetType> widgetTypeRegistry;

    private final Map<String, LayoutDefinition> layoutRegistry;

    private final Map<String, WidgetDefinition> widgetRegistry;

    public WebLayoutManagerImpl() {
        widgetTypeRegistry = new HashMap<String, WidgetType>();
        layoutRegistry = new HashMap<String, LayoutDefinition>();
        widgetRegistry = new HashMap<String, WidgetDefinition>();
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals(WIDGET_TYPES_EP_NAME)) {
            registerWidgetType(contribution);
        } else if (extensionPoint.equals(LAYOUTS_EP_NAME)) {
            registerLayout(contribution);
        } else if (extensionPoint.equals(WIDGETS_EP_NAME)) {
            registerWidget(contribution);
        } else {
            log.error(String.format(
                    "Unknown extension point %s, can't register !",
                    extensionPoint));
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals(WIDGET_TYPES_EP_NAME)) {
            unregisterWidgetType(contribution);
        } else if (extensionPoint.equals(LAYOUTS_EP_NAME)) {
            unregisterLayout(contribution);
        } else if (extensionPoint.equals(WIDGETS_EP_NAME)) {
            unregisterWidget(contribution);
        } else {
            log.error(String.format(
                    "Unknown extension point %s, can't unregister !",
                    extensionPoint));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(WebLayoutManager.class)) {
            return (T) this;
        }
        return null;
    }

    // widget types

    private void registerWidgetType(Object contribution) {
        WidgetTypeDescriptor desc = (WidgetTypeDescriptor) contribution;
        String name = desc.getName();
        if (widgetTypeRegistry.containsKey(name)) {
            log.warn(String.format("Overriding definition for widget type %s",
                    name));
            widgetTypeRegistry.remove(name);
        }
        String className = desc.getHandlerClassName();
        if (className == null) {
            log.error("Handler class missing " + "for widget type " + name);
            return;
        }
        Class<?> widgetTypeClass;
        try {
            // Thread context loader is not working in isolated EARs
            widgetTypeClass = WebLayoutManagerImpl.class.getClassLoader().loadClass(
                    className);
        } catch (Exception e) {
            log.error("Caught error when instanciating widget type handler", e);
            return;
        }
        WidgetType widgetType = new WidgetTypeImpl(name, widgetTypeClass,
                desc.getProperties());
        widgetTypeRegistry.put(name, widgetType);
        log.info("Registered widget type: " + name);
    }

    private void unregisterWidgetType(Object contribution) {
        WidgetTypeDescriptor desc = (WidgetTypeDescriptor) contribution;
        String name = desc.getName();
        if (widgetTypeRegistry.containsKey(name)) {
            widgetTypeRegistry.remove(name);
            log.debug("Unregistered widget type: " + name);
        }
    }

    // layouts

    private void registerLayout(Object contribution) {
        LayoutDefinition layoutDef = (LayoutDefinition) contribution;
        String name = layoutDef.getName();
        if (layoutRegistry.containsKey(name)) {
            // TODO: implement merge
            layoutRegistry.remove(name);
        }
        layoutRegistry.put(name, layoutDef);
        log.info("Registered layout: " + name);
    }

    private void unregisterLayout(Object contribution) {
        LayoutDefinition layoutDef = (LayoutDefinition) contribution;
        String name = layoutDef.getName();
        if (layoutRegistry.containsKey(name)) {
            layoutRegistry.remove(name);
            log.debug("Unregistered layout: " + name);
        }
    }

    // layouts

    private void registerWidget(Object contribution) {
        WidgetDefinition widgetDef = (WidgetDefinition) contribution;
        String name = widgetDef.getName();
        if (widgetRegistry.containsKey(name)) {
            // TODO: implement merge
            widgetRegistry.remove(name);
        }
        widgetRegistry.put(name, widgetDef);
        log.info("Registered widget: " + name);
    }

    private void unregisterWidget(Object contribution) {
        WidgetDefinition widgetDef = (WidgetDefinition) contribution;
        String name = widgetDef.getName();
        if (widgetRegistry.containsKey(name)) {
            widgetRegistry.remove(name);
            log.debug("Unregistered widget: " + name);
        }
    }

    // service api

    public WidgetType getWidgetType(String typeName) {
        return widgetTypeRegistry.get(typeName);
    }

    public LayoutDefinition getLayoutDefinition(String layoutName) {
        return layoutRegistry.get(layoutName);
    }

    public WidgetDefinition getWidgetDefinition(String widgetName) {
        return widgetRegistry.get(widgetName);
    }

    public WidgetTypeHandler getWidgetTypeHandler(String typeName)
            throws WidgetException {
        WidgetType type = getWidgetType(typeName);
        if (type == null) {
            return null;
        }
        WidgetTypeHandler handler;
        try {
            // Thread context loader is not working in isolated EARs
            handler = (WidgetTypeHandler) type.getWidgetTypeClass().newInstance();
        } catch (Exception e) {
            log.error("Caught error when instanciating widget type handler", e);
            return null;
        }
        // set properties
        handler.setProperties(type.getProperties());
        return handler;
    }

    /**
     * Evaluates an EL expression in given context.
     * <p>
     * If the expression resolves to an EL expression, evaluate it again this is
     * useful when retrieving the expression from a configuration file.
     * <p>
     * If given context is null, do no try to evaluate it and return the
     * expression itself.
     *
     * @param context the facelet context.
     * @param expression the string expression.
     * @return
     */
    private static Object evaluateExpression(FaceletContext context,
            String expression) {
        if (expression == null) {
            return null;
        }
        if (context == null) {
            return expression;
        }
        ExpressionFactory eFactory = context.getExpressionFactory();
        ELContext elContext = context.getFacesContext().getELContext();
        ValueExpression expr = eFactory.createValueExpression(context,
                expression, Object.class);
        Object value = expr.getValue(elContext);
        if (value != null && value instanceof String) {
            String strValue = (String) value;
            if (strValue.contains("#{")
                    && (strValue.indexOf("#{") < strValue.indexOf('}'))) {
                // evaluate again
                expr = eFactory.createValueExpression(context, strValue,
                        Object.class);
                value = expr.getValue(elContext);
            }
        }
        return value;
    }

    /**
     * Evaluates an expression to a boolean value.
     */
    private static Boolean getBooleanValue(FaceletContext context,
            String expression) {
        Object value = evaluateExpression(context, expression);
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value == null || value instanceof String) {
            return Boolean.valueOf((String) value);
        } else {
            log.error("Could not get boolean value for " + value);
            return false;
        }
    }

    /**
     * Evaluates an expression to a string value.
     */
    private static String getStringValue(FaceletContext context,
            String expression) {
        Object value = evaluateExpression(context, expression);
        if (value == null || value instanceof String) {
            return (String) value;
        } else {
            log.error("Could not get string value for " + value);
            return null;
        }
    }

    private static String getModeFromLayoutMode(FaceletContext context,
            WidgetDefinition wDef, String layoutMode) {
        String wMode = getStringValue(context, wDef.getMode(layoutMode));
        if (wMode == null) {
            if (BuiltinModes.CREATE.equals(layoutMode)
                    || BuiltinModes.EDIT.equals(layoutMode)
                    || BuiltinModes.SEARCH.equals(layoutMode)) {
                wMode = BuiltinWidgetModes.EDIT;
            } else {
                wMode = BuiltinWidgetModes.VIEW;
            }
        }
        return wMode;
    }

    /**
     * Computes a widget from a definition for a mode in a given context.
     * <p>
     * If the widget is configured not to be rendered in the given mode, returns
     * null.
     * <p>
     * Sub widgets are also computed recursively.
     */
    private Widget getWidget(FaceletContext context, LayoutDefinition lDef,
            WidgetDefinition wDef, String layoutMode, String valueName,
            int level) {
        VariableMapper orig = null;
        // avoid variable mapper changes if context is null for tests
        if (context != null) {
            // expose widget mode so that it can be used in a mode el expression
            orig = context.getVariableMapper();
            VariableMapper vm = new VariableMapperWrapper(orig);
            context.setVariableMapper(vm);
            ExpressionFactory eFactory = context.getExpressionFactory();
            ValueExpression modeVe = eFactory.createValueExpression(layoutMode,
                    String.class);
            vm.setVariable(RenderVariables.globalVariables.mode.name(), modeVe);
        }
        String wMode = getModeFromLayoutMode(context, wDef, layoutMode);
        if (context != null) {
            context.setVariableMapper(orig);
        }

        if (BuiltinWidgetModes.HIDDEN.equals(wMode)) {
            return null;
        }
        List<Widget> subWidgets = new ArrayList<Widget>();
        for (WidgetDefinition swDef : wDef.getSubWidgetDefinitions()) {
            Widget subWidget = getWidget(context, lDef, swDef, wMode,
                    valueName, level + 1);
            if (subWidget != null) {
                subWidgets.add(subWidget);
            }
        }

        boolean required = getBooleanValue(context, wDef.getRequired(
                layoutMode, wMode));
        Widget widget = new WidgetImpl(lDef.getName(), wDef.getName(), wMode,
                wDef.getType(), valueName, wDef.getFieldDefinitions(),
                wDef.getLabel(layoutMode), wDef.getHelpLabel(layoutMode),
                wDef.isTranslated(), wDef.getProperties(layoutMode, wMode),
                required, subWidgets.toArray(new Widget[] {}), level);
        return widget;
    }

    public Layout getLayout(FaceletContext ctx, String layoutName, String mode,
            String valueName) throws LayoutException {
        LayoutDefinition lDef = getLayoutDefinition(layoutName);
        if (lDef == null) {
            throw new LayoutException(String.format("Layout %s not found",
                    layoutName));
        }
        if (ctx == null) {
            log.warn("Layout creation computed in a null facelet context: expressions "
                    + "found in the layout definition will not be evaluated");
        }
        LayoutRowDefinition[] rowsDef = lDef.getRows();
        List<LayoutRow> rows = new ArrayList<LayoutRow>();
        for (LayoutRowDefinition rowDef : rowsDef) {
            boolean emptyRow = true;
            List<Widget> widgets = new ArrayList<Widget>();
            for (String widgetName : rowDef.getWidgets()) {
                if (widgetName == null || widgetName.length() == 0) {
                    // no widget at this place
                    widgets.add(null);
                    continue;
                }
                WidgetDefinition wDef = lDef.getWidgetDefinition(widgetName);
                if (wDef == null) {
                    // try in global registry
                    wDef = getWidgetDefinition(widgetName);
                }
                if (wDef == null) {
                    log.error(String.format("Widget %s not found in layout %s",
                            widgetName, layoutName));
                    widgets.add(null);
                    continue;
                }
                Widget widget = getWidget(ctx, lDef, wDef, mode, valueName, 0);
                if (widget != null) {
                    emptyRow = false;
                }
                widgets.add(widget);
            }
            if (!emptyRow) {
                rows.add(new LayoutRowImpl(widgets));
            }
        }
        int columns = lDef.getColumns();
        Layout layout = new LayoutImpl(lDef.getName(), mode,
                lDef.getTemplate(mode), rows, columns);
        return layout;
    }

    public FaceletHandler getFaceletHandler(FaceletContext ctx,
            TagConfig config, Widget widget) {
        String widgetTypeName = widget.getType();
        WidgetTypeHandler handler = getWidgetTypeHandler(widgetTypeName);
        if (handler == null) {
            log.error("No widget handler found for type " + widgetTypeName);
        } else {
            FaceletHandler[] subHandlers = null;
            Widget[] subWidgets = widget.getSubWidgets();
            if (subWidgets != null) {
                List<FaceletHandler> subHandlersList = new ArrayList<FaceletHandler>();
                for (Widget subWidget : subWidgets) {
                    subHandlersList.add(getFaceletHandler(ctx, config,
                            subWidget));
                }
                subHandlers = subHandlersList.toArray(new FaceletHandler[] {});
            }
            FaceletHandler fHandler = handler.getFaceletHandler(ctx, config,
                    widget, subHandlers);
            return fHandler;
        }
        return null;
    }

    public Widget createWidget(FaceletContext ctx, String type, String mode,
            String valueName, Map<String, Serializable> properties,
            Widget[] subWidgets) {
        Serializable requiredProp = properties.get(WidgetDefinition.REQUIRED_PROPERTY_NAME);
        boolean required = false;
        if (requiredProp != null) {
            if (requiredProp instanceof String) {
                required = getBooleanValue(ctx, (String) requiredProp);
            } else {
                log.error(String.format(
                        "Invalid property \"%s\" on widget: %s",
                        WidgetDefinition.REQUIRED_PROPERTY_NAME, requiredProp));
            }
        }
        Widget widget = new WidgetImpl("layout", "widget", mode, type,
                valueName, null, null, null, required, properties, required,
                subWidgets, 0);
        return widget;
    }
}
