/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.standardworkflow;

import org.apache.wicket.util.io.IClusterable;

public class RenameDocumentArguments implements IClusterable {
    private String targetName;
    private String uriName;
    private String nodeType;

    public RenameDocumentArguments() {
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(final String targetName) {
        this.targetName = targetName;
    }

    public String getUriName() {
        return uriName;
    }

    public void setUriName(final String uriName) {
        this.uriName = uriName;
    }

    public String getNodeType() {
        return nodeType;
    }

    public void setNodeType(final String nodeType) {
        this.nodeType = nodeType;
    }
}