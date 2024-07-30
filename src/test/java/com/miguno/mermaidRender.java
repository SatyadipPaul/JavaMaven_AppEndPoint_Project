@Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        // Load Mermaid and render the diagram
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

        // Listen for the mermaidRendered event and initialize svg-pan-zoom
        getElement().executeJs(
            "document.querySelector('#mermaid-diag').addEventListener('mermaidRendered', function () {\n" +
            "  import('https://cdn.jsdelivr.net/npm/svg-pan-zoom@3.6.1/dist/svg-pan-zoom.min.js').then((module) => {\n" +
            "    const svgPanZoom = module.default;\n" +
            "    svgPanZoom('#graphDiv', {\n" +
            "      zoomEnabled: true,\n" +
            "      controlIconsEnabled: true,\n" +
            "      fit: true,\n" +
            "      center: true\n" +
            "    });\n" +
            "  });\n" +
            "});"
        );
    }
