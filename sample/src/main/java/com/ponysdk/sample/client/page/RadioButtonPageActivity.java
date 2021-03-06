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

package com.ponysdk.sample.client.page;

import com.ponysdk.core.UIContext;
import com.ponysdk.sample.client.event.DemoBusinessEvent;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PRadioButton;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.server.basic.event.PValueChangeEvent;
import com.ponysdk.ui.server.basic.event.PValueChangeHandler;

public class RadioButtonPageActivity extends SamplePageActivity {

    public RadioButtonPageActivity() {
        super("Radio Button", "Widgets");
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        final PVerticalPanel panelTop = new PVerticalPanel();
        panelTop.setSpacing(10);

        panelTop.add(new PLabel("Select your favorite color:"));
        panelTop.add(new PRadioButton("color", "blue"));
        panelTop.add(new PRadioButton("color", "red"));

        PRadioButton yellow = new PRadioButton("color", "yellow");
        yellow.setEnabled(false);
        panelTop.add(yellow);
        panelTop.add(new PRadioButton("color", "green"));

        final PVerticalPanel panelBottom = new PVerticalPanel();
        panelBottom.setSpacing(10);

        panelBottom.add(new PLabel("Select your favorite sport:"));
        final PRadioButton baseBall = new PRadioButton("sport", "Baseball");
        panelBottom.add(baseBall);
        final PRadioButton basketBall = new PRadioButton("sport", "Basketball");
        panelBottom.add(basketBall);
        final PRadioButton footBall = new PRadioButton("sport", "Football");
        panelBottom.add(footBall);
        final PRadioButton hockey = new PRadioButton("sport", "Hockey");
        panelBottom.add(hockey);
        final PRadioButton soccer = new PRadioButton("sport", "Soccer");
        panelBottom.add(soccer);
        final PRadioButton waterPolo = new PRadioButton("sport", "Water Polo");
        panelBottom.add(waterPolo);

        PValueChangeHandler<Boolean> valueChangeHandler = new PValueChangeHandler<Boolean>() {

            @Override
            public void onValueChange(final PValueChangeEvent<Boolean> event) {
                PRadioButton radioButton = (PRadioButton) event.getSource();
                String msg = "Value changed : name = " + radioButton.getName() + " caption = " + radioButton.getText() + " value = " + radioButton.getValue();
                UIContext.getRootEventBus().fireEvent(new DemoBusinessEvent(msg));
            }
        };

        baseBall.addValueChangeHandler(valueChangeHandler);
        basketBall.addValueChangeHandler(valueChangeHandler);
        footBall.addValueChangeHandler(valueChangeHandler);
        hockey.addValueChangeHandler(valueChangeHandler);
        soccer.addValueChangeHandler(valueChangeHandler);
        waterPolo.addValueChangeHandler(valueChangeHandler);

        panelTop.add(panelBottom);

        final PVerticalPanel panel = new PVerticalPanel();
        panel.add(panelTop);
        panel.add(panelBottom);

        examplePanel.setWidget(panel);
    }
}
