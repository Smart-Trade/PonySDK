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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.ponysdk.core.UIContext;
import com.ponysdk.core.instruction.AddHandler;
import com.ponysdk.core.instruction.RemoveHandler;
import com.ponysdk.ui.server.basic.event.HasPSelectionHandlers;
import com.ponysdk.ui.server.basic.event.PSelectionEvent;
import com.ponysdk.ui.server.basic.event.PSelectionHandler;
import com.ponysdk.ui.terminal.Dictionnary.HANDLER;
import com.ponysdk.ui.terminal.WidgetType;

/**
 * A standard hierarchical tree widget. The tree contains a hierarchy of {@link PTreeItem TreeItems} that the
 * user can open, close, and select.
 * <p>
 * <img class='gallery' src='doc-files/PTree.png'/>
 * </p>
 * <h3>CSS Style Rules</h3>
 * <dl>
 * <dt>.gwt-Tree</dt>
 * <dd>the tree itself</dd>
 * <dt>.gwt-Tree .gwt-TreeItem</dt>
 * <dd>a tree item</dd>
 * <dt>.gwt-Tree .gwt-TreeItem-selected</dt>
 * <dd>a selected tree item</dd>
 * </dl>
 * <p>
 * <h3>Example</h3> {@example http://ponysdk.com/sample/#Tree}
 * </p>
 */
public class PTree extends PWidget implements HasPSelectionHandlers<PTreeItem> {

    private final List<PSelectionHandler<PTreeItem>> selectionHandlers = new ArrayList<PSelectionHandler<PTreeItem>>();

    private final Map<PWidget, PTreeItem> childWidgets = new HashMap<PWidget, PTreeItem>();

    private final PTreeItem root;

    private PTreeItem curSelection;

    public PTree() {
        root = new PTreeItem(true);
        root.setTree(this);
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.TREE;
    }

    public PTreeItem addItem(final String item) {
        return root.addItem(item);
    }

    public PTreeItem addItem(final PTreeItem item) {
        return root.addItem(item);
    }

    public void removeItem(final PTreeItem item) {
        root.removeItem(item);
    }

    public PTreeItem getSelectedItem() {
        return curSelection;
    }

    public void setSelectedItem(final PTreeItem item) {
        if (curSelection != null) {
            curSelection.setSelected(false);
        }
        curSelection = item;
        curSelection.setSelected(true);
    }

    public PTreeItem getItem(final int index) {
        return root.getChild(index);
    }

    /**
     * Gets the number of items contained at the root of this tree.
     * 
     * @return this tree's item count
     */
    public int getItemCount() {
        return root.getChildCount();
    }

    void orphan(final PWidget widget) {
        assert (widget.getParent() == this);
        widget.setParent(null);
        childWidgets.remove(widget);
    }

    void adopt(final PWidget widget, final PTreeItem item) {
        assert (!childWidgets.containsKey(widget));
        childWidgets.put(widget, item);
        widget.setParent(this);
    }

    @Override
    public void addSelectionHandler(final PSelectionHandler<PTreeItem> handler) {
        selectionHandlers.add(handler);
        final AddHandler addHandler = new AddHandler(getID(), HANDLER.KEY_.SELECTION_HANDLER);
        getUIContext().stackInstruction(addHandler);
    }

    @Override
    public void removeSelectionHandler(final PSelectionHandler<PTreeItem> handler) {
        selectionHandlers.remove(handler);
        final RemoveHandler removeHandler = new RemoveHandler(getID(), HANDLER.KEY_.SELECTION_HANDLER);
        getUIContext().stackInstruction(removeHandler);
    }

    @Override
    public List<PSelectionHandler<PTreeItem>> getSelectionHandlers() {
        return selectionHandlers;
    }

    @Override
    public void onEventInstruction(final JSONObject event) throws JSONException {
        if (HANDLER.KEY_.SELECTION_HANDLER.equals(event.getString(HANDLER.KEY))) {
            final PTreeItem treeItem = UIContext.get().getObject(event.getLong(HANDLER.KEY_.SELECTION_HANDLER));
            final PSelectionEvent<PTreeItem> selectionEvent = new PSelectionEvent<PTreeItem>(this, treeItem);
            for (final PSelectionHandler<PTreeItem> handler : getSelectionHandlers()) {
                handler.onSelection(selectionEvent);
            }
        } else {
            super.onEventInstruction(event);
        }
    }

    public PTreeItem getRoot() {
        return root;
    }
}
