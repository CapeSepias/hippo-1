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
package org.hippoecm.hst.core.component;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;

import org.w3c.dom.Comment;
import org.w3c.dom.Element;

/**
 * Temporarily holds the current state of a HST response.
 * Basically the response of each HstComponent is buffered and stored in
 * a HstResponseState implementation.
 * At the final stage, the HstComponent container will flush every buffered content
 * by using this HstResponseState implementation.
 * Therefore, this interface has all the similar methods which can be found in 
 * <CODE>{@link javax.servlet.http.HttpServletResponse}</CODE>
 * to keep the contents buffered.
 * 
 * @version $Id: HstResponseState.java 40604 2013-08-26 08:57:24Z aschrijvers $
 */
public interface HstResponseState
{
    
    boolean isActionResponse();

    boolean isRenderResponse();

    boolean isResourceResponse();

    boolean isMimeResponse();
    
    boolean isStateAwareResponse();

    void addCookie(Cookie cookie);

    void addDateHeader(String name, long date);

    void addHeader(String name, String value);

    void addIntHeader(String name, int value);

    boolean containsHeader(String name);

    void sendError(int errorCode, String errorMessage) throws IOException;

    void sendError(int errorCode) throws IOException;
    
    int getErrorCode();
    
    String getErrorMessage();

    void sendRedirect(String redirectLocation) throws IOException;

    String getRedirectLocation();
    
    void forward(String pathInfo) throws IOException;
    
    String getForwardPathInfo();
    
    void setDateHeader(String name, long date);

    void setHeader(String name, String value);

    void setIntHeader(String name, int value);

    void setStatus(int statusCode, String message);

    void setStatus(int statusCode);

    /**
     * @return the status when set through {@link #setStatus(int)} and 0 if not set
     */
    int getStatus();

    void flushBuffer() throws IOException;

    int getBufferSize();

    String getCharacterEncoding();

    String getContentType();

    Locale getLocale();

    ServletOutputStream getOutputStream() throws IOException;

    PrintWriter getWriter() throws IOException;

    boolean isCommitted();

    void reset();

    void resetBuffer();

    void setBufferSize(int size);

    void setCharacterEncoding(String charset);

    void setContentLength(int len);

    void setContentType(String type);

    void setLocale(Locale locale);

    Comment createComment(String comment);
    
    Element createElement(String tagName);

    void addHeadElement(Element element, String keyHint);
    
    List<Element> getHeadElements();
    
    boolean containsHeadElement(String keyHint);

    public void addPreambleNode(Comment comment);

    /**
     * Preamble {@link org.w3c.dom.Node}s are written before the rest of the content of this {@link HstResponseState}. Note that
     * from this <code>element</code> <b>only</b> the attributes and text of the <code>element</code> are printed, and *not*
     * any descendant @link org.w3c.dom.Node}s of <code>element</code>
     * @param element the element that is a preamble
     */
    public void addPreambleNode(Element element);

    void setWrapperElement(Element element);
    
    Element getWrapperElement();
    
    void clear();

    void flush() throws IOException;

    boolean isFlushed();

}
