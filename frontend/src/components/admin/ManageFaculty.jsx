import React, { useState, useEffect, useRef } from 'react';
import { facultyAPI, departmentAPI, designationAPI } from '../../utils/api';
import facultyTemplate from '../common/faculty_template.csv';
import cache from '../../utils/cache';
import useSortableTable from '../../hooks/useSortableTable';

const ManageFaculty = ({ deptId }) => {
  const [facultyList, setFacultyList] = useState([]);
  const [departmentList, setDepartmentList] = useState([]);
  const [designationList, setDesignationList] = useState([]);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState({ type: '', text: '' });
  const [bulkMessage, setBulkMessage] = useState({ type: '', text: '' });
  const [showForm, setShowForm] = useState(false);
  const [formData, setFormData] = useState({
    email: '',
    password: '',
    firstName: '',
    lastName: '',
    middleInitial: '',
    designation: '',
    departmentId: '',
  });
  const [bulkFile, setBulkFile] = useState(null);
  const [bulkFileName, setBulkFileName] = useState('');
  const [bulkUploading, setBulkUploading] = useState(false);
  const fileInputRef = useRef(null);
  const [editingId, setEditingId] = useState(null);
  const [editData, setEditData] = useState({
    email: '',
    password: '',
    firstName: '',
    lastName: '',
    middleInitial: '',
    designation: '',
    departmentId: '',
  });
  const [saving, setSaving] = useState(false);
  const [deletingId, setDeletingId] = useState(null);
  const [randomizing, setRandomizing] = useState(false);
  const [randomMessage, setRandomMessage] = useState({ type: '', text: '' });

  const { sortedData: sortedFaculty, handleSort, sortIcon } = useSortableTable(facultyList, 'lastName');

  useEffect(() => {
    loadFaculty();
    loadMetadata();
  }, [deptId]);

  const loadMetadata = async () => {
    try {
      const [depts, desigs] = await Promise.all([
        cache.getOrFetch('departments', () => departmentAPI.getAll()),
        cache.getOrFetch('designations', () => designationAPI.getAll()),
      ]);
      setDepartmentList(depts);
      setDesignationList(desigs);
    } catch (error) {
      console.error('Error loading metadata:', error);
    }
  };

  const loadFaculty = async (forceRefresh = false) => {
    setLoading(true);
    const cacheKey = `faculty-${deptId || 'all'}`;
    try {
      if (forceRefresh) cache.invalidate(cacheKey);
      const data = await cache.getOrFetch(cacheKey, () => facultyAPI.getAll(deptId));
      setFacultyList(data);
      setMessage({ type: '', text: '' });
    } catch (error) {
      setMessage({ type: 'error', text: `Error loading faculty: ${error.message}` });
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
        departmentId: deptId || (formData.departmentId ? parseInt(formData.departmentId) : null),
      };
      await facultyAPI.create(payload);
      setMessage({ type: 'success', text: 'Faculty created successfully!' });
      setShowForm(false);
      setFormData({
        email: '',
        password: '',
        firstName: '',
        lastName: '',
        middleInitial: '',
        designation: '',
        departmentId: '',
      });
      loadFaculty(true);
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
      setBulkMessage({ type: 'error', text: 'Department context is missing for bulk upload.' });
      return;
    }

    setBulkUploading(true);
    setBulkMessage({ type: '', text: '' });

    try {
      const formDataUpload = new FormData();
      // bulkFile is already an in-memory Blob snapshot — immune to disk changes
      formDataUpload.append('file', bulkFile, bulkFileName || 'faculty.csv');
      const result = await facultyAPI.bulkUpload(formDataUpload, deptId);
      const count = Array.isArray(result) ? result.length : 0;
      setBulkMessage({
        type: 'success',
        text: `Successfully imported ${count} faculty record${count === 1 ? '' : 's'}.`,
      });
      setBulkFile(null);
      setBulkFileName('');
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
      loadFaculty(true);
    } catch (error) {
      setBulkMessage({ type: 'error', text: `Bulk upload failed: ${error.message}` });
    } finally {
      setBulkUploading(false);
    }
  };

  // ----- CRUD: Edit/Delete Handlers -----
  const startEdit = (faculty) => {
    setEditingId(faculty.id);
    setEditData({
      email: faculty.user?.email || '',
      password: '',
      firstName: faculty.firstName || '',
      lastName: faculty.lastName || '',
      middleInitial: faculty.middleInitial || '',
      designation: faculty.designationConstraint?.designation || faculty.designation || '',
      departmentId: faculty.department?.id || '',
    });
    setMessage({ type: '', text: '' });
  };

  const cancelEdit = () => {
    setEditingId(null);
    setEditData({
      email: '', password: '', firstName: '', lastName: '', middleInitial: '',
      designation: '', departmentId: '',
    });
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
        departmentId: deptId || (editData.departmentId ? parseInt(editData.departmentId) : null),
      };
      await facultyAPI.update(editingId, payload);
      setMessage({ type: 'success', text: 'Faculty updated successfully!' });
      setSaving(false);
      setEditingId(null);
      loadFaculty(true);
    } catch (error) {
      setSaving(false);
      setMessage({ type: 'error', text: `Update failed: ${error.message}` });
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this faculty?')) return;
    setDeletingId(id);
    try {
      await facultyAPI.delete(id);
      loadFaculty(true);
    } catch (error) {
      setMessage({ type: 'error', text: `Delete failed: ${error.message}` });
    } finally {
      setDeletingId(null);
    }
  };

  const handleRandomizePreferences = async () => {
    const targetDeptId = deptId || formData.departmentId;
    if (!targetDeptId) {
      setRandomMessage({ type: 'error', text: 'No department selected. Cannot randomize.' });
      return;
    }
    if (!window.confirm(
      'This will REPLACE all existing course preferences for every faculty in this department with random ones. Continue?'
    )) return;

    setRandomizing(true);
    setRandomMessage({ type: '', text: '' });
    try {
      const result = await facultyAPI.randomizePreferences(targetDeptId);
      setRandomMessage({
        type: 'success',
        text: `Done! Created ${result.totalPreferencesCreated} preference entries across all faculty.`,
      });
    } catch (error) {
      setRandomMessage({ type: 'error', text: `Randomize failed: ${error.message}` });
    } finally {
      setRandomizing(false);
    }
  };

  return (
    <div>
      <div className="flex flex-wrap justify-between items-center mb-6 gap-2">
        <h2 className="text-2xl font-bold text-gray-900 dark:text-white">Manage Faculty</h2>
        <div className="flex gap-2">
          <button
            onClick={handleRandomizePreferences}
            disabled={randomizing}
            title="Randomly assign up to 5 course preferences for every faculty in this department"
            className="px-4 py-2 bg-purple-600 hover:bg-purple-700 disabled:opacity-50 text-white rounded-md transition-colors"
          >
            {randomizing ? '⏳ Randomizing…' : '🎲 Randomize Preferences'}
          </button>
          <button
            onClick={() => setShowForm(!showForm)}
            className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-md transition-colors"
          >
            {showForm ? 'Cancel' : 'Add New Faculty'}
          </button>
        </div>
      </div>

      {randomMessage.text && (
        <div className={`mb-4 px-4 py-3 rounded-md text-sm font-medium ${randomMessage.type === 'success'
          ? 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200'
          : 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200'
          }`}>
          {randomMessage.text}
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
          <h3 className="text-lg font-semibold mb-4 text-gray-900 dark:text-white">Create New Faculty</h3>
          <form onSubmit={handleSubmit}>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Email <span className="text-red-500">*</span>
                </label>
                <input
                  type="email"
                  required
                  value={formData.email}
                  onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Password <span className="text-red-500">*</span>
                </label>
                <input
                  type="password"
                  required
                  value={formData.password}
                  onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  First Name <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  required
                  value={formData.firstName}
                  onChange={(e) => setFormData({ ...formData, firstName: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Last Name <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  required
                  value={formData.lastName}
                  onChange={(e) => setFormData({ ...formData, lastName: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Middle Initial
                </label>
                <input
                  type="text"
                  maxLength={1}
                  value={formData.middleInitial}
                  onChange={(e) => setFormData({ ...formData, middleInitial: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Designation
                </label>
                <select
                  value={formData.designation}
                  onChange={(e) => setFormData({ ...formData, designation: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
                >
                  <option value="">Select Designation</option>
                  {designationList.map((d) => (
                    <option key={d.designation} value={d.designation}>
                      {d.designation}
                    </option>
                  ))}
                </select>
              </div>

              {!deptId && (
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                    Department
                  </label>
                  <select
                    value={formData.departmentId}
                    onChange={(e) => setFormData({ ...formData, departmentId: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
                  >
                    <option value="">Select Department</option>
                    {departmentList.map((dept) => (
                      <option key={dept.id} value={dept.id}>
                        {dept.name}
                      </option>
                    ))}
                  </select>
                </div>
              )}
            </div>
            <button
              type="submit"
              disabled={loading}
              className="mt-4 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-md transition-colors disabled:opacity-50"
            >
              {loading ? 'Creating...' : 'Create Faculty'}
            </button>
          </form>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
        <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow-md">
          <h3 className="text-lg font-semibold mb-2 text-gray-900 dark:text-white">Bulk Import via CSV</h3>
          <p className="text-sm text-gray-600 dark:text-gray-300 mb-4">
            Upload a CSV file with the required headers to create multiple faculty records at once.
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
                  const file = e.target.files?.[0];
                  if (!file) { setBulkFile(null); setBulkFileName(''); return; }
                  setBulkFileName(file.name);
                  // Read into memory immediately to avoid ERR_UPLOAD_FILE_CHANGED
                  const reader = new FileReader();
                  reader.onload = (ev) => {
                    setBulkFile(new Blob([ev.target.result], { type: 'text/csv' }));
                  };
                  reader.readAsArrayBuffer(file);
                }}
                className="block w-full text-sm text-gray-900 dark:text-gray-200 border border-gray-300 dark:border-gray-600 rounded-md cursor-pointer bg-gray-50 dark:bg-gray-700 focus:outline-none"
              />
              <div className="flex items-center justify-between text-sm text-gray-600 dark:text-gray-300">
                <a
                  href={facultyTemplate}
                  download="faculty_template.csv"
                  className="text-blue-600 dark:text-blue-400 hover:underline"
                >
                  Download sample CSV template
                </a>
                <span>Required headers: email, password, firstName, lastName, designation</span>
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
            <li>Department IDs should correspond to existing departments.</li>
            <li>Each row must include an email and password for the faculty user.</li>
            <li>Include a header row exactly matching the template column names.</li>
          </ul>
        </div>
      </div>

      {editingId && (
        <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow-md mb-6">
          <h3 className="text-lg font-semibold mb-4 text-gray-900 dark:text-white">Edit Faculty</h3>
          <form onSubmit={(e) => { e.preventDefault(); handleEditSave(); }}>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">First Name</label>
                <input
                  type="text"
                  value={editData.firstName}
                  onChange={(e) => handleEditChange('firstName', e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Last Name</label>
                <input
                  type="text"
                  value={editData.lastName}
                  onChange={(e) => handleEditChange('lastName', e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Middle Initial</label>
                <input
                  type="text"
                  maxLength={1}
                  value={editData.middleInitial}
                  onChange={(e) => handleEditChange('middleInitial', e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Designation</label>
                <select
                  value={editData.designation}
                  onChange={(e) => handleEditChange('designation', e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
                >
                  <option value="">Select Designation</option>
                  {designationList.map((d) => (
                    <option key={d.designation} value={d.designation}>
                      {d.designation}
                    </option>
                  ))}
                </select>
              </div>
              {!deptId && (
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Department</label>
                  <select
                    value={editData.departmentId}
                    onChange={(e) => handleEditChange('departmentId', e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
                  >
                    <option value="">Select Department</option>
                    {departmentList.map((dept) => (
                      <option key={dept.id} value={dept.id}>
                        {dept.name}
                      </option>
                    ))}
                  </select>
                </div>
              )}
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  New Password <span className="text-gray-400 text-xs">(leave blank to keep current)</span>
                </label>
                <input
                  type="password"
                  value={editData.password}
                  onChange={(e) => handleEditChange('password', e.target.value)}
                  placeholder="••••••••"
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
                />
              </div>
            </div>
            <div className="mt-4 flex gap-3">
              <button
                type="submit"
                disabled={saving}
                className="px-4 py-2 bg-green-600 hover:bg-green-700 text-white rounded-md disabled:opacity-50"
              >
                {saving ? 'Saving...' : 'Save Changes'}
              </button>
              <button
                type="button"
                onClick={cancelEdit}
                className="px-4 py-2 bg-gray-500 hover:bg-gray-600 text-white rounded-md"
              >
                Cancel
              </button>
            </div>
          </form>
        </div>
      )}

      <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md overflow-hidden">
        <div className="p-4 border-b border-gray-200 dark:border-gray-700">
          <h3 className="text-lg font-semibold text-gray-900 dark:text-white">Faculty List</h3>
        </div>
        {loading && !showForm ? (
          <div className="p-4 text-center text-gray-600 dark:text-gray-400">Loading...</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-50 dark:bg-gray-700">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">#</th>
                  <th onClick={() => handleSort('lastName')} className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase cursor-pointer select-none hover:text-gray-800 dark:hover:text-white">Name{sortIcon('lastName')}</th>
                  <th onClick={() => handleSort('user.email')} className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase cursor-pointer select-none hover:text-gray-800 dark:hover:text-white">Email{sortIcon('user.email')}</th>
                  <th onClick={() => handleSort('designationConstraint.designation')} className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase cursor-pointer select-none hover:text-gray-800 dark:hover:text-white">Designation{sortIcon('designationConstraint.designation')}</th>
                  <th onClick={() => handleSort('department.name')} className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase cursor-pointer select-none hover:text-gray-800 dark:hover:text-white">Department{sortIcon('department.name')}</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                {facultyList.length === 0 ? (
                  <tr><td colSpan="6" className="px-4 py-4 text-center text-gray-500 dark:text-gray-400">No faculty members found</td></tr>
                ) : (
                  sortedFaculty.map((faculty, idx) => (
                    <tr key={faculty.id} className="hover:bg-gray-50 dark:hover:bg-gray-700">
                      <td className="px-4 py-3 text-sm text-gray-500 dark:text-gray-400">{idx + 1}</td>
                      <td className="px-4 py-3 text-sm text-gray-900 dark:text-white">
                        {faculty.firstName} {faculty.middleInitial} {faculty.lastName}
                      </td>
                      <td className="px-4 py-3 text-sm text-gray-900 dark:text-white">{faculty.user?.email || 'N/A'}</td>
                      <td className="px-4 py-3 text-sm text-gray-900 dark:text-white">{faculty.designationConstraint?.designation || 'N/A'}</td>
                      <td className="px-4 py-3 text-sm text-gray-900 dark:text-white">{faculty.department?.name || 'N/A'}</td>
                      <td className="px-4 py-3 text-sm">
                        <div className="flex gap-2">
                          <button
                            onClick={() => startEdit(faculty)}
                            className="px-3 py-1 bg-yellow-500 hover:bg-yellow-600 text-white rounded-md"
                          >
                            Edit
                          </button>
                          <button
                            onClick={() => handleDelete(faculty.id)}
                            disabled={deletingId === faculty.id}
                            className="px-3 py-1 bg-red-600 hover:bg-red-700 text-white rounded-md disabled:opacity-60"
                          >
                            {deletingId === faculty.id ? 'Deleting...' : 'Delete'}
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div >
  );
};

export default ManageFaculty;

