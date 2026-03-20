import request from '../utils/request'

export function getMyAccount() {
  return request({
    url: '/api/account/me',
    method: 'get'
  })
}

export function submitRecharge(data) {
  return request({
    url: '/api/recharges',
    method: 'post',
    data
  })
}

export function getMyTransactions(params) {
  return request({
    url: '/api/account/transactions',
    method: 'get',
    params
  })
}

export function getMyRecharges(params) {
  return request({
    url: '/api/account/recharges',
    method: 'get',
    params
  })
}

export function getRechargeOrdersPage(params) {
  return request({
    url: '/api/admin/finance/recharges/page',
    method: 'get',
    params
  })
}

export function getReconciliationOverview(params) {
  return request({
    url: '/api/admin/finance/reconciliation/overview',
    method: 'get',
    params
  })
}

export function getReconciliationAnomalies(params) {
  return request({
    url: '/api/admin/finance/reconciliation/anomalies',
    method: 'get',
    params
  })
}

export function handleReconciliationAnomaly(data) {
  return request({
    url: '/api/admin/finance/reconciliation/anomalies/handle',
    method: 'post',
    data
  })
}

export function exportRechargeOrders(params) {
  return request({
    url: '/api/admin/finance/recharges/export',
    method: 'get',
    params,
    responseType: 'blob',
    rawResponse: true
  })
}

export function auditRecharge(id, data) {
  return request({
    url: `/api/admin/finance/recharges/${id}/audit`,
    method: 'post',
    data
  })
}

export function getFinanceDetailsPage(params) {
  return request({
    url: '/api/admin/finance/details',
    method: 'get',
    params
  })
}

export function exportFinanceDetails(params) {
  return request({
    url: '/api/admin/finance/details/export',
    method: 'get',
    params,
    responseType: 'blob',
    rawResponse: true
  })
}

export function createFinanceExpense(data) {
  return request({
    url: '/api/admin/finance/expenses',
    method: 'post',
    data
  })
}

export function getFinanceBudgetsPage(params) {
  return request({
    url: '/api/admin/finance/budgets',
    method: 'get',
    params
  })
}

export function saveFinanceBudget(data) {
  return request({
    url: '/api/admin/finance/budgets',
    method: 'post',
    data
  })
}

export function getFinanceBudgetWarnings(params) {
  return request({
    url: '/api/admin/finance/budget-warnings',
    method: 'get',
    params
  })
}
