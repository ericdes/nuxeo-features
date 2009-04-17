/*
 * (C) Copyright 2006-2009 Nuxeo SAS <http://nuxeo.com> and others
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

package org.nuxeo.theme.editor;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.nuxeo.theme.elements.Element;
import org.nuxeo.theme.properties.FieldIO;
import org.nuxeo.theme.properties.FieldInfo;

public class Utils {

    private static FieldInfo getFieldInfo(Class<?> c, String name) {
        try {
            return c.getField(name).getAnnotation(FieldInfo.class);
        } catch (Exception e) {
        }
        return null;
    }

    public static List<FieldProperty> getPropertiesOf(final Element element) {
        List<FieldProperty> fieldProperties = new ArrayList<FieldProperty>();
        if (element == null) {
            return fieldProperties;
        }
        Properties properties = new Properties();
        try {
            properties = FieldIO.dumpFieldsToProperties(element);
        } catch (Exception e) {
            return fieldProperties;
        }
        if (properties == null) {
            return fieldProperties;
        }
        Class<? extends Element> c = element.getClass();
       Enumeration<?> names = properties.propertyNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            String value = properties.getProperty(name);
            fieldProperties.add(new FieldProperty(name, value.trim(),
                    getFieldInfo(c, name)));
        }
        return fieldProperties;
    }

}
