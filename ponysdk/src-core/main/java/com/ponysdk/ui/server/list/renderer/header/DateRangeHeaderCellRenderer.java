
package com.ponysdk.ui.server.list.renderer.header;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.ponysdk.impl.theme.PonySDKTheme;
import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.basic.PAnchor;
import com.ponysdk.ui.server.basic.PAttachedPopupPanel;
import com.ponysdk.ui.server.basic.PHorizontalPanel;
import com.ponysdk.ui.server.basic.PKeyCodes;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PPopupPanel;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.server.basic.event.PCloseEvent;
import com.ponysdk.ui.server.basic.event.PCloseHandler;
import com.ponysdk.ui.server.basic.event.PKeyUpEvent;
import com.ponysdk.ui.server.basic.event.PKeyUpFilterHandler;
import com.ponysdk.ui.server.basic.event.PValueChangeEvent;
import com.ponysdk.ui.server.basic.event.PValueChangeHandler;
import com.ponysdk.ui.server.form.FormField;
import com.ponysdk.ui.server.form.FormField.ResetHandler;
import com.ponysdk.ui.server.form.renderer.DateBoxFormFieldRenderer;
import com.ponysdk.ui.server.form.renderer.TextBoxFormFieldRenderer;
import com.ponysdk.ui.server.list.event.RefreshListEvent;
import com.ponysdk.ui.terminal.basic.PVerticalAlignment;

public class DateRangeHeaderCellRenderer extends ComplexHeaderCellRenderer implements PClickHandler {

    final PHorizontalPanel fieldContainer = new PHorizontalPanel();

    private final PVerticalPanel popupContent;

    private final FormField from;

    private final FormField to;

    private final TextBoxFormFieldRenderer mainformFieldRenderer;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");

    public DateRangeHeaderCellRenderer(final String caption, final FormField from, final FormField to, final String pojoProperty) {
        super(caption, pojoProperty);

        final PAnchor anchor = new PAnchor("");
        anchor.addStyleName(PonySDKTheme.COMPLEXLIST_HEADERCELLRENDERER_DATERANGE);

        mainformFieldRenderer = (TextBoxFormFieldRenderer) formField.getFormFieldRenderer();
        mainformFieldRenderer.addClickHandler(this);
        mainformFieldRenderer.setEnabled(false);

        this.from = from;
        this.to = to;

        popupContent = new PVerticalPanel();
        popupContent.add(new PLabel("from"));
        popupContent.add(from.render().asWidget());
        popupContent.add(new PLabel("to"));
        popupContent.add(to.render().asWidget());
        final PKeyUpFilterHandler handler = new PKeyUpFilterHandler(PKeyCodes.ENTER) {

            @Override
            public void onKeyUp(final PKeyUpEvent keyUpEvent) {
                final RefreshListEvent refreshListEvent = new RefreshListEvent(this, from);
                eventBus.fireEvent(refreshListEvent);
            }
        };

        from.addResetHandler(new ResetHandler() {

            @Override
            public void onReset() {
                updateMainFormField();
            }
        });

        to.addResetHandler(new ResetHandler() {

            @Override
            public void onReset() {
                updateMainFormField();
            }
        });

        from.addDomHandler(handler, PKeyUpEvent.TYPE);
        to.addDomHandler(handler, PKeyUpEvent.TYPE);

        final DateBoxFormFieldRenderer fromRenderer = (DateBoxFormFieldRenderer) from.getFormFieldRenderer();
        fromRenderer.addValueChangeHandler(new PValueChangeHandler<Date>() {

            @Override
            public void onValueChange(final PValueChangeEvent<Date> event) {
                updateMainFormField();
            }

        });

        final DateBoxFormFieldRenderer toRenderer = (DateBoxFormFieldRenderer) to.getFormFieldRenderer();
        toRenderer.addValueChangeHandler(new PValueChangeHandler<Date>() {

            @Override
            public void onValueChange(final PValueChangeEvent<Date> event) {
                updateMainFormField();
            }
        });

        anchor.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent arg0) {
                showPopup();
            }

        });

        fieldContainer.add(container);
        fieldContainer.add(anchor);
        fieldContainer.setCellVerticalAlignment(anchor, PVerticalAlignment.ALIGN_BOTTOM);
    }

    @Override
    public IsPWidget render() {
        return fieldContainer;
    }

    protected void updateMainFormField() {
        final String fromFormat = from.getValue() == null ? null : dateFormat.format(from.getValue());
        final String toFormat = to.getValue() == null ? null : dateFormat.format(to.getValue());

        if (fromFormat == null && toFormat == null) mainformFieldRenderer.reset();
        else if (fromFormat == null) mainformFieldRenderer.setText("<= " + toFormat);
        else if (toFormat == null) mainformFieldRenderer.setText(">= " + fromFormat);
        else mainformFieldRenderer.setText(fromFormat + " - " + toFormat);
    }

    protected void showPopup() {
        final PAttachedPopupPanel levelPopupPanel = new PAttachedPopupPanel(true, fieldContainer);
        final PPopupPanel popupWidget = levelPopupPanel;
        popupWidget.setWidget(popupContent);
        popupWidget.show();

        popupWidget.addCloseHandler(new PCloseHandler() {

            @Override
            public void onClose(final PCloseEvent closeEvent) {
                updateMainFormField();
            }
        });

    }

    @Override
    public void onClick(final PClickEvent event) {
        showPopup();
    }

    public void setDateFormat(final SimpleDateFormat dateFormat) {
        this.dateFormat = dateFormat;
    }
}
