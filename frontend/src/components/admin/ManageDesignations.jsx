import React, { useState, useEffect } from 'react';
import { designationAPI } from '../../utils/api';
import cache from '../../utils/cache';
import useSortableTable from '../../hooks/useSortableTable';

const CACHE_KEY = 'designations';

const ManageDesignations = () => {
  const [designations, setDesignations] = useState([]);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState({ type: '', text: '' });
  const [showForm, setShowForm] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [formData, setFormData] = useState({ designation: '', maxLectureHours: '', maxLabHours: '' });

  const { sortedData, handleSort, sortIcon } = useSortableTable(designations, 'designation');

  useEffect(() => { loadDesignations(); }, []);

  const loadDesignations = async (forceRefresh = false) => {
    setLoading(true);
    try {
      if (forceRefresh) cache.invalidate(CACHE_KEY);
      const data = await cache.getOrFetch(CACHE_KEY, () => designationAPI.getAll());
      setDesignations(data);
      setMessage({ type: '', text: '' });
    } catch (error) {
      setMessage({ type: 'error', text: `Error loading designations: ${error.message}` });
    } finally {
      setLoading(false);
    }
  };

  const resetForm = () => { setShowForm(false); setIsEditing(false); setFormData({ designation: '', maxLectureHours: '', maxLabHours: '' }); };

  const handleEdit = (d) => {
    setFormData({ designation: d.designation, maxLectureHours: d.maxLectureHours, maxLabHours: d.maxLabHours });
    setIsEditing(true);
    setShowForm(true);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setMessage({ type: '', text: '' });
    try {
      const payload = { ...formData, maxLectureHours: parseInt(formData.maxLectureHours), maxLabHours: parseInt(formData.maxLabHours) };
      await designationAPI.create(payload);
      setMessage({ type: 'success', text: isEditing ? 'Designation updated successfully!' : 'Designation created successfully!' });
      resetForm();
      loadDesignations(true);
    } catch (error) {
      setMessage({ type: 'error', text: `Error: ${error.message}` });
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (name) => {
    if (!window.confirm(`Are you sure you want to delete designation "${name}"?`)) return;
    setLoading(true);
    setMessage({ type: '', text: '' });
    try {
      await designationAPI.delete(name);
      setMessage({ type: 'success', text: 'Designation deleted successfully!' });
      loadDesignations(true);
    } catch (error) {
      setMessage({ type: 'error', text: `Error: ${error.message}` });
    } finally {
      setLoading(false);
    }
  };

  const thClass = 'px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase cursor-pointer select-none hover:text-gray-800 dark:hover:text-white';

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-2xl font-bold text-gray-900 dark:text-white">Manage Designations</h2>
        <button onClick={() => { if (showForm) resetForm(); else setShowForm(true); }}
          className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-md transition-colors">
          {showForm ? 'Cancel' : 'Add New Designation'}
        </button>
      </div>

      {message.text && (
        <div className={`mb-4 p-3 rounded-md ${message.type === 'success' ? 'bg-green-100 dark:bg-green-900 text-green-700 dark:text-green-300' : 'bg-red-100 dark:bg-red-900 text-red-700 dark:text-red-300'}`}>
          {message.text}
        </div>
      )}

      {showForm && (
        <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow-md mb-6">
          <h3 className="text-lg font-semibold mb-4 text-gray-900 dark:text-white">{isEditing ? 'Edit Designation' : 'Create New Designation'}</h3>
          <form onSubmit={handleSubmit}>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Designation <span className="text-red-500">*</span></label>
                <input type="text" required value={formData.designation}
                  onChange={(e) => setFormData({ ...formData, designation: e.target.value })}
                  className={`w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white ${isEditing ? 'bg-gray-100 dark:bg-gray-600 cursor-not-allowed' : ''}`}
                  placeholder="e.g., Professor" readOnly={isEditing} />
                {isEditing && <p className="text-xs text-gray-500 mt-1">Designation name cannot be changed.</p>}
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Max Lecture Hours <span className="text-red-500">*</span></label>
                <input type="number" required min="0" value={formData.maxLectureHours}
                  onChange={(e) => setFormData({ ...formData, maxLectureHours: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white" />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Max Lab Hours <span className="text-red-500">*</span></label>
                <input type="number" required min="0" value={formData.maxLabHours}
                  onChange={(e) => setFormData({ ...formData, maxLabHours: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white" />
              </div>
            </div>
            <button type="submit" disabled={loading}
              className="mt-4 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-md transition-colors disabled:opacity-50">
              {loading ? 'Saving...' : (isEditing ? 'Update Designation' : 'Save Designation')}
            </button>
          </form>
        </div>
      )}

      <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md overflow-hidden">
        <div className="p-4 border-b border-gray-200 dark:border-gray-700">
          <h3 className="text-lg font-semibold text-gray-900 dark:text-white">Designations List</h3>
        </div>
        {loading && !showForm ? (
          <div className="p-4 text-center text-gray-600 dark:text-gray-400">Loading...</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-50 dark:bg-gray-700">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">#</th>
                  <th className={thClass} onClick={() => handleSort('designation')}>Designation{sortIcon('designation')}</th>
                  <th className={thClass} onClick={() => handleSort('maxLectureHours')}>Max Lecture Hrs{sortIcon('maxLectureHours')}</th>
                  <th className={thClass} onClick={() => handleSort('maxLabHours')}>Max Lab Hrs{sortIcon('maxLabHours')}</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                {sortedData.length === 0 ? (
                  <tr><td colSpan="5" className="px-4 py-4 text-center text-gray-500 dark:text-gray-400">No designations found</td></tr>
                ) : (
                  sortedData.map((d, idx) => (
                    <tr key={d.designation} className="hover:bg-gray-50 dark:hover:bg-gray-700">
                      <td className="px-4 py-3 text-sm text-gray-500 dark:text-gray-400">{idx + 1}</td>
                      <td className="px-4 py-3 text-sm text-gray-900 dark:text-white">{d.designation}</td>
                      <td className="px-4 py-3 text-sm text-gray-900 dark:text-white">{d.maxLectureHours}</td>
                      <td className="px-4 py-3 text-sm text-gray-900 dark:text-white">{d.maxLabHours}</td>
                      <td className="px-4 py-3 text-sm">
                        <div className="flex gap-2">
                          <button onClick={() => handleEdit(d)} className="px-3 py-1 bg-yellow-500 hover:bg-yellow-600 text-white rounded-md transition-colors text-xs">Edit</button>
                          <button onClick={() => handleDelete(d.designation)} className="px-3 py-1 bg-red-600 hover:bg-red-700 text-white rounded-md transition-colors text-xs">Delete</button>
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
    </div>
  );
};

export default ManageDesignations;
