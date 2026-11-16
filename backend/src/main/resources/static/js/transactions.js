/**
 * Smart Banking Management System - Customer Transactions Logic
 */
let currentPage = 0;
const pageSize = 10;

document.addEventListener('DOMContentLoaded', async () => {
    if (!Auth.requireAuth()) return;
    Auth.initNavbar();

    // Check if URL has ?account= query param
    const urlParams = new URLSearchParams(window.location.search);
    const initialAccount = urlParams.get('account');
    if (initialAccount) {
        document.getElementById('filter-reference').value = initialAccount;
    }

    await loadTransactions();

    document.getElementById('filter-form').addEventListener('submit', (e) => {
        e.preventDefault();
        currentPage = 0;
        loadTransactions();
    });

    document.getElementById('reset-filter-btn').addEventListener('click', () => {
        document.getElementById('filter-form').reset();
        currentPage = 0;
        loadTransactions();
    });
});

async function loadTransactions() {
    const tbody = document.getElementById('transactions-tbody');
    const type = document.getElementById('filter-type').value;
    const reference = document.getElementById('filter-reference').value.trim();
    const start = document.getElementById('filter-start').value;
    const end = document.getElementById('filter-end').value;

    let queryParams = `page=${currentPage}&size=${pageSize}&sort=transactionDate,desc`;
    if (type) queryParams += `&type=${encodeURIComponent(type)}`;
    if (reference) queryParams += `&reference=${encodeURIComponent(reference)}`;
    if (start) queryParams += `&startDate=${encodeURIComponent(start + 'T00:00:00')}`;
    if (end) queryParams += `&endDate=${encodeURIComponent(end + 'T23:59:59')}`;

    try {
        const data = await API.get(`/transactions?${queryParams}`);
        const transactions = (data && data.content) ? data.content : [];

        renderTable(transactions);
        renderPagination(data);
    } catch (err) {
        tbody.innerHTML = `
            <tr>
                <td colspan="8" class="text-center text-danger py-4">
                    <i class="bi bi-exclamation-triangle-fill me-2"></i>Failed to load transactions: ${err.message}
                </td>
            </tr>
        `;
    }
}

function renderTable(transactions) {
    const tbody = document.getElementById('transactions-tbody');
    if (!transactions || transactions.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="8" class="text-center py-5 text-muted">
                    <i class="bi bi-search fs-3 d-block mb-2"></i>
                    No transactions match your criteria
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = transactions.map(tx => {
        const isCredit = tx.transactionType === 'DEPOSIT' || tx.transactionType === 'TRANSFER_IN';
        const amountClass = isCredit ? 'text-success fw-bold' : 'text-danger fw-bold';
        const amountPrefix = isCredit ? '+' : '-';

        let typeBadge = '';
        if (tx.transactionType === 'DEPOSIT') {
            typeBadge = '<span class="tx-badge-deposit"><i class="bi bi-arrow-down-left me-1"></i>Deposit</span>';
        } else if (tx.transactionType === 'WITHDRAWAL') {
            typeBadge = '<span class="tx-badge-withdrawal"><i class="bi bi-arrow-up-right me-1"></i>Withdrawal</span>';
        } else if (tx.transactionType === 'TRANSFER_IN') {
            typeBadge = '<span class="tx-badge-transfer-in"><i class="bi bi-box-arrow-in-down-left me-1"></i>Transfer IN</span>';
        } else {
            typeBadge = '<span class="tx-badge-transfer-out"><i class="bi bi-box-arrow-up-right me-1"></i>Transfer OUT</span>';
        }

        const statusBadge = tx.transactionStatus === 'SUCCESS' 
            ? '<span class="badge bg-success-subtle text-success border border-success-subtle">SUCCESS</span>' 
            : '<span class="badge bg-danger-subtle text-danger border border-danger-subtle">FAILED</span>';

        return `
            <tr>
                <td>
                    <div class="d-flex align-items-center gap-1">
                        <code>${tx.transactionReference}</code>
                        <button class="btn btn-sm btn-link text-muted p-0" title="Copy Reference" onclick="copyToClipboard('${tx.transactionReference}')">
                            <i class="bi bi-copy"></i>
                        </button>
                    </div>
                </td>
                <td>${typeBadge}</td>
                <td><strong>${tx.accountNumber}</strong></td>
                <td>${tx.relatedAccountNumber ? `<code>${tx.relatedAccountNumber}</code>` : '<span class="text-muted">-</span>'}</td>
                <td>${tx.description || '-'}</td>
                <td class="small text-muted">${API.formatDateTime(tx.transactionDate)}</td>
                <td>${statusBadge}</td>
                <td class="text-end ${amountClass}">${amountPrefix}${API.formatCurrency(tx.amount)}</td>
            </tr>
        `;
    }).join('');
}

function renderPagination(data) {
    const info = document.getElementById('pagination-info');
    const controls = document.getElementById('pagination-controls');

    if (!data) return;

    const totalElements = data.totalElements || 0;
    const totalPages = data.totalPages || 0;
    const startItem = totalElements === 0 ? 0 : (currentPage * pageSize) + 1;
    const endItem = Math.min((currentPage + 1) * pageSize, totalElements);

    info.textContent = `Showing ${startItem} to ${endItem} of ${totalElements} entries`;

    if (totalPages <= 1) {
        controls.innerHTML = '';
        return;
    }

    let html = `
        <li class="page-item ${currentPage === 0 ? 'disabled' : ''}">
            <button class="page-link" onclick="changePage(${currentPage - 1})"><i class="bi bi-chevron-left"></i></button>
        </li>
    `;

    for (let p = 0; p < totalPages; p++) {
        if (p === currentPage || p === 0 || p === totalPages - 1 || Math.abs(p - currentPage) <= 1) {
            html += `
                <li class="page-item ${p === currentPage ? 'active' : ''}">
                    <button class="page-link" onclick="changePage(${p})">${p + 1}</button>
                </li>
            `;
        } else if (p === currentPage - 2 || p === currentPage + 2) {
            html += `<li class="page-item disabled"><span class="page-link">...</span></li>`;
        }
    }

    html += `
        <li class="page-item ${currentPage >= totalPages - 1 ? 'disabled' : ''}">
            <button class="page-link" onclick="changePage(${currentPage + 1})"><i class="bi bi-chevron-right"></i></button>
        </li>
    `;

    controls.innerHTML = html;
}

function changePage(page) {
    currentPage = page;
    loadTransactions();
}

function copyToClipboard(text) {
    navigator.clipboard.writeText(text).then(() => {
        API.showToast(`Copied ${text}`, 'info');
    });
}
