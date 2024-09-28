package com.example.application.views;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.ArrayList;
import java.util.List;

@Route("mermaid-view-interact")
@CssImport("./styles/shared-styles.css")
public class MermaidViewInteractions extends VerticalLayout {

    private Div diagramDiv;
    private int currentStage = 0;
    private final int totalStages;
    private Span progressIndicator;  // Instance variable

    // Define the diagram stages
    private final List<String> stages = new ArrayList<>();

    public MermaidViewInteractions() {
        // Initialize the stages using text blocks
        stages.add("""
                   graph LR;
                   id1([A]);
                   """);  // Initial stage with only Node A

        stages.add("""
                   graph LR;
                   id1([A])-->id2([B]);
                   """);  // First click: A --> B

        stages.add("""
                   graph LR;
                   id1([A])-->id2([B]);
                   id2([B])-->id3([C]) & id4([D]);
                   id2([B])-->id4([D]);
                   """);  // Second click: B --> C & D

        stages.add("""
                   graph LR;
                   id1([A])-->id2([B]);
                   id2([B])-->id3([C]);
                   id2([B])-->id4([D]);
                   id4([D])-->id4([Y]);
                   """);  // Final diagram

        totalStages = stages.size();

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        // Create the diagram container
        diagramDiv = new Div();
        diagramDiv.setId("diagram");
        diagramDiv.addClassName("diagram-container");
        diagramDiv.setSizeFull();

        // Create the "+" and "-" buttons with icons
        Button plusButton = new Button(new Icon(VaadinIcon.PLUS));
        plusButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        plusButton.addClickListener(event -> {
            if (currentStage < stages.size() - 1) {
                currentStage++;
                renderDiagram();
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
                renderDiagram();
            } else {
                Notification.show("Already at the initial stage", 2000, Notification.Position.MIDDLE);
            }
        });
        minusButton.getElement().setProperty("title", "Previous Stage");

        // Progress Indicator
        progressIndicator = new Span();  // Initialize the instance variable

        // Update the progress indicator for the initial stage
        updateProgressIndicator();

        // Button layout
        HorizontalLayout buttonLayout = new HorizontalLayout(minusButton, plusButton, progressIndicator);
        buttonLayout.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        buttonLayout.setPadding(true);
        buttonLayout.setSpacing(true);
        buttonLayout.addClassName("button-layout");

        // Add the components to the view
        add(buttonLayout, diagramDiv);
        expand(diagramDiv);

        // Load Mermaid.js and render the initial diagram
        loadMermaidJs();
    }

    private void loadMermaidJs() {
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
        renderDiagram();
    }

    private void renderDiagram() {
        String graphDefinition = stages.get(currentStage);
        updateProgressIndicator();

        String jsCode = """
                mermaid.initialize({ startOnLoad: false, securityLevel: 'loose' });
                mermaid.render('theGraph', $0).then(function(result) {
                    var svg = result.svg;
                    var diagramDiv = document.getElementById('diagram');
                    diagramDiv.innerHTML = svg;

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
                    $1.$server.renderingFailed(err.message);
                });
                """;

        UI.getCurrent().getPage().executeJs(jsCode, graphDefinition, getElement());
    }

    @ClientCallable
    public void nodeClicked(String nodeId) {
        Notification.show("Node " + nodeId + " is clicked", 3000, Notification.Position.MIDDLE);
    }

    @ClientCallable
    public void renderingFailed(String errorMessage) {
        Notification.show("Failed to render diagram: " + errorMessage, 3000, Notification.Position.MIDDLE);
    }

    private void updateProgressIndicator() {
        progressIndicator.setText("Stage " + (currentStage + 1) + " of " + totalStages);
    }
}
