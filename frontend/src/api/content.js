import request from '../utils/request'

export function getNotices() {
  return request({
    url: '/api/notices',
    method: 'get'
  })
}

export function getNotice(id) {
  return request({
    url: `/api/notices/${id}`,
    method: 'get'
  })
}

export function getHelpDocs() {
  return request({
    url: '/api/help-docs',
    method: 'get'
  })
}

export function getHelpDoc(id) {
  return request({
    url: `/api/help-docs/${id}`,
    method: 'get'
  })
}

export function getMessages() {
  return request({
    url: '/api/messages',
    method: 'get'
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
