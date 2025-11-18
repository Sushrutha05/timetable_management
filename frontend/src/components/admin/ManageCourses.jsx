import React, { useState, useEffect } from 'react';
import { courseAPI } from '../../utils/api';

const ManageCourses = () => {
  const [courses, setCourses] = useState([]);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState({ type: '', text: '' });
  const [showForm, setShowForm] = useState(false);
  const [formData, setFormData] = useState({
    courseCode: '',
    courseName: '',
    creditHours: '',
  });

  useEffect(() => {
    loadCourses();
  }, []);

  const loadCourses = async () => {
    setLoading(true);
    try {
      const data = await courseAPI.getAll();
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
      };
      await courseAPI.create(payload);
      setMessage({ type: 'success', text: 'Course created successfully!' });
      setShowForm(false);
      setFormData({ courseCode: '', courseName: '', creditHours: '' });
      loadCourses();
    } catch (error) {
      setMessage({ type: 'error', text: `Error: ${error.message}` });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-2xl font-bold text-gray-900 dark:text-white">Manage Courses</h2>
        <button
          onClick={() => setShowForm(!showForm)}
          className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-md transition-colors"
        >
          {showForm ? 'Cancel' : 'Add New Course'}
        </button>
      </div>

      {message.text && (
        <div
          className={`mb-4 p-3 rounded-md ${
            message.type === 'success'
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
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Course Code <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  required
                  value={formData.courseCode}
                  onChange={(e) => setFormData({ ...formData, courseCode: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Course Name <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  required
                  value={formData.courseName}
                  onChange={(e) => setFormData({ ...formData, courseName: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Credit Hours <span className="text-red-500">*</span>
                </label>
                <input
                  type="number"
                  required
                  min="1"
                  value={formData.creditHours}
                  onChange={(e) => setFormData({ ...formData, creditHours: e.target.value })}
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
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">Course Code</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">Course Name</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">Credit Hours</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                {courses.length === 0 ? (
                  <tr>
                    <td colSpan="4" className="px-4 py-4 text-center text-gray-500 dark:text-gray-400">
                      No courses found
                    </td>
                  </tr>
                ) : (
                  courses.map((course) => (
                    <tr key={course.id} className="hover:bg-gray-50 dark:hover:bg-gray-700">
                      <td className="px-4 py-3 text-sm text-gray-900 dark:text-white">{course.id}</td>
                      <td className="px-4 py-3 text-sm text-gray-900 dark:text-white">{course.courseCode}</td>
                      <td className="px-4 py-3 text-sm text-gray-900 dark:text-white">{course.courseName}</td>
                      <td className="px-4 py-3 text-sm text-gray-900 dark:text-white">{course.creditHours}</td>
                    </tr>
                  ))
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

