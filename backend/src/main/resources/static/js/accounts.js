/**
 * Smart Banking Management System - Customer Accounts Logic
 */
document.addEventListener('DOMContentLoaded', async () => {
    if (!Auth.requireAuth()) return;
    Auth.initNavbar();

    await loadAccountsPage();

    document.getElementById('create-account-form').addEventListener('submit', async function(e) {
        e.preventDefault();
        const accountType = document.getElementById('account-type-select').value;
        const initialDeposit = document.getElementById('initial-deposit-input').value;

        try {
            const newAcc = await API.post('/accounts', {
                accountType,
                initialDeposit: parseFloat(initialDeposit || 0)
            });
            API.showToast(`New ${newAcc.accountType} account (${newAcc.accountNumber}) opened!`, 'success');
            bootstrap.Modal.getInstance(document.getElementById('createAccountModal')).hide();
            this.reset();
            await loadAccountsPage();
        } catch (err) {
            API.showToast(err.message || 'Failed to open account', 'danger');
        }
    });
});

async function loadAccountsPage() {
    const grid = document.getElementById('accounts-grid');
    try {
        const accounts = await API.get('/accounts');

        let total = 0;
        (accounts || []).forEach(acc => total += Number(acc.balance || 0));

        document.getElementById('total-balance-display').textContent = API.formatCurrency(total);
        document.getElementById('total-accounts-badge').textContent = `${(accounts || []).length} Accounts Active`;

        if (!accounts || accounts.length === 0) {
            grid.innerHTML = `
                <div class="col-12">
                    <div class="sb-card text-center p-5">
                        <i class="bi bi-credit-card-2-front fs-1 text-muted mb-3"></i>
                        <h4 class="fw-bold">No Bank Accounts Found</h4>
                        <p class="text-muted">You do not currently have any active bank accounts. Click below to open an account.</p>
                        <button class="btn btn-sb-accent px-4" data-bs-toggle="modal" data-bs-target="#createAccountModal">
                            <i class="bi bi-plus-circle me-1"></i>Open Account
                        </button>
                    </div>
                </div>
            `;
            return;
        }

        grid.innerHTML = accounts.map(acc => {
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
                            <button class="btn btn-sm btn-link text-white p-0 text-decoration-none" title="Copy Number" onclick="copyToClipboard('${acc.accountNumber}')">
                                <i class="bi bi-copy"></i>
                            </button>
                        </div>
                        <div class="small opacity-75 text-uppercase">Balance</div>
                        <div class="acc-balance mb-3">${API.formatCurrency(acc.balance)}</div>
                        <div class="d-flex justify-content-between align-items-center pt-2 border-top border-white border-opacity-10 small">
                            <span class="opacity-75">Owner: ${acc.ownerName || 'You'}</span>
                            <a href="/pages/transactions.html?account=${acc.accountNumber}" class="btn btn-light btn-sm text-dark px-2 py-0 fw-semibold">
                                View Ledger <i class="bi bi-arrow-right"></i>
                            </a>
                        </div>
                    </div>
                </div>
            `;
        }).join('');

    } catch (err) {
        grid.innerHTML = `
            <div class="col-12">
                <div class="alert alert-danger">Error: ${err.message}</div>
            </div>
        `;
    }
}

function copyToClipboard(text) {
    navigator.clipboard.writeText(text).then(() => {
        API.showToast(`Copied ${text} to clipboard!`, 'info');
    });
}
