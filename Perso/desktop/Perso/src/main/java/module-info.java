module org.example.perso {
    requires javafx.controls;
    requires javafx.fxml;
    requires kotlin.stdlib;
    requires smile.core;
    requires org.slf4j;

    opens org.example.perso to javafx.fxml;
    exports org.example.perso;
}
