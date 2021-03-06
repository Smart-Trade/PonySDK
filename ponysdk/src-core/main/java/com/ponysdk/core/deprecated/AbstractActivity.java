/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *	Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *	Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
 *  
 *  WebSite:
 *  http://code.google.com/p/pony-sdk/
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.ponysdk.core.deprecated;

import com.ponysdk.core.UIContext;
import com.ponysdk.core.event.BroadcastEventHandler;
import com.ponysdk.core.event.Event;
import com.ponysdk.core.event.Event.Type;
import com.ponysdk.core.event.EventBus;
import com.ponysdk.core.event.EventHandler;
import com.ponysdk.core.event.HandlerRegistration;

public abstract class AbstractActivity implements Activity {

    public AbstractActivity() {}

    public EventBus getRootEventBus() {
        return UIContext.getRootEventBus();
    }

    public <H extends EventHandler> HandlerRegistration addHandler(final Type<H> type, final H handler) {
        return UIContext.addHandler(type, handler);
    }

    public <H extends EventHandler> HandlerRegistration addHandlerToSource(final Type<H> type, final Object source, final H handler) {
        return UIContext.addHandlerToSource(type, source, handler);
    }

    @SuppressWarnings("unchecked")
    public <H extends EventHandler> HandlerRegistration addHandlerToSource(final Type<H> type, final Object source) {
        return addHandlerToSource(type, source, (H) this);
    }

    public void fireEvent(final Event<?> event) {
        UIContext.fireEvent(event);
    }

    public void fireEventFromSource(final Event<?> event, final Object source) {
        UIContext.fireEventFromSource(event, source);
    }

    public void addHandler(final BroadcastEventHandler handler) {
        UIContext.addHandler(handler);
    }

}