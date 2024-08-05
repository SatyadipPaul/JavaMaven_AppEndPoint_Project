import com.vaadin.flow.component.grid.contextmenu.GridMenuItem;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class YourView extends VerticalLayout {

    public YourView() {
        // Your existing code for creating the Grid and GridMenuItem

        GridMenuItem downloadMenuItem = contextMenu.addItem("Download Zip", event -> {
            try {
                Path zipFilePath = createZipFile(); // Your method to create the zip file
                downloadZipFile(zipFilePath);
            } catch (IOException e) {
                e.printStackTrace();
                // Handle the exception (e.g., show an error message to the user)
            }
        });

        // Add other components to your layout
    }

    private void downloadZipFile(Path zipFilePath) throws IOException {
        String fileName = zipFilePath.getFileName().toString();

        StreamResource streamResource = new StreamResource(fileName,
                () -> {
                    try {
                        byte[] zipContent = Files.readAllBytes(zipFilePath);
                        return new ByteArrayInputStream(zipContent);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    }
                });

        Anchor downloadLink = new Anchor(streamResource, "");
        downloadLink.getElement().setAttribute("download", true);
        downloadLink.add(new com.vaadin.flow.component.html.Span("Download " + fileName));
        downloadLink.getElement().addEventListener("click", event -> {
            // Optionally, you can delete the zip file after download starts
            // Files.deleteIfExists(zipFilePath);
        });

        add(downloadLink);
        downloadLink.getElement().callJsFunction("click");
        remove(downloadLink);
    }

    private Path createZipFile() throws IOException {
        // Implement your zip file creation logic here
        // Return the Path to the created zip file
    }
}
