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

package com.ponysdk.impl.webapplication.notification;

import com.ponysdk.core.event.BusinessEvent;
import com.ponysdk.ui.server.basic.PNotificationManager;
import com.ponysdk.ui.server.basic.PNotificationManager.Notification;

public class LogsPopupPanel {

    public void addEvent(final BusinessEvent<?> event) {
        switch (event.getLevel()) {
            case INFO:
                PNotificationManager.notify(event.getBusinessMessage(), Notification.HUMANIZED);
                break;
            case WARNING:
                PNotificationManager.notify(event.getBusinessMessage(), Notification.WARNING_MESSAGE);
                break;
            case ERROR:
                PNotificationManager.notify(event.getBusinessMessage(), Notification.ERROR_MESSAGE);
                break;
            default:
                break;
        }
    }
}
