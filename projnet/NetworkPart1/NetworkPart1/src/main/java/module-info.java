module com.example.networkpart1 {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.networkpart1 to javafx.fxml;
    exports com.example.networkpart1;
}