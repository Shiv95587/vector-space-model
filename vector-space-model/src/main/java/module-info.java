module com.example.fxjavatest {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.bootstrapfx.core;
    requires org.apache.opennlp.tools;

    opens com.example.fxjavatest to javafx.fxml;
    exports com.example.fxjavatest;
}