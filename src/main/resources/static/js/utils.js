/**
 * Smart Code Review - Utility Functions
 */

// Utility functions that might be used across the application
const Utils = {
    // Format file size
    formatFileSize: function(bytes) {
        if (bytes < 1024) return bytes + ' B';
        else if (bytes < 1048576) return Math.round(bytes / 1024) + ' KB';
        else return Math.round(bytes / 1048576) + ' MB';
    },
    
    // Validate email
    validateEmail: function(email) {
        const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return re.test(email);
    },
    
    // Escape HTML
    escapeHtml: function(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
};

// Export if needed
if (typeof module !== 'undefined' && module.exports) {
    module.exports = Utils;
}