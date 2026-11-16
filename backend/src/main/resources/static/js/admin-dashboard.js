/**
 * Smart Banking Management System - Admin Dashboard Logic
 */
document.addEventListener('DOMContentLoaded', async () => {
    // Route guard: strictly ROLE_ADMIN
    if (!Auth.requireAuth(['ROLE_ADMIN'])) return;
    Auth.initNavbar();

    await loadAdminAnalytics();
});

async function loadAdminAnalytics() {
    try {
        const stats = await API.get('/admin/dashboard');
        if (!stats) return;

        // Render counters
        document.getElementById('metric-total-customers').textContent = stats.totalCustomers;
        document.getElementById('metric-active-customers').textContent = `${stats.totalActiveCustomers} Active (${stats.totalCustomers - stats.totalActiveCustomers} Disabled)`;

        document.getElementById('metric-total-accounts').textContent = stats.totalAccounts;
        document.getElementById('metric-active-accounts').textContent = `${stats.totalActiveAccounts} Active (${stats.totalAccounts - stats.totalActiveAccounts} Inactive/Frozen)`;

        document.getElementById('metric-total-balances').textContent = API.formatCurrency(stats.totalAccountBalances);
        document.getElementById('metric-total-tx-count').textContent = stats.totalTransactionCount;

        document.getElementById('metric-deposit-volume').textContent = API.formatCurrency(stats.totalDepositVolume);
        document.getElementById('metric-withdraw-volume').textContent = API.formatCurrency(stats.totalWithdrawalVolume);
        document.getElementById('metric-transfer-volume').textContent = API.formatCurrency(stats.totalTransferVolume);

        // Render recent activity table
        renderRecentTransactions(stats.recentTransactions || []);
    } catch (err) {
        API.showToast(err.message || 'Failed to load administrative analytics', 'danger');
    }
}

function renderRecentTransactions(transactions) {
    const tbody = document.getElementById('admin-recent-tx-tbody');
    if (transactions.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="8" class="text-center py-4 text-muted">
                    No transactions recorded in system
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
                <td class="text-truncate" style="max-width: 200px;">${tx.description || '-'}</td>
                <td class="small text-muted">${API.formatDateTime(tx.transactionDate)}</td>
                <td>${statusBadge}</td>
                <td class="text-end fw-bold">${API.formatCurrency(tx.amount)}</td>
            </tr>
        `;
    }).join('');
}
