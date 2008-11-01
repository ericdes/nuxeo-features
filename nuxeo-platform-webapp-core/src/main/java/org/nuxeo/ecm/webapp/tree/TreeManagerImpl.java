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

package org.nuxeo.ecm.webapp.tree;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.Sorter;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Component for tree service, handing registries for trees managing
 * {@link DocumentModel} object.
 *
 * @author Anahide Tchertchian
 */
public class TreeManagerImpl extends DefaultComponent implements TreeManager {

    private static final long serialVersionUID = 1L;

    public static final String NAME = TreeManager.class.getName();

    public static final String PLUGIN_EXTENSION_POINT = "plugin";

    private static final Log log = LogFactory.getLog(TreeManager.class);

    protected Map<String, Filter> filters;

    protected Map<String, Filter> leafFilters;

    protected Map<String, Sorter> sorters;

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(TreeManager.class)) {
            return (T) this;
        }
        return null;
    }

    @Override
    public void activate(ComponentContext context) throws Exception {
        super.activate(context);
        filters = new HashMap<String, Filter>();
        leafFilters = new HashMap<String, Filter>();
        sorters = new HashMap<String, Sorter>();
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        filters = null;
        leafFilters = null;
        sorters = null;
        super.deactivate(context);
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (PLUGIN_EXTENSION_POINT.equals(extensionPoint)) {
            TreeManagerPluginDescriptor plugin = (TreeManagerPluginDescriptor) contribution;
            String name = plugin.getName();
            // filter
            if (filters.containsKey(name)) {
                // FIXME handle merge?
                log.info("Overriding filter for plugin " + name);
                filters.remove(name);
            }
            filters.put(name, buildFilter(plugin));
            // leaf filter
            Filter leafFilter = buildLeafFilter(plugin);
            if (leafFilter != null) {
                if (leafFilters.containsKey(name)) {
                    // FIXME handle merge?
                    log.info("Overriding leaf filter for plugin " + name);
                    leafFilters.remove(name);
                }
                log.info("Registering leaf filter for plugin " + name);
                leafFilters.put(name, leafFilter);
            }
            // sorter
            if (sorters.containsKey(name)) {
                // FIXME handle merge?
                log.info("Overriding sorter for plugin " + name);
                sorters.remove(name);
            }
            log.info("Registering sorter for plugin " + name);
            sorters.put(name, buildSorter(plugin));
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (PLUGIN_EXTENSION_POINT.equals(extensionPoint)) {
            TreeManagerPluginDescriptor plugin = (TreeManagerPluginDescriptor) contribution;
            String name = plugin.getName();
            // filter
            if (filters.containsKey(name)) {
                log.info("Unregistering filter for plugin " + name);
                filters.remove(name);
            }
            if (leafFilters.containsKey(name)) {
                log.info("Unregistering leaf filter for plugin " + name);
                leafFilters.remove(name);
            }
            // sorter
            if (sorters.containsKey(name)) {
                log.info("Unregistering sorter for plugin " + name);
                sorters.remove(name);
            }
        }
    }

    protected Filter buildFilter(TreeManagerPluginDescriptor plugin) {
        Filter filter = null;

        String filterClass = plugin.getFilterClassName();
        if (filterClass == null || "".equals(filterClass)) {
            // built-in filter
            filter = new DefaultDocumentTreeFilter();
        } else {
            // custom filter
            try {
                Object instance = TreeManagerImpl.class.getClassLoader().loadClass(
                        filterClass).newInstance();
                if (instance instanceof Filter) {
                    filter = (Filter) instance;
                } else {
                    log.error(String.format(
                            "Class %s should follow %s interface", filterClass,
                            Filter.class.getName()));
                }
            } catch (Throwable e) {
                log.error(e);
            }
        }

        // setup config when possible
        if (filter instanceof DocumentTreeFilter) {
            DocumentTreeFilter treeFilter = (DocumentTreeFilter) filter;
            treeFilter.setIncludedFacets(plugin.getIncludedFacets());
            treeFilter.setExcludedFacets(plugin.getExcludedFacets());
            treeFilter.setExcludedTypes(plugin.getExcludedTypes());
        }

        return filter;
    }

    protected Filter buildLeafFilter(TreeManagerPluginDescriptor plugin) {
        String leafFilterClass = plugin.getLeafFilterClassName();
        if (leafFilterClass == null || "".equals(leafFilterClass)) {
            return null;
        }
        try {
            Object instance = TreeManagerImpl.class.getClassLoader().loadClass(
                    leafFilterClass).newInstance();
            if (instance instanceof Filter) {
                return (Filter) instance;
            } else {
                log.error(String.format("Class %s should follow %s interface",
                        leafFilterClass, Filter.class.getName()));
            }
        } catch (Throwable e) {
            log.error(e);
        }
        return null;
    }

    protected Sorter buildSorter(TreeManagerPluginDescriptor plugin) {
        Sorter sorter = null;

        String sorterClass = plugin.getSorterClassName();
        if (sorterClass == null || "".equals(sorterClass)) {
            // built-in sorter
            sorter = new DefaultDocumentTreeSorter();
        } else {
            // custom sorter
            try {
                Object instance = TreeManagerImpl.class.getClassLoader().loadClass(
                        sorterClass).newInstance();
                if (instance instanceof Sorter) {
                    sorter = (Sorter) instance;
                } else {
                    log.error(String.format(
                            "Class %s should follow %s interface", sorterClass,
                            Sorter.class.getName()));
                }
            } catch (Throwable e) {
                log.error(e);
            }
        }

        // setup config when possible
        if (sorter instanceof DocumentTreeSorter) {
            DocumentTreeSorter treeSorter = (DocumentTreeSorter) sorter;
            treeSorter.setSortPropertyPath(plugin.getSortPropertyPath());
        }

        return sorter;
    }

    public Filter getFilter(String pluginName) {
        return filters.get(pluginName);
    }

    public Filter getLeafFilter(String pluginName) {
        return leafFilters.get(pluginName);
    }

    public Sorter getSorter(String pluginName) {
        return sorters.get(pluginName);
    }

}
