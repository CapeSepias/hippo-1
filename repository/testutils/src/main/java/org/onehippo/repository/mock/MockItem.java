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
package org.onehippo.repository.mock;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mock version of a {@link Item}. Limitations:
 * <ul>
 *     <li>Item cannot be removed</li>
 *     <li>Saving changes is ignored</li>
 * </ul>
 * All methods that are not implemented throw an {@link UnsupportedOperationException}.
 */
public abstract class MockItem implements Item {

    private static Logger log = LoggerFactory.getLogger(MockItem.class);

    private String name;
    private MockNode parent;

    public MockItem(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return isRootNode() ? "" : name;
    }

    protected boolean isRootNode() {
        return parent == null;
    }

    void setParent(final MockNode parent) {
        this.parent = parent;
    }

    @Override
    public Node getParent() throws ItemNotFoundException {
        if (isRootNode()) {
            throw new ItemNotFoundException("A root node does not have a parent");
        }
        return parent;
    }

    MockNode getMockParent() {
        return parent;
    }

    @Override
    public String getPath() throws RepositoryException {
        StringBuilder pathBuilder = new StringBuilder();
        buildPath(pathBuilder);
        return pathBuilder.toString();
    }

    void buildPath(StringBuilder builder) throws RepositoryException {
        if (isRootNode()) {
            builder.append('/');
        } else {
            parent.buildPath(builder);
            if (builder.charAt(builder.length() - 1) != '/') {
                builder.append('/');
            }
            builder.append(name);

            if (this instanceof MockNode) {
                int index = ((MockNode) this).getIndex();
                if (index > 1) {
                    builder.append('[').append(index).append(']');
                }
            }
        }
    }

    @Override
    public boolean isSame(final Item otherItem) {
        return equals(otherItem);
    }

    @Override
    public Session getSession() throws RepositoryException {
        MockNode root = getRootNode();
        return new MockSession(root);
    }

    MockNode getRootNode() {
        if (isRootNode()) {
            return (MockNode)this;
        }
        return parent.getRootNode();
    }

    @Override
    public void save() {
        // do nothing
    }

    @Override
    public void refresh(final boolean keepChanges) {
        // do nothing
    }

    // REMAINING METHODS ARE NOT IMPLEMENTED

    @Override
    public Item getAncestor(final int depth) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getDepth() throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isNew() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isModified() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void accept(final ItemVisitor visitor) {
        throw new UnsupportedOperationException();
    }

}
