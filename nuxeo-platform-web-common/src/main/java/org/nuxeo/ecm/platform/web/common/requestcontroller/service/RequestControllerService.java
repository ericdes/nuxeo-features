package org.nuxeo.ecm.platform.web.common.requestcontroller.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Runtime component that implements the {@link RequestControllerManager} interface.
 * Contains both the Extension point logic and the service immplementation.
 *
 * @author tiry
 */
public class RequestControllerService extends DefaultComponent implements
        RequestControllerManager {

    public static final String FILTER_CONFIG_EP ="filterConfig";

    private static final Log log = LogFactory.getLog(RequestControllerService.class);

    protected static final Map<String, FilterConfigDescriptor> grantPatterns = new HashMap<String, FilterConfigDescriptor>();
    protected static final Map<String, FilterConfigDescriptor> denyPatterns = new HashMap<String, FilterConfigDescriptor>();

    protected static final Map<String, RequestFilterConfig> configCache = new LRUCachingMap<String, RequestFilterConfig>(250);
    protected static final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (FILTER_CONFIG_EP.equals(extensionPoint)) {

            FilterConfigDescriptor desc = (FilterConfigDescriptor) contribution;
            if (desc.isGrantRule()) {
                grantPatterns.put(desc.getName(), desc);
                log.debug("Registred grant filter config");
            }
            else {
                denyPatterns.put(desc.getName(), desc);
                log.debug("Registred deny filter config");
            }
        }
        else {
            log.error("Unknow ExtensionPoint " + extensionPoint);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
    }

    /* Service interface */

    public RequestFilterConfig getConfigForRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        RequestFilterConfig config=null;

        try {
            cacheLock.readLock().lock();
            config = configCache.get(uri);
        }
        finally {
            cacheLock.readLock().unlock();
        }
        if (config==null) {
            config = computeConfigForRequest(uri);
            try {
                cacheLock.writeLock().lock();
                configCache.put(uri, config);
            }
            finally {
                cacheLock.writeLock().unlock();
            }
        }
        return config;
    }

    public RequestFilterConfig computeConfigForRequest(String uri) {
        // handle deny patterns
        for (FilterConfigDescriptor desc : denyPatterns.values()) {
            Pattern pat = desc.getCompiledPattern();
            Matcher m = pat.matcher(uri);
            if(m.matches()){
                return new RequestFilterConfigImpl(false,false);
            }
        }

        // handle grant patterns
        for (FilterConfigDescriptor desc : grantPatterns.values()) {
            Pattern pat = desc.getCompiledPattern();
            Matcher m = pat.matcher(uri);
            if(m.matches()) {
                return new RequestFilterConfigImpl(desc.useSync(),desc.useTx());
            }
        }

        // return deny by default
        return new RequestFilterConfigImpl(false,false);
    }

}
