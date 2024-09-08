package com.example.application.views;

import com.nimbusds.jose.shaded.gson.Gson;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.Route;

@Route("diff-viewer-enhanced")
public class DiffViewerEnhanced extends VerticalLayout {

    private final TextArea input1 = new TextArea("Enter first string");
    private final TextArea input2 = new TextArea("Enter second string");
    private final Button compareButton = new Button("Compare");
    private final Div diffOutput = new Div();
    private final RadioButtonGroup<String> viewModeGroup = new RadioButtonGroup<>();
    private final ComboBox<String> matchingSelect = new ComboBox<>("Matching Level");
    private final ComboBox<String> diffStyleSelect = new ComboBox<>("Diff Style");
    private final ComboBox<String> colorSchemeSelect = new ComboBox<>("Color Scheme");
    private final Checkbox drawFileListCheckbox = new Checkbox("Show File List");
    private final IntegerField diffMaxChangesField = new IntegerField("Max Changes");
    private final IntegerField diffMaxLineLengthField = new IntegerField("Max Line Length");

    public DiffViewerEnhanced() {
        H1 title = new H1("Enhanced Diff Viewer");

        configureComponents();
        addRequiredResources();

        Details inputDetails1 = new Details("First Input", input1);
        input1.setWidthFull();
        inputDetails1.setOpened(true);
        inputDetails1.setWidthFull();
        Details inputDetails2 = new Details("Second Input", input2);
        input2.setWidthFull();
        inputDetails2.setOpened(true);
        inputDetails2.setWidthFull();
        HorizontalLayout compConfig = new HorizontalLayout(matchingSelect, diffMaxChangesField, diffMaxLineLengthField);
        compConfig.setWidthFull();
        compConfig.setAlignItems(Alignment.AUTO);


        HorizontalLayout styleConfig = new HorizontalLayout(diffStyleSelect, colorSchemeSelect,drawFileListCheckbox);
        styleConfig.setWidthFull();
        styleConfig.setAlignItems(Alignment.BASELINE);

        VerticalLayout configurationLayout = new VerticalLayout(compareButton, compConfig, styleConfig, viewModeGroup);
        configurationLayout.setPadding(false);
        configurationLayout.setSpacing(true);

        diffOutput.setId("diffOutput");
        diffOutput.getStyle()
                .set("border", "1px solid #ddd")
                .set("padding", "10px")
                .set("overflow", "auto")
                .set("height", "100%");

        SplitLayout splitLayout = new SplitLayout(new VerticalLayout(inputDetails1, inputDetails2, configurationLayout), diffOutput);
        splitLayout.setSizeFull();
        splitLayout.setSplitterPosition(40);

        configureListeners();
        setSpacing(true);
        setSizeFull();
        add(title, splitLayout);
    }

    private void configureComponents() {
        input1.setWidthFull();
        input2.setWidthFull();

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

    private void configureListeners() {
        compareButton.addClickListener(e -> {
            UI.getCurrent().access(() -> {
                String diffScript = generateDiffScript(input1.getValue(), input2.getValue());
                UI.getCurrent().getPage().executeJs(diffScript);
            });
        });
    }

    private void addRequiredResources() {
        UI.getCurrent().getPage().addStyleSheet("https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.8.0/styles/github.min.css");
        UI.getCurrent().getPage().addStyleSheet("https://cdn.jsdelivr.net/npm/diff2html/bundles/css/diff2html.min.css");
        UI.getCurrent().getPage().addJavaScript("https://cdn.jsdelivr.net/npm/diff/dist/diff.min.js");
        UI.getCurrent().getPage().addJavaScript("https://cdn.jsdelivr.net/npm/diff2html/bundles/js/diff2html-ui.min.js");
    }

    private String generateDiffScript(String text1, String text2) {
        return String.format(
                "const text1 = %s;" +
                        "const text2 = %s;" +
                        "const diff = Diff.createTwoFilesPatch('Original', 'Modified', text1, text2);" +
                        "const targetElement = document.getElementById('diffOutput');" +
                        "const configuration = {" +
                        "    drawFileList: %s," +
                        "    matching: '%s'," +
                        "    diffStyle: '%s'," +
                        "    colorScheme: '%s'," +
                        "    outputFormat: '%s'," +
                        "    highlight: true," +
                        "    synchronisedScroll: true," +
                        "    diffMaxChanges: %s," +
                        "    diffMaxLineLength: %s" +
                        "};" +
                        "const diff2htmlUi = new Diff2HtmlUI(targetElement, diff, configuration);" +
                        "diff2htmlUi.draw();" +
                        "diff2htmlUi.highlightCode();",
                toJsonString(text1), toJsonString(text2),
                drawFileListCheckbox.getValue(),
                matchingSelect.getValue(),
                diffStyleSelect.getValue(),
                colorSchemeSelect.getValue(),
                viewModeGroup.getValue().equals("Unified") ? "line-by-line" : "side-by-side",
                diffMaxChangesField.isEmpty() ? "undefined" : diffMaxChangesField.getValue(),
                diffMaxLineLengthField.isEmpty() ? "undefined" : diffMaxLineLengthField.getValue()
        );
    }

    private String toJsonString(String s) {
        return new Gson().toJson(s);
    }
}
