/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.tag;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagData;
import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.TagSupport;
import javax.servlet.jsp.tagext.VariableInfo;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.internal.ContextualizableMount;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.util.EncodingUtils;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * This tag creates a cms edit url for some HippoBean. It can do so in two different modes:
 * </p>
 * <ol>
 *    <li> <b>With</b> a <code>var</code> attribute specified: Then, a url String is set on the var attribute
 *    </li> 
 *    <li> <b>Without</b> a <code>var</code> attribute specified: Then, directly to the output a cms edit url and html comment is written.
 *    </li>
 * </ol>
 */
public class HstCmsEditLinkTag extends TagSupport  {
    
    private final static Logger log = LoggerFactory.getLogger(HstCmsEditLinkTag.class);
    
    private static final long serialVersionUID = 1L;

    protected HippoBean hippoBean;
    
    protected String var;
    
    protected String scope;
    
    protected boolean skipTag;
    
    /* (non-Javadoc)
     * @see javax.servlet.jsp.tagext.TagSupport#doStartTag()
     */
    @Override
    public int doStartTag() throws JspException{
    
        if (var != null) {
            pageContext.removeAttribute(var, PageContext.PAGE_SCOPE);
        }
        
        return EVAL_BODY_INCLUDE;
    }
    
    
    /* (non-Javadoc)
     * @see javax.servlet.jsp.tagext.TagSupport#doEndTag()
     */
    @Override
    public int doEndTag() throws JspException{
        try {
            if(skipTag) {
                return EVAL_PAGE;
            }
            if(this.hippoBean == null || this.hippoBean.getNode() == null || !(this.hippoBean.getNode() instanceof HippoNode)) {
                log.warn("Cannot create a cms edit url for a bean that is null or has a jcr node that is null or not an instanceof HippoNode");
                return EVAL_PAGE;
            }

            HttpServletRequest servletRequest = (HttpServletRequest) pageContext.getRequest();

            HstRequestContext requestContext = RequestContextProvider.get();

            if(requestContext == null) {
                log.warn("Cannot create a cms edit url outside the hst request processing for '{}'", this.hippoBean.getPath());
                return EVAL_PAGE;
            }


            if(!requestContext.isPreview()) {
                log.debug("Skipping cms edit url because not in preview.");
                return EVAL_PAGE;
            }
            if (var == null && servletRequest.getSession(false) != null && !Boolean.TRUE.equals(servletRequest.getSession(false).getAttribute(ContainerConstants.CMS_SSO_AUTHENTICATED)) ) {
                log.debug("Skipping cms edit html comment snippet because request is not in a SSO CMS CONTEXT.");
                return EVAL_PAGE;
            }

            ContextualizableMount mount = (ContextualizableMount)requestContext.getResolvedMount().getMount();


            // cmsBaseUrl is something like : http://localhost:8080
            String cmsBaseUrl = mount.getCmsLocation();

            if(cmsBaseUrl == null || "".equals(cmsBaseUrl)) {
                log.warn("Skipping cms edit url because cms location property is not configured in hst hostgroup configuration");
                 return EVAL_PAGE;
            }
            if(cmsBaseUrl.endsWith("/")) {
                cmsBaseUrl = cmsBaseUrl.substring(0, cmsBaseUrl.length() -1);
            }

            HippoNode node = (HippoNode)this.hippoBean.getNode();
            String nodeLocation;
            String nodeId;
            try {
                Node editNode = node.getCanonicalNode();
                if( editNode == null) {
                    log.debug("Cannot create a 'surf and edit' link for a pure virtual jcr node: '{}'", node.getPath());
                    return EVAL_PAGE;
                }  else {
                    Node rootNode = (Node)editNode.getAncestor(0);
                    if (editNode.isSame(rootNode)) {
                        log.warn("Cannot create a 'surf and edit' link for a jcr root node.");
                    }
                    if (editNode.isNodeType(HstNodeTypes.NODETYPE_HST_SITES)) {
                        log.warn("Cannot create a 'surf and edit' link for a jcr node of type '{}'.", HstNodeTypes.NODETYPE_HST_SITES);
                    }
                    if (editNode.isNodeType(HstNodeTypes.NODETYPE_HST_SITE)) {
                        log.warn("Cannot create a 'surf and edit' link for a jcr node of type '{}'.", HstNodeTypes.NODETYPE_HST_SITE);
                    }

                    Node handleNode = getHandleNodeIfIsAncestor(editNode, rootNode);
                    if(handleNode != null) {
                        // take the handle node as this is the one expected by the cms edit url:
                        editNode = handleNode;
                        log.debug("The nodepath for the edit link in cms is '{}'", editNode.getPath());
                    } else {
                        // do nothing, most likely, editNode is a folder node.
                    }
                    nodeId = editNode.getIdentifier();
                    nodeLocation = editNode.getPath();
                    log.debug("The nodepath for the edit link in cms is '{}'", nodeLocation);

                }
            } catch (RepositoryException e) {
                log.error("Exception while trying to retrieve the node path for the edit location", e);
                return EVAL_PAGE;
            }

            if(nodeLocation == null) {
                log.warn("Did not find a jcr node location for the bean to create a cms edit location with. ");
                return EVAL_PAGE;
            }

            String encodedPath = EncodingUtils.getEncodedPath(nodeLocation, servletRequest);

            String cmsEditLink = cmsBaseUrl + "?path="+encodedPath;

            if (var == null) {
                try {
                    write(cmsEditLink, nodeId);
                 } catch (IOException ioe) {
                    throw new JspException("ResourceURL-Tag Exception: cannot write to the output writer.");
                }
            }
            else {
                int varScope = PageContext.PAGE_SCOPE;

                if (this.scope != null) {
                    if ("request".equals(this.scope)) {
                        varScope = PageContext.REQUEST_SCOPE;
                    } else if ("session".equals(this.scope)) {
                        varScope = PageContext.SESSION_SCOPE;
                    } else if ("application".equals(this.scope)) {
                        varScope = PageContext.APPLICATION_SCOPE;
                    }
                }

                pageContext.setAttribute(var, cmsEditLink, varScope);
            }

            return EVAL_PAGE;
        } finally {
            cleanup();
        }
    }

    protected void cleanup() {
        var = null;
        hippoBean = null;
        scope = null;
        skipTag = false;
    }

    protected void write(String url, String nodeId) throws IOException {
        JspWriter writer = pageContext.getOut();
        StringBuilder htmlComment = new StringBuilder(); 
        htmlComment.append("<!-- ");
        htmlComment.append(" {\"type\":\"cmslink\"");
        htmlComment.append(" , \"url\":\"");
        htmlComment.append(url);
        htmlComment.append("\"");
        // add uuid
        htmlComment.append(", \"uuid\":\"");
        htmlComment.append(nodeId);
        htmlComment.append("\" } -->");
        writer.print(htmlComment.toString());
    }


    /*
     * when a currentNode is of type hippo:handle, we return this node, else we check the parent, until we are at the jcr root node.
     * When we hit the jcr root node, we return null;
     */ 
    private Node getHandleNodeIfIsAncestor(Node currentNode, Node rootNode) throws RepositoryException{
        if(currentNode.isNodeType(HippoNodeType.NT_HANDLE)) {
            return currentNode;
        }
        if(currentNode.isSame(rootNode)) {
            return null;
        }
        return getHandleNodeIfIsAncestor(currentNode.getParent(), rootNode);
    }


    /* (non-Javadoc)
     * @see javax.servlet.jsp.tagext.TagSupport#release()
     */
    @Override
    public void release(){
        super.release();        
    }
    
    /**
     * Returns the var property.
     * @return String
     */
    public String getVar() {
        return var;
    }
    
    public String getScope() {
        return scope;
    }
    
    public HippoBean getHippobean(){
        return this.hippoBean;
    }
     
    public void setHippobean(HippoBean hippoBean) {
        this.hippoBean = hippoBean;
    }
    
    /**
     * Sets the var property.
     * @param var The var to set
     */
    public void setVar(String var) {
        this.var = var;
    }
    
    public void setScope(String scope) {
        this.scope = scope;
    }
    
    /* -------------------------------------------------------------------*/
        
    /**
     * TagExtraInfo class for HstCmsEditLinkTag.
     */
    public static class TEI extends TagExtraInfo {
        
        public VariableInfo[] getVariableInfo(TagData tagData) {
            VariableInfo vi[] = null;
            String var = tagData.getAttributeString("var");
            if (var != null) {
                vi = new VariableInfo[1];
                vi[0] =
                    new VariableInfo(var, "java.lang.String", true,
                                 VariableInfo.AT_BEGIN);
            }
            return vi;
        }

    }
}





   
