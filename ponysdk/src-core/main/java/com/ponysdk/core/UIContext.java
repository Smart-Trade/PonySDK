/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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

package com.ponysdk.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.event.BroadcastEventHandler;
import com.ponysdk.core.event.Event;
import com.ponysdk.core.event.Event.Type;
import com.ponysdk.core.event.EventBus;
import com.ponysdk.core.event.EventHandler;
import com.ponysdk.core.event.HandlerRegistration;
import com.ponysdk.core.event.StreamHandler;
import com.ponysdk.core.instruction.Add;
import com.ponysdk.core.instruction.AddHandler;
import com.ponysdk.core.instruction.Close;
import com.ponysdk.core.instruction.Instruction;
import com.ponysdk.core.security.Permission;
import com.ponysdk.core.servlet.Session;
import com.ponysdk.core.servlet.SessionListener;
import com.ponysdk.ui.server.basic.PCookies;
import com.ponysdk.ui.server.basic.PHistory;
import com.ponysdk.ui.server.basic.PObject;
import com.ponysdk.ui.server.basic.PTimer;
import com.ponysdk.ui.terminal.Dictionnary.APPLICATION;
import com.ponysdk.ui.terminal.Dictionnary.HANDLER;
import com.ponysdk.ui.terminal.Dictionnary.HISTORY;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.Dictionnary.TYPE;
import com.ponysdk.ui.terminal.exception.PonySessionException;

public class UIContext {

    private static ThreadLocal<UIContext> currentContext = new ThreadLocal<UIContext>();

    private final Logger log = LoggerFactory.getLogger(UIContext.class);

    private long objectCounter = 1;

    private long streamRequestCounter = 0;

    private final WeakHashMap weakReferences = new WeakHashMap();

    private final Map<Long, PTimer> timers = new ConcurrentHashMap<Long, PTimer>();

    // to do a weak reference ?
    private final Map<Long, StreamHandler> streamListenerByID = new HashMap<Long, StreamHandler>();

    private final List<Instruction> pendingInstructions = new ArrayList<Instruction>();

    private Map<String, Permission> permissions = new HashMap<String, Permission>();

    private PHistory history;

    private EventBus rootEventBus;

    private PCookies cookies;

    private final Application ponyApplication;

    private final Map<String, Object> ponySessionAttributes = new ConcurrentHashMap<String, Object>();

    private final ReentrantLock lock = new ReentrantLock();

    public UIContext(final Application ponyApplication) {
        this.ponyApplication = ponyApplication;
    }

    public void stackInstruction(final Instruction instruction) {
        if (instruction instanceof Add) {
            final Add add = (Add) instruction;
            weakReferences.assignParentID(add.getObjectID(), add.getParentID());
        }
        pendingInstructions.add(instruction);
    }

    public void fireInstruction(final JSONObject instruction) throws PonySessionException, JSONException {
        if (instruction.has(TYPE.KEY)) {
            if (instruction.get(TYPE.KEY).equals(TYPE.KEY_.CLOSE)) {
                UIContext.get().invalidate();
                return;
            }

            if (instruction.get(TYPE.KEY).equals(TYPE.KEY_.HISTORY)) {
                if (history != null) {
                    history.fireHistoryChanged(instruction.getString(HISTORY.TOKEN));
                }
                return;
            }

        }

        final PObject object = weakReferences.get(instruction.getLong(PROPERTY.OBJECT_ID));

        if (object == null) {
            log.warn("unknown reference from the browser. Unable to execute instruction: " + instruction);
            try {
                if (instruction.has(PROPERTY.PARENT_ID)) {
                    final PObject parentOfGarbageObject = weakReferences.get(instruction.getLong(PROPERTY.PARENT_ID));
                    log.warn("parent: " + parentOfGarbageObject);
                }
            } catch (final Exception e) {}

            return;
        }
        if (instruction.has(TYPE.KEY)) {
            if (instruction.get(TYPE.KEY).equals(TYPE.KEY_.EVENT)) {
                object.onEventInstruction(instruction);
            }
        }
    }

    public boolean flushInstructions(final JSONObject data) throws JSONException {
        if (pendingInstructions.isEmpty()) return false;
        data.put(APPLICATION.INSTRUCTIONS, pendingInstructions);
        pendingInstructions.clear();
        return true;
    }

    public void acquire() {
        lock.lock();
    }

    public void release() {
        lock.unlock();
    }

    public long nextID() {
        return objectCounter++;
    }

    public long nextStreamRequestID() {
        return streamRequestCounter++;
    }

    public void registerObject(final PObject object) {
        weakReferences.put(object.getID(), object);
    }

    public void unRegisterObject(final PObject object) {
        timers.remove(object.getID());
        weakReferences.remove(object.getID());
    }

    @SuppressWarnings("unchecked")
    public <T> T getObject(final long objectID) {
        return (T) weakReferences.get(objectID);
    }

    public Session getSession() {
        return ponyApplication.getSession();
    }

    public StreamHandler removeStreamListener(final Long streamID) {
        return streamListenerByID.remove(streamID);
    }

    public void stackStreamRequest(final StreamHandler streamListener) {
        final AddHandler addHandler = new AddHandler(0, HANDLER.KEY_.STREAM_REQUEST_HANDLER);
        final long streamRequestID = UIContext.get().nextStreamRequestID();
        addHandler.put(PROPERTY.STREAM_REQUEST_ID, streamRequestID);
        stackInstruction(addHandler);
        streamListenerByID.put(streamRequestID, streamListener);
    }

    public void stackEmbededStreamRequest(final StreamHandler streamListener, final long objectID) {
        final AddHandler addHandler = new AddHandler(objectID, HANDLER.KEY_.EMBEDED_STREAM_REQUEST_HANDLER);
        final long streamRequestID = UIContext.get().nextStreamRequestID();
        addHandler.put(PROPERTY.STREAM_REQUEST_ID, streamRequestID);
        stackInstruction(addHandler);
        streamListenerByID.put(streamRequestID, streamListener);
    }

    public PHistory getHistory() {
        return history;
    }

    private EventBus getEventBus() {
        return rootEventBus;
    }

    public PCookies getCookies() {
        return cookies;
    }

    public void setHistory(final PHistory history) {
        this.history = history;
    }

    public void setRootEventBus(final EventBus eventBus) {
        this.rootEventBus = eventBus;
    }

    public void setCookies(final PCookies cookies) {
        this.cookies = cookies;
    }

    public static UIContext get() {
        return currentContext.get();
    }

    public static void remove() {
        currentContext.remove();
    }

    public static void setCurrent(final UIContext uiContext) {
        currentContext.set(uiContext);
    }

    public static <H extends EventHandler> HandlerRegistration addHandler(final Type<H> type, final H handler) {
        return get().getEventBus().addHandler(type, handler);
    }

    public static <H extends EventHandler> HandlerRegistration addHandlerToSource(final Type<H> type, final Object source, final H handler) {
        return get().getEventBus().addHandlerToSource(type, source, handler);
    }

    public static void fireEvent(final Event<?> event) {
        get().getEventBus().fireEvent(event);
    }

    public static void fireEventFromSource(final Event<?> event, final Object source) {
        get().getEventBus().fireEventFromSource(event, source);
    }

    public static void addHandler(final BroadcastEventHandler handler) {
        get().getEventBus().addHandler(handler);
    }

    public static EventBus getRootEventBus() {
        return get().getEventBus();
    }

    void invalidate() {
        ponyApplication.getSession().invalidate();
    }

    public void close() {
        final Close close = new Close();
        stackInstruction(close);
    }

    public boolean hasPermission(final String key) {
        return permissions.containsKey(key);
    }

    public void setPermissions(final Map<String, Permission> permissions) {
        this.permissions = permissions;
    }

    public void addSessionListener(final SessionListener sessionListener) {
        this.ponyApplication.addSessionListener(sessionListener);
    }

    public boolean removeSessionListener(final SessionListener sessionListener) {
        return this.ponyApplication.removeSessionListener(sessionListener);
    }

    public void setAttribute(final String name, final Object value) {
        ponySessionAttributes.put(name, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(final String name) {
        return (T) ponySessionAttributes.get(name);
    }

    public void setApplicationAttribute(final String name, final Object value) {
        this.ponyApplication.setAttribute(name, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getApplicationAttribute(final String name) {
        return (T) this.ponyApplication.getAttribute(name);
    }
}
