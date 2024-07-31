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

        // Include the svg-pan-zoom library and listen for the mermaidRendered event
        mermaidContainer.getElement().executeJs(
            """
            const script = document.createElement('script');
            script.src = 'https://cdn.jsdelivr.net/npm/svg-pan-zoom@3.6.1/dist/svg-pan-zoom.min.js';
            script.onload = function() {
              document.querySelector('#mermaid-diag').addEventListener('mermaidRendered', function () {
                setTimeout(() => {
                  svgPanZoom('#mermaid-diag svg', {
                    zoomEnabled: true,
                    controlIconsEnabled: true,
                    fit: true,
                    center: true
                  });
                }, 100); // Add a slight delay
              });
            };
            document.head.appendChild(script);
        """
        );
    }
