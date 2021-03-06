/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.upgrade;

import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.ext.UpdaterContext;
import org.hippoecm.repository.ext.UpdaterItemVisitor;
import org.hippoecm.repository.ext.UpdaterModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Upgrader12b implements UpdaterModule {

    static final Logger log = LoggerFactory.getLogger(Upgrader12b.class);
    
    public void register(final UpdaterContext context) {
        context.registerName("upgrade-v12b");
        context.registerStartTag("v12a");
        context.registerEndTag("v12b");

        // re-read parts of the configuration
        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:configuration/hippo:initialize") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                node.getNode("frontend-console").remove();
            }
        });

        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:configuration/hippo:frontend/console") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                node.remove();
            }
        });
    }
}
