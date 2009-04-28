/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.service;

import org.nuxeo.ecm.platform.annotations.api.AnnotationsService;
import org.nuxeo.ecm.platform.annotations.api.UriResolver;
import org.nuxeo.ecm.platform.annotations.api.AnnotationsConstants.ExtensionPoint;
import org.nuxeo.ecm.platform.annotations.descriptors.AnnotabilityManagerDescriptor;
import org.nuxeo.ecm.platform.annotations.descriptors.AnnotationIDGeneratorDescriptor;
import org.nuxeo.ecm.platform.annotations.descriptors.EventListenerDescriptor;
import org.nuxeo.ecm.platform.annotations.descriptors.MetadataMapperDescriptor;
import org.nuxeo.ecm.platform.annotations.descriptors.PermissionManagerDescriptor;
import org.nuxeo.ecm.platform.annotations.descriptors.PermissionMapperDescriptor;
import org.nuxeo.ecm.platform.annotations.descriptors.URLPatternFilterDescriptor;
import org.nuxeo.ecm.platform.annotations.descriptors.UriResolverDescriptor;
import org.nuxeo.ecm.platform.annotations.proxy.AnnotationServiceProxy;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author Alexandre Russel
 *
 */
public class AnnotationsComponent extends DefaultComponent {

    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.ecm.platform.annotations.services.annotationServiceFactory");

    private AnnotationServiceProxy annotationServiceProxy;

    private final AnnotationConfigurationService configuration = new AnnotationConfigurationServiceImpl();

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        ExtensionPoint point = Enum.valueOf(ExtensionPoint.class,
                extensionPoint);
        switch (point) {
        case uriResolver:
            UriResolver resolver = ((UriResolverDescriptor) contribution).getKlass().newInstance();
            configuration.setUriResolver(resolver);
            break;
        case urlPatternFilter:
            URLPatternFilterDescriptor descriptor = (URLPatternFilterDescriptor) contribution;
            boolean order = descriptor.getOrder().equalsIgnoreCase("Allow,Deny");
            URLPatternFilter filter = new URLPatternFilter(order,
                    descriptor.getDenies(), descriptor.getAllows());
            configuration.setFilter(filter);
            break;
        case metadataMapper:
            MetadataMapper mapper = ((MetadataMapperDescriptor) contribution).getKlass().newInstance();
            configuration.setMetadataMapper(mapper);
            break;
        case permissionManager:
            PermissionManager manager = ((PermissionManagerDescriptor) contribution).getKlass().newInstance();
            configuration.setPermissionManager(manager);
            break;
        case annotabilityManager:
            AnnotabilityManager annotabilityManager = ((AnnotabilityManagerDescriptor) contribution).getKlass().newInstance();
            configuration.setAnnotabilityManager(annotabilityManager);
            break;
        case eventListener:
            Class<? extends EventListener> listener = ((EventListenerDescriptor) contribution).getListener();
            configuration.addListener(listener.newInstance());
            break;
        case annotationIDGenerator:
            AnnotationIDGenerator generator = ((AnnotationIDGeneratorDescriptor) contribution).getKlass().newInstance();
            configuration.setIDGenerator(generator);
            break;
        case permissionMapper:
            configuration.setPermissionMapper((PermissionMapperDescriptor) contribution);
            break;
        }
    }

    @Override
    public void activate(ComponentContext context) throws Exception {
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (AnnotationsService.class.isAssignableFrom(adapter)) {
            return (T) getAnnotationServiceProxy();
        } else if (AnnotationConfigurationService.class.isAssignableFrom(adapter)) {
            return (T) configuration;
        }
        return null;
    }

    private AnnotationsService getAnnotationServiceProxy() {
        if (annotationServiceProxy == null) {
            annotationServiceProxy = new AnnotationServiceProxy();
            annotationServiceProxy.initialise();
        }
        return annotationServiceProxy;
    }

}
