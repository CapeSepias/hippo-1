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
package org.hippoecm.repository.replication.config;

import org.apache.jackrabbit.core.config.BeanConfig;
import org.hippoecm.repository.replication.Replicator;

/**
 * Replicator configuration. This bean configuration class
 * is used to create configured {@link Replicator}.
 * <p>
 * This class is currently only used to assign a static type to
 * more generic bean configuration information.
 */
public class ReplicatorConfig extends BeanConfig {

    /**
     * Creates a journal configuration object from the given bean configuration.
     *
     * @param config bean configuration
     */
    public ReplicatorConfig(BeanConfig config) {
        super(config);
    }

}
