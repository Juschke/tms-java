package com.translationagency;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Theme(value = "translation-theme")
public class TranslationManagementApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(TranslationManagementApplication.class, args);
    }
}
