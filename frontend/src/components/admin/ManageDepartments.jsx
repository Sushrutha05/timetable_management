import React, { useState, useEffect } from 'react';
import { departmentAPI } from '../../utils/api';
import cache from '../../utils/cache';
import useSortableTable from '../../hooks/useSortableTable';

const CACHE_KEY = 'departments';

const ManageDepartments = () => {
    const [departments, setDepartments] = useState([]);
    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState('');
    const [error, setError] = useState('');

    const [showForm, setShowForm] = useState(false);
    const [formData, setFormData] = useState({ name: '' });
    const [editingId, setEditingId] = useState(null);
    const [deletingId, setDeletingId] = useState(null);

    const { sortedData, sortKey, handleSort, sortIcon } = useSortableTable(departments, 'name');

    useEffect(() => {
        fetchDepartments();
    }, []);

    const fetchDepartments = async (forceRefresh = false) => {
        setLoading(true);
        try {
            if (forceRefresh) cache.invalidate(CACHE_KEY);
            const data = await cache.getOrFetch(CACHE_KEY, () => departmentAPI.getAll());
            setDepartments(data);
            setError('');
        } catch (err) {
            setError(`Failed to load departments: ${err.message}`);
        } finally {
            setLoading(false);
        }
    };

    const handleInputChange = (e) => setFormData({ ...formData, [e.target.name]: e.target.value });

    const cancelForm = () => { setShowForm(false); setEditingId(null); setFormData({ name: '' }); setMessage(''); setError(''); };

    const startEdit = (dept) => { setShowForm(true); setEditingId(dept.id); setFormData({ name: dept.name }); setMessage(''); setError(''); };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setMessage('');
        setError('');
        try {
            if (editingId) {
                await departmentAPI.update(editingId, formData);
                setMessage('Department updated successfully!');
            } else {
                await departmentAPI.create(formData);
                setMessage('Department created successfully!');
            }
            fetchDepartments(true);
            setFormData({ name: '' });
            setEditingId(null);
            setShowForm(false);
        } catch (err) {
            setError(`Operation failed: ${err.message}`);
        } finally {
            setLoading(false);
        }
    };

    const handleDelete = async (id) => {
        if (!window.confirm('Are you sure you want to delete this department? This may fail if it is assigned to courses or faculty.')) return;
        setDeletingId(id);
        setMessage('');
        setError('');
        try {
            await departmentAPI.delete(id);
            setMessage('Department deleted successfully!');
            fetchDepartments(true);
        } catch (err) {
            setError(`Failed to delete department: ${err.message}`);
        } finally {
            setDeletingId(null);
        }
    };

    const thClass = 'px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase cursor-pointer select-none hover:text-gray-800 dark:hover:text-white';

    return (
        <div>
            <div className="flex justify-between items-center mb-6">
                <h2 className="text-2xl font-bold text-gray-900 dark:text-white">Manage Departments</h2>
                <button
                    onClick={() => { setShowForm(!showForm); setEditingId(null); setFormData({ name: '' }); }}
                    className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-md transition-colors"
                >
                    {showForm ? 'Cancel' : 'Add New Department'}
                </button>
            </div>

            {message && <div className="mb-4 p-3 bg-green-100 text-green-700 dark:bg-green-900 dark:text-green-300 rounded-md">{message}</div>}
            {error && <div className="mb-4 p-3 bg-red-100 text-red-700 dark:bg-red-900 dark:text-red-300 rounded-md">{error}</div>}

            {showForm && (
                <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow-md mb-6">
                    <h3 className="text-lg font-semibold mb-4 text-gray-900 dark:text-white">
                        {editingId ? 'Edit Department' : 'Create New Department'}
                    </h3>
                    <form onSubmit={handleSubmit} className="flex gap-4 items-end">
                        <div className="flex-1">
                            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                                Department Name <span className="text-red-500">*</span>
                            </label>
                            <input
                                type="text" name="name" required value={formData.name}
                                onChange={handleInputChange}
                                className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
                                placeholder="e.g. Computer Science"
                            />
                        </div>
                        <div className="flex gap-2">
                            <button type="submit" disabled={loading}
                                className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-md transition-colors disabled:opacity-50">
                                {loading ? 'Saving...' : (editingId ? 'Save Changes' : 'Create')}
                            </button>
                            {editingId && (
                                <button type="button" onClick={cancelForm}
                                    className="px-4 py-2 bg-gray-500 hover:bg-gray-600 text-white rounded-md transition-colors">
                                    Cancel
                                </button>
                            )}
                        </div>
                    </form>
                </div>
            )}

            <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md overflow-hidden">
                <div className="p-4 border-b border-gray-200 dark:border-gray-700">
                    <h3 className="text-lg font-semibold text-gray-900 dark:text-white">Departments List</h3>
                </div>
                {loading && !showForm ? (
                    <div className="p-4 text-center text-gray-600 dark:text-gray-400">Loading departments...</div>
                ) : (
                    <div className="overflow-x-auto">
                        <table className="w-full">
                            <thead className="bg-gray-50 dark:bg-gray-700">
                                <tr>
                                    <th className={thClass} onClick={() => handleSort('id')}>#</th>
                                    <th className={thClass} onClick={() => handleSort('name')}>
                                        Department Name{sortIcon('name')}
                                    </th>
                                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">Actions</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                                {sortedData.length === 0 ? (
                                    <tr><td colSpan="3" className="px-4 py-4 text-center text-gray-500 dark:text-gray-400">No departments found.</td></tr>
                                ) : (
                                    sortedData.map((dept, idx) => (
                                        <tr key={dept.id} className="hover:bg-gray-50 dark:hover:bg-gray-700">
                                            <td className="px-4 py-3 text-sm text-gray-500 dark:text-gray-400">{idx + 1}</td>
                                            <td className="px-4 py-3 text-sm text-gray-900 dark:text-white font-medium">{dept.name}</td>
                                            <td className="px-4 py-3 text-sm">
                                                <div className="flex gap-2">
                                                    <button onClick={() => startEdit(dept)}
                                                        className="px-3 py-1 bg-yellow-500 hover:bg-yellow-600 text-white rounded-md transition-colors">Edit</button>
                                                    <button onClick={() => handleDelete(dept.id)} disabled={deletingId === dept.id}
                                                        className="px-3 py-1 bg-red-600 hover:bg-red-700 text-white rounded-md transition-colors disabled:opacity-60">
                                                        {deletingId === dept.id ? 'Deleting...' : 'Delete'}
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
        </div>
    );
};

export default ManageDepartments;
