package com.translationagency.ui.login;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.login.LoginOverlay;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("login")
@PageTitle("Login | Translation Management")
@AnonymousAllowed
public class LoginView extends Div implements BeforeEnterObserver {

    private final LoginOverlay loginOverlay = new LoginOverlay();

    public LoginView() {
        loginOverlay.setTitle("Übersetzungs-Portal");
        loginOverlay.setDescription("B2B Übersetzungsmanagement & Partnerportal");
        loginOverlay.setAction("login"); // spring security default form login path
        loginOverlay.setOpened(true);
        loginOverlay.setForgotPasswordButtonVisible(false);
        add(loginOverlay);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (event.getLocation()
                .getQueryParameters()
                .getParameters()
                .containsKey("error")) {
            loginOverlay.setError(true);
        }
    }
}
