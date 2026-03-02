import React, { useState, useEffect } from 'react';
import { offeringAPI, facultyAPI, courseAPI, sectionAPI } from '../../utils/api';

const ManageOfferings = ({ deptId }) => {
  const [offerings, setOfferings] = useState([]);
  const [facultyList, setFacultyList] = useState([]);
  const [coursesList, setCoursesList] = useState([]);
  const [sectionsList, setSectionsList] = useState([]);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState({ type: '', text: '' });
  const [showForm, setShowForm] = useState(false);
  const [formData, setFormData] = useState({
    courseId: '',
    facultyId: '',
    sectionId: '',
  });
  const [autoGenerating, setAutoGenerating] = useState(false);
  const [autoMessage, setAutoMessage] = useState({ type: '', text: '' });

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    setLoading(true);
    try {
      const [offeringsData, facultyData, coursesData, sectionsData] = await Promise.all([
        offeringAPI.getAll(),
        facultyAPI.getAll(),
        courseAPI.getAll(),
        sectionAPI.getAll(),
      ]);
      setOfferings(offeringsData);
      setFacultyList(facultyData);
      setCoursesList(coursesData);
      setSectionsList(sectionsData);
      setMessage({ type: '', text: '' });
    } catch (error) {
      setMessage({ type: 'error', text: `Error loading data: ${error.message}` });
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
        courseId: parseInt(formData.courseId),
        facultyId: parseInt(formData.facultyId),
        sectionId: parseInt(formData.sectionId),
      };
      await offeringAPI.create(payload);
      setMessage({ type: 'success', text: 'Course offering created successfully!' });
      setShowForm(false);
      setFormData({ courseId: '', facultyId: '', sectionId: '' });
      loadData();
    } catch (error) {
      setMessage({ type: 'error', text: `Error: ${error.message}` });
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this offering?')) {
      return;
    }

    setLoading(true);
    setMessage({ type: '', text: '' });

    try {
      await offeringAPI.delete(id);
      setMessage({ type: 'success', text: 'Offering deleted successfully!' });
      loadData();
    } catch (error) {
      setMessage({ type: 'error', text: `Error: ${error.message}` });
    } finally {
      setLoading(false);
    }
  };

  const handleAutoGenerate = async () => {
    // Determine department: use prop first, then derive from first section available
    const targetDeptId = deptId
      || (sectionsList.length > 0 ? sectionsList[0].department?.id : null);

    if (!targetDeptId) {
      setAutoMessage({ type: 'error', text: 'No department available. Add sections first.' });
      return;
    }

    if (!window.confirm(
      'Auto-generate will create offerings for all (section × course) pairs that are not ' +
      'yet assigned, using faculty preferences to pick the best teacher. Continue?'
    )) return;

    setAutoGenerating(true);
    setAutoMessage({ type: '', text: '' });
    try {
      const result = await offeringAPI.autoGenerate(targetDeptId);
      setAutoMessage({
        type: 'success',
        text: `Done! Created ${result.created} offering(s), skipped ${result.skipped} (already existed or no faculty).`,
      });
      loadData();
    } catch (error) {
      setAutoMessage({ type: 'error', text: `Auto-generate failed: ${error.message}` });
    } finally {
      setAutoGenerating(false);
    }
  };

  return (
    <div>
      <div className="flex flex-wrap justify-between items-center mb-6 gap-2">
        <h2 className="text-2xl font-bold text-gray-900 dark:text-white">Manage Course Offerings</h2>
        <div className="flex gap-2">
          <button
            onClick={handleAutoGenerate}
            disabled={autoGenerating}
            title="Auto-assign faculty to every unassigned section × course pair using preferences"
            className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 disabled:opacity-50 text-white rounded-md transition-colors"
          >
            {autoGenerating ? '⏳ Generating…' : '⚡ Auto-Generate Offerings'}
          </button>
          <button
            onClick={() => setShowForm(!showForm)}
            className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-md transition-colors"
          >
            {showForm ? 'Cancel' : 'Add New Offering'}
          </button>
        </div>
      </div>

      {autoMessage.text && (
        <div className={`mb-4 px-4 py-3 rounded-md text-sm font-medium ${autoMessage.type === 'success'
          ? 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200'
          : 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200'
          }`}>
          {autoMessage.text}
        </div>
      )}

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
          <h3 className="text-lg font-semibold mb-4 text-gray-900 dark:text-white">Create New Course Offering</h3>
          <form onSubmit={handleSubmit}>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Course <span className="text-red-500">*</span>
                </label>
                <select
                  required
                  value={formData.courseId}
                  onChange={(e) => setFormData({ ...formData, courseId: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
                >
                  <option value="">Select a course</option>
                  {coursesList.map((course) => (
                    <option key={course.id} value={course.id}>
                      {course.courseCode} - {course.courseName}
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Faculty <span className="text-red-500">*</span>
                </label>
                <select
                  required
                  value={formData.facultyId}
                  onChange={(e) => setFormData({ ...formData, facultyId: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
                >
                  <option value="">Select a faculty</option>
                  {facultyList.map((faculty) => (
                    <option key={faculty.id} value={faculty.id}>
                      {faculty.firstName} {faculty.lastName}
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Section <span className="text-red-500">*</span>
                </label>
                <select
                  required
                  value={formData.sectionId}
                  onChange={(e) => setFormData({ ...formData, sectionId: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
                >
                  <option value="">Select a section</option>
                  {sectionsList.map((section) => (
                    <option key={section.id} value={section.id}>
                      {section.name} - {section.department?.name || 'N/A'}
                    </option>
                  ))}
                </select>
              </div>
            </div>
            <button
              type="submit"
              disabled={loading}
              className="mt-4 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-md transition-colors disabled:opacity-50"
            >
              {loading ? 'Creating...' : 'Create Offering'}
            </button>
          </form>
        </div>
      )}

      <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md overflow-hidden">
        <div className="p-4 border-b border-gray-200 dark:border-gray-700">
          <h3 className="text-lg font-semibold text-gray-900 dark:text-white">Course Offerings List</h3>
        </div>
        {loading && !showForm ? (
          <div className="p-4 text-center text-gray-600 dark:text-gray-400">Loading...</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-50 dark:bg-gray-700">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">ID</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">Course</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">Faculty</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">Section</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                {offerings.length === 0 ? (
                  <tr>
                    <td colSpan="5" className="px-4 py-4 text-center text-gray-500 dark:text-gray-400">
                      No offerings found
                    </td>
                  </tr>
                ) : (
                  offerings.map((offering) => (
                    <tr key={offering.id} className="hover:bg-gray-50 dark:hover:bg-gray-700">
                      <td className="px-4 py-3 text-sm text-gray-900 dark:text-white">{offering.id}</td>
                      <td className="px-4 py-3 text-sm text-gray-900 dark:text-white">
                        {offering.course?.courseCode} - {offering.course?.courseName}
                      </td>
                      <td className="px-4 py-3 text-sm text-gray-900 dark:text-white">
                        {offering.faculty?.firstName} {offering.faculty?.lastName}
                      </td>
                      <td className="px-4 py-3 text-sm text-gray-900 dark:text-white">
                        {offering.section?.name}
                      </td>
                      <td className="px-4 py-3 text-sm">
                        <button
                          onClick={() => handleDelete(offering.id)}
                          className="px-3 py-1 bg-red-600 hover:bg-red-700 text-white rounded-md transition-colors text-xs"
                        >
                          Delete
                        </button>
                      </td>
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

export default ManageOfferings;

