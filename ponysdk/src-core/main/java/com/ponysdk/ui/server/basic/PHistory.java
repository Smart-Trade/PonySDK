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

package com.ponysdk.ui.server.basic;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.ponysdk.core.UIContext;
import com.ponysdk.core.instruction.History;
import com.ponysdk.ui.server.basic.event.PValueChangeEvent;
import com.ponysdk.ui.server.basic.event.PValueChangeHandler;
import com.ponysdk.ui.terminal.Dictionnary.HISTORY;

/**
 * This class allows you to interact with the browser's history stack. Each "item" on the stack is represented
 * by a single string, referred to as a "token". You can create new history items (which have a token
 * associated with them when they are created), and you can programmatically force the current history to move
 * back or forward.
 * <p>
 * In order to receive notification of user-directed changes to the current history item, implement the
 * {@link PValueChangeHandler} interface and attach it via {@link #addValueChangeHandler(PValueChangeHandler)}
 * .
 * </p>
 * <p>
 * <h3>URL Encoding</h3> Any valid characters may be used in the history token and will survive round-trips
 * through {@link #newItem(String)} to {@link #getToken()}/
 * {@link PValueChangeHandler#onValueChange(PValueChangeEvent)} , but most will be encoded in the user-visible
 * URL. The following US-ASCII characters are not encoded on any currently supported browser (but may be in
 * the future due to future browser changes):
 * <ul>
 * <li>a-z
 * <li>A-Z
 * <li>0-9
 * <li>;,/?:@&=+$-_.!~*()
 * </ul>
 * </p>
 */
public class PHistory {

    private final List<PValueChangeHandler<String>> handlers = new CopyOnWriteArrayList<PValueChangeHandler<String>>();

    private String token;

    public void addValueChangeHandler(final PValueChangeHandler<String> handler) {
        handlers.add(handler);
    }

    public void newItem(final String token) {
        newItem(token, true);
    }

    public void newItem(final String token, final boolean fireEvents) {
        this.token = token;
        final History history = new History(token);
        history.put(HISTORY.FIRE_EVENTS, fireEvents);

        UIContext.get().stackInstruction(history);
    }

    public void fireHistoryChanged(final String token) {
        this.token = token;
        final PValueChangeEvent<String> event = new PValueChangeEvent<String>(this, token);
        for (final PValueChangeHandler<String> handler : handlers) {
            handler.onValueChange(event);
        }

    }

    public String getToken() {
        return token;
    }

}
