@Override
protected void onAttach(AttachEvent attachEvent) {
    super.onAttach(attachEvent);
    
    // First executeJs: Load Mermaid and render the diagram
    getElement().executeJs(
        "import('https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.esm.min.mjs')\n" +
        "  .then((mermaid) => {\n" +
        "    mermaid.default.initialize({ startOnLoad: false });\n" +
        "    const drawDiagram = async function () {\n" +
        "      const element = document.querySelector('#mermaid-diag');\n" +
        "      if (!element) return;\n" +
        "      const graphDefinition = element.getAttribute('graph-data');\n" +
        "      const { svg } = await mermaid.default.render('graphDiv', graphDefinition);\n" +
        "      element.innerHTML = svg;\n" +
        "      // Dispatch an event when the diagram is rendered\n" +
        "      element.dispatchEvent(new CustomEvent('mermaidRendered'));\n" +
        "    };\n" +
        "    drawDiagram();\n" +
        "    const observer = new MutationObserver(() => drawDiagram());\n" +
        "    observer.observe(document.querySelector('#mermaid-diag'), {\n" +
        "      attributes: true,\n" +
        "      attributeFilter: ['graph-data']\n" +
        "    });\n" +
        "  });"
    );
    
    // Second executeJs: Load svgPanZoom and apply it to the rendered diagram
    getElement().executeJs(
        "const script = document.createElement('script');\n" +
        "script.src = 'https://cdn.jsdelivr.net/npm/svg-pan-zoom@3.6.1/dist/svg-pan-zoom.min.js';\n" +
        "script.onload = function() {\n" +
        "  const element = document.querySelector('#mermaid-diag');\n" +
        "  if (!element) return;\n" +
        "  const applySvgPanZoom = () => {\n" +
        "    svgPanZoom('#mermaid-diag svg', {\n" +
        "      zoomEnabled: true,\n" +
        "      controlIconsEnabled: true,\n" +
        "      fit: true,\n" +
        "      center: true\n" +
        "    });\n" +
        "  };\n" +
        "  // If the diagram is already rendered, apply svgPanZoom immediately\n" +
        "  if (element.querySelector('svg')) {\n" +
        "    applySvgPanZoom();\n" +
        "  }\n" +
        "  // Otherwise, wait for the mermaidRendered event\n" +
        "  element.addEventListener('mermaidRendered', applySvgPanZoom);\n" +
        "};\n" +
        "document.head.appendChild(script);"
    );
}
