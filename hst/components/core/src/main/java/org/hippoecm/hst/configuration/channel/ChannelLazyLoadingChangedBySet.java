/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration.channel;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.components.HstComponentsConfiguration;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.configuration.site.HstSite;

public class ChannelLazyLoadingChangedBySet implements Set<String> {

    private transient Set<String> delegatee;
    private transient final HstNode channelRootConfigNode;
    private transient final HstSite previewHstSite;

    public ChannelLazyLoadingChangedBySet(final HstNode channelRootConfigNode, final HstSite previewHstSite) {
        this.channelRootConfigNode = channelRootConfigNode;
        this.previewHstSite = previewHstSite;
    }

    private void load(){
        if (delegatee != null) {
            return;
        }
        delegatee = getAllUsersWithAContainerLock(previewHstSite);
        for (HstNode mainConfigNode : channelRootConfigNode.getNodes()) {
            final String lockedBy = mainConfigNode.getValueProvider().getString(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY);
            if (lockedBy != null) {
                delegatee.add(lockedBy);
            }
        }
    }

    private static Set<String> getAllUsersWithAContainerLock(final HstSite previewHstSite) {
        Set<String> usersWithLock = new HashSet<>();
        final HstComponentsConfiguration componentsConfiguration = previewHstSite.getComponentsConfiguration();
        for (HstComponentConfiguration hstComponentConfiguration : componentsConfiguration.getComponentConfigurations().values()) {
            addUsersWithContainerLock(hstComponentConfiguration, usersWithLock);
        }
        return usersWithLock;
    }

    private static void addUsersWithContainerLock(final HstComponentConfiguration config, final Set<String> usersWithLock) {
        if (config.getComponentType() == HstComponentConfiguration.Type.CONTAINER_COMPONENT && config.getLockedBy() != null) {
            usersWithLock.add(config.getLockedBy());
        }
        for (HstComponentConfiguration hstComponentConfiguration : config.getChildren().values()) {
            addUsersWithContainerLock(hstComponentConfiguration, usersWithLock);
        }
    }


    @Override
    public int size() {
        load();
        return delegatee.size();
    }

    @Override
    public boolean isEmpty() {
        load();
        return delegatee.isEmpty();
    }

    @Override
    public boolean contains(final Object o) {
        load();
        return delegatee.contains(o);
    }

    @Override
    public Iterator<String> iterator() {
        load();
        return delegatee.iterator();
    }

    @Override
    public Object[] toArray() {
        load();
        return delegatee.toArray();
    }

    @Override
    public <T> T[] toArray(final T[] a) {
        load();
        return delegatee.toArray(a);
    }

    @Override
    public boolean add(final String string) {
        throw new UnsupportedOperationException("#add not supported in ChannelLazyLoadingChangedBySet");
    }

    @Override
    public boolean remove(final Object o) {
        throw new UnsupportedOperationException("#remove not supported in ChannelLazyLoadingChangedBySet");
    }

    @Override
    public boolean containsAll(final Collection<?> c) {
        return false;
    }

    @Override
    public boolean addAll(final Collection<? extends String> c) {
        throw new UnsupportedOperationException("#addAll not supported in ChannelLazyLoadingChangedBySet");
    }

    @Override
    public boolean retainAll(final Collection<?> c) {
        throw new UnsupportedOperationException("#retainAll not supported in ChannelLazyLoadingChangedBySet");
    }

    @Override
    public boolean removeAll(final Collection<?> c) {
        throw new UnsupportedOperationException("#removeAll not supported in ChannelLazyLoadingChangedBySet");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("#clear not supported in ChannelLazyLoadingChangedBySet");
    }

    @Override
    public int hashCode() {
        load();
        return delegatee.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        load();
        return delegatee.equals(obj);
    }

    @Override
    public String toString() {
        load();
        return "[ChannelLazyLoadingChangedBySet: " + delegatee.toString() + "]";
    }

    @Override
    public Object clone() {
        throw new UnsupportedOperationException("#clone not supported in ChannelLazyLoadingChangedBySet");
    }

}
