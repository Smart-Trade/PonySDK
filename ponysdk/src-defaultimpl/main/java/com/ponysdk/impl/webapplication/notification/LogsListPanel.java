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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.ponysdk.core.event.BusinessEvent;
import com.ponysdk.ui.server.basic.PAnchor;
import com.ponysdk.ui.server.basic.PHorizontalPanel;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PScrollPanel;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;

public class LogsListPanel extends PScrollPanel {

    private static final DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    protected PVerticalPanel content = new PVerticalPanel();;

    protected PVerticalPanel logsPanel = new PVerticalPanel();

    protected PHorizontalPanel actionPanel = new PHorizontalPanel();

    public LogsListPanel(final String caption) {
        addStyleName("pony-LogsListPanel");
        setSizeFull();

        content.setSizeFull();
        setWidget(content);

        initActionPanel();
        logsPanel.setSizeFull();
        logsPanel.addStyleName("pony-LogsListPanel-LogsPanel");
        content.add(actionPanel);
        content.add(logsPanel);
    }

    private void initActionPanel() {
        actionPanel = new PHorizontalPanel();
        actionPanel.addStyleName("pony-LogsListPanel-ActionPanel");

        final PAnchor clearLogs = new PAnchor("Clear logs");

        clearLogs.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent clickEvent) {
                logsPanel.clear();
            }
        });
        actionPanel.add(clearLogs);
    }

    public void addEvent(final BusinessEvent<?> event) {
        final String time = "[" + dateFormat.format(new Date()) + "]";
        final PLabel label = new PLabel(time + " [" + event.getLevel().name() + "] " + event.getBusinessMessage());
        logsPanel.insert(label, 0);
    }

    public void addMessage(final String msg) {
        final String time = "[" + dateFormat.format(new Date()) + "]";
        final PLabel label = new PLabel(time + msg);
        logsPanel.insert(label, 0);
    }
}