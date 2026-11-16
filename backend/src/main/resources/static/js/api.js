/**
 * Smart Banking Management System - Centralized API Service
 */
const API = (() => {
    const BASE_URL = '/api';

    /**
     * Get stored JWT token
     */
    function getToken() {
        return localStorage.getItem('smartbank_token');
    }

    /**
     * Set stored JWT token
     */
    function setToken(token) {
        if (token) {
            localStorage.setItem('smartbank_token', token);
        } else {
            localStorage.removeItem('smartbank_token');
        }
    }

    /**
     * Generic fetch wrapper with automatic JWT authorization and unified error handling
     */
    async function request(endpoint, options = {}) {
        const url = endpoint.startsWith('http') ? endpoint : `${BASE_URL}${endpoint}`;
        
        const headers = {
            'Content-Type': 'application/json',
            'Accept': 'application/json',
            ...(options.headers || {})
        };

        const token = getToken();
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }

        const config = {
            ...options,
            headers
        };

        try {
            const response = await fetch(url, config);

            // Handle 401 Unauthorized (session expired or invalid credentials)
            if (response.status === 401) {
                // If it's the login endpoint itself, let the login page handle the bad credentials error
                if (!endpoint.includes('/auth/login')) {
                    setToken(null);
                    localStorage.removeItem('smartbank_user');
                    showToast('Session expired or unauthorized. Please log in again.', 'warning');
                    setTimeout(() => {
                        window.location.href = '/pages/login.html?expired=true';
                    }, 1200);
                    throw new Error('Unauthorized');
                }
            }

            // Handle 403 Forbidden
            if (response.status === 403) {
                showToast('Access denied: You do not have permission to perform this action.', 'danger');
                throw new Error('Forbidden: Access denied');
            }

            // Parse response body
            const contentType = response.headers.get('content-type');
            let data = null;
            if (contentType && contentType.includes('application/json')) {
                data = await response.json();
            } else {
                data = await response.text();
            }

            if (!response.ok) {
                const errorMessage = (data && data.message) ? data.message : `HTTP Error ${response.status}`;
                throw new Error(errorMessage);
            }

            return data;
        } catch (error) {
            console.error(`API Error on [${options.method || 'GET'} ${url}]:`, error);
            throw error;
        }
    }

    /**
     * Live toast notifications
     */
    function showToast(message, type = 'info', title = null) {
        let container = document.getElementById('toast-container');
        if (!container) {
            container = document.createElement('div');
            container.id = 'toast-container';
            document.body.appendChild(container);
        }

        const toastId = 'toast-' + Date.now();
        const headerTitle = title || (type === 'success' ? 'Success' : type === 'danger' ? 'Error' : type === 'warning' ? 'Notice' : 'Information');
        const iconClass = type === 'success' ? 'bi-check-circle-fill text-success' :
                          type === 'danger' ? 'bi-exclamation-triangle-fill text-danger' :
                          type === 'warning' ? 'bi-exclamation-circle-fill text-warning' : 'bi-info-circle-fill text-info';

        const toastHtml = `
            <div id="${toastId}" class="toast align-items-center border-0 shadow-lg mb-2" role="alert" aria-live="assertive" aria-atomic="true">
                <div class="toast-header">
                    <i class="bi ${iconClass} me-2 fs-5"></i>
                    <strong class="me-auto">${headerTitle}</strong>
                    <small class="text-muted">Just now</small>
                    <button type="button" class="btn-close" data-bs-dismiss="toast" aria-label="Close"></button>
                </div>
                <div class="toast-body">
                    ${message}
                </div>
            </div>
        `;

        container.insertAdjacentHTML('beforeend', toastHtml);
        const toastElement = document.getElementById(toastId);
        if (window.bootstrap && window.bootstrap.Toast) {
            const toast = new bootstrap.Toast(toastElement, { delay: 4500 });
            toast.show();
            toastElement.addEventListener('hidden.bs.toast', () => toastElement.remove());
        } else {
            setTimeout(() => toastElement.remove(), 4500);
        }
    }

    /**
     * Format currency amount in Indian Rupees (₹)
     */
    function formatCurrency(amount) {
        if (amount === null || amount === undefined) return '₹0.00';
        const num = Number(amount);
        return new Intl.NumberFormat('en-IN', {
            style: 'currency',
            currency: 'INR',
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
        }).format(num);
    }

    /**
     * Format date and time
     */
    function formatDateTime(dateStr) {
        if (!dateStr) return '-';
        const d = new Date(dateStr);
        return d.toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    }

    return {
        get: (endpoint, options = {}) => request(endpoint, { ...options, method: 'GET' }),
        post: (endpoint, body, options = {}) => request(endpoint, { ...options, method: 'POST', body: JSON.stringify(body) }),
        put: (endpoint, body, options = {}) => request(endpoint, { ...options, method: 'PUT', body: JSON.stringify(body) }),
        patch: (endpoint, body, options = {}) => request(endpoint, { ...options, method: 'PATCH', body: JSON.stringify(body) }),
        delete: (endpoint, options = {}) => request(endpoint, { ...options, method: 'DELETE' }),
        getToken,
        setToken,
        showToast,
        formatCurrency,
        formatDateTime
    };
})();
