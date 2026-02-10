export const API_BASE_URL = 'http://localhost:8080';

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
  getAll: (deptId) => {
    const params = new URLSearchParams();
    if (deptId) params.append('deptId', deptId);
    return fetchAPI(`/api/admin/faculty?${params.toString()}`);
  },
  getById: (id) => fetchAPI(`/api/admin/faculty/${id}`),
  create: (data) => fetchAPI('/api/admin/faculty', {
    method: 'POST',
    body: JSON.stringify(data),
  }),
  update: (id, data) => fetchAPI(`/api/admin/faculty/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  }),
  delete: (id) => fetchAPI(`/api/admin/faculty/${id}`, {
    method: 'DELETE',
  }),
  bulkUpload: (formData, deptId) => {
    return fetchAPI(`/api/admin/faculty/upload?deptId=${deptId}`, {
      method: 'POST',
      body: formData,
    });
  },
};

// Admin APIs - Courses
export const courseAPI = {
  getAll: (deptId, semester) => {
    const params = new URLSearchParams();
    if (deptId) params.append('deptId', deptId);
    if (semester) params.append('semester', semester);
    return fetchAPI(`/api/admin/course?${params.toString()}`);
  },
  getById: (id) => fetchAPI(`/api/admin/course/${id}`),
  create: (data) => fetchAPI('/api/admin/course', {
    method: 'POST',
    body: JSON.stringify(data),
  }),
  update: (id, data) => fetchAPI(`/api/admin/course/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  }),
  delete: (id) => fetchAPI(`/api/admin/course/${id}`, {
    method: 'DELETE',
  }),
  bulkUpload: (formData, deptId) => {
    // Append deptId to URL or FormData? Controller expects Query Param.
    return fetchAPI(`/api/admin/course/upload?deptId=${deptId}`, {
      method: 'POST',
      body: formData,
    });
  },
};

// Admin APIs - Rooms
export const roomAPI = {
  getAll: () => fetchAPI('/api/admin/room'),
  getById: (id) => fetchAPI(`/api/admin/room/${id}`),
  create: (data) => fetchAPI('/api/admin/room', {
    method: 'POST',
    body: JSON.stringify(data),
  }),
  update: (id, data) => fetchAPI(`/api/admin/room/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  }),
  delete: (id) => fetchAPI(`/api/admin/room/${id}`, {
    method: 'DELETE',
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
  update: (id, data) => fetchAPI(`/api/admin/section/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  }),
  delete: (id) => fetchAPI(`/api/admin/section/${id}`, {
    method: 'DELETE',
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
  update: (id, data) => fetchAPI(`/api/admin/timeslot/${id}`, {
    method: 'PUT',
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

// Admin APIs - Departments
export const departmentAPI = {
  getAll: () => fetchAPI('/api/admin/department'),
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
  getCoursesBySemester: (deptId, semester) => fetchAPI(`/api/faculty/courses/department/${deptId}/semester/${semester}`),
  setPreferences: (facultyId, data) => fetchAPI(`/api/faculty/${facultyId}/preferences`, {
    method: 'POST',
    body: JSON.stringify(data),
  }),
  getTimetable: (facultyId) => fetchAPI(`/api/timetable/faculty/${facultyId}`),
};

// Auth APIs
export const authAPI = {
  login: (credentials) => fetchAPI('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(credentials),
  }),
};


