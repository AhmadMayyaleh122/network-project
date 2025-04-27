package com.example.networkpart1;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HelloController implements Initializable {

    @FXML
    private Button BackButton;

    @FXML
    private AnchorPane Pane1;

    @FXML
    private AnchorPane Pane2;

    @FXML
    private Button archiveMsg;

    @FXML
    private ListView<Text> chatField;

    @FXML
    private Button deleteConv;

    @FXML
    private Button deleteMsg;

    @FXML
    private ChoiceBox<String> interfaceBox=new ChoiceBox<>();

    @FXML
    private TextField localIp;

    @FXML
    private TextField localPort;

    @FXML
    private TextField remoteIp;
    @FXML
    private TextField indexArea;

    @FXML
    private TextField remotePort;

    @FXML
    private TextField sendMessageField;

    @FXML
    private Button sendMsg;

    @FXML
    private Label status;

    @FXML
    private Button testMsg;
    Thread t ;
    DatagramSocket Socket;
    String SendMessage;
    private Boolean flag=true;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        interfaceBox.getItems().addAll("WIFI","Ethernet","Loopback pseudo-Interface");
        interfaceBox.setValue("WIFI");
        sendMessageField.setStyle("-fx-text-fill: black; -fx-font-size: 14;");
        try {
            localIp.setText(InetAddress.getLocalHost().getHostAddress().toString());
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    class P2PCon implements Runnable
    {
        @Override
        public void run() {
            Server();
        }
    }

    @FXML
    public void ONback() {
    Pane2.setVisible(false);
    Pane1.setVisible(true);
    }



    private void Client(){
        try
        {
            String [] ipdest= remoteIp.getText().split("\\.");
            byte []IP_other_device={(byte)Integer.parseInt(ipdest[0]),(byte)Integer.parseInt(ipdest[1]),(byte)Integer.parseInt(ipdest[2]),(byte)Integer.parseInt(ipdest[3])};
            InetAddress IPDest = InetAddress.getByAddress(IP_other_device);

            LocalDateTime now = LocalDateTime.now();
            SendMessage =  "["+now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss a"))+"] :  "+ sendMessageField.getText();
            byte[] SendData= SendMessage.getBytes();
            DatagramPacket SendPacket = new DatagramPacket(SendData, SendData.length, IPDest , Integer.parseInt(remotePort.getText()));
            Socket.send(SendPacket);
            String senmsg="Me "+SendMessage+" from "+Socket.getLocalPort()+"\n";
            Text text1 = new Text(senmsg);
            text1.setFill(Color.YELLOW); // Set color
            chatField.getItems().addAll(text1);
            status.setText("Send to: "+SendPacket.getAddress().getHostAddress()+", Port: "+SendPacket.getPort());
        }
        catch (NumberFormatException e) {
            showAlert("Please enter all fields correctly");
        } catch (IllegalArgumentException e) {
            showAlert("Please enter remote port correctly");
        } catch (ArrayIndexOutOfBoundsException e) {
            showAlert("Please enter IP address correctly");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alert.showAndWait();
    }
    private void Server() {
        try
        {
            while(true) {
                byte[] ReceiveData = new byte[65536];
                DatagramPacket ReceivePacket = new DatagramPacket(ReceiveData, ReceiveData.length);
                Socket.receive(ReceivePacket); // from other client
                String ReceiveMsg = new String(ReceivePacket.getData());
                String sert = "Rem " + ReceiveMsg.trim() + " from " + ReceivePacket.getPort() + "\n";

                // Update UI components on the JavaFX Application Thread
                Platform.runLater(() -> {
                    Text text = new Text(sert);
                    text.setFill(Color.ORANGE);
                    chatField.getItems().add(text);
                    status.setText("Recived from:"+ReceivePacket.getAddress().getHostAddress()+",Port:"+ReceivePacket.getPort());//ServerSocket.getLocalPort()
                });
            }
        }catch(java.lang.NumberFormatException e)
        {
            showAlert("Please enter correct local port ");
        }catch(java.net.BindException e)
        {
            showAlert("port already used, please choose diffrent ");

        }catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    @FXML
    public void onSendButton(){
        writeToLogFile("user with port: "+localPort.getText() + " and IP: " + localIp.getText() + " Sent a message.\n");
        if(flag){
            try{
                if(Socket == null)
                {
                    Socket =new DatagramSocket(Integer.parseInt(localPort.getText()));
                    P2PCon conn =new P2PCon();
                    t =new Thread(conn);
                    t.start();
                }
                else
                {
                    Socket.close();
                    // t.stop();
                    t.interrupt();
                    Socket =new DatagramSocket(Integer.parseInt(localPort.getText()));
                    P2PCon conn =new P2PCon();
                    t =new Thread(conn);
                    t.start();
                }
            }catch(java.lang.NumberFormatException e)
            {
                showAlert( "Please enter Local IP and local port correctly");
            }catch(java.lang.IllegalArgumentException e)
            {
                showAlert( "  \"Please enter correct local port \"");
            }catch(Exception e)
            {
                e.printStackTrace();
            }
            flag=false;
        }
        Client();
        sendMessageField.setText("");
    }
    @FXML
    public void onStartButton(){
        writeToLogFile("user with port: "+localPort.getText() + " and IP: " + localIp.getText() + " started listening.\n");
        try{
            if(Socket == null)
            {
                Socket =new DatagramSocket(Integer.parseInt(localPort.getText()));
                P2PCon conn =new P2PCon();
                t =new Thread(conn);
                t.start();
            }
            else
            {
                Socket.close();
                // t.stop();
                t.interrupt();
                Socket =new DatagramSocket(Integer.parseInt(localPort.getText()));
                P2PCon conn =new P2PCon();
                t =new Thread(conn);
                t.start();
            }
            Pane2.setVisible(true);
            Pane1.setVisible(false);
        }catch(java.lang.NumberFormatException e)
        {
            showAlert( "Please enter Local IP and local port correctly");
        }catch(java.lang.IllegalArgumentException e)
        {
            showAlert( "  \"Please enter correct local port \"");


        }catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    // Assuming these are defined at the class level
    List<Text> archivedMessages = new ArrayList<>();


    @FXML
    void onDeleteButtonClick(ActionEvent event) {
        writeToLogFile("user with port: "+localPort.getText() + " and IP: " + localIp.getText() + " deleted a message.\n");
        Text selectedMessage = chatField.getSelectionModel().getSelectedItem();
        chatField.getItems().remove(selectedMessage); // Remove from chat display
        archivedMessages.add(selectedMessage);
        scheduleMessageDeletion(selectedMessage);

    }
    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    @FXML
    void onArchiveButtonClick(ActionEvent event) {
        writeToLogFile("user with port: "+localPort.getText() + " and IP: " + localIp.getText() + " opened the archived messages.\n");

        Stage archiveStage = new Stage();
        ListView<Text> archiveListView = new ListView<>();
        archiveListView.getItems().addAll(archivedMessages);

        // Wrap the restoreButton in an HBox for horizontal alignment
        HBox buttonContainer = new HBox();
        buttonContainer.setAlignment(Pos.CENTER); // Center the button horizontally

        // Load the CSS for the button
        String btnCssPath = getClass().getResource("/css/components/lbl.css").toExternalForm();
        // Load the CSS for the pane background
        String paneCssPath = getClass().getResource("/css/styles/pane.css").toExternalForm();

        Button restoreButton = new Button("Restore");
        restoreButton.getStyleClass().add("lbl5");
        buttonContainer.getChildren().add(restoreButton); // Add the button to the container

        VBox layout = new VBox(5); // Use spacing to separate elements
        layout.getChildren().addAll(archiveListView, buttonContainer); // Add the list and the button container to the layout
        layout.setAlignment(Pos.CENTER); // This will center the button container, but not affect the list's items alignment
        layout.setStyle("-fx-background-color: #7CF8FD;");
        Scene scene = new Scene(layout, 300, 500);
        scene.getStylesheets().addAll(btnCssPath, paneCssPath); // Add both stylesheets to the scene

        archiveStage.setScene(scene);
        archiveStage.show();

        restoreButton.setOnAction(e -> {
            Text selectedMessage = archiveListView.getSelectionModel().getSelectedItem();
            if (selectedMessage != null) {
                archiveListView.getItems().remove(selectedMessage);
                chatField.getItems().add(selectedMessage);
                archivedMessages.remove(selectedMessage);

                // Sort chat field items based on timestamp after adding the restored message
                sortChatFieldByTimestamp();

                writeToLogFile("user with port: "+localPort.getText() + " and IP: " + localIp.getText() + " restored a message from archive.\n");
            }
        });

        // Schedule task for clearing archived messages after 2 minutes
        scheduler.schedule(() -> {
            // Use Platform.runLater to update JavaFX components from another thread
            Platform.runLater(() -> {
                archivedMessages.clear(); // Clear the archived messages
                archiveListView.getItems().clear(); // Clear the ListView
            });
        }, 2, TimeUnit.MINUTES); // Schedule to run after 2 minutes
    }
    private void sortChatFieldByTimestamp() {
        List<Text> chatMessages = new ArrayList<>(chatField.getItems());
        chatMessages.sort(Comparator.comparing(this::extractTimestamp));

        chatField.getItems().setAll(chatMessages);
    }

    private LocalDateTime extractTimestamp(Text text) {
        String message = text.getText();
        // Assuming the timestamp is enclosed within square brackets
        int startIndex = message.indexOf('[');
        int endIndex = message.indexOf(']');
        String timestamp = message.substring(startIndex + 1, endIndex);
        // Parse the timestamp string to LocalDateTime
        System.out.println(timestamp);
        return LocalDateTime.parse(timestamp, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss a"));
    }
    @FXML
    void onDeleteAllButtonClick(ActionEvent event) {
        writeToLogFile("user with port: "+localPort.getText() + " and IP: " + localIp.getText() + " deleted the conversation.\n");

        // Move all items from chatField to archivedMessages
        archivedMessages.addAll(chatField.getItems());
        // Clear the chatField
        chatField.getItems().clear();
        for (Text message : archivedMessages) {
            scheduleMessageDeletion(message);
        }


    }
    private void scheduleMessageDeletion(Text message) {
        scheduler.schedule(() -> {
            // Use Platform.runLater to update JavaFX components from another thread
            Platform.runLater(() -> {
                archivedMessages.remove(message); // Remove the message from archived messages
            });
        }, 2, TimeUnit.MINUTES); // Schedule to run after 2 minutes
    }
    @FXML
    void TestButtonActionPerformed(ActionEvent event) {
        writeToLogFile("user with port: "+localPort.getText() + " and IP: " + localIp.getText() + " Tested the connection.\n");


        sendMessageField.setText("Hello");
        Client();
        sendMessageField.setText("");
    }
    void writeToLogFile(String msg){
        try {
            FileWriter myWriter = new FileWriter("logfile.txt",true);
            myWriter.append(msg);
            myWriter.close();
            System.out.println("Successfully wrote to the file.");
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
}