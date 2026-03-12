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

export function getRechargeOrders() {
  return request({
    url: '/api/admin/finance/recharges',
    method: 'get'
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
