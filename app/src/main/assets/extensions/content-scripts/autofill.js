(function() {
    console.log("Auto-Scraper & Form Filler script loaded.");
    
    // Find all input fields on the screen
    const inputs = document.querySelectorAll('input[type="text"], input[type="email"], textarea');
    if (inputs.length > 0) {
        if (typeof PolkeJSBridge !== 'undefined') {
            PolkeJSBridge.postMessage(JSON.stringify({
                extensionId: "autofill_assistant",
                status: "detected_fields",
                count: inputs.length,
                message: "Ditemukan " + inputs.length + " input field pada halaman ini. Menunggu aksi pengisian."
            }));
        }
        
        // Add a subtle border glow to indicate autofill compatibility
        inputs.forEach(input => {
            input.style.boxShadow = '0 0 4px rgba(189, 0, 255, 0.4)';
            input.style.borderColor = '#BD00FF';
        });
    }
})();
