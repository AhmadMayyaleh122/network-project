package com.example.networkpart2;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server implements Initializable {
    @FXML
    private Label status;
    @FXML
    private ChoiceBox<String> interfaceBox;

    @FXML
    private ListView<String> onlineUsers;

    @FXML
    private TextFlow serverMessages;

    @FXML
    private TextField Port;

    private ServerSocket socket;
    private HashMap clientsHash = new HashMap();
    boolean t=false;

    @FXML
    public void onStart(){
        int portNumber = 0000;
        boolean isNumbers = false;

        try {
            if (!Port.getText().isEmpty()) {
                portNumber = Integer.parseInt(Port.getText());
                isNumbers = true;
                if (isNumbers) {
                    socket = new ServerSocket(portNumber);

                    String s1 = "Start Listening at port: " + portNumber + "\n";
                    Text text = new Text(s1);
                    text.setFill(Color.BLUE);
                    serverMessages.getChildren().add(text);
                    new ClientAccept(socket).start();
                    status.setText("Address: "+ InetAddress.getByName("localhost").getHostAddress() +", port: "+portNumber);
                }
            } else {
                JOptionPane.showMessageDialog(null, "pleace enter a port number in 'port number' field.");
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "pleace enter only a numbers in 'port number' field.");
            isNumbers = false;
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "The port number is used");
            //               Logger.getLogger(ClientAccept.class.getName()).log(Level.SEVERE, null, ex);

        }

    }

    public void onAddUser(ActionEvent actionEvent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("SignUp.fxml"));
            Parent root = loader.load();

            // Create a new stage
            Stage stage = new Stage();
            stage.setTitle("New Window");

            // Set the scene for the new stage
            Scene scene = new Scene(root);
            stage.setScene(scene);

            // Show the new stage
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        interfaceBox.getItems().addAll("WIFI","Ethernet","Loopback pseudo-Interface");
        interfaceBox.setValue("Loopback pseudo-Interface");
    }

    private class ClientAccept extends Thread {

        private ServerSocket socket;

        public ClientAccept(ServerSocket socket) {
            this.socket = socket;

        }

        public void run() {
            while (true) {
                try {
                    Socket clientSocket = socket.accept();
                    String username = new DataInputStream(clientSocket.getInputStream()).readUTF();
                    DataOutputStream dataOutOfClient = new DataOutputStream(clientSocket.getOutputStream());
                    if (clientsHash.containsKey(username)) {
                        dataOutOfClient.writeUTF("founded");
                    } else {
                        clientsHash.put(username, clientSocket);
                        addTextToArea(username, true);
                        dataOutOfClient.writeUTF("accept");
                        new endToEndList().start();
                        new ReadMessage(clientSocket, username).start();
                    }

                } catch (IOException ex) {
                    Logger.getLogger(ClientAccept.class.getName()).log(Level.SEVERE, null, ex);
                } catch (BadLocationException ex) {
                }

            }

        }

    }
    class ReadMessage extends Thread {

        Socket s;
        String ID;

        ReadMessage(Socket s, String username) {
            this.s = s;
            this.ID = username;

        }

        public void run() {

            while (!clientsHash.isEmpty() && clientsHash.containsKey(ID)) {
                try {
                    String in = new DataInputStream(s.getInputStream()).readUTF();
                    if (in.contains("logout")) {

                        new DataOutputStream(((Socket) clientsHash.get(ID)).getOutputStream()).writeUTF("logout");////////

                        clientsHash.remove(ID);
                        addTextToArea(ID, false);
                        new endToEndList().start();

                    }
                    else if(in.contains("STATUS_UPDATE")){
                        new endToEndList().start();

                    }
                } catch (IOException | BadLocationException ex) {
                    clientsHash.remove(ID);
                    try {
                        addTextToArea(ID, false);
                    } catch (BadLocationException ex1) {
                    }
                    new endToEndList().start();
                    ex.printStackTrace();

                }
            }
        }
    }
    private class endToEndList extends Thread {
        public endToEndList() {
        }

        public void run() {
            try {
                String s = new String();
                Set k = clientsHash.keySet();
                Iterator itr = k.iterator();
                Platform.runLater(() -> {
                    onlineUsers.getItems().clear(); // Clear the ListView before adding updated users
                });




                while (itr.hasNext()) {
                    String key = (String) itr.next();
                    //username,portNum,IP

                    String status="";
                    try {
                        BufferedReader reader = new BufferedReader(new FileReader("accounts.txt"));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            String[] parts = line.split(" ");
                            if (parts.length >= 4 && parts[0].equalsIgnoreCase(key)) {
                                status=parts[4];
                                System.out.println("hiii");
                                break;
                            }
                        }
                        reader.close();
                    } catch (IOException e) {

                        e.printStackTrace();
                    }


                    s += key + "," + String.valueOf(((Socket) clientsHash.get(key)).getPort()) + ","
                            + ((Socket) clientsHash.get(key)).getInetAddress().getHostAddress() +","+status+ "&?";
                    String ele = ((Socket) clientsHash.get(key)).getInetAddress().getHostAddress() + ","
                            + String.valueOf(((Socket) clientsHash.get(key)).getPort());
                    Platform.runLater(() -> {
                        onlineUsers.getItems().add(ele); // Add each user to the ListView
                    });
                }
                if (s.length() != 0) {
                    s = s.substring(0, s.length() - 2);//to delete "&?" from the last element
                }
                itr = k.iterator();
                while (itr.hasNext()) {
                    String key = (String) itr.next();
                    try {
                        new DataOutputStream(((Socket) clientsHash.get(key)).getOutputStream()).writeUTF("add to list" + s);
                    } catch (IOException ex) {
                        clientsHash.remove(key);
                        addTextToArea(key, false);
                    }
                }
            } catch (BadLocationException ex) {
                ex.printStackTrace();
            }
        }
    }
    public void addTextToArea(String username, boolean color) throws BadLocationException {
        Platform.runLater(() -> {
            if (color) {
                //in log in
                String s1 = username + " login" + "\n";
                Text text = new Text(s1);
                text.setFill(Color.RED);
                serverMessages.getChildren().add(text);
            } else {
                //in log out
                String s1 = username + " logout" + "\n";
                Text text = new Text(s1);
                text.setFill(Color.BLUE);
                serverMessages.getChildren().add(text);
            }
        });
    }






}