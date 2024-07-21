App End Point Project Source Code
<!DOCTYPE html>
<html lang="en">
  <head>
    <!-- Include Font Awesome CSS -->
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0-beta3/css/all.min.css">
  </head>
  <body>
    <pre class="mermaid">
      graph LR
        A --- B
        B-->C[fa:fa-ban forbidden]
        B-->D(fa:fa-spinner)
    </pre>
    <script type="module">
      import mermaid from 'https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.esm.min.mjs';
      mermaid.initialize({ startOnLoad: true });
    </script>
  </body>
</html>

 @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        // Inject the script when the component is attached
        getElement().executeJs(
            "import mermaid from 'https://cdn.jsdelivr.net/npm/mermaid/dist/mermaid.esm.min.mjs';" +
            "mermaid.initialize({ startOnLoad: false });" +
            "const drawDiagram = async function () {" +
            "    const element = document.querySelector('#graphDiv');" +
            "    const graphDefinition = element.getAttribute('data-graph');" +
            "    const { svg } = await mermaid.render('graphDiv', graphDefinition);" +
            "    element.innerHTML = svg;" +
            "};" +
            "const observer = new MutationObserver(() => drawDiagram());" +
            "observer.observe(document.querySelector('#graphDiv'), { attributes: true, attributeFilter: ['data-graph'] });" +
            "drawDiagram();"
        );
    }
