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

package com.ponysdk.ui.terminal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.InvocationException;
import com.google.gwt.user.client.rpc.StatusCodeException;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.ponysdk.ui.terminal.Dictionnary.APPLICATION;
import com.ponysdk.ui.terminal.Dictionnary.HANDLER;
import com.ponysdk.ui.terminal.Dictionnary.HISTORY;
import com.ponysdk.ui.terminal.Dictionnary.TYPE;
import com.ponysdk.ui.terminal.exception.PonySessionException;
import com.ponysdk.ui.terminal.instruction.PTInstruction;
import com.ponysdk.ui.terminal.ui.PTObject;
import com.ponysdk.ui.terminal.ui.PTStreamResource;

public class UIBuilder implements ValueChangeHandler<String>, UIService {

    private final static Logger log = Logger.getLogger(UIBuilder.class.getName());

    private final UIFactory uiFactory = new UIFactory();
    private final Map<String, AddonFactory> addonByKey = new HashMap<String, AddonFactory>();
    private final Map<Long, PTObject> objectByID = new HashMap<Long, PTObject>();
    private final Map<UIObject, Long> objectIDByWidget = new HashMap<UIObject, Long>();
    private final Map<Long, UIObject> widgetIDByObjectID = new HashMap<Long, UIObject>();
    private final List<PTInstruction> stackedInstructions = new ArrayList<PTInstruction>();

    private SimplePanel loadingMessageBox;
    private PopupPanel communicationErrorMessagePanel;
    private Timer timer;
    private int numberOfrequestInProgress;

    private boolean updateMode;
    private boolean pendingClose;

    private final RequestBuilder requestBuilder;

    public static long sessionID;

    public UIBuilder(final long ID, final RequestBuilder requestBuilder) {
        this.requestBuilder = requestBuilder;
        UIBuilder.sessionID = ID;
        History.addValueChangeHandler(this);

        final AddonList addonList = GWT.create(PonyAddonList.class);

        final List<AddonFactory> addonFactoryList = addonList.getAddonFactoryList();

        for (final AddonFactory addonFactory : addonFactoryList) {
            addonByKey.put(addonFactory.getSignature(), addonFactory);
        }
    }

    public void init() {

        loadingMessageBox = new SimplePanel();

        communicationErrorMessagePanel = new PopupPanel(false, true);
        communicationErrorMessagePanel.setGlassEnabled(true);
        communicationErrorMessagePanel.setStyleName("pony-notification");
        communicationErrorMessagePanel.addStyleName("error");

        // RootPanel.get().add(loadingMessageBox);

        loadingMessageBox.setStyleName("pony-LoadingMessageBox");
        loadingMessageBox.getElement().getStyle().setVisibility(Visibility.HIDDEN);
        loadingMessageBox.getElement().setInnerText("Loading ...");

        // hide loading component
        final Widget w = RootPanel.get("loading");
        if (w == null) {
            Window.alert("Include splash screen html element into your index.html with id=\"loading\"");
        } else {
            w.setSize("0px", "0px");
            w.setVisible(false);
        }
    }

    public void onCommunicationError(final Throwable exception) {
        if (loadingMessageBox == null) {
            // log.log(Level.SEVERE, "Error ", exception);
            if (exception instanceof StatusCodeException) {
                final StatusCodeException codeException = (StatusCodeException) exception;
                if (codeException.getStatusCode() == 0) return;
            }
            Window.alert("Cannot inititialize the application : " + exception.getMessage() + "\n" + exception + "\nPlease reload your application");
        } else {
            if (pendingClose) return;
            // log.log(Level.SEVERE, "fireInstruction failed", exception);

            if (exception instanceof PonySessionException) {
                reload();
                return;
            }
            numberOfrequestInProgress--;
            showCommunicationErrorMessage(exception);
            hideLoadingMessageBox();
        }
    }

    @Override
    public void update(final List<PTInstruction> instructions) {
        hideLoadingMessageBox();

        updateMode = true;
        PTInstruction currentInstruction = null;
        try {

            // log.info("UPDATING UI with " + instructions.size() + " instructions");
            for (final PTInstruction instruction : instructions) {
                currentInstruction = instruction;

                // log.info("Displaying: " + instruction);

                final String type = instruction.getString(TYPE.KEY);

                if (TYPE.KEY_.CLOSE.equals(type)) {
                    pendingClose = true;
                    triggerEvent(instruction);

                    final ScheduledCommand command = new ScheduledCommand() {

                        @Override
                        public void execute() {
                            reload();
                        }
                    };

                    Scheduler.get().scheduleDeferred(command);
                } else if (TYPE.KEY_.CREATE.equals(type)) {
                    // if (WidgetType.COOKIE.equals(instruction.getWidgetType())) {
                    // final String name = instruction.getString(PROPERTY.NAME);
                    // final String value = instruction.getString(PROPERTY.VALUE);
                    //
                    // if (instruction.containsKey(PROPERTY.COOKIE_EXPIRE)) {
                    // final Date date = new Date(instruction.getLong(PROPERTY.COOKIE_EXPIRE));
                    // Cookies.setCookie(name, value, date);
                    // } else {
                    // Cookies.setCookie(name, value);
                    // }
                    // } else {

                    // log.info("Create: " + create.getObjectID() + ", " + create.getWidgetType().name());
                    PTObject ptObject;
                    final boolean isAddon = instruction.containsKey("addOnSignature");
                    if (isAddon) {
                        final String addOnSignature = instruction.getString("addOnSignature");
                        final AddonFactory addonFactory = addonByKey.get(addOnSignature);
                        if (addonFactory == null) { throw new Exception("UIBuilder: AddOn factory not found for signature: " + addOnSignature + ", available: " + addonByKey.keySet()); }

                        ptObject = addonFactory.newAddon();
                        if (ptObject == null) { throw new Exception("UIBuilder: Failed to instanciate an Addon of type: " + addOnSignature); }
                        ptObject.create(instruction, this);
                    } else {
                        ptObject = uiFactory.newUIObject(this, instruction);
                        ptObject.create(instruction, this);
                    }

                    objectByID.put(instruction.getObjectID(), ptObject);
                    // }

                } else if (TYPE.KEY_.ADD.equals(type)) {
                    // log.info("Add: " + add.getObjectID() + ", " + add.getParentID() + ", " +
                    // add.getProterty());

                    final PTObject uiObject = objectByID.get(instruction.getParentID());
                    uiObject.add(instruction, this);

                } else if (TYPE.KEY_.ADD_HANDLER.equals(type)) {
                    // log.info("AddHandler: " + addHandler.getType() + ", " + addHandler.getObjectID() + ", "
                    // + addHandler.getProterty());
                    final String handler = instruction.getString(HANDLER.KEY);
                    if (HANDLER.KEY_.STREAM_REQUEST_HANDLER.equals(handler)) {
                        new PTStreamResource().addHandler(instruction, this);
                    } else {
                        final PTObject uiObject = objectByID.get(instruction.getObjectID());
                        uiObject.addHandler(instruction, this);
                    }
                } else if (TYPE.KEY_.REMOVE_HANDLER.equals(type)) {
                    // log.info("AddHandler: " + instruction.getType() + ", " + instruction.getObjectID() +
                    // ", " + instruction.getProterty());

                    final PTObject uiObject = objectByID.get(instruction.getObjectID());
                    uiObject.removeHandler(instruction, this);

                } else if (TYPE.KEY_.REMOVE.equals(type)) {
                    PTObject ptObject;

                    // if (PropertyKey.COOKIE.equals(instruction.getProterty().getKey())) { // TODO
                    // // nciaravola
                    // // merge with
                    // // PTCookie ?
                    // Cookies.removeCookie(instruction.getProterty().getValue());
                    // } else {
                    if (instruction.getParentID() == -1) ptObject = objectByID.get(instruction.getObjectID());
                    else {
                        ptObject = objectByID.get(instruction.getParentID());
                    }
                    ptObject.remove(instruction, this);
                    // }
                } else if (TYPE.KEY_.GC.equals(type)) {
                    // log.info("GC: " + remove.getObjectID());
                    final PTObject unRegisterObject = unRegisterObject(instruction.getObjectID());
                    unRegisterObject.gc(this);
                } else if (TYPE.KEY_.UPDATE.equals(type)) {
                    // log.info("Update " + update.getMainProperty().getKey() + " / " +
                    // update.getMainProperty().getValue());
                    final PTObject ptObject = objectByID.get(instruction.getObjectID());
                    ptObject.update(instruction, this);

                } else if (TYPE.KEY_.HISTORY.equals(type)) {
                    final String oldToken = History.getToken();

                    String token = null;

                    if (instruction.containsKey(HISTORY.TOKEN)) {
                        token = instruction.getString(HISTORY.TOKEN);
                    }

                    if (oldToken != null && oldToken.equals(token)) {
                        if (instruction.getBoolean(HISTORY.FIRE_EVENTS)) History.fireCurrentHistoryState();
                    } else {
                        History.newItem(token, instruction.getBoolean(HISTORY.FIRE_EVENTS));
                    }
                }
            }
        } catch (final Throwable e) {
            Window.alert("PonySDK has encountered an internal error on instruction : " + currentInstruction + " => Error Message " + e.getMessage());
            log.log(Level.SEVERE, "PonySDK has encountered an internal error : ", e);
        } finally {
            flushEvents();
            updateMode = false;
        }
    }

    @Override
    public PTObject unRegisterObject(final Long objectId) {
        final PTObject ptObject = objectByID.remove(objectId);
        final UIObject uiObject = widgetIDByObjectID.remove(objectId);
        if (uiObject != null) {
            objectIDByWidget.remove(uiObject);
        }
        return ptObject;
    }

    //
    // private void updateTimer(final Timer object, final Property mainProperty) {
    // object.scheduleRepeating(mainProperty.getIntValue());
    // }

    @Override
    public void stackEvent(final PTInstruction instruction) {
        if (!updateMode) triggerEvent(instruction);
        else stackedInstructions.add(instruction);
    }

    @Override
    public void flushEvents() {
        if (stackedInstructions.isEmpty()) return;
        fireEvents(stackedInstructions);
        stackedInstructions.clear();
    }

    private void fireEvents(final List<PTInstruction> instructions) {
        numberOfrequestInProgress++;

        if (timer == null) timer = scheduleLoadingMessageBox();

        // try {

        // final RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, GWT.getModuleBaseURL() +
        // "p");

        final PTInstruction requestData = new PTInstruction();
        requestData.put(APPLICATION.VIEW_ID, sessionID);

        final JSONArray jsonArray = new JSONArray();

        for (int i = 0; i < instructions.size(); i++) {
            jsonArray.set(i, instructions.get(i));
        }

        requestData.put(APPLICATION.INSTRUCTIONS, jsonArray);

        requestBuilder.send(requestData.toString());

        // builder.sendRequest(requestData.toString(), new RequestCallback() {
        //
        // @Override
        // public void onError(final Request request, final Throwable exception) {
        // if (pendingClose) return;
        // log.log(Level.SEVERE, "fireInstruction failed", exception);
        //
        // if (exception instanceof PonySessionException) {
        // reload();
        // return;
        // }
        // numberOfrequestInProgress--;
        // instructions.clear();
        // showCommunicationErrorMessage(exception);
        // hideLoadingMessageBox();
        // }
        //
        // @Override
        // public void onResponseReceived(final Request request, final Response response) {
        // try {
        // numberOfrequestInProgress--;
        // instructions.clear();
        // if (200 == response.getStatusCode()) {
        //
        // final List<PTInstruction> instructions = new ArrayList<PTInstruction>();
        //
        // if (response.getText() == null || response.getText().isEmpty()) return;
        //
        // GWT.log(response.getText());
        //
        // final JSONObject object = JSONParser.parseLenient(response.getText()).isObject();
        //
        // final JSONArray jsonArray = object.get(APPLICATION.INSTRUCTIONS).isArray();
        //
        // for (int i = 0; i < jsonArray.size(); i++) {
        // instructions.add(new PTInstruction(jsonArray.get(i).isObject().getJavaScriptObject()));
        // }
        //
        // update(instructions);
        // } else {
        // showCommunicationErrorMessage(new Exception("Couldn't retrieve JSON (" + response.getStatusText() +
        // ")"));
        // }
        //
        // } finally {
        // hideLoadingMessageBox();
        // }
        // }
        // });
        // } catch (final RequestException e) {
        // numberOfrequestInProgress--;
        // instructions.clear();
        // showCommunicationErrorMessage(e);
        // hideLoadingMessageBox();
        // }

    }

    @Override
    public void triggerEvent(final PTInstruction instruction) {
        final List<PTInstruction> instructions = new ArrayList<PTInstruction>();
        instructions.add(instruction);
        fireEvents(instructions);
    }

    private Timer scheduleLoadingMessageBox() {
        final Timer timer = new Timer() {

            @Override
            public void run() {
                loadingMessageBox.getElement().getStyle().setVisibility(Visibility.VISIBLE);
            }
        };
        timer.schedule(500);
        return timer;
    }

    private void showCommunicationErrorMessage(final Throwable caught) {

        final VerticalPanel content = new VerticalPanel();
        if (caught instanceof StatusCodeException) {
            final StatusCodeException exception = (StatusCodeException) caught;
            content.add(new HTML("Server connection failed <br/>Code : " + exception.getStatusCode() + "<br/>" + "cause : " + exception.getMessage()));
        } else if (caught instanceof InvocationException) {
            content.add(new HTML("Exception durring server invocation : " + caught.getMessage()));
        } else {
            content.add(new HTML("Failure : " + caught == null ? "" : caught.getMessage()));
        }

        final HorizontalPanel actionPanel = new HorizontalPanel();
        actionPanel.setSize("100%", "100%");

        final Anchor reloadAnchor = new Anchor("reload");
        reloadAnchor.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(final ClickEvent event) {
                History.newItem("");
                reload();
            }
        });

        final Anchor closeAnchor = new Anchor("close");
        closeAnchor.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(final ClickEvent event) {
                communicationErrorMessagePanel.hide();
            }
        });

        actionPanel.add(reloadAnchor);
        actionPanel.add(closeAnchor);

        actionPanel.setCellHorizontalAlignment(reloadAnchor, HasHorizontalAlignment.ALIGN_CENTER);
        actionPanel.setCellHorizontalAlignment(closeAnchor, HasHorizontalAlignment.ALIGN_CENTER);
        actionPanel.setCellVerticalAlignment(reloadAnchor, HasVerticalAlignment.ALIGN_MIDDLE);
        actionPanel.setCellVerticalAlignment(closeAnchor, HasVerticalAlignment.ALIGN_MIDDLE);

        content.add(actionPanel);

        communicationErrorMessagePanel.setWidget(content);
        communicationErrorMessagePanel.setPopupPositionAndShow(new PositionCallback() {

            @Override
            public void setPosition(final int offsetWidth, final int offsetHeight) {
                communicationErrorMessagePanel.setPopupPosition((Window.getClientWidth() - offsetWidth) / 2, (Window.getClientHeight() - offsetHeight) / 2);
            }
        });
    }

    public void hideLoadingMessageBox() {
        if (numberOfrequestInProgress < 1 && timer != null) {
            timer.cancel();
            timer = null;
            loadingMessageBox.getElement().getStyle().setVisibility(Visibility.HIDDEN);
        }
    }

    @Override
    public void onValueChange(final ValueChangeEvent<String> event) {
        if (event.getValue() != null && !event.getValue().isEmpty()) {
            final PTInstruction eventInstruction = new PTInstruction();
            eventInstruction.put(TYPE.KEY, TYPE.KEY_.HISTORY);
            eventInstruction.put(HISTORY.TOKEN, event.getValue());
            stackEvent(eventInstruction);
        }
    }

    @Override
    public PTObject getPTObject(final Long ID) {
        return objectByID.get(ID);
    }

    @Override
    public PTObject getPTObject(final UIObject uiObject) {
        final Long objectID = objectIDByWidget.get(uiObject);
        if (objectID != null) return objectByID.get(objectID);
        return null;
    }

    @Override
    public void registerUIObject(final Long ID, final UIObject uiObject) {
        objectIDByWidget.put(uiObject, ID);
        widgetIDByObjectID.put(ID, uiObject);
    }

    private native void reload() /*-{$wnd.location.reload();}-*/;

}
