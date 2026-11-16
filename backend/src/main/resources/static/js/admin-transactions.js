/**
 * Smart Banking Management System - Admin System Transactions Logic
 */
let txPage = 0;
const txPageSize = 10;

document.addEventListener('DOMContentLoaded', async () => {
    if (!Auth.requireAuth(['ROLE_ADMIN'])) return;
    Auth.initNavbar();

    await loadAdminTransactions();

    document.getElementById('admin-tx-filter-form').addEventListener('submit', (e) => {
        e.preventDefault();
        txPage = 0;
        loadAdminTransactions();
    });

    document.getElementById('admin-reset-tx-filter-btn').addEventListener('click', () => {
        document.getElementById('admin-tx-filter-form').reset();
        txPage = 0;
        loadAdminTransactions();
    });
});

async function loadAdminTransactions() {
    const tbody = document.getElementById('global-transactions-tbody');
    const type = document.getElementById('admin-filter-type').value;
    const status = document.getElementById('admin-filter-status').value;
    const accountNumber = document.getElementById('admin-filter-account').value.trim();
    const customer = document.getElementById('admin-filter-customer').value.trim();
    const reference = document.getElementById('admin-filter-reference').value.trim();

    let url = `/admin/transactions?page=${txPage}&size=${txPageSize}&sort=transactionDate,desc`;
    if (type) url += `&type=${encodeURIComponent(type)}`;
    if (status) url += `&status=${encodeURIComponent(status)}`;
    if (accountNumber) url += `&accountNumber=${encodeURIComponent(accountNumber)}`;
    if (customer) url += `&customerQuery=${encodeURIComponent(customer)}`;
    if (reference) url += `&reference=${encodeURIComponent(reference)}`;

    try {
        const data = await API.get(url);
        const transactions = (data && data.content) ? data.content : [];

        renderGlobalTransactionsTable(transactions);
        renderGlobalTxPagination(data);
    } catch (err) {
        tbody.innerHTML = `
            <tr>
                <td colspan="8" class="text-center text-danger py-4">
                    Error loading transactions: ${err.message}
                </td>
            </tr>
        `;
    }
}

function renderGlobalTransactionsTable(transactions) {
    const tbody = document.getElementById('global-transactions-tbody');
    if (!transactions || transactions.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="8" class="text-center py-5 text-muted">
                    <i class="bi bi-search fs-3 d-block mb-2"></i>
                    No transactions match your search criteria
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = transactions.map(tx => {
        let typeBadge = '';
        if (tx.transactionType === 'DEPOSIT') {
            typeBadge = '<span class="tx-badge-deposit">Deposit</span>';
        } else if (tx.transactionType === 'WITHDRAWAL') {
            typeBadge = '<span class="tx-badge-withdrawal">Withdrawal</span>';
        } else if (tx.transactionType === 'TRANSFER_IN') {
            typeBadge = '<span class="tx-badge-transfer-in">Transfer IN</span>';
        } else {
            typeBadge = '<span class="tx-badge-transfer-out">Transfer OUT</span>';
        }

        const statusBadge = tx.transactionStatus === 'SUCCESS' 
            ? '<span class="badge bg-success-subtle text-success">SUCCESS</span>' 
            : '<span class="badge bg-danger-subtle text-danger">FAILED</span>';

        return `
            <tr>
                <td><code>${tx.transactionReference}</code></td>
                <td>${typeBadge}</td>
                <td><strong>${tx.accountNumber}</strong></td>
                <td>${tx.relatedAccountNumber ? `<code>${tx.relatedAccountNumber}</code>` : '-'}</td>
                <td class="text-truncate" style="max-width: 220px;">${tx.description || '-'}</td>
                <td class="small text-muted">${API.formatDateTime(tx.transactionDate)}</td>
                <td>${statusBadge}</td>
                <td class="text-end fw-bold">${API.formatCurrency(tx.amount)}</td>
            </tr>
        `;
    }).join('');
}

function renderGlobalTxPagination(data) {
    const info = document.getElementById('global-tx-pagination-info');
    const controls = document.getElementById('global-tx-pagination-controls');

    if (!data) return;

    const totalElements = data.totalElements || 0;
    const totalPages = data.totalPages || 0;
    const startItem = totalElements === 0 ? 0 : (txPage * txPageSize) + 1;
    const endItem = Math.min((txPage + 1) * txPageSize, totalElements);

    info.textContent = `Showing ${startItem} to ${endItem} of ${totalElements} transactions`;

    if (totalPages <= 1) {
        controls.innerHTML = '';
        return;
    }

    let html = `
        <li class="page-item ${txPage === 0 ? 'disabled' : ''}">
            <button class="page-link" onclick="changeTxPage(${txPage - 1})"><i class="bi bi-chevron-left"></i></button>
        </li>
    `;

    for (let p = 0; p < totalPages; p++) {
        if (p === txPage || p === 0 || p === totalPages - 1 || Math.abs(p - txPage) <= 1) {
            html += `
                <li class="page-item ${p === txPage ? 'active' : ''}">
                    <button class="page-link" onclick="changeTxPage(${p})">${p + 1}</button>
                </li>
            `;
        } else if (p === txPage - 2 || p === txPage + 2) {
            html += `<li class="page-item disabled"><span class="page-link">...</span></li>`;
        }
    }

    html += `
        <li class="page-item ${txPage >= totalPages - 1 ? 'disabled' : ''}">
            <button class="page-link" onclick="changeTxPage(${txPage + 1})"><i class="bi bi-chevron-right"></i></button>
        </li>
    `;

    controls.innerHTML = html;
}

function changeTxPage(page) {
    txPage = page;
    loadAdminTransactions();
}
