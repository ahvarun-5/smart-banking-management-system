/**
 * Smart Banking Management System - Customer Dashboard Logic
 */
document.addEventListener('DOMContentLoaded', async () => {
    // Route Guard: customer or admin
    if (!Auth.requireAuth()) return;
    Auth.initNavbar();

    const currentUser = Auth.getUser();
    if (currentUser) {
        document.getElementById('greeting-title').textContent = `Welcome back, ${currentUser.fullName || 'Customer'}!`;
        document.getElementById('customer-email-subtitle').textContent = `Customer ID: #${currentUser.id} &bull; ${currentUser.email}`;
    }

    // Load initial dashboard state
    await loadCustomerDashboard();

    // Attach form submission handlers
    setupActionForms();
});

let myAccountsCache = [];

async function loadCustomerDashboard() {
    await Promise.all([
        loadAccounts(),
        loadRecentTransactions()
    ]);
}

/**
 * Load customer accounts from /api/accounts
 */
async function loadAccounts() {
    const container = document.getElementById('accounts-container');
    try {
        const accounts = await API.get('/accounts');
        myAccountsCache = accounts || [];

        // Update total balance display
        let total = 0;
        myAccountsCache.forEach(acc => {
            total += Number(acc.balance || 0);
        });

        document.getElementById('total-balance-display').textContent = API.formatCurrency(total);
        document.getElementById('account-count-display').textContent = myAccountsCache.length;

        // Populate modal select dropdowns
        populateAccountDropdowns(myAccountsCache);

        if (myAccountsCache.length === 0) {
            container.innerHTML = `
                <div class="col-12">
                    <div class="sb-card text-center p-5">
                        <i class="bi bi-wallet2 fs-1 text-muted mb-3"></i>
                        <h5 class="fw-bold">No Active Bank Accounts</h5>
                        <p class="text-muted">You haven't opened any bank accounts yet. Open a Savings or Current account to start simulated banking.</p>
                        <button class="btn btn-sb-accent px-4" data-bs-toggle="modal" data-bs-target="#createAccountModal">
                            <i class="bi bi-plus-circle me-1"></i>Open First Account
                        </button>
                    </div>
                </div>
            `;
            return;
        }

        container.innerHTML = myAccountsCache.map(acc => {
            const isCurrent = acc.accountType === 'CURRENT';
            const statusBadge = acc.accountStatus === 'ACTIVE' 
                ? '<span class="badge badge-active">ACTIVE</span>' 
                : acc.accountStatus === 'FROZEN' 
                    ? '<span class="badge badge-frozen">FROZEN</span>' 
                    : '<span class="badge badge-inactive">INACTIVE</span>';

            return `
                <div class="col-md-6 col-lg-4">
                    <div class="account-card ${isCurrent ? 'current' : ''}">
                        <div class="d-flex justify-content-between align-items-center mb-2">
                            <i class="bi bi-cpu chip-icon"></i>
                            <div class="d-flex align-items-center gap-2">
                                <span class="badge bg-light bg-opacity-25">${acc.accountType}</span>
                                ${statusBadge}
                            </div>
                        </div>
                        <div class="acc-number d-flex align-items-center justify-content-between">
                            <span>${acc.accountNumber}</span>
                            <button class="btn btn-sm btn-link text-white p-0 text-decoration-none" title="Copy Account Number" onclick="copyToClipboard('${acc.accountNumber}')">
                                <i class="bi bi-copy"></i>
                            </button>
                        </div>
                        <div class="small opacity-75 text-uppercase">Available Balance</div>
                        <div class="acc-balance mb-2">${API.formatCurrency(acc.balance)}</div>
                        <div class="d-flex justify-content-between align-items-center pt-2 border-top border-white border-opacity-10 small opacity-75">
                            <span>Opened: ${acc.createdAt ? acc.createdAt.split('T')[0] : 'Active'}</span>
                            <a href="/pages/transactions.html?account=${acc.accountNumber}" class="text-white text-decoration-none">
                                Ledger <i class="bi bi-arrow-right-short"></i>
                            </a>
                        </div>
                    </div>
                </div>
            `;
        }).join('');

    } catch (error) {
        container.innerHTML = `
            <div class="col-12">
                <div class="alert alert-danger">
                    <i class="bi bi-exclamation-triangle-fill me-2"></i>Failed to load accounts: ${error.message}
                </div>
            </div>
        `;
    }
}

/**
 * Load recent customer transactions
 */
async function loadRecentTransactions() {
    const tbody = document.getElementById('recent-transactions-tbody');
    try {
        const response = await API.get('/transactions?size=5&sort=transactionDate,desc');
        const transactions = (response && response.content) ? response.content : [];

        if (transactions.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="7" class="text-center py-4 text-muted">
                        <i class="bi bi-inbox fs-4 d-block mb-1"></i>
                        No transaction activity recorded yet
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

            return `
                <tr>
                    <td><code>${tx.transactionReference}</code></td>
                    <td>${typeBadge}</td>
                    <td><strong>${tx.accountNumber}</strong></td>
                    <td>${tx.relatedAccountNumber ? `<code>${tx.relatedAccountNumber}</code>` : '<span class="text-muted">-</span>'}</td>
                    <td class="text-truncate" style="max-width: 200px;">${tx.description || '-'}</td>
                    <td class="small text-muted">${API.formatDateTime(tx.transactionDate)}</td>
                    <td class="text-end ${amountClass}">${amountPrefix}${API.formatCurrency(tx.amount)}</td>
                </tr>
            `;
        }).join('');

    } catch (error) {
        tbody.innerHTML = `
            <tr>
                <td colspan="7" class="text-center text-danger py-4">
                    Error loading transactions: ${error.message}
                </td>
            </tr>
        `;
    }
}

/**
 * Populate select dropdowns for Deposit, Withdraw, Transfer modals
 */
function populateAccountDropdowns(accounts) {
    const dropdowns = document.querySelectorAll('.account-dropdown');
    dropdowns.forEach(select => {
        if (!accounts || accounts.length === 0) {
            select.innerHTML = '<option value="">No accounts available</option>';
            return;
        }
        select.innerHTML = accounts.map(acc => `
            <option value="${acc.accountNumber}" ${acc.accountStatus !== 'ACTIVE' ? 'disabled' : ''}>
                ${acc.accountNumber} (${acc.accountType} - Bal: ${API.formatCurrency(acc.balance)}) ${acc.accountStatus !== 'ACTIVE' ? `[${acc.accountStatus}]` : ''}
            </option>
        `).join('');
    });
}

/**
 * Setup forms for Deposit, Withdraw, Transfer, Create Account
 */
function setupActionForms() {
    // Deposit Form
    document.getElementById('deposit-form').addEventListener('submit', async function(e) {
        e.preventDefault();
        const accountNumber = document.getElementById('deposit-account').value;
        const amount = document.getElementById('deposit-amount').value;
        const description = document.getElementById('deposit-desc').value;

        try {
            await API.post('/transactions/deposit', {
                accountNumber,
                amount: parseFloat(amount),
                description
            });
            API.showToast(`Successfully deposited ${API.formatCurrency(amount)} into ${accountNumber}`, 'success');
            bootstrap.Modal.getInstance(document.getElementById('depositModal')).hide();
            this.reset();
            await loadCustomerDashboard();
        } catch (err) {
            API.showToast(err.message || 'Deposit failed', 'danger');
        }
    });

    // Withdraw Form
    document.getElementById('withdraw-form').addEventListener('submit', async function(e) {
        e.preventDefault();
        const accountNumber = document.getElementById('withdraw-account').value;
        const amount = document.getElementById('withdraw-amount').value;
        const description = document.getElementById('withdraw-desc').value;

        try {
            await API.post('/transactions/withdraw', {
                accountNumber,
                amount: parseFloat(amount),
                description
            });
            API.showToast(`Successfully withdrew ${API.formatCurrency(amount)} from ${accountNumber}`, 'success');
            bootstrap.Modal.getInstance(document.getElementById('withdrawModal')).hide();
            this.reset();
            await loadCustomerDashboard();
        } catch (err) {
            API.showToast(err.message || 'Withdrawal failed', 'danger');
        }
    });

    // Transfer Form
    document.getElementById('transfer-form').addEventListener('submit', async function(e) {
        e.preventDefault();
        const senderAccountNumber = document.getElementById('transfer-sender').value;
        const receiverAccountNumber = document.getElementById('transfer-receiver').value.trim();
        const amount = document.getElementById('transfer-amount').value;
        const description = document.getElementById('transfer-desc').value;

        try {
            await API.post('/transactions/transfer', {
                senderAccountNumber,
                receiverAccountNumber,
                amount: parseFloat(amount),
                description
            });
            API.showToast(`Transfer of ${API.formatCurrency(amount)} to ${receiverAccountNumber} completed!`, 'success');
            bootstrap.Modal.getInstance(document.getElementById('transferModal')).hide();
            this.reset();
            await loadCustomerDashboard();
        } catch (err) {
            API.showToast(err.message || 'Transfer failed', 'danger');
        }
    });

    // Create Account Form
    document.getElementById('create-account-form').addEventListener('submit', async function(e) {
        e.preventDefault();
        const accountType = document.getElementById('new-account-type').value;
        const initialDeposit = document.getElementById('new-account-deposit').value;

        try {
            const newAcc = await API.post('/accounts', {
                accountType,
                initialDeposit: parseFloat(initialDeposit || 0)
            });
            API.showToast(`New ${newAcc.accountType} account (${newAcc.accountNumber}) opened successfully!`, 'success');
            bootstrap.Modal.getInstance(document.getElementById('createAccountModal')).hide();
            this.reset();
            await loadCustomerDashboard();
        } catch (err) {
            API.showToast(err.message || 'Failed to create account', 'danger');
        }
    });
}

function copyToClipboard(text) {
    navigator.clipboard.writeText(text).then(() => {
        API.showToast(`Copied ${text} to clipboard!`, 'info');
    });
}
