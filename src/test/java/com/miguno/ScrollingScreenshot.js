(async function() {
  // Prompt the user for the XPath of the element
  const xpath = prompt("Enter the XPath of the element:");

  // Use XPath to find the element
  const element = document.evaluate(
    xpath,
    document,
    null,
    XPathResult.FIRST_ORDERED_NODE_TYPE,
    null
  ).singleNodeValue;

  if (!element) {
    console.error("No element found for the given XPath.");
    return;
  }

  // Scroll the element to its bottom
  element.scrollTop = element.scrollHeight;

  // Load html2canvas library
  const script = document.createElement('script');
  script.src = 'https://cdnjs.cloudflare.com/ajax/libs/html2canvas/1.4.1/html2canvas.min.js';
  document.head.appendChild(script);

  // Wait for the script to load
  await new Promise((resolve) => {
    script.onload = resolve;
  });

  // Use html2canvas to capture the element
  html2canvas(element, { useCORS: true }).then(function(canvas) {
    // Convert the canvas to a data URL
    const dataURL = canvas.toDataURL("image/png");

    // Create a link to download the image
    const link = document.createElement('a');
    link.href = dataURL;
    link.download = 'element_screenshot.png';

    // Append the link to the body and trigger a click
    document.body.appendChild(link);
    link.click();

    // Clean up
    document.body.removeChild(link);
    console.log("Screenshot saved as 'element_screenshot.png'.");
  }).catch(function(error) {
    console.error("An error occurred while capturing the element:", error);
  });
})();
