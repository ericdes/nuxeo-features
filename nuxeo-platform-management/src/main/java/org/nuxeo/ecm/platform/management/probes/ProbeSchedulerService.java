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
 *     matic
 */
package org.nuxeo.ecm.platform.management.probes;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.listener.AbstractEventListener;
import org.nuxeo.ecm.core.listener.CoreEventListenerService;
import org.nuxeo.ecm.platform.scheduler.core.interfaces.SchedulerRegistry;
import org.nuxeo.ecm.platform.scheduler.core.service.SchedulerRegistryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.ManagementRuntimeException;
import org.nuxeo.runtime.management.ObjectNameFactory;
import org.nuxeo.runtime.management.ResourcePublisher;
import org.nuxeo.runtime.management.ResourcePublisherService;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author Stephane Lacoin (Nuxeo EP Software Engineer)
 *
 */
public class ProbeSchedulerService extends DefaultComponent implements
        ProbeScheduler, ProbeSchedulerMBean {

    protected static ComponentName NAME = new ComponentName(
            ProbeScheduler.class.getCanonicalName());

    protected static String SCHEDULE_ID = ProbeScheduler.class.getSimpleName();

    public ProbeSchedulerService() {
        super(); // enables breaking
    }

    protected class Scheduler implements
            org.nuxeo.ecm.platform.scheduler.core.interfaces.Schedule {
        public String getCronExpression() {
            return "0 0/5 * * * ?";
        }

        public String getEventCategory() {
            return "default";
        }

        public String getEventId() {
            return SCHEDULE_ID;
        }

        public String getId() {
            return SCHEDULE_ID;
        }

        public String getPassword() {
            return "";
        }

        public String getUsername() {
            return SCHEDULE_ID;
        }

        protected SchedulerRegistryService registry;

        protected void doSchedule() {
            registry = (SchedulerRegistryService)Framework.getLocalService(SchedulerRegistry.class);
            if (registry == null) {
                throw new ManagementRuntimeException(
                        "no scheduler registry service available");
            }
            registry.registerSchedule(this);
        }

        protected void doUnschedule() {
            if (registry == null) {
                return;
            }
            registry.unregisterSchedule(this);
        }
    }

    protected final Scheduler scheduler = new Scheduler();

    protected class ScheduleEventListener extends AbstractEventListener {

        @Override
        public boolean accepts(String eventId) {
            return eventId.equals(SCHEDULE_ID);
        }

        @Override
        public void handleEvent(CoreEvent coreEvent) throws Exception {
            runnerRegistry.doRun();
        }

        protected CoreEventListenerService service;

        protected void doListen() {
            service = Framework.getLocalService(CoreEventListenerService.class);
            if (service == null) {
                throw new ManagementRuntimeException(
                        "no event listener service available");
            }
            service.addEventListener(this);
        }

        protected void doUnlisten() {
            if (service == null) {
                return;
            }
            service.removeEventListener(this);
        }
    }

    protected final ScheduleEventListener scheduleEventListener = new ScheduleEventListener();

    protected Set<String> doExtractProbesName(
            Collection<ProbeContext> runners) {
        Set<String> names = new HashSet<String>();
        for (ProbeContext runner : runners) {
            names.add(runner.shortcutName);
        }
        return names;
    }

    public Set<String> getScheduledProbes() {
        return doExtractProbesName(runnerRegistry.scheduledProbesContext.values());
    }

    public int getScheduledProbesCount() {
        return runnerRegistry.scheduledProbesContext.size();
    }

    public Set<String> getProbesInError() {
        return doExtractProbesName(runnerRegistry.failedProbesContext);
    }

    public int getProbesInErrorCount() {
        return runnerRegistry.failedProbesContext.size();
    }

    public Set<String> getProbesInSuccess() {
        return doExtractProbesName(runnerRegistry.succeedProbesContext);
    }

    public int getProbesInSuccessCount() {
        return runnerRegistry.succeedProbesContext.size();
    }

    public void disable() {
        runnerRegistry.disable();
    }

    public void enable() {
        runnerRegistry.enable();
    }

    public boolean isEnabled() {
        return runnerRegistry.isEnabled();
    }
    
    protected class ManagementPublisher {

        protected ResourcePublisherService service;

        protected void doPublish() {
            service.registerResource("probe-scheduler",
                    ObjectNameFactory.formatProbeQualifiedName(NAME),
                    ProbeSchedulerMBean.class, ProbeSchedulerService.this);
        }

        protected void doUnpublish() {
            service.unregisterResource("probe-scheduler",
                    ObjectNameFactory.formatProbeQualifiedName(NAME));
            for (ProbeContext context : runnerRegistry.scheduledProbesContext.values()) {
                doUnpublishContext(context);
            }
            service = null;
        }

        protected void doPublishContext(ProbeContext context) {
            if (service == null) {
                return;
            }
            service.registerResource(context.shortcutName,
                    context.qualifiedName, ProbeMBean.class,
                    context);
        }

        protected void doUnpublishContext(ProbeContext context) {
            if (service == null) {
                return;
            }
            service.unregisterResource(context.shortcutName,
                    context.qualifiedName);
        }

        protected void doQualifyNames(ProbeContext context,
                ProbeDescriptor descriptor) {
            context.shortcutName = ObjectNameFactory.formatProbeShortName(descriptor.getShortcutName());
            context.qualifiedName = descriptor.getQualifiedName();
            if (context.qualifiedName == null) {
                context.qualifiedName = ObjectNameFactory.formatProbeQualifiedName(new ComponentName(
                        descriptor.getServiceClass().getCanonicalName()));
            }
        }
    }

    protected ManagementPublisher managementPublisher = new ManagementPublisher();

    protected class RunnerRegistry {

        protected final Map<Class<? extends Probe>, ProbeContext> scheduledProbesContext = new HashMap<Class<? extends Probe>, ProbeContext>();

        protected Set<ProbeContext> failedProbesContext = new HashSet<ProbeContext>();

        protected Set<ProbeContext> succeedProbesContext = new HashSet<ProbeContext>();

        protected void doRegisterProbe(ProbeDescriptor descriptor) {
            Class<? extends Probe> probeClass = descriptor.getProbeClass();
            Class<?> serviceClass = descriptor.getServiceClass();
            Object service = Framework.getLocalService(serviceClass);
            Probe probe;
            try {
                probe = probeClass.newInstance();
            } catch (Exception e) {
                throw new ManagementRuntimeException(
                        "Cannot create management probe for " + descriptor);
            }
            probe.init(service);
            ProbeContext context = new ProbeContext(
                    ProbeSchedulerService.this, probe, "default");
            managementPublisher.doQualifyNames(context, descriptor);
            managementPublisher.doPublishContext(context);
            scheduledProbesContext.put(probeClass, context);
        }

        protected void doUnregisterProbe(ProbeDescriptor descriptor) {
            Class<? extends Probe> probeClass = descriptor.getProbeClass();
            ProbeContext context = scheduledProbesContext.remove(probeClass);
            if (context == null) {
                throw new IllegalArgumentException("not registered probe"
                        + descriptor);
            }
            managementPublisher.doUnpublishContext(context);
        }

        protected void doRun() {

            if (!isEnabled) {
                return;
            }

            for (ProbeContext context : scheduledProbesContext.values()) {
                try {
                    context.runner.runWithSafeClassLoader();
                    failedProbesContext.remove(context);
                    succeedProbesContext.add(context);
                } catch (Exception e) {
                    succeedProbesContext.remove(context);
                    failedProbesContext.add(context);
                }
            }
        }

        protected boolean isEnabled = true;

        public void enable() {
            isEnabled = true;
        }

        public void disable() {
            isEnabled = false;
        }

        public boolean isEnabled() {
            return isEnabled;
        }
    }

    protected RunnerRegistry runnerRegistry = new RunnerRegistry();

    @Override
    public void activate(ComponentContext context) throws Exception {
        scheduler.doSchedule();
        managementPublisher.service = (ResourcePublisherService) Framework.getLocalService(ResourcePublisher.class);
        managementPublisher.doPublish();
        scheduleEventListener.doListen();
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        runnerRegistry.isEnabled = false;
        scheduler.doSchedule();
        scheduleEventListener.doUnlisten();
        managementPublisher.doUnpublish();
    }

    public static final String PROBES_EXT_KEY = "probes";

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (extensionPoint.equals(PROBES_EXT_KEY)) {
            runnerRegistry.doRegisterProbe((ProbeDescriptor) contribution);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (extensionPoint.equals(PROBES_EXT_KEY)) {
            runnerRegistry.doUnregisterProbe((ProbeDescriptor) contribution);
        }
    }

    public ProbeContext getScheduledRunner(
            Class<? extends Probe> usecaseClass) {
        ProbeContext runner = runnerRegistry.scheduledProbesContext.get(usecaseClass);
        if (runner == null) {
            throw new IllegalArgumentException("no probe scheduled for "
                    + usecaseClass);
        }
        return runner;
    }

    public Collection<ProbeContext> getScheduledProbesContext() {
        return runnerRegistry.scheduledProbesContext.values();
    }

}
