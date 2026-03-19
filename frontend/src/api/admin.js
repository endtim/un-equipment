import request from '../utils/request'

export function getAdminUsers() {
  return request({
    url: '/api/admin/system/users',
    method: 'get'
  })
}

export function getAdminUsersPage(params) {
  return request({
    url: '/api/admin/system/users',
    method: 'get',
    params
  })
}

export function getUserAuditLogs(id, params) {
  return request({
    url: `/api/admin/system/users/${id}/audit-logs`,
    method: 'get',
    params
  })
}

export function getAdminRoles() {
  return request({
    url: '/api/admin/system/roles',
    method: 'get'
  })
}

export function getAdminRolesPage(params) {
  return request({
    url: '/api/admin/system/roles/page',
    method: 'get',
    params
  })
}

export function getAdminDepartments() {
  return request({
    url: '/api/admin/system/departments',
    method: 'get'
  })
}

export function getAdminDepartmentsPage(params) {
  return request({
    url: '/api/admin/system/departments/page',
    method: 'get',
    params
  })
}

export function createDepartment(data) {
  return request({
    url: '/api/admin/system/departments',
    method: 'post',
    data
  })
}

export function updateDepartment(id, data) {
  return request({
    url: `/api/admin/system/departments/${id}`,
    method: 'put',
    data
  })
}

export function deleteDepartment(id) {
  return request({
    url: `/api/admin/system/departments/${id}`,
    method: 'delete'
  })
}

export function createRole(data) {
  return request({
    url: '/api/admin/system/roles',
    method: 'post',
    data
  })
}

export function updateRole(id, data) {
  return request({
    url: `/api/admin/system/roles/${id}`,
    method: 'put',
    data
  })
}

export function deleteRole(id) {
  return request({
    url: `/api/admin/system/roles/${id}`,
    method: 'delete'
  })
}

export function createUser(data) {
  return request({
    url: '/api/admin/system/users',
    method: 'post',
    data
  })
}

export function updateUser(id, data) {
  return request({
    url: `/api/admin/system/users/${id}`,
    method: 'put',
    data
  })
}

export function auditUser(id, data) {
  return request({
    url: `/api/admin/system/users/${id}/audit`,
    method: 'post',
    data
  })
}

export function updateUserStatus(id, data) {
  return request({
    url: `/api/admin/system/users/${id}/status`,
    method: 'post',
    data
  })
}

export function deleteUser(id) {
  return request({
    url: `/api/admin/system/users/${id}`,
    method: 'delete'
  })
}

export function getAdminInstruments(params) {
  return request({
    url: '/api/admin/instruments',
    method: 'get',
    params
  })
}

export function getAdminInstrumentCategories() {
  return request({
    url: '/api/admin/instruments/categories',
    method: 'get'
  })
}

export function getAdminInstrumentCategoriesPage(params) {
  return request({
    url: '/api/admin/instruments/categories/page',
    method: 'get',
    params
  })
}

export function createInstrument(data) {
  return request({
    url: '/api/admin/instruments',
    method: 'post',
    data
  })
}

export function updateInstrument(id, data) {
  return request({
    url: `/api/admin/instruments/${id}`,
    method: 'put',
    data
  })
}

export function deleteInstrument(id) {
  return request({
    url: `/api/admin/instruments/${id}`,
    method: 'delete'
  })
}

export function createInstrumentCategory(data) {
  return request({
    url: '/api/admin/instruments/categories',
    method: 'post',
    data
  })
}

export function updateInstrumentCategory(id, data) {
  return request({
    url: `/api/admin/instruments/categories/${id}`,
    method: 'put',
    data
  })
}

export function deleteInstrumentCategory(id) {
  return request({
    url: `/api/admin/instruments/categories/${id}`,
    method: 'delete'
  })
}

export function createOpenRule(data) {
  return request({
    url: '/api/admin/instruments/open-rules',
    method: 'post',
    data
  })
}

export function getAdminOpenRulesPage(params) {
  return request({
    url: '/api/admin/instruments/open-rules',
    method: 'get',
    params
  })
}

export function updateOpenRule(id, data) {
  return request({
    url: `/api/admin/instruments/open-rules/${id}`,
    method: 'put',
    data
  })
}

export function deleteOpenRule(id) {
  return request({
    url: `/api/admin/instruments/open-rules/${id}`,
    method: 'delete'
  })
}

export function getAdminNoticePage(params) {
  return request({
    url: '/api/admin/content/notices',
    method: 'get',
    params
  })
}

export function getAdminHelpDocPage(params) {
  return request({
    url: '/api/admin/content/help-docs',
    method: 'get',
    params
  })
}

export function getAdminAttachments() {
  return request({
    url: '/api/admin/instruments/attachments',
    method: 'get'
  })
}

export function getAdminAttachmentsPage(params) {
  return request({
    url: '/api/admin/instruments/attachments/page',
    method: 'get',
    params
  })
}

export function createAttachment(data) {
  return request({
    url: '/api/admin/instruments/attachments',
    method: 'post',
    data
  })
}

export function updateAttachment(id, data) {
  return request({
    url: `/api/admin/instruments/attachments/${id}`,
    method: 'put',
    data
  })
}

export function deleteAttachment(id) {
  return request({
    url: `/api/admin/instruments/attachments/${id}`,
    method: 'delete'
  })
}

export function createNotice(data) {
  return request({
    url: '/api/admin/content/notices',
    method: 'post',
    data
  })
}

export function updateNotice(id, data) {
  return request({
    url: `/api/admin/content/notices/${id}`,
    method: 'put',
    data
  })
}

export function deleteNotice(id) {
  return request({
    url: `/api/admin/content/notices/${id}`,
    method: 'delete'
  })
}

export function createHelpDoc(data) {
  return request({
    url: '/api/admin/content/help-docs',
    method: 'post',
    data
  })
}

export function updateHelpDoc(id, data) {
  return request({
    url: `/api/admin/content/help-docs/${id}`,
    method: 'put',
    data
  })
}

export function deleteHelpDoc(id) {
  return request({
    url: `/api/admin/content/help-docs/${id}`,
    method: 'delete'
  })
}
