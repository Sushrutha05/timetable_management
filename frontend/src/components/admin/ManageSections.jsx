import React, { useState, useEffect, useRef } from 'react';
import { sectionAPI } from '../../utils/api';
import sectionTemplate from '../common/section_template.csv';

const ManageSections = () => {
  const [sections, setSections] = useState([]);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState({ type: '', text: '' });
  const [bulkMessage, setBulkMessage] = useState({ type: '', text: '' });
  const [showForm, setShowForm] = useState(false);
  const [formData, setFormData] = useState({
    departmentId: '',
    name: '',
    semester: '',
    year: '',
  });
  const [bulkFile, setBulkFile] = useState(null);
  const [bulkUploading, setBulkUploading] = useState(false);
  const fileInputRef = useRef(null);

  useEffect(() => {
    loadSections();
  }, []);

  const loadSections = async () => {
    setLoading(true);
    try {
      const data = await sectionAPI.getAll();
      setSections(data);
      setMessage({ type: '', text: '' });
    } catch (error) {
      setMessage({ type: 'error', text: `Error loading sections: ${error.message}` });
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
        departmentId: parseInt(formData.departmentId),
        semester: parseInt(formData.semester),
        year: parseInt(formData.year),
      };
      await sectionAPI.create(payload);
      setMessage({ type: 'success', text: 'Section created successfully!' });
      setShowForm(false);
      setFormData({ departmentId: '', name: '', semester: '', year: '' });
      loadSections();
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

    setBulkUploading(true);
    setBulkMessage({ type: '', text: '' });

    try {
      const formDataUpload = new FormData();
      formDataUpload.append('file', bulkFile);
      const result = await sectionAPI.bulkUpload(formDataUpload);
      const count = Array.isArray(result) ? result.length : 0;
      setBulkMessage({
        type: 'success',
        text: `Successfully imported ${count} section${count === 1 ? '' : 's'}.`,
      });
      setBulkFile(null);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
      loadSections();
    } catch (error) {
      setBulkMessage({ type: 'error', text: `Bulk upload failed: ${error.message}` });
    } finally {
      setBulkUploading(false);
    }
  };

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-2xl font-bold text-gray-900 dark:text-white">Manage Sections</h2>
        <button
          onClick={() => setShowForm(!showForm)}
          className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-md transition-colors"
        >
          {showForm ? 'Cancel' : 'Add New Section'}
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
          <h3 className="text-lg font-semibold mb-4 text-gray-900 dark:text-white">Create New Section</h3>
          <form onSubmit={handleSubmit}>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Department ID <span className="text-red-500">*</span>
                </label>
                <input
                  type="number"
                  required
                  value={formData.departmentId}
                  onChange={(e) => setFormData({ ...formData, departmentId: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Name <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  required
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
                  placeholder="e.g., Section A"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Semester <span className="text-red-500">*</span>
                </label>
                <input
                  type="number"
                  required
                  min="1"
                  value={formData.semester}
                  onChange={(e) => setFormData({ ...formData, semester: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Year <span className="text-red-500">*</span>
                </label>
                <input
                  type="number"
                  required
                  min="2020"
                  value={formData.year}
                  onChange={(e) => setFormData({ ...formData, year: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
                />
              </div>
            </div>
            <button
              type="submit"
              disabled={loading}
              className="mt-4 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-md transition-colors disabled:opacity-50"
            >
              {loading ? 'Creating...' : 'Create Section'}
            </button>
          </form>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
        <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow-md">
          <h3 className="text-lg font-semibold mb-2 text-gray-900 dark:text-white">Bulk Import Sections via CSV</h3>
          <p className="text-sm text-gray-600 dark:text-gray-300 mb-4">
            Upload a CSV file with the required headers to add multiple sections simultaneously.
          </p>
          {bulkMessage.text && (
            <div
              className={`mb-4 p-3 rounded-md ${
                bulkMessage.type === 'success'
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
                  href={sectionTemplate}
                  download="section_template.csv"
                  className="text-blue-600 dark:text-blue-400 hover:underline"
                >
                  Download sample CSV template
                </a>
                <span>Headers: departmentId, name, semester, year</span>
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
            <li>Department IDs must reference existing departments.</li>
            <li>`semester` and `year` should be whole numbers.</li>
            <li>Keep the header row intact for accurate parsing.</li>
            <li>Use UTF-8 encoding for best compatibility.</li>
          </ul>
        </div>
      </div>

      <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md overflow-hidden">
        <div className="p-4 border-b border-gray-200 dark:border-gray-700">
          <h3 className="text-lg font-semibold text-gray-900 dark:text-white">Sections List</h3>
        </div>
        {loading && !showForm ? (
          <div className="p-4 text-center text-gray-600 dark:text-gray-400">Loading...</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-50 dark:bg-gray-700">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">ID</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">Name</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">Department</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">Semester</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">Year</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                {sections.length === 0 ? (
                  <tr>
                    <td colSpan="5" className="px-4 py-4 text-center text-gray-500 dark:text-gray-400">
                      No sections found
                    </td>
                  </tr>
                ) : (
                  sections.map((section) => (
                    <tr key={section.id} className="hover:bg-gray-50 dark:hover:bg-gray-700">
                      <td className="px-4 py-3 text-sm text-gray-900 dark:text-white">{section.id}</td>
                      <td className="px-4 py-3 text-sm text-gray-900 dark:text-white">{section.name}</td>
                      <td className="px-4 py-3 text-sm text-gray-900 dark:text-white">{section.department?.name || 'N/A'}</td>
                      <td className="px-4 py-3 text-sm text-gray-900 dark:text-white">{section.semester}</td>
                      <td className="px-4 py-3 text-sm text-gray-900 dark:text-white">{section.year}</td>
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

export default ManageSections;

