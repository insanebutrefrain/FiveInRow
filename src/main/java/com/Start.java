package com;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;


public class Start extends Application {

    public Button robot;
    public Button doublePlayer;
    public Button exit;
    public Button singlePlayer;

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(Start.class.getResource("Start.fxml"));
        Pane root = loader.load();
        Scene scene = new Scene(root);
        primaryStage.setTitle("五子棋游戏");
        primaryStage.getIcons().add(new Image(String.valueOf(Start.class.getResource("images/icon.png"))));
        primaryStage.setScene(scene);
        primaryStage.show();
        primaryStage.setOnCloseRequest(event -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("退出");
            alert.setHeaderText("将要关闭窗口！");
            alert.setContentText("确定要退出游戏吗？");
            alert.showAndWait();

            // 如果用户选择取消关闭窗口，则阻止窗口关闭
            if (alert.getResult() == ButtonType.CANCEL) {
                event.consume();
            } else
                System.exit(0); // 退出程序
        });
    }

    public static void main(String[] args) {
        launch(args);
    }

    public void start(ActionEvent Event) {

        if (Event.getSource() == singlePlayer)
            FiveInRowController.Pattern = FiveInRowController.Mode.SingleMode;
        else if (Event.getSource() == doublePlayer)
            FiveInRowController.Pattern = FiveInRowController.Mode.DoubleMode;
        else {
            FiveInRowController.Pattern = FiveInRowController.Mode.RobotMode;
        }
        try {
            // 加载第二个界面的FXML文件
            FXMLLoader loader = new FXMLLoader(Start.class.getResource("fiveInRow.fxml"));
            Parent secondSceneRoot = loader.load();
            // 创建一个新的场景并将其设置为当前舞台的场景
            Scene secondScene = new Scene(secondSceneRoot);
            Stage currentStage = (Stage) robot.getScene().getWindow();
            currentStage.setScene(secondScene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void exit(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("退出");
        alert.setHeaderText("将要退出游戏！");
        alert.setContentText("确定要退出游戏吗？");
        alert.showAndWait();

        // 如果用户选择取消关闭窗口，则阻止窗口关闭
        if (alert.getResult() == ButtonType.CANCEL) {
            event.consume();
        } else
            System.exit(0); // 退出程序
    }
}
