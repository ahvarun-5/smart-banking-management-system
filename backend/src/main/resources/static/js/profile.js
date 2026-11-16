/**
 * Smart Banking Management System - Customer Profile & Security Logic
 */
document.addEventListener('DOMContentLoaded', async () => {
    if (!Auth.requireAuth()) return;
    Auth.initNavbar();

    await loadProfile();

    // Profile update form handler
    document.getElementById('profile-update-form').addEventListener('submit', async function(e) {
        e.preventDefault();
        const fullName = document.getElementById('edit-fullname').value.trim();
        const phoneNumber = document.getElementById('edit-phone').value.trim();
        const address = document.getElementById('edit-address').value.trim();

        const btn = document.getElementById('save-profile-btn');
        btn.disabled = true;

        try {
            const updated = await API.put('/customers/profile', {
                fullName,
                phoneNumber,
                address
            });
            // Update local user session
            const currentUser = Auth.getUser();
            if (currentUser) {
                currentUser.fullName = updated.fullName;
                currentUser.phoneNumber = updated.phoneNumber;
                currentUser.address = updated.address;
                Auth.setUser(currentUser);
            }
            Auth.initNavbar();
            API.showToast('Profile updated successfully!', 'success');
            await loadProfile();
        } catch (err) {
            API.showToast(err.message || 'Failed to update profile', 'danger');
        } finally {
            btn.disabled = false;
        }
    });

    // Password change form handler
    document.getElementById('password-change-form').addEventListener('submit', async function(e) {
        e.preventDefault();
        const currentPassword = document.getElementById('current-password').value;
        const newPassword = document.getElementById('new-password').value;
        const confirmPassword = document.getElementById('confirm-new-password').value;

        if (newPassword !== confirmPassword) {
            API.showToast('New passwords do not match!', 'warning');
            return;
        }

        if (newPassword.length < 6) {
            API.showToast('Password must be at least 6 characters.', 'warning');
            return;
        }

        const btn = document.getElementById('save-password-btn');
        btn.disabled = true;

        try {
            await API.put('/customers/change-password', {
                currentPassword,
                newPassword,
                confirmPassword
            });
            API.showToast('Password changed successfully!', 'success');
            this.reset();
        } catch (err) {
            API.showToast(err.message || 'Failed to change password. Verify current password.', 'danger');
        } finally {
            btn.disabled = false;
        }
    });
});

async function loadProfile() {
    try {
        const profile = await API.get('/customers/profile');
        if (!profile) return;

        document.getElementById('profile-name').textContent = profile.fullName || 'User';
        document.getElementById('profile-email').textContent = profile.email || '-';
        document.getElementById('profile-role').textContent = profile.role || '-';
        document.getElementById('profile-id').textContent = `#${profile.id}`;
        document.getElementById('profile-created').textContent = profile.createdAt ? profile.createdAt.split('T')[0] : '-';

        document.getElementById('edit-fullname').value = profile.fullName || '';
        document.getElementById('edit-phone').value = profile.phoneNumber || '';
        document.getElementById('edit-address').value = profile.address || '';
    } catch (err) {
        API.showToast(err.message || 'Failed to load profile', 'danger');
    }
}
