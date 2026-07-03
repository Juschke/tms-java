package com.translationagency.shared.ui;

import com.vaadin.flow.component.confirmdialog.ConfirmDialog;

public final class Confirmations {

    private Confirmations() {
    }

    public static void delete(String title, String text, Runnable onConfirm) {
        confirm(title, text, "Loeschen", true, onConfirm);
    }

    public static void destructive(String title, String text, String confirmText, Runnable onConfirm) {
        confirm(title, text, confirmText, true, onConfirm);
    }

    public static void confirm(String title, String text, String confirmText, boolean destructive, Runnable onConfirm) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader(title);
        dialog.setText(text);
        dialog.setCancelable(true);
        dialog.setCancelText("Abbrechen");
        dialog.setConfirmText(confirmText);
        if (destructive) {
            dialog.setConfirmButtonTheme("error primary");
        } else {
            dialog.setConfirmButtonTheme("primary");
        }
        dialog.addConfirmListener(event -> onConfirm.run());
        dialog.open();
    }
}
