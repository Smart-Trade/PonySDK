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

package com.ponysdk.core.command.event;

import com.ponysdk.core.event.BusinessEvent;
import com.ponysdk.core.event.Event;

public class ServiceFailedEvent extends BusinessEvent<ServiceFailedHandler> {

    public static final Event.Type<ServiceFailedHandler> TYPE = new Event.Type<ServiceFailedHandler>();

    private final Throwable throwable;

    public ServiceFailedEvent(Object sourceComponent, Throwable throwable) {
        super(sourceComponent);
        setLevel(Level.ERROR);
        this.throwable = throwable;
        if (throwable != null) {
            setBusinessMessage(throwable.toString());
        }
    }

    @Override
    protected void dispatch(ServiceFailedHandler handler) {
        handler.onServiceFailed(this);
    }

    @Override
    public Event.Type<ServiceFailedHandler> getAssociatedType() {
        return TYPE;
    }

    public Throwable getThrowable() {
        return throwable;
    }

}
