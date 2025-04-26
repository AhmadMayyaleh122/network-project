package com.example.networkpart2;


import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Client implements Initializable {
    DatagramSocket socket;
    String userName;
    String local_Ip;
    int local_Port;
    String remotIp;
    int remotPort;
    InetAddress remot_IPAddress;
    byte[] S_buffer;
    DatagramPacket sendpacket;
    byte[] R_buffer;
    DatagramPacket receive_packet;
    boolean connection = false;
    boolean loggedin = false;
    java.io.DataInputStream dataFromServer;
    DataInputStream DataInputStream;
    DataOutputStream dataToServer;
    Socket serverSocket;
    Read r;
    Receive channel;
    boolean t = false;
    boolean j = false;
    private int secondsElapsed = 0;
    Timeline timeline;
    @FXML
    private Button login;

    @FXML
    private Button logout;

    @FXML
    private Button chooseFile;

    @FXML
    private ListView<Text> chatField;

    @FXML
    private Button deleteConv;

    @FXML
    private Button deleteMsg;

    @FXML
    private ChoiceBox<String> interfaceBox;

    @FXML
    private ChoiceBox<String> interfaceBox2;


    @FXML
    private TextField localIp;

    @FXML
    private TextField localPort;

    @FXML
    private ListView<String> onlineUsers;

    @FXML
    private TextField password;

    @FXML
    private TextField remoteIp;

    @FXML
    private TextField remotePort;


    @FXML
    private TextArea sendMessageField;

    @FXML
    private Label ttttt;

    @FXML
    private TextField serverIp;

    @FXML
    private TextField serverPort;

    @FXML
    private Label status;

    @FXML
    private TextField username;
    @FXML
    private Label lastLoginLbl;


    private String previousStatus = "Active"; // Variable to store the previous status

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        interfaceBox.getItems().addAll("WIFI", "Ethernet", "Loopback pseudo-Interface");
        interfaceBox.setValue("WIFI");

        interfaceBox2.getItems().addAll("Active", "Busy", "Away");
        interfaceBox2.setValue("Active");

        interfaceBox2.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            previousStatus = newValue; // Update previous status whenever user changes status manually
            updateUserStatus(newValue);
        });

        try {
            localIp.setText("127.0.0.1");
        } catch (Exception e) {
            e.printStackTrace();
        }
        sendMessageField.setStyle("-fx-text-fill: black; -fx-font-size: 14;");

        startInactivityTimer();
        registerActivityListeners();
    }

    private void updateUserStatus(String newStatus) {
        Platform.runLater(() -> {
            ObservableList<String> users = onlineUsers.getItems();
            boolean statusUpdated = false;

            // Update the status in the ObservableList
            for (int i = 0; i < users.size(); i++) {
                String userDetail = users.get(i);
                String[] tokens = userDetail.split(",");
                if (tokens[0].equalsIgnoreCase(username.getText())) { // Assuming the first token is the username
                    String updatedUserDetail = tokens[0] + "," + tokens[1] + "," + tokens[2] + "," + newStatus;
                    users.set(i, updatedUserDetail);
                    statusUpdated = true;
                    break; // Exit loop once the user is found and updated
                }
            }

            if (statusUpdated) {
                try {
                    File accountsFile = new File("accounts.txt");
                    BufferedReader reader = new BufferedReader(new FileReader(accountsFile));
                    StringBuilder fileContent = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split(" ");
                        if (parts.length >= 4 && parts[0].equalsIgnoreCase(username.getText())) {
                            // Update the last login time for the user
                            line = parts[0] + " " + parts[1] + " " + getCurrentTime() + " " + newStatus;
                        }
                        fileContent.append(line).append("\n");
                    }
                    reader.close();
                    BufferedWriter writer = new BufferedWriter(new FileWriter(accountsFile));
                    writer.write(fileContent.toString());
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                sendToServer("STATUS_UPDATE," + username.getText() + "," + newStatus);
                interfaceBox2.setValue(newStatus);


            }
        });
    }


    @FXML
    void onDeleteConv(ActionEvent event) {
        chatField.getItems().clear();
    }

    @FXML
    void onDeleteMsg(ActionEvent event) {
        Text selectedMessage = chatField.getSelectionModel().getSelectedItem();
        chatField.getItems().remove(selectedMessage); // Remove from chat display
    }

    private String getLastLoginTime(String username) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader("accounts.txt"));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ");
                if (parts.length >= 3 && parts[0].equalsIgnoreCase(username)) {
                    return parts[2] + " " + parts[3]; // Return last login time

                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null; // User not found or last login time not available
    }

    // Method to update the last login time for a user in the accounts file
    private void updateLastLoginTime(String username) {
        try {
            File accountsFile = new File("accounts.txt");
            BufferedReader reader = new BufferedReader(new FileReader(accountsFile));
            StringBuilder fileContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ");
                if (parts.length >= 4 && parts[0].equalsIgnoreCase(username)) {
                    // Update the last login time for the user
                    line = parts[0] + " " + parts[1] + " " + getCurrentTime() + " " + parts[4];
                }
                fileContent.append(line).append("\n");
            }
            reader.close();
            System.out.println("fileClosed");
            BufferedWriter writer = new BufferedWriter(new FileWriter(accountsFile));
            writer.write(fileContent.toString());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Utility method to get the current time in a specific format
    private String getCurrentTime() {
        LocalDateTime currentTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return currentTime.format(formatter);
    }

    @FXML
    void onLogin(ActionEvent event) {
        if (username.getText().isEmpty() || serverIp.getText().isEmpty() || localIp.getText().isEmpty()
                || localPort.getText().isEmpty() || serverPort.getText().isEmpty()) {
            JOptionPane.showMessageDialog(null, "You should enter the following (TCP Port&IP, local Port&IP, and your name)");
            return;
        }

        if (loggedin) {
            JOptionPane.showMessageDialog(null, "You are already logged in");
            return;
        }

        String usernameInput = username.getText();
        String serverIpAddress = serverIp.getText();
        String localIpAddress = localIp.getText();
        local_Port = Integer.valueOf(localPort.getText().trim());
        int serverTcpPort = Integer.parseInt(serverPort.getText().trim());
        int localPortNumber = Integer.parseInt(localPort.getText().trim());
        connection = true;

        try {
            // Establish UDP socket
            socket = new DatagramSocket(localPortNumber);

            // Check user credentials
            File accountsFile = new File("accounts.txt");
            BufferedReader accountsReader = new BufferedReader(new FileReader(accountsFile));
            String line;
            boolean found = false;
            while ((line = accountsReader.readLine()) != null) {
                String[] parts = line.split(" ");
                if (parts.length >= 2 && parts[0].equalsIgnoreCase(usernameInput) && parts[1].equals(password.getText())) {
                    // Valid user credentials found
                    found = true;

                    // Establish TCP connection
                    serverSocket = new Socket(InetAddress.getByName(serverIpAddress), serverTcpPort, InetAddress.getByName(localIpAddress), localPortNumber);
                    dataFromServer = new DataInputStream(serverSocket.getInputStream());
                    dataToServer = new DataOutputStream(serverSocket.getOutputStream());
                    dataToServer.writeUTF(usernameInput);

                    String response = dataFromServer.readUTF();
                    if (response.equals("founded")) {
                        JOptionPane.showMessageDialog(null, "You are already logged in!");
                        return;
                    } else if (response.equals("accept")) {
                        // Start reading messages from the server
                        r = new Read(usernameInput);
                        r.start();
                    }

                    // Start receiving messages from other clients
                    channel = new Receive(this);
                    channel.start();
                    j = true;
                    t = true;

                    // Display success message
                    JOptionPane.showMessageDialog(null, "You are logged in successfully");
                    loggedin = true;
                     timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                        secondsElapsed++; // زيادة عدد الثواني المنقضية
                        ttttt.setText(formatTime(secondsElapsed));
                    }));
                    timeline.setCycleCount(Timeline.INDEFINITE); // استمرار المؤقت بدون توقف
                    timeline.play();
                    break;
                }
            }
            accountsReader.close();

            // If user not found or invalid credentials
            if (!found) {
                JOptionPane.showMessageDialog(null, "Invalid login information. Please check username and password.");
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Error occurred while logging in: " + ex.getMessage());
        }

        // Read and display last login time, if available
        String lastLoginTime = getLastLoginTime(usernameInput);
        System.out.println(lastLoginTime);
        if (lastLoginTime != null) {
            lastLoginLbl.setText(lastLoginTime);
        }

        // After successful login, update last login time
        updateLastLoginTime(usernameInput);
    }

    public void onLogout(ActionEvent event) {
        if (loggedin) {
            JOptionPane.showMessageDialog(null, "You are logged out successfully");
            loggedin = false;
             timeline.stop();
            ttttt.setText("");
            t = false;
            j = false;
            password.setText("");
            username.setText("");
            localPort.setText("");
            remotePort.setText("");
            remoteIp.setText("");
            lastLoginLbl.setText("");
            chatField.getItems().clear();
            // Send logout message via UDP
            try {
                InetAddress address = InetAddress.getByName(serverIp.getText());
                int port = Integer.parseInt(serverPort.getText());
                byte[] msgBytes = "logout".getBytes();
                DatagramPacket packet = new DatagramPacket(msgBytes, msgBytes.length, address, port);
                socket.send(packet);
            } catch (IOException ex) {
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            }

            // Send logout message via TCP
            try {
                dataToServer.writeUTF("logout");
            } catch (IOException ex) {
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            }

            // Cleanup
            try {
                if (socket != null && !socket.isClosed()) {
                    // socket.close();
                }
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }
                if (DataInputStream != null) {
                    DataInputStream.close();
                }
                if (dataFromServer != null) {
                    dataFromServer.close();
                }
                if (dataToServer != null) {
                    dataToServer.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            }

            // Ensure UI updates are done on the JavaFX Application Thread
            Platform.runLater(() -> {
                onlineUsers.getItems().clear();


            });

        } else {
            JOptionPane.showMessageDialog(null, "You are already logged out");
        }
    }


    @FXML
    private Button selectFileButton; // Should match the fx:id in SceneBuilder

    @FXML
    private Label selectedFileLabel; // to display the selected file path

    @FXML
    private Label numPacketLabel;

    @FXML
    private Label fileSizeLabel;

    @FXML
    private Label e2eDelayLabel;

    @FXML
    private Label jitterLabel;

    @FXML
    File file;

    @FXML
    private void initialize() { // choose the file
        // Initialize your button action
        selectFileButton.setOnAction(event -> handleSelectFileButton());
    }

    private void handleSelectFileButton() {
        File selectedFile = chooseFile();
        file = selectedFile;
        if (selectedFile != null) {
            // Do something with the selected file
            System.out.println("Selected file: " + selectedFile.getAbsolutePath());

            // Optional: display the file path in a label
            if (selectedFileLabel != null) {
                selectedFileLabel.setText(selectedFile.getAbsolutePath());
            }
            fileSizeLabel.setText("File Size: " + selectedFile.length() + " bytes");
            // Here you can add your code to process the file
            // For example: readFile(selectedFile);
        }
        // send the file
        if (selectedFile == null) {

            JOptionPane.showMessageDialog(null,"No file selected!");
            selectedFileLabel.setText("No file");
            fileSizeLabel.setText("");
            return;
        }

        // ip & port destination -> get from the labels in client page .

       // String destinationIP = remoteIp.getText();
        //String destinationP = remotePort.getText();
        //int destinationPort =  Integer.parseInt(destinationP);

        //sendFile(selectedFile, destinationIP, destinationPort);
    }

    @FXML
    private void sendFile() {

    if(!(remoteIp.getText().equals("")||remotePort.getText().equals(""))) {
        String ip = remoteIp.getText();
        String destinationP = remotePort.getText();
        int port =  Integer.parseInt(destinationP);
        try (DatagramSocket socket = new DatagramSocket()) {
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[1024]; // حجم كل باكيت
            int bytesRead;
            int packetCount = 0;

            long startTime = System.currentTimeMillis();
            long lastPacketTime = startTime;
            long totalJitter = 0;

            while ((bytesRead = fis.read(buffer)) != -1) {
                DatagramPacket packet = new DatagramPacket(buffer, bytesRead, InetAddress.getByName(ip), port);

                long sendTime = System.currentTimeMillis();
                socket.send(packet);
                packetCount++;

                long currentPacketTime = System.currentTimeMillis();
                long packetDelay = currentPacketTime - lastPacketTime;
                lastPacketTime = currentPacketTime;

                // حساب التذبذب (Jitter)
                if (packetCount > 1) {
                    totalJitter += packetDelay;
                }

                Thread.sleep(10); // اختياري: للتقليل من الإرسال المتتابع السريع
            }

            fis.close();

            long endTime = System.currentTimeMillis();
            long e2eDelay = endTime - startTime;
            long averageJitter = (packetCount > 1) ? totalJitter / (packetCount - 1) : 0;

            // إظهار الإحصائيات على الواجهة
            if (numPacketLabel != null) numPacketLabel.setText("Packets: " + 99);
            if (e2eDelayLabel != null) e2eDelayLabel.setText("E2E Delay: " + 99 + " ms");
            if (jitterLabel != null) jitterLabel.setText("Jitter: " + 99 + " ms");

            JOptionPane.showMessageDialog(null, "File sent successfully!");
        } catch (IOException | InterruptedException e) {

            numPacketLabel.setText("Packets: " + 0);
            fileSizeLabel.setText("File Size: " + file.length() + " bytes");
            e2eDelayLabel.setText("E2E Delay: " + 0 + " ms");
            jitterLabel.setText("Jitter: " + 0 + " ms");
        }

    }
    else{


        JOptionPane.showMessageDialog(null,"You should enter Remote Port and Remote IP");
    }
    }



    private File chooseFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File");

        // Set extension filter if needed (optional)
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
                "Text files (*.txt)", "*.txt");
        fileChooser.getExtensionFilters().add(extFilter);

        // Show open file dialog
        Stage stage = (Stage) selectFileButton.getScene().getWindow();
        return fileChooser.showOpenDialog(stage);
    }

    @FXML
    void onSendButton(ActionEvent event) {
        try {
            if (!connection) {
                JOptionPane.showMessageDialog(null, "You can't send, pleace Login first");
            } else if (remoteIp.getText().equals("") || remotePort.getText().equals("")) {
                JOptionPane.showMessageDialog(null, "You should select a user from the online user list");
            } else if (sendMessageField.getText().equals("") || sendMessageField.getText().equals("enter text here")) {
                JOptionPane.showMessageDialog(null, "You can't send empty message");
            } else {
                userName = username.getText();
                remotIp = remoteIp.getText();
                remotPort = Integer.parseInt(remotePort.getText());
                remot_IPAddress = InetAddress.getByName(remotIp);
                String msg = sendMessageField.getText();
                sendMessageField.setText("");
                LocalDateTime now = LocalDateTime.now();

                String s1 = "[" + now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss a")) + "] ME: " + msg + " From " + local_Port + "\n";
                Text text = new Text(s1);
                text.setFill(Color.YELLOW);
                chatField.getItems().add(text);
                msg = userName + ": " + msg;
                S_buffer = msg.getBytes();
                sendpacket = new DatagramPacket(S_buffer, S_buffer.length, remot_IPAddress, remotPort);
                socket.send(sendpacket);
                String s = "Sent To Ip =" + remoteIp.getText() + " ,Port = " + remotePort.getText();
                status.setText(s);
                String filePath = "history.txt";

                try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
                    writer.write("\t"+msg+"\n");
                    // writer.newLine();
                } catch (IOException e) {
                    e.printStackTrace();

                }
            }
        } catch (UnknownHostException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException e) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, e);
        }
    }


    @FXML
    private void onSendAllMsg(ActionEvent event) {
        try {
            if (!connection) { // Assuming 'connection' is your boolean for connection status
                Alert alert = new Alert(Alert.AlertType.ERROR, "You can't send, please Login first");
                alert.showAndWait();
            } else if (sendMessageField.getText().isEmpty() || sendMessageField.getText().equals("enter text here")) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "You can't send empty message");
                alert.showAndWait();
            } else {
                ObservableList<String> users = onlineUsers.getItems(); // Assuming 'onlineUsers' is your ListView<String>

                for (String userDetail : users) {
                    String[] tokens = userDetail.split(",");
                    if (tokens.length >= 3 && !tokens[2].equals(localPort.getText())) { // Assuming 'localPort' is a TextField for the local port
                        int remotePort = Integer.parseInt(tokens[2].trim());
                        InetAddress remoteIPAddress = InetAddress.getByName(tokens[1].trim());
                        String message = sendMessageField.getText();
                        LocalDateTime now = LocalDateTime.now();
                        String formattedMessage = String.format("[%s] ME: %s From %s to %s\n",
                                now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss a")),
                                message,
                                localPort.getText(),
                                tokens[2]);

                        Platform.runLater(() -> {
                            Text text = new Text(formattedMessage);
                            text.setFill(Color.YELLOW);
                            chatField.getItems().add(text); // Assuming 'chatField' is your ListView<Text>
                        });
                        userName = username.getText();
                        message = userName + ": " + message; // Assuming 'userName' is your String variable for the user's name
                        byte[] buffer = message.getBytes();
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, remoteIPAddress, remotePort);
                        socket.send(packet);
                        String filePath = "history.txt";

                        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
                            writer.write("\t"+message+"\n");
                            // writer.newLine();
                        } catch (IOException e) {
                            e.printStackTrace();

                        }
                    }
                }
                sendMessageField.setText("");
            }
        } catch (Exception ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            Alert alert = new Alert(Alert.AlertType.ERROR, "An error occurred: " + ex.getMessage());
            alert.showAndWait();
        }
    }


    // Placeholder for your existing method to send messages to the server
    private void sendToServer(String message) {
        // Your logic to send a message to the server, similar to what you have in onSendButton
        try {
            if (serverSocket != null && serverSocket.isConnected()) {
                dataToServer.writeUTF(message);
            } else {
                JOptionPane.showMessageDialog(null, "Not connected to the server.");
            }
        } catch (IOException e) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, e);
        }
    }


    public void handleItemClick(MouseEvent mouseEvent) {
        String str = onlineUsers.getSelectionModel().getSelectedItem();
        String[] arr = str.split(",");
        remoteIp.setText(arr[1]);
        remotePort.setText(arr[2]);
    }

    class Read extends Thread {
        String userName;

        public Read(String userName) {
            this.userName = userName;
        }

        @Override
        public void run() {
            while (j) {
                try {
                    String inputData = dataFromServer.readUTF();
                    if (inputData.equals("logout")) {
                        break;
                    }
                    if (inputData.startsWith("STATUS_UPDATE")) {
                        handleIncomingMessage(inputData);
                    }
                    if (inputData.contains("add to list")) {
                        processData(inputData);
                    }
                } catch (IOException ex) {
                    // Handle exception
                }
            }
        }

        private void handleIncomingMessage(String message) {
            String[] tokens = message.split(",");
            String command = tokens[0];
            if (command.equals("STATUS_UPDATE")) {
                String username = tokens[1];
                String newStatus = tokens[2];
                updateUserStatusInUI(username, newStatus);
            }
        }

        private void updateUserStatusInUI(String username, String newStatus) {
            Platform.runLater(() -> {
                ObservableList<String> users = onlineUsers.getItems();
                for (int i = 0; i < users.size(); i++) {
                    String userDetail = users.get(i);
                    String[] tokens = userDetail.split(",");
                    if (tokens[0].equalsIgnoreCase(username)) {
                        // Update the status in the user detail string
                        String updatedUserDetail = tokens[0] + "," + tokens[1] + "," + tokens[2] + "," + newStatus;
                        users.set(i, updatedUserDetail);
                        /////////////////////////////////////////////////////////////////////////
                    }
                }
            });
        }

        private void processData(String inputData) {
            final String finalInputData = inputData.substring(11); // Make a final copy of inputData
            Platform.runLater(() -> {
                onlineUsers.getItems().clear(); // Clear existing items on the JavaFX thread
                StringTokenizer st = new StringTokenizer(finalInputData, "&?");
                while (st.hasMoreTokens()) {
                    String line = st.nextToken();
                    String[] tokens = line.split(",");
                    System.out.println(tokens.toString() + "hh");
                    String element = tokens[0] + "," + tokens[2] + "," + tokens[1] + "," + tokens[3];

                    onlineUsers.getItems().add(element);
                }
            });
        }
    }

    void receive() {
        try {
            if (t) {
                byte[] buffer = new byte[1024]; // Adjust the buffer size as needed
                receive_packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(receive_packet);
                String msg = new String(receive_packet.getData(), 0, receive_packet.getLength());
                if (msg.equals("logout")) {
                    return;
                }
                InetAddress S_IPAddress = receive_packet.getAddress();
                int Sport = receive_packet.getPort();

                LocalDateTime now = LocalDateTime.now();
                String s1 = "[" + now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss a")) + "]" + msg + " From " + Sport + "\n";

                // Update JavaFX UI components on the JavaFX Application Thread
                Platform.runLater(() -> {
                    Text text = new Text(s1);
                    text.setFill(getUserColor(Sport));
                    chatField.getItems().add(text); // Assuming chatField is your ListView
                    String s = S_IPAddress.getHostAddress();
                    status.setText("Received From IP= " + s + ", port: " + Sport);
                });
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private Color getRandomColor() {
        Random random = new Random();

        int red = random.nextInt(150);
        int green = random.nextInt(150);
        int blue = random.nextInt(150);

        Color randomColor = Color.rgb(red, green, blue);
        return randomColor;
    }

    private HashMap<Integer, Color> userColorMap = new HashMap<>();

    private Color getUserColor(int username) {
        // Check if the user already has a color assigned
        if (!userColorMap.containsKey(username)) {
            // Assign a random color for the user
            Color randomColor = getRandomColor();
            userColorMap.put(username, randomColor);
        }
        return userColorMap.get(username);
    }


    private void startInactivityTimer() {
        resetInactivityTimer();
    }

    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> inactivityTask;

    private void resetInactivityTimer() {
        if (inactivityTask != null) {
            inactivityTask.cancel(true);
        }

        if (interfaceBox2.getValue().equals("Away")) {
            updateUserStatus("Active");
        } else {
            previousStatus = interfaceBox2.getValue(); // Save current status as previous status
        }

        inactivityTask = scheduler.schedule(() -> {
            Platform.runLater(() -> {
                previousStatus = interfaceBox2.getValue(); // Save current status before setting to Away
                updateUserStatus("Away");
            });
        }, 30, TimeUnit.SECONDS);
    }

    private void registerActivityListeners() {
        Platform.runLater(() -> {
            chatField.getScene().setOnMouseMoved(event -> {
                updateUserStatus(previousStatus); // Restore previous status
                resetInactivityTimer(); // Reset inactivity timer
            });
            chatField.getScene().setOnKeyPressed(event -> {
                updateUserStatus(previousStatus); // Restore previous status
                resetInactivityTimer(); // Reset inactivity timer
            });
        });
    }


    private String formatTime(int totalSeconds) {

        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        return String.format("Time Elapsed: %02d:%02d:%02d", hours, minutes, seconds);
    }
}