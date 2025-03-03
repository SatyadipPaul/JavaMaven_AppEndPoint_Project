package com.example.application.views.dbschemadesigner;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.annotation.Secured;

import com.example.application.views.MainLayout;
import com.fasterxml.jackson.databind.ObjectMapper;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

@PageTitle("DB Schema Designer")
@Route(value = "db-schema-designer", layout = MainLayout.class)
@Secured("ROLE_ADMIN")
public class DBSchemaDesignerView extends VerticalLayout {

    private static final Logger logger = LoggerFactory.getLogger(DBSchemaDesignerView.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private final JdbcTemplate jdbcTemplate;
    private Div designerContainer;

    @Autowired
    public DBSchemaDesignerView(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        
        designerContainer = new Div();
        designerContainer.setClassName("db-schema-designer-container");
        designerContainer.setSizeFull();
        designerContainer.getElement().setProperty("innerHTML", getDesignerHTML());
        add(designerContainer);

        // Set up client-server communication
        setupClientToServerCommunication();
    }

    private void setupClientToServerCommunication() {
        Page page = UI.getCurrent().getPage();
        
        // Add JavaScript to handle communication between the HTML/JS and Java
        page.executeJs(
            "window.addEventListener('message', function(event) {" +
            "   if (event.data) {" +
            "       if (event.data.type === 'describe-table-request') {" +
            "           const tableName = event.data.tableName;" +
            "           $0.$server.getTableSchema(tableName);" +
            "       } else if (event.data.type === 'export-schema-request') {" +
            "           const schemaData = event.data.schemaData;" +
            "           $0.$server.exportSchema(schemaData);" +
            "       }" +
            "   }" +
            "});" +
            
            // Method to forward table schema data to the iframe
            "window.sendTableSchemaToIframe = function(schemaData) {" +
            "   const iframe = document.querySelector('.db-schema-designer-container iframe');" +
            "   if (iframe && iframe.contentWindow) {" +
            "       iframe.contentWindow.postMessage({" +
            "           type: 'describe-table-response'," +
            "           tableData: JSON.parse(schemaData)" +
            "       }, '*');" +
            "   }" +
            "};",
            getElement()
        );
    }

    /**
     * Client callable method to fetch table schema using DESCRIBE query
     */
    @ClientCallable
    public void getTableSchema(String tableName) {
        try {
            // Execute DESCRIBE query to get table schema
            List<Map<String, Object>> columns = describeTable(tableName);
            
            if (columns.isEmpty()) {
                showNotification("Table not found or no columns available", true);
                return;
            }
            
            // Convert columns to format expected by the client
            JsonObject tableData = Json.createObject();
            tableData.put("name", tableName);
            
            JsonArray columnsArray = Json.createArray();
            int index = 0;
            
            for (Map<String, Object> column : columns) {
                JsonObject columnObj = Json.createObject();
                
                // Extract column details
                String fieldName = String.valueOf(column.get("Field"));
                String fieldType = String.valueOf(column.get("Type"));
                String keyType = String.valueOf(column.get("Key"));
                String nullableValue = String.valueOf(column.get("Null"));
                
                columnObj.put("name", fieldName);
                columnObj.put("type", fieldType);
                
                // Check if column is a primary key
                boolean isPrimaryKey = "PRI".equals(keyType);
                columnObj.put("primaryKey", isPrimaryKey);
                
                // Check if column is nullable
                boolean isNullable = "YES".equals(nullableValue);
                columnObj.put("nullable", isNullable);
                
                // Add to columns array
                columnsArray.set(index++, columnObj);
            }
            
            tableData.put("columns", columnsArray);
            
            // Send the table schema back to the client
            UI.getCurrent().getPage().executeJs("window.sendTableSchemaToIframe($0)", tableData.toJson());
            
        } catch (Exception e) {
            logger.error("Error getting table schema", e);
            showNotification("Error retrieving table schema: " + e.getMessage(), true);
        }
    }

    /**
     * Client callable method to handle schema export
     */
    @ClientCallable
    public void exportSchema(String schemaJson) {
        try {
            // Here you would implement the actual export logic
            // For example, saving to a file, database, or processing the schema
            logger.info("Received schema export request");
            
            // For demonstration, just log a portion of the schema
            if (schemaJson != null && schemaJson.length() > 100) {
                logger.info("Schema export (truncated): {}...", schemaJson.substring(0, 100));
            } else {
                logger.info("Schema export: {}", schemaJson);
            }
            
            // Send success response back to client
            UI.getCurrent().getPage().executeJs(
                "const iframe = document.querySelector('.db-schema-designer-container iframe');" +
                "if (iframe && iframe.contentWindow) {" +
                "   iframe.contentWindow.postMessage({" +
                "       type: 'export-schema-response'," +
                "       success: true" +
                "   }, '*');" +
                "}"
            );
            
            showNotification("Schema exported successfully", false);
            
        } catch (Exception e) {
            logger.error("Error exporting schema", e);
            showNotification("Error exporting schema: " + e.getMessage(), true);
        }
    }

    /**
     * Execute DESCRIBE query to get table columns
     */
    private List<Map<String, Object>> describeTable(String tableName) {
        // Sanitize table name to prevent SQL injection
        if (!tableName.matches("[a-zA-Z0-9_]+")) {
            throw new IllegalArgumentException("Invalid table name format");
        }
        
        // Execute DESCRIBE query
        String sql = "DESCRIBE " + tableName;
        try {
            return jdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            logger.error("Error executing DESCRIBE query for table: {}", tableName, e);
            return new ArrayList<>();
        }
    }

    /**
     * Show notification to user
     */
    private void showNotification(String message, boolean isError) {
        Notification notification = Notification.show(message, 3000, Notification.Position.TOP_CENTER);
        if (isError) {
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        } else {
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        }
    }

    /**
     * Returns the HTML content for the designer
     */
    private String getDesignerHTML() {
        // In a real implementation, this would include the full HTML or reference an external resource
        return "<iframe style='width: 100%; height: 100%; border: none;' src='db-schema-designer-content.html'></iframe>";
    }
}
