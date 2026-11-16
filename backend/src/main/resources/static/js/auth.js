/**
 * Smart Banking Management System - Authentication Service & Route Guards
 */
const Auth = (() => {

    function getUser() {
        const u = localStorage.getItem('smartbank_user');
        if (!u) return null;
        try {
            return JSON.parse(u);
        } catch (e) {
            return null;
        }
    }

    function setUser(user) {
        if (user) {
            localStorage.setItem('smartbank_user', JSON.stringify(user));
        } else {
            localStorage.removeItem('smartbank_user');
        }
    }

    function isAuthenticated() {
        return !!API.getToken() && !!getUser();
    }

    function isAdmin() {
        const user = getUser();
        return user && user.role === 'ROLE_ADMIN';
    }

    function isCustomer() {
        const user = getUser();
        return user && user.role === 'ROLE_CUSTOMER';
    }

    /**
     * Authenticate with email and password
     */
    async function login(email, password) {
        try {
            const response = await API.post('/auth/login', { email, password });
            const authData = (response && response.data) ? response.data : response;
            const token = authData ? (authData.token || authData.accessToken) : null;
            const user = authData ? authData.user : null;

            if (token && user) {
                API.setToken(token);
                setUser(user);
                return {
                    ...authData,
                    token: token,
                    accessToken: token,
                    user: user
                };
            } else {
                throw new Error('Invalid login response from server');
            }
        } catch (error) {
            throw error;
        }
    }

    /**
     * Log out current user
     */
    async function logout() {
        try {
            if (API.getToken()) {
                await API.post('/auth/logout');
            }
        } catch (e) {
            console.warn('Server logout notice:', e);
        } finally {
            API.setToken(null);
            setUser(null);
            window.location.href = '/pages/login.html';
        }
    }

    /**
     * Route protection guard for HTML pages
     */
    function requireAuth(allowedRoles = []) {
        if (!isAuthenticated()) {
            window.location.href = '/pages/login.html';
            return false;
        }

        const user = getUser();
        if (allowedRoles.length > 0 && !allowedRoles.includes(user.role)) {
            API.showToast('Access restricted: Insufficient privileges for this portal.', 'danger');
            if (user.role === 'ROLE_ADMIN') {
                window.location.href = '/pages/admin-dashboard.html';
            } else {
                window.location.href = '/pages/dashboard.html';
            }
            return false;
        }

        return true;
    }

    /**
     * Render dynamic navbar state across all pages
     */
    function initNavbar() {
        const navUserSlot = document.getElementById('nav-user-slot');
        if (!navUserSlot) return;

        const user = getUser();
        if (isAuthenticated() && user) {
            const roleBadge = user.role === 'ROLE_ADMIN' 
                ? '<span class="badge bg-danger ms-1">ADMIN</span>' 
                : '<span class="badge bg-info ms-1">CUSTOMER</span>';
            
            navUserSlot.innerHTML = `
                <div class="dropdown">
                    <button class="btn btn-outline-light dropdown-toggle d-flex align-items-center" type="button" data-bs-toggle="dropdown" aria-expanded="false">
                        <i class="bi bi-person-circle fs-5 me-2"></i>
                        <span>${user.fullName || user.email}</span>
                        ${roleBadge}
                    </button>
                    <ul class="dropdown-menu dropdown-menu-end shadow">
                        <li><h6 class="dropdown-header">${user.email}</h6></li>
                        ${user.role === 'ROLE_ADMIN' 
                            ? '<li><a class="dropdown-item" href="/pages/admin-dashboard.html"><i class="bi bi-speedometer2 me-2"></i>Admin Dashboard</a></li>' 
                            : '<li><a class="dropdown-item" href="/pages/dashboard.html"><i class="bi bi-speedometer2 me-2"></i>My Dashboard</a></li>'
                        }
                        <li><a class="dropdown-item" href="/pages/profile.html"><i class="bi bi-gear me-2"></i>Profile & Security</a></li>
                        <li><hr class="dropdown-divider"></li>
                        <li><button class="dropdown-item text-danger" onclick="Auth.logout()"><i class="bi bi-box-arrow-right me-2"></i>Sign Out</button></li>
                    </ul>
                </div>
            `;
        } else {
            navUserSlot.innerHTML = `
                <a href="/pages/login.html" class="btn btn-outline-light me-2"><i class="bi bi-box-arrow-in-right me-1"></i>Sign In</a>
                <a href="/pages/register.html" class="btn btn-sb-accent"><i class="bi bi-person-plus me-1"></i>Register</a>
            `;
        }
    }

    return {
        getUser,
        setUser,
        isAuthenticated,
        isAdmin,
        isCustomer,
        login,
        logout,
        requireAuth,
        initNavbar
    };
})();
