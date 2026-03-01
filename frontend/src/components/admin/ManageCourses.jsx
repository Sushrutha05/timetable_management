import React, { useState, useEffect, useRef } from 'react';
import { courseAPI } from '../../utils/api';
import courseTemplate from '../common/course_template.csv';

const ManageCourses = ({ deptId }) => {
  const [courses, setCourses] = useState([]);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState({ type: '', text: '' });
  const [bulkMessage, setBulkMessage] = useState({ type: '', text: '' });
  const [showForm, setShowForm] = useState(false);

  // Semester filter for list
  const [semesterFilter, setSemesterFilter] = useState('');

  const [formData, setFormData] = useState({
    courseCode: '',
    courseName: '',
    creditHours: '',
    semester: '1',
    courseType: 'THEORY',
    lectureHours: '',
    tutorialHours: '',
    practicalHours: '',
  });

  const [bulkFile, setBulkFile] = useState(null);
  const [bulkUploading, setBulkUploading] = useState(false);
  const fileInputRef = useRef(null);

  const [editingId, setEditingId] = useState(null);
  const [editData, setEditData] = useState({
    courseCode: '',
    courseName: '',
    creditHours: '',
    semester: '1',
    courseType: 'THEORY',
    lectureHours: '',
    tutorialHours: '',
    practicalHours: '',
  });

  const [saving, setSaving] = useState(false);
  const [deletingId, setDeletingId] = useState(null);

  useEffect(() => {
    loadCourses();
  }, [deptId, semesterFilter]);

  const loadCourses = async () => {
    setLoading(true);
    try {
      // Pass deptId and semester filter
      const data = await courseAPI.getAll(deptId, semesterFilter || null);
      setCourses(data);
      setMessage({ type: '', text: '' });
    } catch (error) {
      setMessage({ type: 'error', text: `Error loading courses: ${error.message}` });
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setMessage({ type: '', text: '' });

    try {
      const payload = {
        ...formData,
        creditHours: parseInt(formData.creditHours),
        semester: parseInt(formData.semester),
        lectureHours: formData.lectureHours !== '' ? parseInt(formData.lectureHours) : 0,
        tutorialHours: formData.tutorialHours !== '' ? parseInt(formData.tutorialHours) : 0,
        practicalHours: formData.practicalHours !== '' ? parseInt(formData.practicalHours) : 0,
        departmentId: deptId,
      };

      await courseAPI.create(payload);
      setMessage({ type: 'success', text: 'Course created successfully!' });
      setShowForm(false);
      setFormData({ courseCode: '', courseName: '', creditHours: '', semester: '1', courseType: 'THEORY', lectureHours: '', tutorialHours: '', practicalHours: '' });
      loadCourses();
    } catch (error) {
      setMessage({ type: 'error', text: `Error: ${error.message}` });
    } finally {
      setLoading(false);
    }
  };

  const handleBulkUpload = async (e) => {
    e.preventDefault();

    if (!bulkFile) {
      setBulkMessage({ type: 'error', text: 'Please select a CSV file before uploading.' });
      return;
    }

    if (!deptId) {
      setBulkMessage({ type: 'error', text: 'Department context is missing.' });
      return;
    }

    setBulkUploading(true);
    setBulkMessage({ type: '', text: '' });

    try {
      const formDataUpload = new FormData();
      formDataUpload.append('file', bulkFile);
      // deptId passed as second arg
      const result = await courseAPI.bulkUpload(formDataUpload, deptId);
      const count = Array.isArray(result) ? result.length : 0;
      setBulkMessage({
        type: 'success',
        text: `Successfully imported ${count} course${count === 1 ? '' : 's'}.`,
      });
      setBulkFile(null);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
      loadCourses();
    } catch (error) {
      setBulkMessage({ type: 'error', text: `Bulk upload failed: ${error.message}` });
    } finally {
      setBulkUploading(false);
    }
  };

  // ---- Edit/Delete Handlers ----
  const startEdit = (item) => {
    const course = item.course || item;
    const semester = item.semester || 1;
    const courseId = course.id;

    setEditingId(courseId);
    setEditData({
      courseCode: course.courseCode || '',
      courseName: course.courseName || '',
      creditHours: String(course.creditHours ?? ''),
      semester: String(semester),
      courseType: course.courseType || 'THEORY',
      lectureHours: String(course.lectureHours ?? ''),
      tutorialHours: String(course.tutorialHours ?? ''),
      practicalHours: String(course.practicalHours ?? ''),
    });
    setMessage({ type: '', text: '' });
  };

  const cancelEdit = () => {
    setEditingId(null);
    setEditData({ courseCode: '', courseName: '', creditHours: '', semester: '1', courseType: 'THEORY', lectureHours: '', tutorialHours: '', practicalHours: '' });
  };

  const handleEditChange = (field, value) => {
    setEditData((prev) => ({ ...prev, [field]: value }));
  };

  const handleEditSave = async () => {
    if (!editingId) return;
    setSaving(true);
    try {
      const payload = {
        ...editData,
        creditHours: parseInt(editData.creditHours),
        semester: parseInt(editData.semester),
        lectureHours: editData.lectureHours !== '' ? parseInt(editData.lectureHours) : 0,
        tutorialHours: editData.tutorialHours !== '' ? parseInt(editData.tutorialHours) : 0,
        practicalHours: editData.practicalHours !== '' ? parseInt(editData.practicalHours) : 0,
        departmentId: deptId,
      };
      await courseAPI.update(editingId, payload);
      setSaving(false);
      setEditingId(null);
      setMessage({ type: 'success', text: 'Course updated successfully!' });
      loadCourses();
    } catch (error) {
      setSaving(false);
      setMessage({ type: 'error', text: `Update failed: ${error.message}` });
    }
  };

  const handleDelete = async (item) => {
    const id = item.course ? item.course.id : item.id;
    if (!window.confirm('Delete this course?')) return;
    setDeletingId(id);
    try {
      await courseAPI.delete(id);
      loadCourses();
    } catch (error) {
      setMessage({ type: 'error', text: `Delete failed: ${error.message}` });
    } finally {
      setDeletingId(null);
    }
  };

  // Helper to extract display data
  const getDisplayData = (item) => {
    if (item.course) {
      return {
        id: item.course.id,
        courseCode: item.course.courseCode,
        courseName: item.course.courseName,
        creditHours: item.course.creditHours,
        courseType: item.course.courseType || '-',
        lectureHours: item.course.lectureHours ?? 0,
        tutorialHours: item.course.tutorialHours ?? 0,
        practicalHours: item.course.practicalHours ?? 0,
        semester: item.semester,
      };
    } else {
      return {
        id: item.id,
        courseCode: item.courseCode,
        courseName: item.courseName,
        creditHours: item.creditHours,
        courseType: item.courseType || '-',
        lectureHours: item.lectureHours ?? 0,
        tutorialHours: item.tutorialHours ?? 0,
        practicalHours: item.practicalHours ?? 0,
        semester: '-',
      };
    }
  };

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-2xl font-bold text-gray-900 dark:text-white">Manage Courses</h2>
        <div className="flex gap-4">
          {/* Semester Filter */}
          <select
            value={semesterFilter}
            onChange={(e) => setSemesterFilter(e.target.value)}
            className="px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
          >
            <option value="">All Semesters</option>
            {[1, 2, 3, 4, 5, 6, 7, 8].map(sem => (
              <option key={sem} value={sem}>Semester {sem}</option>
            ))}
          </select>

          <button
            onClick={() => setShowForm(!showForm)}
            className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-md transition-colors"
          >
            {showForm ? 'Cancel' : 'Add New Course'}
          </button>
        </div>
      </div>

      {message.text && (
        <div
          className={`mb-4 p-3 rounded-md ${message.type === 'success'
            ? 'bg-green-100 dark:bg-green-900 text-green-700 dark:text-green-300'
            : 'bg-red-100 dark:bg-red-900 text-red-700 dark:text-red-300'
            }`}
        >
          {message.text}
        </div>
      )}

      {showForm && (
        <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow-md mb-6">
          <h3 className="text-lg font-semibold mb-4 text-gray-900 dark:text-white">Create New Course</h3>
          <form onSubmit={handleSubmit}>
            <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Course Code <span className="text-red-500">*</span>
                </label>
                <input
                  type="text" required value={formData.courseCode}
                  onChange={(e) => setFormData({ ...formData, courseCode: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
                />
              </div>
              <div className="col-span-2">
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Course Name <span className="text-red-500">*</span>
                </label>
                <input
                  type="text" required value={formData.courseName}
                  onChange={(e) => setFormData({ ...formData, courseName: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Semester <span className="text-red-500">*</span>
                </label>
                <select required value={formData.semester}
                  onChange={(e) => setFormData({ ...formData, semester: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
                >
                  {[1, 2, 3, 4, 5, 6, 7, 8].map(sem => (
                    <option key={sem} value={sem}>{sem}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Course Type <span className="text-red-500">*</span>
                </label>
                <select required value={formData.courseType}
                  onChange={(e) => setFormData({ ...formData, courseType: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
                >
                  <option value="THEORY">Theory</option>
                  <option value="LAB">Lab</option>
                  <option value="TUTORIAL">Tutorial</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Credit Hours <span className="text-red-500">*</span></label>
                <input type="number" required min="1" value={formData.creditHours}
                  onChange={(e) => setFormData({ ...formData, creditHours: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Lecture Hrs (L)</label>
                <input type="number" min="0" value={formData.lectureHours}
                  onChange={(e) => setFormData({ ...formData, lectureHours: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Tutorial Hrs (T)</label>
                <input type="number" min="0" value={formData.tutorialHours}
                  onChange={(e) => setFormData({ ...formData, tutorialHours: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Practical Hrs (P)</label>
                <input type="number" min="0" value={formData.practicalHours}
                  onChange={(e) => setFormData({ ...formData, practicalHours: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
                />
              </div>
            </div>
            <button
              type="submit"
              disabled={loading}
              className="mt-4 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-md transition-colors disabled:opacity-50"
            >
              {loading ? 'Creating...' : 'Create Course'}
            </button>
          </form>
        </div>
      )}

      {/* Bulk Upload Section */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
        <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow-md">
          <h3 className="text-lg font-semibold mb-2 text-gray-900 dark:text-white">Bulk Import Courses via CSV</h3>
          <p className="text-sm text-gray-600 dark:text-gray-300 mb-4">
            Upload a CSV file with the required headers.
          </p>
          {bulkMessage.text && (
            <div
              className={`mb-4 p-3 rounded-md ${bulkMessage.type === 'success'
                ? 'bg-green-100 dark:bg-green-900 text-green-700 dark:text-green-300'
                : 'bg-red-100 dark:bg-red-900 text-red-700 dark:text-red-300'
                }`}
            >
              {bulkMessage.text}
            </div>
          )}

          <form onSubmit={handleBulkUpload}>
            <div className="flex flex-col gap-4">
              <input
                type="file"
                accept=".csv"
                ref={fileInputRef}
                onChange={(e) => {
                  const file = e.target.files?.[0] || null;
                  setBulkFile(file);
                }}
                className="block w-full text-sm text-gray-900 dark:text-gray-200 border border-gray-300 dark:border-gray-600 rounded-md cursor-pointer bg-gray-50 dark:bg-gray-700 focus:outline-none"
              />
              <div className="flex items-center justify-between text-sm text-gray-600 dark:text-gray-300 gap-2 flex-wrap">
                <a
                  href={courseTemplate}
                  download="course_template.csv"
                  className="text-blue-600 dark:text-blue-400 hover:underline"
                >
                  Download template
                </a>
                <span>Headers: courseCode, courseName, creditHours, semester (optional)</span>
              </div>
              <button
                type="submit"
                disabled={bulkUploading}
                className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-md transition-colors disabled:opacity-50"
              >
                {bulkUploading ? 'Uploading...' : 'Upload CSV'}
              </button>
            </div>
          </form>
        </div>
        <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow-md">
          <h3 className="text-lg font-semibold mb-2 text-gray-900 dark:text-white">CSV Format Tips</h3>
          <ul className="list-disc list-inside text-sm text-gray-600 dark:text-gray-300 space-y-2">
            <li>Include the header row exactly as shown.</li>
            <li>`creditHours` must be a whole number.</li>
            <li>Course codes must be unique.</li>
            <li>Semester will be set to default (1) or handled by backend logic if column missing.</li>
          </ul>
        </div>
      </div>

      {editingId && (
        <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow-md mb-6">
          <h3 className="text-lg font-semibold mb-4 text-gray-900 dark:text-white">Edit Course</h3>
          <form onSubmit={(e) => { e.preventDefault(); handleEditSave(); }}>
            <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Course Code</label>
                <input type="text" required value={editData.courseCode}
                  onChange={(e) => handleEditChange('courseCode', e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
                />
              </div>
              <div className="col-span-2">
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Course Name</label>
                <input type="text" required value={editData.courseName}
                  onChange={(e) => handleEditChange('courseName', e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Semester</label>
                <select required value={editData.semester}
                  onChange={(e) => handleEditChange('semester', e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
                >
                  {[1, 2, 3, 4, 5, 6, 7, 8].map(sem => (
                    <option key={sem} value={sem}>{sem}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Course Type</label>
                <select required value={editData.courseType}
                  onChange={(e) => handleEditChange('courseType', e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
                >
                  <option value="THEORY">Theory</option>
                  <option value="LAB">Lab</option>
                  <option value="TUTORIAL">Tutorial</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Credit Hours</label>
                <input type="number" required min="1" value={editData.creditHours}
                  onChange={(e) => handleEditChange('creditHours', e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Lecture Hrs (L)</label>
                <input type="number" min="0" value={editData.lectureHours}
                  onChange={(e) => handleEditChange('lectureHours', e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Tutorial Hrs (T)</label>
                <input type="number" min="0" value={editData.tutorialHours}
                  onChange={(e) => handleEditChange('tutorialHours', e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Practical Hrs (P)</label>
                <input type="number" min="0" value={editData.practicalHours}
                  onChange={(e) => handleEditChange('practicalHours', e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
                />
              </div>
            </div>
            <div className="mt-4 flex gap-3">
              <button type="submit" disabled={saving}
                className="px-4 py-2 bg-green-600 hover:bg-green-700 text-white rounded-md disabled:opacity-50">
                {saving ? 'Saving...' : 'Save Changes'}
              </button>
              <button type="button" onClick={cancelEdit}
                className="px-4 py-2 bg-gray-500 hover:bg-gray-600 text-white rounded-md">
                Cancel
              </button>
            </div>
          </form>
        </div>
      )}

      <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md overflow-hidden">
        <div className="p-4 border-b border-gray-200 dark:border-gray-700">
          <h3 className="text-lg font-semibold text-gray-900 dark:text-white">Courses List</h3>
        </div>
        {loading && !showForm ? (
          <div className="p-4 text-center text-gray-600 dark:text-gray-400">Loading...</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-50 dark:bg-gray-700">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">ID</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">Code</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">Name</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">Sem</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">Type</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">Credits</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">L-T-P</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                {courses.length === 0 ? (
                  <tr>
                    <td colSpan="6" className="px-4 py-4 text-center text-gray-500 dark:text-gray-400">
                      No courses found
                    </td>
                  </tr>
                ) : (
                  courses.map((item) => {
                    const data = getDisplayData(item);
                    return (
                      <tr key={data.id} className="hover:bg-gray-50 dark:hover:bg-gray-700">
                        <td className="px-4 py-3 text-sm text-gray-900 dark:text-white">{data.id}</td>
                        <td className="px-4 py-3 text-sm font-mono text-gray-900 dark:text-white">{data.courseCode}</td>
                        <td className="px-4 py-3 text-sm text-gray-900 dark:text-white">{data.courseName}</td>
                        <td className="px-4 py-3 text-sm text-gray-900 dark:text-white">{data.semester}</td>
                        <td className="px-4 py-3 text-sm text-gray-900 dark:text-white">
                          <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${data.courseType === 'LAB'
                              ? 'bg-purple-100 text-purple-700 dark:bg-purple-900 dark:text-purple-200'
                              : data.courseType === 'TUTORIAL'
                                ? 'bg-blue-100 text-blue-700 dark:bg-blue-900 dark:text-blue-200'
                                : 'bg-green-100 text-green-700 dark:bg-green-900 dark:text-green-200'
                            }`}>{data.courseType}</span>
                        </td>
                        <td className="px-4 py-3 text-sm text-gray-900 dark:text-white">{data.creditHours}</td>
                        <td className="px-4 py-3 text-sm text-gray-500 dark:text-gray-400">{data.lectureHours}-{data.tutorialHours}-{data.practicalHours}</td>
                        <td className="px-4 py-3 text-sm">
                          <div className="flex gap-2">
                            <button
                              onClick={() => startEdit(item)}
                              className="px-3 py-1 bg-yellow-500 hover:bg-yellow-600 text-white rounded-md"
                            >
                              Edit
                            </button>
                            <button
                              onClick={() => handleDelete(item)}
                              disabled={deletingId === data.id}
                              className="px-3 py-1 bg-red-600 hover:bg-red-700 text-white rounded-md disabled:opacity-60"
                            >
                              {deletingId === data.id ? 'Deleting...' : 'Delete'}
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};

export default ManageCourses;

