import com.nimbusds.jose.shaded.gson.Gson;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.Route;

@Route("diff-viewer")
public class DiffViewerView extends VerticalLayout {

    public DiffViewerView() {
        H1 title = new H1("Diff Viewer");
        TextArea input1 = new TextArea("Enter first string");
        TextArea input2 = new TextArea("Enter second string");
        Button compareButton = new Button("Compare");
        Div diffOutput = new Div();

        compareButton.addClickListener(e -> {
            String diffScript = generateDiffScript(input1.getValue(), input2.getValue());
            UI.getCurrent().getPage().executeJs(diffScript);
        });

        add(title, input1, input2, compareButton, diffOutput);

        // Add required CSS
        UI.getCurrent().getPage().addStyleSheet("https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.8.0/styles/github.min.css");
        UI.getCurrent().getPage().addStyleSheet("https://cdn.jsdelivr.net/npm/diff2html/bundles/css/diff2html.min.css");

        // Add required JavaScript libraries
        UI.getCurrent().getPage().addJavaScript("https://cdn.jsdelivr.net/npm/diff/dist/diff.min.js");
        UI.getCurrent().getPage().addJavaScript("https://cdn.jsdelivr.net/npm/diff2html/bundles/js/diff2html-ui.min.js");

        // Add custom styles
        getStyle().set("margin", "20px");
        input1.setWidthFull();
        input2.setWidthFull();
        diffOutput.getStyle()
                .set("border", "1px solid #ddd")
                .set("padding", "10px")
                .set("overflow", "auto");
    }

    private String generateDiffScript(String text1, String text2) {
        return String.format(
                "const text1 = %s;" +
                        "const text2 = %s;" +
                        "const diff = Diff.createTwoFilesPatch('Original', 'Modified', text1, text2);" +
                        "const targetElement = document.querySelector('div');" +
                        "const configuration = {" +
                        "    drawFileList: false," +
                        "    matching: 'lines'," +
                        "    highlight: true," +
                        "    synchronisedScroll: true" +
                        "};" +
                        "const diff2htmlUi = new Diff2HtmlUI(targetElement, diff, configuration);" +
                        "diff2htmlUi.draw();" +
                        "diff2htmlUi.highlightCode();",
                toJsonString(text1), toJsonString(text2)
        );
    }

    private String toJsonString(String s) {
        return new Gson().toJson(s);
    }
}
