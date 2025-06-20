/**
 * Smart Code Review - Main Application JavaScript
 * Handles core functionality, session management, and UI interactions
 */

class SmartCodeReviewApp {
    constructor() {
        this.API_BASE = window.location.origin.replace(':8083', ':8083'); // Ensure port 8083
        this.sessionData = null;
        this.currentAnalysis = null;
        this.timerInterval = null;
        
        this.init();
    }
    
    /**
     * Initialize the application
     */
    init() {
        this.bindEvents();
        this.checkExistingSession();
        this.initializeComponents();
        
        // Track page load
        this.trackEvent('page_view', {
            page_title: document.title,
            page_location: window.location.href
        });
    }
    
    /**
     * Bind event listeners
     */
    bindEvents() {
        // Global error handling
        window.addEventListener('error', (event) => {
            console.error('Global error:', event.error);
            this.showToast('An unexpected error occurred', 'error');
        });
        
        // Handle unhandled promise rejections
        window.addEventListener('unhandledrejection', (event) => {
            console.error('Unhandled promise rejection:', event.reason);
            this.showToast('A network error occurred', 'error');
        });
        
        // Form submissions
        document.addEventListener('submit', (event) => {
            const form = event.target;
            if (form.id === 'email-form') {
                event.preventDefault();
                this.handleEmailSubmission(form);
            } else if (form.id === 'otp-form') {
                event.preventDefault();
                this.handleOtpSubmission(form);
            } else if (form.id === 'code-analysis-form') {
                event.preventDefault();
                this.handleCodeAnalysis(form);
            }
        });
        
        // File upload handling
        document.addEventListener('change', (event) => {
            if (event.target.type === 'file' && event.target.name === 'codeFile') {
                this.handleFileSelection(event.target);
            }
        });
        
        // Drag and drop
        document.addEventListener('dragover', (event) => {
            event.preventDefault();
            const dropZone = event.target.closest('.file-upload-area');
            if (dropZone) {
                dropZone.classList.add('dragover');
            }
        });
        
        document.addEventListener('dragleave', (event) => {
            const dropZone = event.target.closest('.file-upload-area');
            if (dropZone && !dropZone.contains(event.relatedTarget)) {
                dropZone.classList.remove('dragover');
            }
        });
        
        document.addEventListener('drop', (event) => {
            event.preventDefault();
            const dropZone = event.target.closest('.file-upload-area');
            if (dropZone) {
                dropZone.classList.remove('dragover');
                this.handleFileDrop(event.dataTransfer.files);
            }
        });
        
        // Keyboard shortcuts
        document.addEventListener('keydown', (event) => {
            // Escape key closes modals
            if (event.key === 'Escape') {
                this.closeAllModals();
            }
            
            // Ctrl+Enter submits forms
            if (event.ctrlKey && event.key === 'Enter') {
                const activeForm = document.querySelector('form:focus-within');
                if (activeForm) {
                    activeForm.dispatchEvent(new Event('submit'));
                }
            }
        });
        
        // Tab switching
        document.addEventListener('click', (event) => {
            const tabButton = event.target.closest('[data-tab]');
            if (tabButton) {
                event.preventDefault();
                this.switchTab(tabButton.dataset.tab);
            }
        });
        
        // Modal management
        document.addEventListener('click', (event) => {
            const modal = event.target.closest('.modal-overlay');
            if (modal) {
                this.closeAllModals();
            }
        });
    }
    
    /**
     * Initialize UI components
     */
    initializeComponents() {
        // Initialize tooltips
        this.initializeTooltips();
        
        // Initialize code syntax highlighting
        this.initializeCodeHighlighting();
        
        // Initialize progress tracking
        this.initializeProgressTracking();
        
        // Check service status
        this.checkServiceStatus();
    }
    
    /**
     * Check for existing session in localStorage
     */
    checkExistingSession() {
        try {
            const storedSession = localStorage.getItem('smartcode_session');
            if (storedSession) {
                const sessionData = JSON.parse(storedSession);
                
                // Verify session is still valid
                if (sessionData.expiresAt > Date.now()) {
                    this.sessionData = sessionData;
                    this.updateSessionUI();
                    this.startSessionTimer();
                } else {
                    localStorage.removeItem('smartcode_session');
                }
            }
        } catch (error) {
            console.error('Error checking existing session:', error);
            localStorage.removeItem('smartcode_session');
        }
    }
    
    /**
     * Handle email form submission for session creation
     */
    async handleEmailSubmission(form) {
        const formData = new FormData(form);
        const email = formData.get('email');
        const name = formData.get('name');
        
        if (!this.validateEmail(email)) {
            this.showToast('Please enter a valid email address', 'error');
            return;
        }
        
        this.showLoading('Sending verification code...');
        
        try {
            const response = await fetch(`${this.API_BASE}/api/v1/code-review/session/create`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ email, name })
            });
            
            const data = await response.json();
            
            if (data.success) {
                this.showOtpForm(data.sessionId, email);
                this.trackEvent('otp_sent', {
                    event_category: 'engagement',
                    event_label: 'session_creation'
                });
                this.showToast('Verification code sent to your email', 'success');
            } else {
                throw new Error(data.message || 'Failed to create session');
            }
        } catch (error) {
            console.error('Error creating session:', error);
            this.showToast(error.message || 'Failed to send verification code', 'error');
        } finally {
            this.hideLoading();
        }
    }
    
    /**
     * Handle OTP form submission for session verification
     */
    async handleOtpSubmission(form) {
        const formData = new FormData(form);
        const otp = formData.get('otp');
        const sessionId = formData.get('sessionId');
        
        if (!otp || otp.length !== 6) {
            this.showToast('Please enter a valid 6-digit code', 'error');
            return;
        }
        
        this.showLoading('Verifying code...');
        
        try {
            const response = await fetch(`${this.API_BASE}/api/v1/code-review/session/verify`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ sessionId, otp })
            });
            
            const data = await response.json();
            
            if (data.success) {
                this.sessionData = data;
                this.saveSession(data);
                this.updateSessionUI();
                this.startSessionTimer();
                this.closeAllModals();
                
                this.trackEvent('session_verified', {
                    event_category: 'engagement',
                    event_label: 'otp_verification'
                });
                
                this.showToast('Session verified! You can now analyze your code.', 'success');
            } else {
                throw new Error(data.message || 'Invalid verification code');
            }
        } catch (error) {
            console.error('Error verifying OTP:', error);
            this.showToast(error.message || 'Failed to verify code', 'error');
        } finally {
            this.hideLoading();
        }
    }
    
    /**
     * Handle code analysis submission
     */
    async handleCodeAnalysis(form) {
        if (!this.sessionData) {
            this.showToast('Please create a session first', 'error');
            return;
        }
        
        const formData = new FormData(form);
        const code = formData.get('code');
        const language = formData.get('language');
        
        if (!code || code.trim().length === 0) {
            this.showToast('Please enter some code to analyze', 'error');
            return;
        }
        
        if (code.length > 100000) {
            this.showToast('Code is too large (max 100KB)', 'error');
            return;
        }
        
        this.showLoading('Analyzing your code...');
        this.trackEvent('code_analysis_started', {
            event_category: 'analysis',
            event_label: 'code_paste',
            custom_parameters: {
                language: language,
                code_length: code.length
            }
        });
        
        try {
            const response = await fetch(`${this.API_BASE}/api/v1/code-review/analyze/code`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    code,
                    language,
                    sessionToken: this.sessionData.sessionToken
                })
            });
            
            const data = await response.json();
            
            if (data.success) {
                this.currentAnalysis = data;
                this.showAnalysisProgress(data.analysisId);
                this.switchTab('results');
            } else {
                throw new Error(data.message || 'Failed to start analysis');
            }
        } catch (error) {
            console.error('Error analyzing code:', error);
            this.showToast(error.message || 'Failed to analyze code', 'error');
            this.hideLoading();
        }
    }
    
    /**
     * Handle file upload analysis
     */
    async handleFileUpload(file) {
        if (!this.sessionData) {
            this.showToast('Please create a session first', 'error');
            return;
        }
        
        if (file.size > 50 * 1024 * 1024) { // 50MB limit
            this.showToast('File is too large (max 50MB)', 'error');
            return;
        }
        
        const allowedTypes = ['.zip', '.tar.gz', '.rar'];
        const fileExtension = file.name.toLowerCase().substring(file.name.lastIndexOf('.'));
        
        if (!allowedTypes.includes(fileExtension)) {
            this.showToast('Please upload a ZIP, TAR.GZ, or RAR file', 'error');
            return;
        }
        
        this.showLoading('Uploading and analyzing file...');
        this.trackEvent('file_analysis_started', {
            event_category: 'analysis',
            event_label: 'file_upload',
            custom_parameters: {
                file_size: file.size,
                file_type: fileExtension
            }
        });
        
        try {
            const formData = new FormData();
            formData.append('file', file);
            formData.append('sessionToken', this.sessionData.sessionToken);
            
            const response = await fetch(`${this.API_BASE}/api/v1/code-review/analyze/zip`, {
                method: 'POST',
                body: formData
            });
            
            const data = await response.json();
            
            if (data.success) {
                this.currentAnalysis = data;
                this.showAnalysisProgress(data.analysisId);
                this.switchTab('results');
            } else {
                throw new Error(data.message || 'Failed to start analysis');
            }
        } catch (error) {
            console.error('Error uploading file:', error);
            this.showToast(error.message || 'Failed to upload file', 'error');
            this.hideLoading();
        }
    }
    
    /**
     * Show analysis progress and poll for results
     */
    async showAnalysisProgress(analysisId) {
        const progressContainer = document.getElementById('analysis-progress');
        const progressBar = document.getElementById('progress-bar');
        const progressText = document.getElementById('progress-text');
        
        if (progressContainer) {
            progressContainer.classList.remove('hidden');
        }
        
        const pollInterval = setInterval(async () => {
            try {
                const response = await fetch(
                    `${this.API_BASE}/api/v1/code-review/analysis/${analysisId}?sessionToken=${this.sessionData.sessionToken}`
                );
                
                const data = await response.json();
                
                if (data.success) {
                    const progress = data.progressPercentage || 0;
                    
                    if (progressBar) {
                        progressBar.style.width = `${progress}%`;
                    }
                    
                    if (progressText) {
                        progressText.textContent = data.message || 'Processing...';
                    }
                    
                    if (data.status === 'COMPLETED') {
                        clearInterval(pollInterval);
                        this.hideLoading();
                        this.displayAnalysisResults(data);
                        
                        this.trackEvent('analysis_completed', {
                            event_category: 'analysis',
                            event_label: 'success',
                            custom_parameters: {
                                analysis_id: analysisId,
                                overall_score: data.result?.overallScore || 0
                            }
                        });
                    } else if (data.status === 'FAILED') {
                        clearInterval(pollInterval);
                        this.hideLoading();
                        this.showToast('Analysis failed: ' + (data.message || 'Unknown error'), 'error');
                        
                        this.trackEvent('analysis_failed', {
                            event_category: 'analysis',
                            event_label: 'error'
                        });
                    }
                } else {
                    throw new Error('Failed to get analysis status');
                }
            } catch (error) {
                console.error('Error polling analysis:', error);
                clearInterval(pollInterval);
                this.hideLoading();
                this.showToast('Error getting analysis results', 'error');
            }
        }, 2000); // Poll every 2 seconds
        
        // Stop polling after 5 minutes
        setTimeout(() => {
            clearInterval(pollInterval);
            this.hideLoading();
        }, 300000);
    }
    
    /**
     * Display analysis results in the UI
     */
    displayAnalysisResults(analysisData) {
        const resultsContainer = document.getElementById('results-container');
        if (!resultsContainer) return;
        
        const result = analysisData.result;
        if (!result) {
            this.showToast('No analysis results available', 'error');
            return;
        }
        
        resultsContainer.innerHTML = this.generateResultsHTML(result);
        
        // Initialize interactive elements in results
        this.initializeResultsInteractivity();
        
        // Scroll to results
        resultsContainer.scrollIntoView({ behavior: 'smooth' });
    }
    
    /**
     * Generate HTML for analysis results
     */
    generateResultsHTML(result) {
        const scoreClass = this.getScoreClass(result.overallScore);
        
        return `
            <div class="analysis-results">
                <!-- Overall Score -->
                <div class="analysis-card">
                    <div class="analysis-header">
                        <div class="score-circle ${scoreClass}">
                            ${result.overallScore.toFixed(1)}
                        </div>
                        <div>
                            <h3>Overall Code Quality</h3>
                            <p class="text-gray-600">${result.summary || 'Analysis completed successfully'}</p>
                        </div>
                    </div>
                </div>
                
                <!-- Metrics Grid -->
                <div class="grid md:grid-cols-3 gap-6 mb-8">
                    <div class="analysis-card">
                        <h4 class="font-semibold text-red-600 mb-2">🔒 Security</h4>
                        <div class="text-2xl font-bold">${result.security?.securityScore?.toFixed(1) || 'N/A'}</div>
                        <p class="text-sm text-gray-600">${result.security?.hasSecurityIssues ? 'Issues found' : 'No issues detected'}</p>
                    </div>
                    <div class="analysis-card">
                        <h4 class="font-semibold text-blue-600 mb-2">⚡ Performance</h4>
                        <div class="text-2xl font-bold">${result.performance?.performanceScore?.toFixed(1) || 'N/A'}</div>
                        <p class="text-sm text-gray-600">${result.performance?.complexity || 'Unknown'} complexity</p>
                    </div>
                    <div class="analysis-card">
                        <h4 class="font-semibold text-green-600 mb-2">📊 Quality</h4>
                        <div class="text-2xl font-bold">${result.quality?.maintainabilityScore?.toFixed(1) || 'N/A'}</div>
                        <p class="text-sm text-gray-600">${result.quality?.linesOfCode || 0} lines of code</p>
                    </div>
                </div>
                
                <!-- Issues Section -->
                ${result.issues && result.issues.length > 0 ? `
                    <div class="analysis-card">
                        <h3 class="text-xl font-bold mb-4">🔍 Issues Found (${result.issues.length})</h3>
                        <div class="issue-list">
                            ${result.issues.map(issue => this.generateIssueHTML(issue)).join('')}
                        </div>
                    </div>
                ` : ''}
                
                <!-- Suggestions Section -->
                ${result.suggestions && result.suggestions.length > 0 ? `
                    <div class="analysis-card">
                        <h3 class="text-xl font-bold mb-4">💡 Recommendations (${result.suggestions.length})</h3>
                        <div class="space-y-4">
                            ${result.suggestions.map(suggestion => this.generateSuggestionHTML(suggestion)).join('')}
                        </div>
                    </div>
                ` : ''}
                
                <!-- Security Details -->
                ${result.security?.vulnerabilities && result.security.vulnerabilities.length > 0 ? `
                    <div class="analysis-card">
                        <h3 class="text-xl font-bold mb-4">🛡️ Security Analysis</h3>
                        <div class="space-y-2">
                            ${result.security.vulnerabilities.map(vuln => `
                                <div class="p-3 bg-red-50 border border-red-200 rounded">
                                    <div class="font-medium text-red-800">${vuln}</div>
                                </div>
                            `).join('')}
                        </div>
                        ${result.security.recommendations && result.security.recommendations.length > 0 ? `
                            <h4 class="font-semibold mt-4 mb-2">Recommendations:</h4>
                            <ul class="list-disc list-inside space-y-1">
                                ${result.security.recommendations.map(rec => `<li class="text-gray-700">${rec}</li>`).join('')}
                            </ul>
                        ` : ''}
                    </div>
                ` : ''}
                
                <!-- Performance Details -->
                ${result.performance?.bottlenecks && result.performance.bottlenecks.length > 0 ? `
                    <div class="analysis-card">
                        <h3 class="text-xl font-bold mb-4">🚀 Performance Analysis</h3>
                        <div class="space-y-2">
                            ${result.performance.bottlenecks.map(bottleneck => `
                                <div class="p-3 bg-yellow-50 border border-yellow-200 rounded">
                                    <div class="font-medium text-yellow-800">${bottleneck}</div>
                                </div>
                            `).join('')}
                        </div>
                        ${result.performance.optimizations && result.performance.optimizations.length > 0 ? `
                            <h4 class="font-semibold mt-4 mb-2">Optimizations:</h4>
                            <ul class="list-disc list-inside space-y-1">
                                ${result.performance.optimizations.map(opt => `<li class="text-gray-700">${opt}</li>`).join('')}
                            </ul>
                        ` : ''}
                    </div>
                ` : ''}
                
                <!-- Action Buttons -->
                <div class="flex flex-col sm:flex-row gap-4 mt-8">
                    <button onclick="app.downloadReport()" class="btn btn-primary">
                        📄 Download Report
                    </button>
                    <button onclick="app.analyzeNewCode()" class="btn btn-secondary">
                        🔄 Analyze New Code
                    </button>
                    <button onclick="window.print()" class="btn btn-secondary">
                        🖨️ Print Results
                    </button>
                </div>
            </div>
        `;
    }
    
    /**
     * Generate HTML for individual issues
     */
    generateIssueHTML(issue) {
        const severityClass = `issue-${issue.severity?.toLowerCase() || 'medium'}`;
        const severityIcon = {
            'CRITICAL': '🔴',
            'HIGH': '🟠',
            'MEDIUM': '🟡',
            'LOW': '🟢'
        }[issue.severity] || '🟡';
        
        return `
            <div class="issue-item ${severityClass}">
                <div class="flex items-start justify-between">
                    <div class="flex-1">
                        <div class="issue-title">
                            ${severityIcon} ${issue.title || 'Code Issue'}
                        </div>
                        <div class="issue-description">
                            ${issue.description || 'No description available'}
                        </div>
                        ${issue.fileName ? `
                            <div class="text-sm text-gray-500 mt-1">
                                📁 ${issue.fileName}${issue.lineNumber ? ` (Line ${issue.lineNumber})` : ''}
                            </div>
                        ` : ''}
                        ${issue.codeSnippet ? `
                            <pre class="bg-gray-100 p-2 rounded mt-2 text-sm overflow-x-auto"><code>${this.escapeHtml(issue.codeSnippet)}</code></pre>
                        ` : ''}
                        ${issue.suggestion ? `
                            <div class="issue-suggestion">
                                <strong>💡 Suggestion:</strong> ${issue.suggestion}
                            </div>
                        ` : ''}
                    </div>
                </div>
            </div>
        `;
    }
    
    /**
     * Generate HTML for suggestions
     */
    generateSuggestionHTML(suggestion) {
        return `
            <div class="p-4 bg-blue-50 border border-blue-200 rounded-lg">
                <h4 class="font-semibold text-blue-800 mb-2">${suggestion.title || 'Suggestion'}</h4>
                <p class="text-blue-700 mb-2">${suggestion.description || 'No description available'}</p>
                ${suggestion.category ? `
                    <span class="inline-block px-2 py-1 bg-blue-100 text-blue-700 text-sm rounded">
                        ${suggestion.category}
                    </span>
                ` : ''}
                ${suggestion.impact ? `
                    <span class="inline-block px-2 py-1 bg-gray-100 text-gray-700 text-sm rounded ml-2">
                        Impact: ${suggestion.impact}
                    </span>
                ` : ''}
                ${suggestion.implementation ? `
                    <div class="mt-3 p-3 bg-white border border-blue-200 rounded">
                        <strong>Implementation:</strong> ${suggestion.implementation}
                    </div>
                ` : ''}
            </div>
        `;
    }
    
    /**
     * Utility functions
     */
    getScoreClass(score) {
        if (score >= 8) return 'score-excellent';
        if (score >= 6) return 'score-good';
        if (score >= 4) return 'score-fair';
        return 'score-poor';
    }
    
    validateEmail(email) {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
    }
    
    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
    
    /**
     * Event tracking
     */
    trackEvent(eventName, parameters = {}) {
        if (typeof gtag !== 'undefined') {
            gtag('event', eventName, parameters);
        }
    }
    
    /**
     * Session management
     */
    saveSession(sessionData) {
        try {
            localStorage.setItem('smartcode_session', JSON.stringify(sessionData));
        } catch (error) {
            console.error('Error saving session:', error);
        }
    }
    
    clearSession() {
        this.sessionData = null;
        this.currentAnalysis = null;
        if (this.timerInterval) {
            clearInterval(this.timerInterval);
            this.timerInterval = null;
        }
        try {
            localStorage.removeItem('smartcode_session');
        } catch (error) {
            console.error('Error clearing session:', error);
        }
        this.updateSessionUI();
    }
    
    updateSessionUI() {
        const timerElement = document.getElementById('session-timer');
        const timerDisplay = document.getElementById('timer-display');
        
        if (this.sessionData && timerElement) {
            timerElement.classList.remove('hidden');
            this.updateTimerDisplay();
        } else if (timerElement) {
            timerElement.classList.add('hidden');
        }
        
        // Show/hide session-dependent elements
        const sessionElements = document.querySelectorAll('[data-requires-session]');
        sessionElements.forEach(element => {
            if (this.sessionData) {
                element.classList.remove('hidden');
            } else {
                element.classList.add('hidden');
            }
        });
        
        // Update create session buttons
        const createSessionBtns = document.querySelectorAll('[data-action="create-session"]');
        createSessionBtns.forEach(btn => {
            if (this.sessionData) {
                btn.style.display = 'none';
            } else {
                btn.style.display = '';
            }
        });
    }
    
    startSessionTimer() {
        if (this.timerInterval) {
            clearInterval(this.timerInterval);
        }
        
        this.timerInterval = setInterval(() => {
            this.updateTimerDisplay();
        }, 1000);
    }
    
    updateTimerDisplay() {
        if (!this.sessionData) return;
        
        const now = Date.now();
        const expiresAt = this.sessionData.expiresAt;
        const remainingMs = expiresAt - now;
        
        if (remainingMs <= 0) {
            this.handleSessionExpired();
            return;
        }
        
        const remainingMinutes = Math.floor(remainingMs / 60000);
        const remainingSeconds = Math.floor((remainingMs % 60000) / 1000);
        
        const timerDisplay = document.getElementById('timer-display');
        if (timerDisplay) {
            timerDisplay.textContent = `${remainingMinutes}:${remainingSeconds.toString().padStart(2, '0')}`;
            
            // Change color when time is running low
            const timerElement = document.getElementById('session-timer');
            if (remainingMinutes < 2) {
                timerElement?.classList.add('text-red-600', 'bg-red-50');
                timerElement?.classList.remove('text-blue-600', 'bg-blue-50');
            }
        }
    }
    
    handleSessionExpired() {
        this.clearSession();
        this.showToast('Your session has expired. Please create a new session to continue.', 'warning');
        
        this.trackEvent('session_expired', {
            event_category: 'session',
            event_label: 'timeout'
        });
    }
    
    /**
     * UI Helper Methods
     */
    showLoading(message = 'Loading...') {
        const overlay = document.getElementById('loading-overlay');
        const loadingText = document.querySelector('.loading-text');
        
        if (overlay) {
            overlay.classList.remove('hidden');
            if (loadingText) {
                loadingText.textContent = message;
            }
        }
    }
    
    hideLoading() {
        const overlay = document.getElementById('loading-overlay');
        if (overlay) {
            overlay.classList.add('hidden');
        }
    }
    
    showToast(message, type = 'info', duration = 5000) {
        const container = document.getElementById('toast-container');
        if (!container) return;
        
        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;
        
        const icon = {
            success: '✅',
            error: '❌',
            warning: '⚠️',
            info: 'ℹ️'
        }[type] || 'ℹ️';
        
        toast.innerHTML = `
            <div class="flex items-center">
                <span class="text-lg mr-2">${icon}</span>
                <span class="flex-1">${message}</span>
                <button class="toast-close ml-2" onclick="this.parentElement.parentElement.remove()">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
                    </svg>
                </button>
            </div>
        `;
        
        container.appendChild(toast);
        
        // Auto-remove after duration
        setTimeout(() => {
            if (toast.parentElement) {
                toast.remove();
            }
        }, duration);
        
        // Track toast events for errors
        if (type === 'error') {
            this.trackEvent('error_shown', {
                event_category: 'error',
                event_label: message
            });
        }
    }
    
    switchTab(tabName) {
        // Update tab buttons
        const tabButtons = document.querySelectorAll('[data-tab]');
        tabButtons.forEach(btn => {
            if (btn.dataset.tab === tabName) {
                btn.classList.add('active');
            } else {
                btn.classList.remove('active');
            }
        });
        
        // Update tab content
        const tabContents = document.querySelectorAll('[data-tab-content]');
        tabContents.forEach(content => {
            if (content.dataset.tabContent === tabName) {
                content.classList.remove('hidden');
            } else {
                content.classList.add('hidden');
            }
        });
        
        // Track tab switching
        this.trackEvent('tab_switched', {
            event_category: 'navigation',
            event_label: tabName
        });
    }
    
    showOtpForm(sessionId, email) {
        const formContainer = document.getElementById('session-form-container');
        if (!formContainer) return;
        
        formContainer.innerHTML = `
            <div class="text-center mb-6">
                <div class="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
                    <svg class="w-8 h-8 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"></path>
                    </svg>
                </div>
                <h3 class="text-lg font-semibold mb-2">Check Your Email</h3>
                <p class="text-gray-600">We sent a 6-digit verification code to <strong>${email}</strong></p>
            </div>
            
            <form id="otp-form" class="space-y-4">
                <input type="hidden" name="sessionId" value="${sessionId}">
                <div>
                    <label for="otp" class="block text-sm font-medium text-gray-700 mb-2">Verification Code</label>
                    <input type="text" id="otp" name="otp" required 
                           maxlength="6" pattern="[0-9]{6}" 
                           class="w-full px-4 py-3 text-center text-2xl tracking-widest border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                           placeholder="000000"
                           autocomplete="one-time-code">
                </div>
                <button type="submit" class="w-full bg-blue-600 text-white py-3 rounded-lg font-medium hover:bg-blue-700 transition-colors">
                    Verify Code
                </button>
                <div class="text-center">
                    <button type="button" onclick="app.resendOtp('${sessionId}')" class="text-blue-600 hover:text-blue-700 text-sm">
                        Didn't receive the code? Resend
                    </button>
                </div>
            </form>
        `;
        
        // Focus on OTP input
        setTimeout(() => {
            const otpInput = document.getElementById('otp');
            if (otpInput) {
                otpInput.focus();
            }
        }, 100);
    }
    
    async resendOtp(sessionId) {
        this.showLoading('Resending code...');
        
        try {
            // Note: This would need to be implemented in the backend
            this.showToast('Verification code resent to your email', 'success');
            
            this.trackEvent('otp_resent', {
                event_category: 'engagement',
                event_label: 'resend_request'
            });
        } catch (error) {
            console.error('Error resending OTP:', error);
            this.showToast('Failed to resend code. Please try again.', 'error');
        } finally {
            this.hideLoading();
        }
    }
    
    closeAllModals() {
        const modals = document.querySelectorAll('.session-modal, .modal');
        modals.forEach(modal => {
            modal.classList.add('hidden');
        });
    }
    
    /**
     * File handling methods
     */
    handleFileSelection(input) {
        const files = input.files;
        if (files && files.length > 0) {
            this.handleFileUpload(files[0]);
        }
    }
    
    handleFileDrop(files) {
        if (files && files.length > 0) {
            this.handleFileUpload(files[0]);
        }
    }
    
    /**
     * Analysis result actions
     */
    downloadReport() {
        if (!this.currentAnalysis || !this.currentAnalysis.result) {
            this.showToast('No analysis results to download', 'error');
            return;
        }
        
        const report = this.generateReportData(this.currentAnalysis.result);
        const blob = new Blob([JSON.stringify(report, null, 2)], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        
        const a = document.createElement('a');
        a.href = url;
        a.download = `code-analysis-report-${new Date().toISOString().split('T')[0]}.json`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
        
        this.trackEvent('report_downloaded', {
            event_category: 'analysis',
            event_label: 'json_report'
        });
    }
    
    generateReportData(result) {
        return {
            generatedAt: new Date().toISOString(),
            service: 'Smart Code Review',
            version: '1.0.0',
            analysis: {
                overallScore: result.overallScore,
                summary: result.summary,
                security: result.security,
                performance: result.performance,
                quality: result.quality,
                issues: result.issues,
                suggestions: result.suggestions
            }
        };
    }
    
    analyzeNewCode() {
        this.currentAnalysis = null;
        this.switchTab('upload');
        
        // Clear any existing form data
        const forms = document.querySelectorAll('form');
        forms.forEach(form => {
            if (form.id !== 'otp-form' && form.id !== 'email-form') {
                form.reset();
            }
        });
        
        this.trackEvent('analyze_new_code', {
            event_category: 'engagement',
            event_label: 'restart_analysis'
        });
    }
    
    /**
     * Service status checking
     */
    async checkServiceStatus() {
        try {
            const response = await fetch(`${this.API_BASE}/api/v1/code-review/health`);
            const data = await response.json();
            
            if (data.status === 'UP') {
                this.updateServiceStatus(true);
            } else {
                this.updateServiceStatus(false);
            }
        } catch (error) {
            console.error('Error checking service status:', error);
            this.updateServiceStatus(false);
        }
    }
    
    updateServiceStatus(isOnline) {
        const statusElements = document.querySelectorAll('.service-status');
        statusElements.forEach(element => {
            const indicator = element.querySelector('.w-2.h-2');
            const text = element.querySelector('span:last-child');
            
            if (indicator && text) {
                if (isOnline) {
                    indicator.className = 'w-2 h-2 bg-green-500 rounded-full animate-pulse';
                    text.textContent = 'Service Online';
                } else {
                    indicator.className = 'w-2 h-2 bg-red-500 rounded-full';
                    text.textContent = 'Service Offline';
                }
            }
        });
    }
    
    /**
     * Initialize additional UI components
     */
    initializeTooltips() {
        const tooltipElements = document.querySelectorAll('[data-tooltip]');
        tooltipElements.forEach(element => {
            element.addEventListener('mouseenter', (e) => {
                this.showTooltip(e.target, e.target.dataset.tooltip);
            });
            
            element.addEventListener('mouseleave', () => {
                this.hideTooltip();
            });
        });
    }
    
    showTooltip(element, text) {
        const tooltip = document.createElement('div');
        tooltip.className = 'tooltip absolute z-50 px-2 py-1 bg-gray-900 text-white text-sm rounded shadow-lg';
        tooltip.textContent = text;
        tooltip.id = 'active-tooltip';
        
        document.body.appendChild(tooltip);
        
        const rect = element.getBoundingClientRect();
        tooltip.style.left = `${rect.left + rect.width / 2 - tooltip.offsetWidth / 2}px`;
        tooltip.style.top = `${rect.top - tooltip.offsetHeight - 5}px`;
    }
    
    hideTooltip() {
        const tooltip = document.getElementById('active-tooltip');
        if (tooltip) {
            tooltip.remove();
        }
    }
    
    initializeCodeHighlighting() {
        const codeElements = document.querySelectorAll('pre code, .code-editor');
        codeElements.forEach(element => {
            // Basic syntax highlighting could be added here
            // For now, we'll just ensure proper formatting
            element.style.fontFamily = '"Monaco", "Menlo", "Ubuntu Mono", monospace';
        });
    }
    
    initializeProgressTracking() {
        // Track scroll depth for analytics
        let maxScrollPercentage = 0;
        
        window.addEventListener('scroll', () => {
            const scrollTop = window.pageYOffset || document.documentElement.scrollTop;
            const documentHeight = document.documentElement.scrollHeight - document.documentElement.clientHeight;
            const scrollPercentage = Math.round((scrollTop / documentHeight) * 100);
            
            if (scrollPercentage > maxScrollPercentage) {
                maxScrollPercentage = scrollPercentage;
                
                // Track milestone scroll depths
                if ([25, 50, 75, 90].includes(scrollPercentage)) {
                    this.trackEvent('scroll_depth', {
                        event_category: 'engagement',
                        event_label: `${scrollPercentage}%`,
                        value: scrollPercentage
                    });
                }
            }
        });
    }
    
    initializeResultsInteractivity() {
        // Add click handlers for expandable sections
        const expandableElements = document.querySelectorAll('[data-expandable]');
        expandableElements.forEach(element => {
            element.addEventListener('click', (e) => {
                const target = e.target.closest('[data-expandable]');
                const content = target.querySelector('[data-expandable-content]');
                
                if (content) {
                    const isExpanded = !content.classList.contains('hidden');
                    content.classList.toggle('hidden');
                    
                    const icon = target.querySelector('.expand-icon');
                    if (icon) {
                        icon.style.transform = isExpanded ? 'rotate(0deg)' : 'rotate(180deg)';
                    }
                }
            });
        });
        
        // Add copy-to-clipboard functionality
        const copyButtons = document.querySelectorAll('[data-copy]');
        copyButtons.forEach(button => {
            button.addEventListener('click', (e) => {
                const textToCopy = e.target.dataset.copy || e.target.closest('[data-copy]').dataset.copy;
                
                if (navigator.clipboard) {
                    navigator.clipboard.writeText(textToCopy).then(() => {
                        this.showToast('Copied to clipboard', 'success', 2000);
                    });
                } else {
                    // Fallback for older browsers
                    const textArea = document.createElement('textarea');
                    textArea.value = textToCopy;
                    document.body.appendChild(textArea);
                    textArea.select();
                    document.execCommand('copy');
                    document.body.removeChild(textArea);
                    this.showToast('Copied to clipboard', 'success', 2000);
                }
            });
        });
    }
}

// Global app instance
let app;

// Initialize app when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    app = new SmartCodeReviewApp();
    
    // Expose global functions for HTML onclick handlers
    window.startDemo = () => {
        const modal = document.getElementById('session-modal');
        if (modal) {
            modal.classList.remove('hidden');
        }
        
        app.trackEvent('demo_start_clicked', {
            event_category: 'engagement',
            event_label: 'main_cta'
        });
    };
    
    window.closeSessionModal = () => {
        app.closeAllModals();
    };
    
    // Handle browser back/forward buttons
    window.addEventListener('popstate', (event) => {
        if (event.state && event.state.tab) {
            app.switchTab(event.state.tab);
        }
    });
    
    // Handle page visibility changes
    document.addEventListener('visibilitychange', () => {
        if (document.hidden) {
            app.trackEvent('page_hidden', {
                event_category: 'engagement',
                event_label: 'tab_inactive'
            });
        } else {
            app.trackEvent('page_visible', {
                event_category: 'engagement',
                event_label: 'tab_active'
            });
        }
    });
    
    // Handle beforeunload for session cleanup
    window.addEventListener('beforeunload', () => {
        if (app.sessionData) {
            app.trackEvent('session_exit', {
                event_category: 'session',
                event_label: 'page_unload'
            });
        }
    });
});

// Export for module usage if needed
if (typeof module !== 'undefined' && module.exports) {
    module.exports = SmartCodeReviewApp;
}