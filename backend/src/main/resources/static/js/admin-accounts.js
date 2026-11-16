/**
 * Smart Banking Management System - Admin Accounts Logic
 */
let accPage = 0;
const accPageSize = 10;

document.addEventListener('DOMContentLoaded', async () => {
    if (!Auth.requireAuth(['ROLE_ADMIN'])) return;
    Auth.initNavbar();

    await loadAdminAccounts();

    document.getElementById('accounts-filter-form').addEventListener('submit', (e) => {
        e.preventDefault();
        accPage = 0;
        loadAdminAccounts();
    });

    document.getElementById('reset-acc-filter-btn').addEventListener('click', () => {
        document.getElementById('accounts-filter-form').reset();
        accPage = 0;
        loadAdminAccounts();
    });

    document.getElementById('account-status-form').addEventListener('submit', async function(e) {
        e.preventDefault();
        const accountId = document.getElementById('edit-account-id').value;
        const accountStatus = document.getElementById('select-account-status').value;
        const reason = document.getElementById('status-change-reason').value.trim();

        try {
            await API.patch(`/admin/accounts/${accountId}/status`, {
                accountStatus,
                reason
            });
            API.showToast(`Account status updated to ${accountStatus}`, 'success');
            bootstrap.Modal.getInstance(document.getElementById('accountStatusModal')).hide();
            await loadAdminAccounts();
        } catch (err) {
            API.showToast(err.message || 'Failed to update account status', 'danger');
        }
    });
});

async function loadAdminAccounts() {
    const tbody = document.getElementById('admin-accounts-tbody');
    const type = document.getElementById('filter-acc-type').value;
    const status = document.getElementById('filter-acc-status').value;
    const query = document.getElementById('filter-acc-query').value.trim();

    let url = `/admin/accounts?page=${accPage}&size=${accPageSize}&sort=createdAt,desc`;
    if (type) url += `&type=${encodeURIComponent(type)}`;
    if (status) url += `&status=${encodeURIComponent(status)}`;
    if (query) url += `&query=${encodeURIComponent(query)}`;

    try {
        const data = await API.get(url);
        const accounts = (data && data.content) ? data.content : [];

        renderAdminAccountsTable(accounts);
        renderAccountPagination(data);
    } catch (err) {
        tbody.innerHTML = `
            <tr>
                <td colspan="8" class="text-center text-danger py-4">
                    Error loading accounts: ${err.message}
                </td>
            </tr>
        `;
    }
}

function renderAdminAccountsTable(accounts) {
    const tbody = document.getElementById('admin-accounts-tbody');
    if (!accounts || accounts.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="8" class="text-center py-5 text-muted">
                    <i class="bi bi-credit-card-2-front fs-3 d-block mb-2"></i>
                    No accounts matching your criteria
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = accounts.map(a => {
        const statusBadge = a.accountStatus === 'ACTIVE' 
            ? '<span class="badge badge-active">ACTIVE</span>' 
            : a.accountStatus === 'FROZEN' 
                ? '<span class="badge badge-frozen">FROZEN</span>' 
                : '<span class="badge badge-inactive">INACTIVE</span>';

        return `
            <tr>
                <td><code>${a.accountNumber}</code></td>
                <td><span class="badge bg-secondary">${a.accountType}</span></td>
                <td class="fw-semibold">${a.ownerName || '-'}</td>
                <td class="small text-muted">${a.ownerEmail || '-'}</td>
                <td>${statusBadge}</td>
                <td class="small text-muted">${a.createdAt ? a.createdAt.split('T')[0] : '-'}</td>
                <td class="text-end fw-bold">${API.formatCurrency(a.balance)}</td>
                <td class="text-end">
                    <button class="btn btn-outline-danger btn-sm" onclick="openAccountStatusModal(${a.id}, '${a.accountNumber}', '${a.accountStatus}')">
                        <i class="bi bi-pencil-square me-1"></i>Status
                    </button>
                </td>
            </tr>
        `;
    }).join('');
}

function renderAccountPagination(data) {
    const info = document.getElementById('acc-pagination-info');
    const controls = document.getElementById('acc-pagination-controls');

    if (!data) return;

    const totalElements = data.totalElements || 0;
    const totalPages = data.totalPages || 0;
    const startItem = totalElements === 0 ? 0 : (accPage * accPageSize) + 1;
    const endItem = Math.min((accPage + 1) * accPageSize, totalElements);

    info.textContent = `Showing ${startItem} to ${endItem} of ${totalElements} accounts`;

    if (totalPages <= 1) {
        controls.innerHTML = '';
        return;
    }

    let html = `
        <li class="page-item ${accPage === 0 ? 'disabled' : ''}">
            <button class="page-link" onclick="changeAccPage(${accPage - 1})"><i class="bi bi-chevron-left"></i></button>
        </li>
    `;

    for (let p = 0; p < totalPages; p++) {
        if (p === accPage || p === 0 || p === totalPages - 1 || Math.abs(p - accPage) <= 1) {
            html += `
                <li class="page-item ${p === accPage ? 'active' : ''}">
                    <button class="page-link" onclick="changeAccPage(${p})">${p + 1}</button>
                </li>
            `;
        } else if (p === accPage - 2 || p === accPage + 2) {
            html += `<li class="page-item disabled"><span class="page-link">...</span></li>`;
        }
    }

    html += `
        <li class="page-item ${accPage >= totalPages - 1 ? 'disabled' : ''}">
            <button class="page-link" onclick="changeAccPage(${accPage + 1})"><i class="bi bi-chevron-right"></i></button>
        </li>
    `;

    controls.innerHTML = html;
}

function changeAccPage(page) {
    accPage = page;
    loadAdminAccounts();
}

function openAccountStatusModal(accountId, accountNumber, currentStatus) {
    document.getElementById('edit-account-id').value = accountId;
    document.getElementById('modal-account-number').textContent = accountNumber;
    document.getElementById('select-account-status').value = currentStatus;
    document.getElementById('status-change-reason').value = '';

    const modal = new bootstrap.Modal(document.getElementById('accountStatusModal'));
    modal.show();
}
