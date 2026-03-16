import request from '../utils/request'

export function getNotices(params) {
  return request({
    url: '/api/notices',
    method: 'get',
    params
  })
}

export function getNotice(id) {
  return request({
    url: `/api/notices/${id}`,
    method: 'get'
  })
}

export function getHelpDocs(params) {
  return request({
    url: '/api/help-docs',
    method: 'get',
    params
  })
}

export function getHelpDoc(id) {
  return request({
    url: `/api/help-docs/${id}`,
    method: 'get'
  })
}

export function getMessages(params) {
  return request({
    url: '/api/messages',
    method: 'get',
    params
  })
}

export function markMessageRead(id) {
  return request({
    url: `/api/messages/${id}/read`,
    method: 'post'
  })
}

export function markAllMessagesRead() {
  return request({
    url: '/api/messages/read-all',
    method: 'post'
  })
}
