/**
 * Smart Banking Management System - Admin Customers Logic
 */
let customerPage = 0;
const customerPageSize = 10;

document.addEventListener('DOMContentLoaded', async () => {
    if (!Auth.requireAuth(['ROLE_ADMIN'])) return;
    Auth.initNavbar();

    await loadCustomers();

    document.getElementById('customer-search-form').addEventListener('submit', (e) => {
        e.preventDefault();
        customerPage = 0;
        loadCustomers();
    });

    document.getElementById('reset-search-btn').addEventListener('click', () => {
        document.getElementById('customer-search-form').reset();
        customerPage = 0;
        loadCustomers();
    });

    document.getElementById('status-update-form').addEventListener('submit', async function(e) {
        e.preventDefault();
        const customerId = document.getElementById('status-customer-id').value;
        const enabled = document.getElementById('status-select').value === 'true';
        const reason = document.getElementById('status-reason').value.trim();

        try {
            await API.patch(`/admin/customers/${customerId}/status`, {
                enabled,
                reason
            });
            API.showToast(`Customer #${customerId} status updated to ${enabled ? 'ACTIVE' : 'DEACTIVATED'}`, 'success');
            bootstrap.Modal.getInstance(document.getElementById('statusModal')).hide();
            await loadCustomers();
        } catch (err) {
            API.showToast(err.message || 'Failed to update status', 'danger');
        }
    });
});

async function loadCustomers() {
    const tbody = document.getElementById('customers-tbody');
    const query = document.getElementById('search-query').value.trim();

    let url = `/admin/customers?page=${customerPage}&size=${customerPageSize}&sort=createdAt,desc`;
    if (query) {
        url += `&query=${encodeURIComponent(query)}`;
    }

    try {
        const data = await API.get(url);
        const customers = (data && data.content) ? data.content : [];

        renderCustomersTable(customers);
        renderCustomerPagination(data);
    } catch (err) {
        tbody.innerHTML = `
            <tr>
                <td colspan="7" class="text-center text-danger py-4">
                    Error loading customers: ${err.message}
                </td>
            </tr>
        `;
    }
}

function renderCustomersTable(customers) {
    const tbody = document.getElementById('customers-tbody');
    if (!customers || customers.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="7" class="text-center py-5 text-muted">
                    <i class="bi bi-people fs-3 d-block mb-2"></i>
                    No customers found matching query
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = customers.map(c => {
        const statusBadge = c.enabled 
            ? '<span class="badge badge-active">ACTIVE</span>' 
            : '<span class="badge badge-frozen">DISABLED</span>';

        return `
            <tr>
                <td><strong>#${c.id}</strong></td>
                <td>
                    <div class="fw-semibold">${c.fullName}</div>
                    <small class="text-muted">${c.address || ''}</small>
                </td>
                <td>${c.email}</td>
                <td>${c.phoneNumber}</td>
                <td>${statusBadge}</td>
                <td class="small text-muted">${c.createdAt ? c.createdAt.split('T')[0] : '-'}</td>
                <td class="text-end">
                    <button class="btn btn-outline-primary btn-sm me-1" onclick="viewCustomerDetails(${c.id})">
                        <i class="bi bi-eye me-1"></i>View
                    </button>
                    <button class="btn btn-sm ${c.enabled ? 'btn-outline-danger' : 'btn-outline-success'}" onclick="openStatusModal(${c.id}, ${c.enabled}, '${c.fullName.replace(/'/g, "\\'")}')">
                        <i class="bi ${c.enabled ? 'bi-slash-circle' : 'bi-check-circle'} me-1"></i>${c.enabled ? 'Deactivate' : 'Activate'}
                    </button>
                </td>
            </tr>
        `;
    }).join('');
}

function renderCustomerPagination(data) {
    const info = document.getElementById('pagination-info');
    const controls = document.getElementById('pagination-controls');

    if (!data) return;

    const totalElements = data.totalElements || 0;
    const totalPages = data.totalPages || 0;
    const startItem = totalElements === 0 ? 0 : (customerPage * customerPageSize) + 1;
    const endItem = Math.min((customerPage + 1) * customerPageSize, totalElements);

    info.textContent = `Showing ${startItem} to ${endItem} of ${totalElements} customers`;

    if (totalPages <= 1) {
        controls.innerHTML = '';
        return;
    }

    let html = `
        <li class="page-item ${customerPage === 0 ? 'disabled' : ''}">
            <button class="page-link" onclick="changeCustomerPage(${customerPage - 1})"><i class="bi bi-chevron-left"></i></button>
        </li>
    `;

    for (let p = 0; p < totalPages; p++) {
        if (p === customerPage || p === 0 || p === totalPages - 1 || Math.abs(p - customerPage) <= 1) {
            html += `
                <li class="page-item ${p === customerPage ? 'active' : ''}">
                    <button class="page-link" onclick="changeCustomerPage(${p})">${p + 1}</button>
                </li>
            `;
        } else if (p === customerPage - 2 || p === customerPage + 2) {
            html += `<li class="page-item disabled"><span class="page-link">...</span></li>`;
        }
    }

    html += `
        <li class="page-item ${customerPage >= totalPages - 1 ? 'disabled' : ''}">
            <button class="page-link" onclick="changeCustomerPage(${customerPage + 1})"><i class="bi bi-chevron-right"></i></button>
        </li>
    `;

    controls.innerHTML = html;
}

function changeCustomerPage(page) {
    customerPage = page;
    loadCustomers();
}

async function viewCustomerDetails(customerId) {
    const modal = new bootstrap.Modal(document.getElementById('customerDetailModal'));
    modal.show();

    const body = document.getElementById('customer-detail-body');
    body.innerHTML = '<div class="text-center py-4"><div class="spinner-border text-primary"></div></div>';

    try {
        const detail = await API.get(`/admin/customers/${customerId}`);
        if (!detail) throw new Error('Customer data not found');

        let accountsHtml = '<p class="text-muted small">No bank accounts opened by this customer.</p>';
        if (detail.accounts && detail.accounts.length > 0) {
            accountsHtml = `
                <table class="table table-sm table-bordered mt-2">
                    <thead class="table-light">
                        <tr>
                            <th>Account Number</th>
                            <th>Type</th>
                            <th>Status</th>
                            <th class="text-end">Balance</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${detail.accounts.map(a => `
                            <tr>
                                <td><code>${a.accountNumber}</code></td>
                                <td><span class="badge bg-secondary">${a.accountType}</span></td>
                                <td>${a.accountStatus}</td>
                                <td class="text-end fw-bold">${API.formatCurrency(a.balance)}</td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            `;
        }

        body.innerHTML = `
            <div class="row g-3 mb-3">
                <div class="col-md-6">
                    <label class="text-muted small">Full Name</label>
                    <div class="fw-bold">${detail.fullName}</div>
                </div>
                <div class="col-md-6">
                    <label class="text-muted small">Email Address</label>
                    <div class="fw-bold">${detail.email}</div>
                </div>
                <div class="col-md-6">
                    <label class="text-muted small">Phone Number</label>
                    <div>${detail.phoneNumber}</div>
                </div>
                <div class="col-md-6">
                    <label class="text-muted small">Account Status</label>
                    <div>${detail.enabled ? '<span class="badge badge-active">ACTIVE</span>' : '<span class="badge badge-frozen">DISABLED</span>'}</div>
                </div>
                <div class="col-12">
                    <label class="text-muted small">Residential Address</label>
                    <div>${detail.address || '-'}</div>
                </div>
            </div>

            <div class="border-top pt-3">
                <div class="d-flex justify-content-between align-items-center mb-2">
                    <h6 class="fw-bold mb-0">Bank Accounts Portfolio</h6>
                    <span class="badge bg-primary fs-6">Total Balance: ${API.formatCurrency(detail.totalBalance)}</span>
                </div>
                ${accountsHtml}
            </div>
        `;
    } catch (err) {
        body.innerHTML = `<div class="alert alert-danger">${err.message}</div>`;
    }
}

function openStatusModal(customerId, currentlyEnabled, customerName) {
    document.getElementById('status-customer-id').value = customerId;
    document.getElementById('status-select').value = (!currentlyEnabled).toString();
    document.getElementById('status-modal-prompt').textContent = 
        `Confirm changing status for customer "${customerName}" (ID #${customerId}):`;
    document.getElementById('status-reason').value = '';

    const modal = new bootstrap.Modal(document.getElementById('statusModal'));
    modal.show();
}
