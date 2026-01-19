module org.example.cobraflex {
  requires javafx.controls;
  requires javafx.fxml;
  requires lombok;
  requires org.apache.httpcomponents.core5.httpcore5;
  requires org.apache.httpcomponents.client5.httpclient5;
  requires org.slf4j;
  requires com.fasterxml.jackson.core;
  requires com.fasterxml.jackson.databind;

  opens org.example.cobraflex to javafx.fxml;
  exports org.example.cobraflex;
  exports org.example.cobraflex.clients;
}