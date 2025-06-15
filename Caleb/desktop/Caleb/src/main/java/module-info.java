module org.example.caleb {
    requires javafx.controls;
    requires javafx.fxml;
    requires kotlin.stdlib;
    requires kotlinx.coroutines.javafx;
    requires kotlinx.coroutines.core;
    requires retrofit2;
    requires retrofit2.converter.gson;
    requires okhttp3;
    requires okhttp3.logging;
    requires com.google.gson;
    requires jasypt;
    requires java.desktop;

    opens org.example.caleb to javafx.fxml, com.google.gson;
    exports org.example.caleb;
}
