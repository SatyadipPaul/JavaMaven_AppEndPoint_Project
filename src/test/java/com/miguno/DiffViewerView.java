package com.example.application.views;

import com.google.gson.Gson;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.Route;

import java.util.ArrayList;
import java.util.List;

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

    // Fields for diagram stages
    private int currentStage = 0;
    private int totalStages;
    private final List<String> stages = new ArrayList<>();
    private Span progressIndicator;  // Instance variable

    public DiffViewerView() {
        H1 title = new H1("Enhanced Diff Viewer");

        configureComponents();
        configureLayout();
        configureListeners();
        initializeDiagramStages();
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

        // Create the "+" and "-" buttons with icons
        Button plusButton = new Button(new Icon(VaadinIcon.PLUS));
        plusButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        plusButton.addClickListener(event -> {
            if (currentStage < totalStages - 1) {
                currentStage++;
                updateProgressIndicator();
                renderMermaidDiagram();
            } else {
                Notification.show("Already at the final stage", 2000, Notification.Position.MIDDLE);
            }
        });
        plusButton.getElement().setProperty("title", "Next Stage");

        Button minusButton = new Button(new Icon(VaadinIcon.MINUS));
        minusButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        minusButton.addClickListener(event -> {
            if (currentStage > 0) {
                currentStage--;
                updateProgressIndicator();
                renderMermaidDiagram();
            } else {
                Notification.show("Already at the initial stage", 2000, Notification.Position.MIDDLE);
            }
        });
        minusButton.getElement().setProperty("title", "Previous Stage");

        // Progress Indicator
        progressIndicator = new Span();
        updateProgressIndicator();

        // Create a layout for the diagram controls
        HorizontalLayout diagramControlsLayout = new HorizontalLayout(minusButton, plusButton, progressIndicator);
        diagramControlsLayout.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        diagramControlsLayout.setPadding(true);
        diagramControlsLayout.setSpacing(true);

        // Add all components to the view
        add(title, input1, input2, controlsLayout1, controlsLayout2, diffOutput, diagramControlsLayout, mermaidDiagramDiv);
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

            // Reset to the first stage when new inputs are compared
            currentStage = 0;
            updateProgressIndicator();
            renderMermaidDiagram();
        });
    }

    private void initializeDiagramStages() {
        // Define your diagram stages here
        stages.clear();
        stages.add("""
                   graph LR;
                   A[Input 1];
                   """);  // Stage 1

        stages.add("""
                   graph LR;
                   A[Input 1]-->Comparison;
                   """);  // Stage 2

        stages.add("""
                   graph LR;
                   A[Input 1]-->Comparison;
                   B[Input 2]-->Comparison;
                   """);  // Stage 3

        stages.add("""
                   graph LR;
                   A[Input 1]-->Comparison;
                   B[Input 2]-->Comparison;
                   Comparison-->Result[Diff Result];
                   """);  // Stage 4

        totalStages = stages.size();
    }

    private void addRequiredResources() {
        UI.getCurrent().getPage().addStyleSheet("https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.8.0/styles/github.min.css");
        UI.getCurrent().getPage().addStyleSheet("https://cdn.jsdelivr.net/npm/diff2html/bundles/css/diff2html.min.css");
        UI.getCurrent().getPage().addJavaScript("https://cdn.jsdelivr.net/npm/diff/dist/diff.min.js");
        UI.getCurrent().getPage().addJavaScript("https://cdn.jsdelivr.net/npm/diff2html/bundles/js/diff2html-ui.min.js");
        // Load Mermaid.js and svg-pan-zoom with callback
        UI.getCurrent().getPage().executeJs(
                """
                (function() {
                  function loadSvgPanZoom(callback) {
                    if (typeof svgPanZoom === 'undefined') {
                      var script = document.createElement('script');
                      script.src = 'https://cdn.jsdelivr.net/npm/svg-pan-zoom@3.6.1/dist/svg-pan-zoom.min.js';
                      script.onload = callback;
                      document.head.appendChild(script);
                    } else {
                      callback();
                    }
                  }
                  if (typeof mermaid === 'undefined') {
                    var script = document.createElement('script');
                    script.src = 'https://cdn.jsdelivr.net/npm/mermaid@11.2.1/dist/mermaid.min.js';
                    script.onload = function() {
                      loadSvgPanZoom(function() {
                        $0.$server.mermaidLoaded();
                      });
                    };
                    document.head.appendChild(script);
                  } else {
                    loadSvgPanZoom(function() {
                      $0.$server.mermaidLoaded();
                    });
                  }
                })();
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
        String graphDefinition = stages.get(currentStage);

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

                    // Initialize or re-initialize pan and zoom
                    (function() {
                        // Destroy existing svg-pan-zoom instance if it exists
                        if (window.svgPanZoomInstance) {
                            window.svgPanZoomInstance.destroy();
                            window.svgPanZoomInstance = null;
                        }
                        // Load svg-pan-zoom library if not already loaded
                        function initPanZoom() {
                            window.svgPanZoomInstance = svgPanZoom(svgElement, {
                                controlIconsEnabled: false,
                                zoomEnabled: true,
                                panEnabled: true,
                                fit: true,
                                center: true
                            });
                        }
                        if (typeof svgPanZoom === 'undefined') {
                            var script = document.createElement('script');
                            script.src = 'https://cdn.jsdelivr.net/npm/svg-pan-zoom@3.6.1/dist/svg-pan-zoom.min.js';
                            script.onload = initPanZoom;
                            document.head.appendChild(script);
                        } else {
                            initPanZoom();
                        }
                    })();
                }).catch(function(err) {
                    console.error('Error rendering Mermaid diagram:', err);
                });
                """;

        UI.getCurrent().getPage().executeJs(jsCode, graphDefinition, getElement());
    }

    private void updateProgressIndicator() {
        progressIndicator.setText("Stage " + (currentStage + 1) + " of " + totalStages);
    }

    @ClientCallable
    public void nodeClicked(String nodeId) {
        Notification.show("Node " + nodeId + " is clicked", 3000, Notification.Position.MIDDLE);
    }
}
