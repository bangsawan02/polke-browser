(function() {
    console.log("Highlighter Extension loaded successfully.");
    
    // Notify our Native Engine via safe JS-Kotlin communication bridge
    if (typeof PolkeJSBridge !== 'undefined') {
        PolkeJSBridge.postMessage(JSON.stringify({
            extensionId: "page_highlighter",
            status: "activated",
            message: "Halaman disorot menggunakan CSS Cyberpunk!"
        }));
    }
    
    // Neon glow highlight styled decoration on content headings
    const headings = document.querySelectorAll('h1, h2, h3');
    headings.forEach(el => {
        el.style.borderLeft = '4px solid #00E5FF';
        el.style.paddingLeft = '10px';
        el.style.textShadow = '0 0 8px rgba(0, 229, 255, 0.4)';
    });
})();
