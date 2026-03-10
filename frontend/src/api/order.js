import request from '../utils/request'

export function createMachineReservation(data) {
  return request({
    url: '/api/reservations/machine',
    method: 'post',
    data
  })
}

export function createSampleReservation(data) {
  return request({
    url: '/api/reservations/sample',
    method: 'post',
    data
  })
}

export function getMyOrders() {
  return request({
    url: '/api/reservations/my',
    method: 'get'
  })
}

export function getOrderDetail(id) {
  return request({
    url: `/api/reservations/${id}`,
    method: 'get'
  })
}

export function cancelOrder(id) {
  return request({
    url: `/api/reservations/${id}/cancel`,
    method: 'post'
  })
}

export function getAdminOrders() {
  return request({
    url: '/api/admin/orders',
    method: 'get'
  })
}

export function auditOrder(id, data) {
  return request({
    url: `/api/admin/orders/${id}/audit`,
    method: 'post',
    data
  })
}

export function checkInOrder(id) {
  return request({
    url: `/api/admin/orders/${id}/check-in`,
    method: 'post'
  })
}

export function receiveSampleOrder(id) {
  return request({
    url: `/api/admin/orders/${id}/receive`,
    method: 'post'
  })
}

export function finishOrder(id, data) {
  return request({
    url: `/api/admin/orders/${id}/finish`,
    method: 'post',
    data
  })
}

export function settleOrder(id) {
  return request({
    url: `/api/admin/orders/${id}/settle`,
    method: 'post'
  })
}

export function uploadAdminOrderResult(id, data) {
  return request({
    url: `/api/admin/orders/${id}/result`,
    method: 'post',
    data
  })
}
