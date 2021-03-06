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
package org.hippoecm.frontend.plugins.yui;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;
import org.hippoecm.frontend.plugins.yui.webapp.IYuiManager;

/**
 * Base class for {@link AbstractDefaultAjaxBehavior}s that want to use YUI modules. It uses a {@link IYuiContext} to
 * register all required components. The {@link IYuiContext} is created by a (global) {@link IYuiManager} which, in this
 * case, lives inside the {@link Page} (as an {@link Behavior}) that is retrieved by <code>component.getPage()</code>
 * 
 * <p>
 * To allow clientside javascript components to do callbacks in a more flexible way than, for example, just sticking 
 * them inside an element's onclick attribute, this behavior uses a bean that implements {@link IAjaxSettings} in which
 * it will store the callbackUrl, a list of allowed callbackParameters and an anonymous function that takes the
 * callbackUrl as an argument and executes the callbackScript that is generated by the {@link AbstractDefaultAjaxBehavior}.  
 * </p>
 * 
 * <p>
 * Subclasses should override <code>addHeaderContribution(IYuiContext context)</code> to get access to the
 * {@link IYuiContext}.
 * </p>
 * 
 * <p>
 * <b>Note:</b> This behavior skips adding the necessary Wicket-Ajax header contributions, as this is handled by the 
 * {@link org.hippoecm.frontend.plugins.yui.header.YuiHeaderCache} 
 * </p>
 */

public abstract class AbstractYuiAjaxBehavior extends AbstractDefaultAjaxBehavior {

    private static final long serialVersionUID = 1L;

    private IYuiContext context;
    private IAjaxSettings settings;

    public AbstractYuiAjaxBehavior(IAjaxSettings settings) {
        this.settings = settings;
    }

    protected void updateAjaxSettings() {
        if (settings != null) {
            settings.setCallbackFunction(new JsFunction(getCallback()));
        }
    }

    /**
     * Wrap the callback script in an anonymous function that takes the callback url as a parameter to allow client side
     * code to add request parameters
     *
     * @return A javascript function that takes a callback URL as parameter and executes wicketAjax-GET
     */
    private String getCallback() {
        CharSequence ajaxAttributes = renderAjaxAttributes(getComponent().getPage());
        return "function(params) {\n" +
                "  return Wicket.Ajax.ajax(" + ajaxAttributes + ");\n" +
                "}";
    }

    @Override
    protected void updateAjaxAttributes(final AjaxRequestAttributes attributes) {
        super.updateAjaxAttributes(attributes);
        attributes.getDynamicExtraParameters().add("return params;");
    }

    /**
     * Override this method to get access to the IYuiContext
     * 
     * @param context
     *            The IYuiContext this behavior can use to register YUI-modules and the likes.
     */
    public void addHeaderContribution(IYuiContext context) {
    }

    /**
     * Don't call super since WicketAjax is loaded by Yui webapp behavior 
     * TODO: webapp ajax is configurable, maybe check here and still load it.
     */
    @Override
    public void renderHead(Component component, IHeaderResponse response) {
        if (context == null) {
            Page page = component.getPage();
            for (Behavior behavior : page.getBehaviors()) {
                if (behavior instanceof IYuiManager) {
                    context = ((IYuiManager) behavior).newContext();
                    addHeaderContribution(context);
                    break;
                }
            }
            if (context == null) {
                throw new IllegalStateException(
                        "Page has no yui manager behavior, unable to register module dependencies.");
            }
        }
        onRenderHead(response);
        context.renderHead(response);
    }

    protected void onRenderHead(IHeaderResponse response) {
        updateAjaxSettings();
    }

}
