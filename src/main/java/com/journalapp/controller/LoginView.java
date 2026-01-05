package main.java.com.journalapp.controller;

import main.java.com.journalapp.controller.MainController;
import main.java.com.journalapp.util.Session;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class LoginView {
    private final MainController mainController;
    private final VBox mainContainer;

    public LoginView(MainController mainController) {
        this.mainController = mainController;

        // Main container
        mainContainer = new VBox();
        // Just using a color style since CSS file might be missing
        mainContainer.setStyle("-fx-background-color: linear-gradient(to bottom right, #CFE3F3, #FAD0C4);");
        mainContainer.setAlignment(Pos.CENTER);
        mainContainer.setMaxWidth(Double.MAX_VALUE);
        mainContainer.setMaxHeight(Double.MAX_VALUE);

        // Glass form container
        VBox formContainer = new VBox(15);
        //formContainer.setStyle("-fx-background-color: white; -fx-padding: 40; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");
        formContainer.setMaxWidth(350);
        formContainer.setAlignment(Pos.CENTER);

        formContainer.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.4);" +
                "-fx-background-radius: 15;" +
                "-fx-border-color: rgba(255, 255, 255, 0.8);" +
                "-fx-border-radius: 15;" +
                "-fx-border-width: 1px;" +
                "-fx-padding: 40;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 15, 0, 0, 4);"
        );

        Label titleLabel = new Label("Login");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #333;");

        // Input Fields Style (Slightly transparent white)
        String inputStyle = "-fx-background-color: rgba(255,255,255,0.7); -fx-background-radius: 5; -fx-padding: 10; -fx-font-size: 14px; -fx-border-color: rgba(0,0,0,0.1); -fx-border-radius: 5;";

        TextField emailField = new TextField();
        emailField.setPromptText("Email address");
        emailField.setStyle(inputStyle);

        PasswordField passField = new PasswordField();
        passField.setPromptText("Password");
        passField.setStyle(inputStyle);

        Label errorLabel = new Label("Incorrect credentials!");
        errorLabel.setStyle("-fx-text-fill: red;");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        Button loginBtn = new Button("Login");
        loginBtn.setStyle(
                "-fx-background-color: #3498db;" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 10 20;" +
                "-fx-font-size: 14px;" +
                "-fx-background-radius: 5;" +
                "-fx-cursor: hand;"
        );
        loginBtn.setMaxWidth(Double.MAX_VALUE);

        loginBtn.setOnAction(e -> {
            String identifier = emailField.getText();
            String password = passField.getText();
            // Call the Session API
            if (Session.login(identifier, password)) {
                System.out.println("Login Successful");
                mainController.showMainApp();
            } else {
                errorLabel.setText("Incorrect credentials!");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
            }
        });

        Button createAccountLink = new Button("Or create a new account");
        createAccountLink.setStyle("-fx-background-color: transparent; -fx-text-fill: #3498db; -fx-cursor: hand;");
        createAccountLink.setOnAction(e -> mainController.showSignUpView());

        formContainer.getChildren().addAll(titleLabel, emailField, passField, errorLabel, loginBtn, createAccountLink);

        Label footerLabel = new Label("By signing in you agree to our terms.");
        footerLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 12px; -fx-padding: 20 0 0 0;");

        mainContainer.getChildren().addAll(formContainer, footerLabel);
    }

    public VBox getView() {
        return mainContainer;
    }
}