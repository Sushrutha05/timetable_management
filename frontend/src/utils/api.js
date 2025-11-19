const API_BASE_URL = 'http://localhost:8080';

// Generic fetch wrapper
async function fetchAPI(endpoint, options = {}) {
  const isFormData = options.body instanceof FormData;
  const defaultHeaders = isFormData ? {} : { 'Content-Type': 'application/json' };

  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    headers: {
      ...defaultHeaders,
      ...options.headers,
    },
    ...options,
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(errorText || `HTTP error! status: ${response.status}`);
  }

  // Handle 204 No Content
  if (response.status === 204) {
    return null;
  }

  return response.json();
}

// Admin APIs - Faculty
export const facultyAPI = {
  getAll: () => fetchAPI('/api/admin/faculty'),
  getById: (id) => fetchAPI(`/api/admin/faculty/${id}`),
  create: (data) => fetchAPI('/api/admin/faculty', {
    method: 'POST',
    body: JSON.stringify(data),
  }),
  bulkUpload: (formData) => fetchAPI('/api/admin/faculty/upload', {
    method: 'POST',
    body: formData,
  }),
};

// Admin APIs - Courses
export const courseAPI = {
  getAll: () => fetchAPI('/api/admin/course'),
  getById: (id) => fetchAPI(`/api/admin/course/${id}`),
  create: (data) => fetchAPI('/api/admin/course', {
    method: 'POST',
    body: JSON.stringify(data),
  }),
  bulkUpload: (formData) => fetchAPI('/api/admin/course/upload', {
    method: 'POST',
    body: formData,
  }),
};

// Admin APIs - Rooms
export const roomAPI = {
  getAll: () => fetchAPI('/api/admin/room'),
  getById: (id) => fetchAPI(`/api/admin/room/${id}`),
  create: (data) => fetchAPI('/api/admin/room', {
    method: 'POST',
    body: JSON.stringify(data),
  }),
  bulkUpload: (formData) => fetchAPI('/api/admin/room/upload', {
    method: 'POST',
    body: formData,
  }),
};

// Admin APIs - Sections
export const sectionAPI = {
  getAll: () => fetchAPI('/api/admin/section'),
  getById: (id) => fetchAPI(`/api/admin/section/${id}`),
  create: (data) => fetchAPI('/api/admin/section', {
    method: 'POST',
    body: JSON.stringify(data),
  }),
  bulkUpload: (formData) => fetchAPI('/api/admin/section/upload', {
    method: 'POST',
    body: formData,
  }),
};

// Admin APIs - Designations
export const designationAPI = {
  getAll: () => fetchAPI('/api/admin/designation'),
  getByName: (name) => fetchAPI(`/api/admin/designation/${name}`),
  create: (data) => fetchAPI('/api/admin/designation', {
    method: 'POST',
    body: JSON.stringify(data),
  }),
  delete: (name) => fetchAPI(`/api/admin/designation/${name}`, {
    method: 'DELETE',
  }),
};

// Admin APIs - Time Slots
export const timeSlotAPI = {
  getAll: () => fetchAPI('/api/admin/timeslot'),
  create: (data) => fetchAPI('/api/admin/timeslot', {
    method: 'POST',
    body: JSON.stringify(data),
  }),
  delete: (id) => fetchAPI(`/api/admin/timeslot/${id}`, {
    method: 'DELETE',
  }),
};

// Admin APIs - Course Offerings
export const offeringAPI = {
  getAll: () => fetchAPI('/api/admin/offering'),
  create: (data) => fetchAPI('/api/admin/offering', {
    method: 'POST',
    body: JSON.stringify(data),
  }),
  delete: (id) => fetchAPI(`/api/admin/offering/${id}`, {
    method: 'DELETE',
  }),
};

// Admin APIs - Timetable
export const timetableAPI = {
  generate: () => fetchAPI('/api/admin/timetable/generate', {
    method: 'POST',
  }),
  getFull: () => fetchAPI('/api/admin/timetable'),
};

// Faculty APIs
export const facultyPreferenceAPI = {
  getPreferences: (facultyId) => fetchAPI(`/api/faculty/${facultyId}/preferences`),
  setPreferences: (facultyId, data) => fetchAPI(`/api/faculty/${facultyId}/preferences`, {
    method: 'POST',
    body: JSON.stringify(data),
  }),
  getTimetable: (facultyId) => fetchAPI(`/api/faculty/${facultyId}/timetable`),
};

