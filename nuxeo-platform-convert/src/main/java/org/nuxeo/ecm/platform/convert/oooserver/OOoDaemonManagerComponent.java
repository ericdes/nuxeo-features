package org.nuxeo.ecm.platform.convert.oooserver;

import java.lang.Thread.UncaughtExceptionHandler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;

import com.anwrt.ooserver.daemon.Config;
import com.anwrt.ooserver.daemon.Log4JLogger;
import com.sun.star.bridge.XUnoUrlResolver;
import com.sun.star.comp.helper.Bootstrap;
import com.sun.star.container.XNameAccess;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

public class OOoDaemonManagerComponent extends DefaultComponent implements
        OOoDaemonService , FrameworkListener {

    protected static Thread runner;
    protected static OOoServerDescriptor serverDescriptor = new OOoServerDescriptor();
    protected static Config daemonConfig = null;
    protected static boolean configured = false;
    protected static String SERVER_CONFIG_EP ="oooServerConfig";

    private static final Log log = LogFactory.getLog(OOoDaemonManagerComponent.class);

    /** Component impl **/

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        if (isAvailable() && isRunning()) {
            stopDaemon();
        }
    }

    @Override
    public void activate(ComponentContext context) throws Exception {
        context.getRuntimeContext().getBundle().getBundleContext().addFrameworkListener(this);
    }

    public void registerContribution(Object contribution, String extensionPoint,
            ComponentInstance contributor) throws Exception {
        if (SERVER_CONFIG_EP.equals(extensionPoint)) {
            OOoServerDescriptor desc = (OOoServerDescriptor) contribution;
            serverDescriptor=desc;
            Log4JLogger.logInfoAsDebug = desc.getLogInfoAsDebug();
        }
    }

    public void unregisterContribution(Object contribution, String extensionPoint,
            ComponentInstance contributor) throws Exception {
    }

    /** Service interface **/
    protected Config getOrBuildConfig() {
        if (daemonConfig == null) {
            ConfigBuilderHelper helper = new ConfigBuilderHelper(
                    serverDescriptor);
            daemonConfig = helper.getServerConfig();
            if (daemonConfig != null) {
                configured = true;
            }
        }
        return daemonConfig;
    }

    public boolean isConfigured() {
        if (!configured) {
            getOrBuildConfig();
        }
        return configured;
    }

    public boolean isEnabled() {
        return serverDescriptor.isDaemonEnabled();
    }

    protected boolean isAvailable() {
        return isEnabled() && isConfigured();
    }

    public boolean isRunning() {
        if (!isAvailable()) {
            log.error("Daemon is not available, it can't be running");
            return false;
        }
        if (runner==null || !runner.isAlive() )
        {
            return false;
        }
        int nbWorkers = getNbWorkers();
        return (nbWorkers > 0);
    }

    protected class ThreadExceptionHandler implements UncaughtExceptionHandler {

        public void uncaughtException(Thread t, Throwable e) {
            log.error("OOo Daemon thread existed", e);
            runner=null;
        }

    }
    public int startDaemon() {
        if (!isAvailable()) {
            log.error("Daemon is not available, don't try to start it");
            return -1;
        }
        log.debug("Starting new Thread that will handle the Daemon");
        runner = new Thread(new NXOOoServerRunner(getOrBuildConfig()));
        runner.setDaemon(true);
        runner.setUncaughtExceptionHandler(new ThreadExceptionHandler());
        runner.start();
        log.debug("Daemon thread started");
        //runner.run();
        return 0;
    }

    public boolean startDaemonAndWaitUntilReady() {
        int start = startDaemon();
        if (start < 0) {
            log.error("Unable to start Daemon thread");
            return false;
        }
        int nbTry = 0;
        while (!isRunning()) {
            if (!runner.isAlive()) {
                log.error("Daemon thread exited !!!!");
                return false;
            }
            try {
                Thread.sleep(500);
                log.info("Waiting for Listener to be available ...");
            } catch (InterruptedException e) {
                // NOP
            }
            nbTry++;
            if (nbTry > 20) {
                log.info("Maximum tries reached, kill Daemon thread");
                runner.interrupt();
                return false;
            }
        }
        log.info("Daemon thread ready to accept connections");
        return true;
    }

    public void stopDaemon() {
        if (!isAvailable()) {
            log.error("No need to stop, Daemon is not Running");
            return;
        }
        Config ooServerConfig = getOrBuildConfig();
        String urlStop = "uno:" + ooServerConfig.adminAcceptor + ";urp"
                + ";daemon.stop";

        try {
            XComponentContext initialContext = Bootstrap
                    .createInitialComponentContext(null);
            XMultiComponentFactory manager = initialContext.getServiceManager();
            Object unoResolverObj = manager.createInstanceWithContext(
                    "com.sun.star.bridge.UnoUrlResolver", initialContext);
            XUnoUrlResolver unoUrlResolver = (XUnoUrlResolver) UnoRuntime
                    .queryInterface(XUnoUrlResolver.class, unoResolverObj);

            unoUrlResolver.resolve(urlStop);
        } catch (Exception e) {
        }
    }

    public boolean stopDaemonAndWaitForCompletion() {
        if (!isAvailable()) {
            log.error("Daemon is not available, no need to stop it");
            return false;
        }
        stopDaemon();
        while (isRunning()) {
            try {
                Thread.sleep(400);
                log.debug("trying to stop OOo");
            } catch (InterruptedException e) {
                log.error("Error while waiting for Daemon exit", e);
            }
        }
        log.debug("OOo Daemon stoped");
        return true;
    }

    public int getNbWorkers() {
        if (!isAvailable()) {
            log.error("Daemon is not available, can not get workers");
            return 0;
        }
        if (runner==null || !runner.isAlive() )
        {
            return 0;
        }

        Config ooServerConfig = getOrBuildConfig();
        String urlStatus = "uno:" + ooServerConfig.adminAcceptor + ";urp"
                + ";daemon.status";

        try {
            XComponentContext initialContext = Bootstrap
                    .createInitialComponentContext(null);
            XMultiComponentFactory manager = initialContext.getServiceManager();
            Object unoResolverObj = manager.createInstanceWithContext(
                    "com.sun.star.bridge.UnoUrlResolver", initialContext);
            XUnoUrlResolver unoUrlResolver = (XUnoUrlResolver) UnoRuntime
                    .queryInterface(XUnoUrlResolver.class, unoResolverObj);

            Object statusObj = unoUrlResolver.resolve(urlStatus);
            XNameAccess status = (XNameAccess) UnoRuntime.queryInterface(
                    XNameAccess.class, statusObj);
            Object[] workers = (Object[]) status.getByName("workers");
            return workers.length;
        } catch (Exception e) {
            return 0;
        }
    }

    /** Startup listener **/
    public void frameworkEvent(FrameworkEvent event) {
         if (event.getType() == FrameworkEvent.STARTED) {

             ClassLoader jbossCL =  Thread.currentThread().getContextClassLoader();
             ClassLoader nuxeoCL = OOoDaemonManagerComponent.class.getClassLoader();
             try
             {
                 Thread.currentThread().setContextClassLoader(nuxeoCL);
                 log.debug("OOoDaemon Service initialization");
                 if (serverDescriptor.isAutoStart()) {
                     if (isAvailable()) {
                         log.info("Starting OOo Daemon");
                         startDaemon();
                     }
                     else
                     {
                         log.info("OOo Server is not well configured, can not start OpenOffice server Daemon");
                     }
                 }
             }
             finally
             {
                 Thread.currentThread().setContextClassLoader(jbossCL);
                 log.debug("JBoss ClassLoader restored");
             }
         }
    }

}
