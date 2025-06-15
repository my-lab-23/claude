module org.example.seatour {
    requires javafx.controls;
    requires javafx.fxml;
    requires kotlin.stdlib;
    requires com.google.gson;

    opens org.example.seatour to javafx.fxml, com.google.gson;
    exports org.example.seatour;
}
