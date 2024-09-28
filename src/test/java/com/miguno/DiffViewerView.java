package com.example.application.views;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.Route;

@Route("diff-viewer")
public class DiffViewerView extends VerticalLayout {

    private final TextArea input1 = new TextArea("Enter first string");
    private final TextArea input2 = new TextArea("Enter second string");
    private final Button compareButton = new Button("Compare");
    private final Div diffOutput = new Div();
    private final Div mermaidDiagramDiv = new Div(); // Mermaid diagram container
    private final RadioButtonGroup<String> viewModeGroup = new RadioButtonGroup<>();
    private final ComboBox<String> matchingSelect = new ComboBox<>("Matching Level");
    private final ComboBox<String> diffStyleSelect = new ComboBox<>("Diff Style");
    private final ComboBox<String> colorSchemeSelect = new ComboBox<>("Color Scheme");
    private final Checkbox drawFileListCheckbox = new Checkbox("Show File List");
    private final IntegerField diffMaxChangesField = new IntegerField("Max Changes");
    private final IntegerField diffMaxLineLengthField = new IntegerField("Max Line Length");

    public DiffViewerView() {
        H1 title = new H1("Enhanced Diff Viewer");

        configureComponents();
        configureLayout();
        configureListeners();
        addRequiredResources();

        HorizontalLayout controlsLayout1 = new HorizontalLayout(compareButton, viewModeGroup, matchingSelect, diffStyleSelect);
        HorizontalLayout controlsLayout2 = new HorizontalLayout(colorSchemeSelect, drawFileListCheckbox, diffMaxChangesField, diffMaxLineLengthField);
        controlsLayout1.setAlignItems(Alignment.BASELINE);
        controlsLayout2.setAlignItems(Alignment.BASELINE);

        // Configure Mermaid diagram container
        mermaidDiagramDiv.setId("mermaidDiagram");
        mermaidDiagramDiv.setWidthFull();
        mermaidDiagramDiv.setHeight("400px"); // Adjust as needed
        mermaidDiagramDiv.getStyle().set("border", "1px solid #ddd").set("padding", "10px").set("margin-top", "10px");

        // Assign IDs to Div components
        diffOutput.setId("diffOutput");

        // Add all components to the view
        add(title, input1, input2, controlsLayout1, controlsLayout2, diffOutput, mermaidDiagramDiv);
    }

    private void configureComponents() {
        viewModeGroup.setLabel("View Mode");
        viewModeGroup.setItems("Unified", "Side by Side");
        viewModeGroup.setValue("Unified");

        matchingSelect.setItems("none", "lines", "words");
        matchingSelect.setValue("none");

        diffStyleSelect.setItems("word", "char");
        diffStyleSelect.setValue("word");

        colorSchemeSelect.setItems("light", "dark", "auto");
        colorSchemeSelect.setValue("light");

        drawFileListCheckbox.setValue(true);

        diffMaxChangesField.setStepButtonsVisible(true);
        diffMaxChangesField.setStep(10);
        diffMaxChangesField.setMin(0);

        diffMaxLineLengthField.setStepButtonsVisible(true);
        diffMaxLineLengthField.setStep(100);
        diffMaxLineLengthField.setMin(0);
    }

    private void configureLayout() {
        setSizeFull();
        setMargin(true);
        setSpacing(true);

        input1.setWidthFull();
        input2.setWidthFull();
        diffOutput.getStyle()
                .set("border", "1px solid #ddd")
                .set("padding", "10px")
                .set("overflow", "auto")
                .set("margin-top", "10px");
    }

    private void configureListeners() {
        compareButton.addClickListener(e -> {
            generateDiffScript(input1.getValue(), input2.getValue());

            // Generate a graph definition based on inputs
            String graphDefinition = generateGraphDefinition(input1.getValue(), input2.getValue());
            renderMermaidDiagram(graphDefinition);
        });
    }

    private void addRequiredResources() {
        UI.getCurrent().getPage().addStyleSheet("https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.8.0/styles/github.min.css");
        UI.getCurrent().getPage().addStyleSheet("https://cdn.jsdelivr.net/npm/diff2html/bundles/css/diff2html.min.css");
        UI.getCurrent().getPage().addJavaScript("https://cdn.jsdelivr.net/npm/diff/dist/diff.min.js");
        UI.getCurrent().getPage().addJavaScript("https://cdn.jsdelivr.net/npm/diff2html/bundles/js/diff2html-ui.min.js");
        // Add Mermaid.js with callback
        UI.getCurrent().getPage().executeJs(
                """
                if (typeof mermaid === 'undefined') {
                  var script = document.createElement('script');
                  script.src = 'https://cdn.jsdelivr.net/npm/mermaid@11.2.1/dist/mermaid.min.js';
                  script.onload = function() {
                    $0.$server.mermaidLoaded();
                  };
                  document.head.appendChild(script);
                } else {
                  $0.$server.mermaidLoaded();
                }
                """,
                getElement()
        );
    }

    @ClientCallable
    public void mermaidLoaded() {
        renderMermaidDiagram();
    }

    private void generateDiffScript(String text1, String text2) {
        UI.getCurrent().getPage().executeJs(
                """
                const text1 = $0;
                const text2 = $1;
                const diff = Diff.createTwoFilesPatch('Original', 'Modified', text1, text2);
                const targetElement = document.getElementById('diffOutput');
                const configuration = {
                    drawFileList: $2,
                    matching: $3,
                    diffStyle: $4,
                    colorScheme: $5,
                    outputFormat: $6,
                    highlight: true,
                    synchronisedScroll: true,
                    diffMaxChanges: $7,
                    diffMaxLineLength: $8
                };
                const diff2htmlUi = new Diff2HtmlUI(targetElement, diff, configuration);
                diff2htmlUi.draw();
                diff2htmlUi.highlightCode();
                """,
                text1,
                text2,
                drawFileListCheckbox.getValue(),
                matchingSelect.getValue(),
                diffStyleSelect.getValue(),
                colorSchemeSelect.getValue(),
                viewModeGroup.getValue().equals("Unified") ? "line-by-line" : "side-by-side",
                diffMaxChangesField.isEmpty() ? null : diffMaxChangesField.getValue(),
                diffMaxLineLengthField.isEmpty() ? null : diffMaxLineLengthField.getValue()
        );
    }

    private void renderMermaidDiagram() {
        String graphDefinition = """
                graph LR;
                A[Input 1]-->Comparison;
                B[Input 2]-->Comparison;
                Comparison-->Result[Diff Result];
                """;

        renderMermaidDiagram(graphDefinition);
    }

    private void renderMermaidDiagram(String graphDefinition) {
        String jsCode = """
                mermaid.initialize({ startOnLoad: false, securityLevel: 'loose' });
                mermaid.render('mermaidGraph', $0).then(function(result) {
                    var diagramDiv = document.getElementById('mermaidDiagram');
                    diagramDiv.innerHTML = result.svg;

                    // Add click event listeners to the nodes
                    var svgElement = diagramDiv.querySelector('svg');
                    var nodes = svgElement.querySelectorAll('.node');

                    nodes.forEach(function(node) {
                        node.style.cursor = 'pointer';
                        node.addEventListener('click', function(event) {
                            var nodeId = node.id.replace('flowchart-', '').replace('graph-', '');
                            // Call server-side method
                            $1.$server.nodeClicked(nodeId);
                        });
                    });
                }).catch(function(err) {
                    console.error('Error rendering Mermaid diagram:', err);
                });
                """;

        UI.getCurrent().getPage().executeJs(jsCode, graphDefinition, getElement());
    }

    private String generateGraphDefinition(String text1, String text2) {
        // For demonstration, let's create a simple diagram that shows the lengths of the inputs
        int length1 = text1.length();
        int length2 = text2.length();

        return String.format("""
                graph LR;
                Input1[Input 1 (%d chars)]-->Comparison;
                Input2[Input 2 (%d chars)]-->Comparison;
                Comparison-->Result[Diff Result];
                """, length1, length2);
    }

    @ClientCallable
    public void nodeClicked(String nodeId) {
        Notification.show("Node " + nodeId + " is clicked", 3000, Notification.Position.MIDDLE);
    }
}
