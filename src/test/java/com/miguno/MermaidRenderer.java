public class MermaidDiagramView extends Div {

    private final Div mermaidContainer;

    public MermaidDiagramView() {
        setSizeFull();
        addClassName("mermaid-view");

        mermaidContainer = new Div();
        mermaidContainer.setId("mermaid-diag");
        mermaidContainer.addClassName("mermaid");
        mermaidContainer.getStyle().set("width", "100%").set("height", "100%")
            .set("min-height", "500px")  // Ensure a minimum height
            .set("border", "1px solid red");  // Debug border
        add(mermaidContainer);

        initializeMermaid();
        initializeSvgPanZoom();
    }

    private void initializeMermaid() {
        mermaidContainer.getElement().executeJs(
            "import('https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.esm.min.mjs')" +
            ".then((mermaid) => {" +
            "  mermaid.default.initialize({ startOnLoad: false });" +
            "  const drawDiagram = async function () {" +
            "    const element = document.querySelector('#mermaid-diag');" +
            "    if (!element) return;" +
            "    const graphDefinition = element.getAttribute('graph-data');" +
            "    const { svg } = await mermaid.default.render('graphDiv', graphDefinition);" +
            "    element.innerHTML = svg;" +
            "    const svgElement = element.querySelector('svg');" +
            "    if (svgElement) {" +
            "      svgElement.style.width = '100%';" +
            "      svgElement.style.height = '100%';" +
            "      svgElement.style.minHeight = '500px';" +
            "    }" +
            "    element.dispatchEvent(new CustomEvent('mermaidRendered'));" +
            "  };" +
            "  drawDiagram();" +
            "  new MutationObserver(() => drawDiagram()).observe(" +
            "    document.querySelector('#mermaid-diag')," +
            "    { attributes: true, attributeFilter: ['graph-data'] }" +
            "  );" +
            "});"
        );
    }

    private void initializeSvgPanZoom() {
        mermaidContainer.getElement().executeJs(
            "const script = document.createElement('script');" +
            "script.src = 'https://cdn.jsdelivr.net/npm/svg-pan-zoom@3.6.1/dist/svg-pan-zoom.min.js';" +
            "script.onload = function() {" +
            "  document.querySelector('#mermaid-diag').addEventListener('mermaidRendered', function () {" +
            "    setTimeout(() => {" +
            "      const svgElement = document.querySelector('#mermaid-diag svg');" +
            "      if (svgElement) {" +
            "        svgPanZoom(svgElement, {" +
            "          zoomEnabled: true," +
            "          controlIconsEnabled: true," +
            "          fit: true," +
            "          center: true," +
            "          minZoom: 0.1," +
            "          maxZoom: 10" +
            "        });" +
            "      } else {" +
            "        console.error('SVG element not found for svg-pan-zoom initialization');" +
            "      }" +
            "    }, 100);" +
            "  });" +
            "};" +
            "document.head.appendChild(script);"
        );
    }

    public void setMermaidFlow(String mermaidFlow) {
        mermaidContainer.getElement().setAttribute("graph-data", mermaidFlow);
    }
}
