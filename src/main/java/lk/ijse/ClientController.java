package lk.ijse;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;


public class ClientController {

    public VBox messageContainer;
    public Button btnConnect;
    public Button btnFileSelect;
    public Button btnSend;
    Socket socket;
    DataOutputStream out;
    DataInputStream in;

    boolean isConnect = false;

    @FXML
    private AnchorPane ancMessagePanel;

    @FXML
    private TextField txtHost;

    @FXML
    private TextField txtMessageBox;

    @FXML
    private TextField txtPort;

    @FXML
    private TextField txtUserName;

    @FXML
    void btnConnectOnAction(ActionEvent event) {

        if (isConnect) {
           Alert alert = new Alert(Alert.AlertType.CONFIRMATION,"Confirm Disconnect Connection");
           alert.showAndWait();
           if (alert.getResult() == ButtonType.OK) {
               disconnect();
           }
            return;
        }

        String host = txtHost.getText();
        Integer port = Integer.parseInt(txtPort.getText());
        String userName = txtUserName.getText();

        if (host.isEmpty() || port!=0 || userName.isEmpty()) {}

        try {
            socket = new Socket(host, port);
            System.out.println("Connected to server: " + socket.getInetAddress().getHostAddress());

            connect();

            out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());

            out.writeUTF(userName);
            out.flush();

            new Thread(() -> {
                try {
                    String resMessage = in.readUTF();

                    System.out.println("Server : "+resMessage);

                    if (resMessage.equals("IMAGE")) {
                        int imageSize = in.readInt();
                        byte[] imageBytes = new byte[imageSize];
                        in.readFully(imageBytes);

                        Platform.runLater(() -> {
                            loadImage(imageBytes,false);
                        });
                    } else {
                        Platform.runLater(() -> {
                            loadMessage(resMessage,false);
                        });
                    }

                } catch (IOException e) {
                    Platform.runLater(() -> {
                       disconnect();
                        new Alert(Alert.AlertType.ERROR,"Thread connection Disconnectes!").showAndWait();
                    });
                }
            }).start();
        } catch (Exception e) {
            System.out.println("Thread connection Disconnect");
            new Alert(Alert.AlertType.ERROR,"Thread connection Disconnectes!").showAndWait();

        }
    }

    void disconnect(){
        try {
            socket.close();
            isConnect = false;
            System.out.println("Connection Disconnect!");
            socket = null;
            in = null;
            out = null;
            isConnect = false;
            btnConnect.setText("Connect");

            messageContainer.getChildren().clear();

            txtUserName.setDisable(false);
            txtHost.setDisable(false);
            txtPort.setDisable(false);
            txtUserName.clear();

            btnSend.setDisable(true);
            btnFileSelect.setDisable(true);
            txtMessageBox.setDisable(true);
            txtMessageBox.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void reset(){

    }

    void connect (){
        isConnect = true;

        txtHost.setDisable(true);
        txtUserName.setDisable(true);
        txtPort.setDisable(true);

        btnConnect.setText("Disconnect");

        txtMessageBox.setDisable(false);
        btnSend.setDisable(false);
        btnFileSelect.setDisable(false);
    }

    @FXML
    void btnSelectFileOnAction(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Image");
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif");
        fileChooser.getExtensionFilters().add(extFilter);
        java.io.File file = fileChooser.showOpenDialog(txtMessageBox.getScene().getWindow());

        if (file != null) {
            try {
                byte[] imageBytes = Files.readAllBytes(file.toPath());
                out.writeUTF("IMAGE");
                out.writeInt(imageBytes.length);
                out.write(imageBytes);
                out.flush();
                loadImage(imageBytes, true);
            } catch (IOException e) {
                new Alert(Alert.AlertType.ERROR, "File Not Found").show();
            }
        }
    }

    @FXML
    void btnSendMessageOnAction(ActionEvent event) {
        String message = txtMessageBox.getText();

        if (message.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "Please enter to message : " + message).showAndWait();
        }

        try {
            loadMessage(message,true);
            out.writeUTF(message);
            out.flush();
            System.out.println("Client send : " + message);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            txtMessageBox.clear();
        }
    }

    public void loadMessage(String text, boolean isSent) {
        System.out.println(text);

        HBox messageRow = new HBox(10);

        Label messageLabel = new Label(text);
        messageLabel.setWrapText(true);

        if (isSent) {
            messageRow.setAlignment(Pos.TOP_RIGHT);
            messageRow.getChildren().addAll(messageLabel);
        } else {
            messageRow.setAlignment(Pos.TOP_LEFT);
            messageRow.getChildren().addAll(messageLabel);
        }

        messageContainer.getChildren().add(messageRow);
    }

    public void loadImage(byte[] imageBytes , boolean isSent) {
        HBox messageRow = new HBox(10);

        ImageView imageView = new ImageView();
        Image image = new Image(new ByteArrayInputStream(imageBytes));
        imageView.setImage(image);
        imageView.setImage(image);
        imageView.setFitWidth(200);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setCache(true);

        if (isSent) {
            messageRow.setAlignment(Pos.TOP_RIGHT);
            messageRow.getChildren().addAll(imageView);
        } else {
            messageRow.setAlignment(Pos.TOP_LEFT);
            messageRow.getChildren().addAll(imageView);
        }

        messageContainer.getChildren().add(messageRow);
    }

}