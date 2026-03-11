import request from '../utils/request'

export function getInstruments(params) {
  return request({
    url: '/api/instruments',
    method: 'get',
    params
  })
}

export function getInstrumentDetail(id) {
  return request({
    url: `/api/instruments/${id}`,
    method: 'get'
  })
}

export function getCategories() {
  return request({
    url: '/api/instruments/categories',
    method: 'get'
  })
}

export function getInstrumentReservedSlots(id, date) {
  return request({
    url: `/api/instruments/${id}/reserved-slots`,
    method: 'get',
    params: { date }
  })
}
