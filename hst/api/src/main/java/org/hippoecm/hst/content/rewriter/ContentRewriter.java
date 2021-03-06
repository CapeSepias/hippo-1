/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.content.rewriter;

import javax.jcr.Node;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.request.HstRequestContext;

/**
 * ContentRewriter to rewrite document content such as links.
 * 
 * @version $Id: ContentRewriter.java 37754 2013-01-09 16:14:45Z mdenburger $
 */
public interface ContentRewriter<T> {
    
    /**
     * Rewrites the content of the content node.
     * @param content content object. It can be type of String or whatever, depending on the implementation and the context.
     * @param contentNode content node
     * @param requestContext
     * @return
     */
    T rewrite(T content, Node contentNode, HstRequestContext requestContext);
    
    /**
     * Rewrites the content of the content node.
     * @param content
     * @param contentNode
     * @param requestContext
     * @param targetMountAlias
     * @return
     */
    T rewrite(T content, Node contentNode, HstRequestContext requestContext, String targetMountAlias);
    
    /**
     * Rewrites the content of the content node.
     * @param content
     * @param contentNode
     * @param requestContext
     * @param targetMount
     * @return
     */
    T rewrite(T content, Node contentNode, HstRequestContext requestContext, Mount targetMount);
    
    /**
     * Sets whether this {@link ContentRewriter} should create fully qualified links (URLs) for internal links.  
     * @param fullyQualifiedLinks
     */
    void setFullyQualifiedLinks(boolean fullyQualifiedLinks);
    
    /**
     * @return <code>true</code> when fully qualified links (URLs) should be created
     */
    boolean isFullyQualifiedLinks();

    /**
     * Sets whether this {@link ContentRewriter} should use an imageVariant. 
     * @param imageVariant
     */
    void setImageVariant(ImageVariant imageVariant);

    /**
     * @return an ImageVariant when there was set one through {@link #setImageVariant(ImageVariant)} or <code>null</code> when
     * no image variant was set
     */
    ImageVariant getImageVariant();
}
