/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.pagecomposer.jaxrs;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventListener;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.hippoecm.hst.configuration.cache.HstEventsCollector;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.model.HstConfigurationEventListener;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.internal.HstRequestContextComponent;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.site.container.SpringComponentManager;
import org.hippoecm.hst.util.HstRequestUtils;
import org.hippoecm.repository.util.JcrUtils;
import org.junit.After;
import org.junit.Before;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class AbstractPageComposerTest {

    protected SpringComponentManager componentManager;
    protected HstManager hstManager;
    protected HstURLFactory hstURLFactory;
    protected HstSiteMapMatcher siteMapMatcher;
    protected HstEventsCollector hstEventsCollector;
    protected Object hstModelMutex;

    @Before
    public void setUp() throws Exception {
        this.componentManager = new SpringComponentManager(getContainerConfiguration());
        this.componentManager.setConfigurationResources(getConfigurations());

        this.componentManager.initialize();
        this.componentManager.start();
        HstServices.setComponentManager(getComponentManager());
        this.hstManager = HstServices.getComponentManager().getComponent(HstManager.class.getName());
        this.siteMapMatcher = HstServices.getComponentManager().getComponent(HstSiteMapMatcher.class.getName());
        this.hstURLFactory = HstServices.getComponentManager().getComponent(HstURLFactory.class.getName());
        this.hstEventsCollector = HstServices.getComponentManager().getComponent("hstEventsCollector");
        this.hstModelMutex = HstServices.getComponentManager().getComponent("hstModelMutex");
    }

    @After
    public void tearDown() throws Exception {
        this.componentManager.stop();
        this.componentManager.close();
        HstServices.setComponentManager(null);
        ModifiableRequestContextProvider.clear();
    }

    protected String[] getConfigurations() {
        String classXmlFileName = AbstractPageComposerTest.class.getName().replace(".", "/") + ".xml";
        String classXmlFileName2 = AbstractPageComposerTest.class.getName().replace(".", "/") + "-*.xml";
        return new String[] { classXmlFileName, classXmlFileName2 };
    }

    protected ComponentManager getComponentManager() {
        return this.componentManager;
    }


    protected Session createSession() throws RepositoryException {
        Repository repository = HstServices.getComponentManager().getComponent(Repository.class.getName() + ".delegating");
        return repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
    }

    protected Configuration getContainerConfiguration() {
        PropertiesConfiguration propConf = new PropertiesConfiguration();
        return propConf;
    }



    protected HstRequestContext getRequestContextWithResolvedSiteMapItemAndContainerURL(final MockHttpServletRequest request, String hostAndPort, String requestURI) throws Exception {
        return getRequestContextWithResolvedSiteMapItemAndContainerURL(request, null, hostAndPort, requestURI, null);
    }

    protected HstRequestContext getRequestContextWithResolvedSiteMapItemAndContainerURL(final MockHttpServletRequest request,String scheme, String hostAndPort, String requestURI, String queryString) throws Exception {
        HstRequestContextComponent rcc = HstServices.getComponentManager().getComponent(HstRequestContextComponent.class.getName());
        HstMutableRequestContext requestContext = rcc.create();
        HstContainerURL containerUrl = createContainerUrl(request, scheme, hostAndPort, requestURI, requestContext, queryString);
        requestContext.setBaseURL(containerUrl);
        ResolvedSiteMapItem resolvedSiteMapItem = getResolvedSiteMapItem(containerUrl);
        requestContext.setResolvedSiteMapItem(resolvedSiteMapItem);
        requestContext.setResolvedMount(resolvedSiteMapItem.getResolvedMount());
        HstURLFactory hstURLFactory = HstServices.getComponentManager().getComponent(HstURLFactory.class.getName());
        requestContext.setURLFactory(hstURLFactory);
        requestContext.setSiteMapMatcher(siteMapMatcher);

        request.setAttribute(ContainerConstants.HST_REQUEST_CONTEXT, requestContext);
        ModifiableRequestContextProvider.set(requestContext);
        return requestContext;
    }

    protected HstContainerURL createContainerUrl(final MockHttpServletRequest request,String scheme, String hostAndPort, String requestURI,
                                                 HstMutableRequestContext requestContext, String queryString) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        requestContext.setServletRequest(request);
        requestContext.setServletResponse(response);
        String host = hostAndPort.split(":")[0];
        if (hostAndPort.split(":").length > 1) {
            int port = Integer.parseInt(hostAndPort.split(":")[1]);
            request.setLocalPort(port);
            request.setServerPort(port);
        }
        if (scheme == null) {
            request.setScheme("http");
        } else {
            request.setScheme(scheme);
        }
        request.setServerName(host);
        request.addHeader("Host", hostAndPort);
        request.setContextPath("/site");
        requestURI = "/site" + requestURI;
        request.setRequestURI(requestURI);
        if (queryString != null) {
            request.setQueryString(queryString);
        }
        VirtualHosts vhosts = hstManager.getVirtualHosts();
        ResolvedMount mount = vhosts.matchMount(HstRequestUtils.getFarthestRequestHost(request), request.getContextPath(), HstRequestUtils.getRequestPath(request));
        return hstURLFactory.getContainerURLProvider().parseURL(request, response, mount);
    }

    protected ResolvedSiteMapItem getResolvedSiteMapItem(HstContainerURL url) throws ContainerException {
        VirtualHosts vhosts = hstManager.getVirtualHosts();
        return vhosts.matchSiteMapItem(url);
    }

    public class CommonHstConfigSetup implements AutoCloseable {
        public Session session;
        public EventListener listener;

        public CommonHstConfigSetup() throws RepositoryException {
            session = createSession();
            listener = registerConfigListener();
            createHstConfigBackup();
        }

        @Override
        public void close() throws Exception {
            removeListener();
            restoreHstConfigBackup();
            session.logout();
        }

        protected void createHstConfigBackup() throws RepositoryException {
            if (!session.nodeExists("/hst-backup")) {
                JcrUtils.copy(session, "/hst:hst", "/hst-backup");
                session.save();
            }
        }

        protected void restoreHstConfigBackup() throws RepositoryException {
            if (session.nodeExists("/hst-backup")) {
                session.removeItem("/hst:hst");
                JcrUtils.copy(session, "/hst-backup", "/hst:hst");
                session.removeItem("/hst-backup");
                session.save();
            }
        }

        private void removeListener() throws RepositoryException {
            session.getWorkspace().getObservationManager().removeEventListener(listener);
        }

        private EventListener registerConfigListener() throws RepositoryException {
            HstConfigurationEventListener configurationEventListener = new HstConfigurationEventListener();
            configurationEventListener.setHstEventsCollector(hstEventsCollector);
            configurationEventListener.setHstManager(hstManager);
            configurationEventListener.setHstModelMutex(hstModelMutex);
            session.getWorkspace().getObservationManager().addEventListener(configurationEventListener,
                    Event.NODE_ADDED | Event.NODE_MOVED | Event.NODE_REMOVED | Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED,
                    "/hst:hst",
                    true,
                    null,
                    null,
                    false);
            return configurationEventListener;
        }
    }
}
