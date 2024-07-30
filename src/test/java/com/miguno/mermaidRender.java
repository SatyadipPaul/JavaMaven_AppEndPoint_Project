@Override
protected void onAttach(AttachEvent attachEvent) {
    super.onAttach(attachEvent);
    // Inject the script when the component is attached
    getElement().executeJs(
        "import('https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.esm.min.mjs')\n" +
        "  .then((mermaid) => {\n" +
        "    mermaid.default.initialize({ startOnLoad: false });\n" +
        "    const drawDiagram = async function () {\n" +
        "      const element = document.querySelector('#mermaid-diag');\n" +
        "      const graphDefinition = element.getAttribute('graph-data');\n" +
        "      const { svg } = await mermaid.default.render('graphDiv', graphDefinition);\n" +
        "      element.innerHTML = svg;\n" +
        "      // Initialize svgPanZoom after rendering the diagram\n" +
        "      const script = document.createElement('script');\n" +
        "      script.src = 'https://cdn.jsdelivr.net/npm/svg-pan-zoom@3.6.1/dist/svg-pan-zoom.min.js';\n" +
        "      script.onload = function() {\n" +
        "        svgPanZoom('#mermaid-diag svg', {\n" +
        "          zoomEnabled: true,\n" +
        "          controlIconsEnabled: true,\n" +
        "          fit: true,\n" +
        "          center: true\n" +
        "        });\n" +
        "      };\n" +
        "      document.head.appendChild(script);\n" +
        "    };\n" +
        "    drawDiagram();\n" +
        "    const observer = new MutationObserver(() => drawDiagram());\n" +
        "    observer.observe(document.querySelector('#mermaid-diag'), {\n" +
        "      attributes: true,\n" +
        "      attributeFilter: ['graph-data']\n" +
        "    });\n" +
        "  });"
    );
}
