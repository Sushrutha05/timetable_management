import React, { useState, useEffect, useRef } from 'react';
import { roomAPI } from '../../utils/api';
import roomTemplate from '../common/room_template.csv';

const ManageRooms = () => {
  const [rooms, setRooms] = useState([]);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState({ type: '', text: '' });
  const [bulkMessage, setBulkMessage] = useState({ type: '', text: '' });
  const [showForm, setShowForm] = useState(false);
  const [formData, setFormData] = useState({
    roomNumber: '',
    type: 'CLASSROOM',
    capacity: '',
  });
  const [bulkFile, setBulkFile] = useState(null);
  const [bulkUploading, setBulkUploading] = useState(false);
  const fileInputRef = useRef(null);
  const [editingId, setEditingId] = useState(null);
  const [editData, setEditData] = useState({
    roomNumber: '',
    type: 'CLASSROOM',
    capacity: '',
  });
  const [saving, setSaving] = useState(false);
  const [deletingId, setDeletingId] = useState(null);

  useEffect(() => {
    loadRooms();
  }, []);

  const loadRooms = async () => {
    setLoading(true);
    try {
      const data = await roomAPI.getAll();
      setRooms(data);
      setMessage({ type: '', text: '' });
    } catch (error) {
      setMessage({ type: 'error', text: `Error loading rooms: ${error.message}` });
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
        capacity: parseInt(formData.capacity),
      };
      await roomAPI.create(payload);
      setMessage({ type: 'success', text: 'Room created successfully!' });
      setShowForm(false);
      setFormData({ roomNumber: '', type: 'CLASSROOM', capacity: '' });
      loadRooms();
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
      const result = await roomAPI.bulkUpload(formDataUpload);
      const count = Array.isArray(result) ? result.length : 0;
      setBulkMessage({
        type: 'success',
        text: `Successfully imported ${count} room${count === 1 ? '' : 's'}.`,
      });
      setBulkFile(null);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
      loadRooms();
    } catch (error) {
      setBulkMessage({ type: 'error', text: `Bulk upload failed: ${error.message}` });
    } finally {
      setBulkUploading(false);
    }
  };

  // ---- Edit/Delete Handlers ----
  const startEdit = (room) => {
    setEditingId(room.id);
    setEditData({
      roomNumber: room.roomNumber || '',
      type: room.type || 'CLASSROOM',
      capacity: String(room.capacity ?? ''),
    });
    setMessage({ type: '', text: '' });
  };

  const cancelEdit = () => {
    setEditingId(null);
    setEditData({ roomNumber: '', type: 'CLASSROOM', capacity: '' });
  };

  const handleEditChange = (field, value) => {
    setEditData((prev) => ({ ...prev, [field]: value }));
  };

  const handleEditSave = async () => {
    if (!editingId) return;
    setSaving(true);
    try {
      const payload = { ...editData, capacity: parseInt(editData.capacity) };
      await roomAPI.update(editingId, payload);
      setSaving(false);
      setEditingId(null);
      setMessage({ type: 'success', text: 'Room updated successfully!' });
      loadRooms();
    } catch (error) {
      setSaving(false);
      setMessage({ type: 'error', text: `Update failed: ${error.message}` });
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this room?')) return;
    setDeletingId(id);
    try {
      await roomAPI.delete(id);
      loadRooms();
    } catch (error) {
      setMessage({ type: 'error', text: `Delete failed: ${error.message}` });
    } finally {
      setDeletingId(null);
    }
  };

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-2xl font-bold text-gray-900 dark:text-white">Manage Rooms</h2>
        <button
          onClick={() => setShowForm(!showForm)}
          className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-md transition-colors"
        >
          {showForm ? 'Cancel' : 'Add New Room'}
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
          <h3 className="text-lg font-semibold mb-4 text-gray-900 dark:text-white">Create New Room</h3>
          <form onSubmit={handleSubmit}>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Room Number <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  required
                  value={formData.roomNumber}
                  onChange={(e) => setFormData({ ...formData, roomNumber: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Type <span className="text-red-500">*</span>
                </label>
                <select
                  required
                  value={formData.type}
                  onChange={(e) => setFormData({ ...formData, type: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
                >
                  <option value="CLASSROOM">Classroom</option>
                  <option value="LAB">Lab</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Capacity <span className="text-red-500">*</span>
                </label>
                <input
                  type="number"
                  required
                  min="1"
                  value={formData.capacity}
                  onChange={(e) => setFormData({ ...formData, capacity: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
                />
              </div>
            </div>
            <button
              type="submit"
              disabled={loading}
              className="mt-4 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-md transition-colors disabled:opacity-50"
            >
              {loading ? 'Creating...' : 'Create Room'}
            </button>
          </form>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
        <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow-md">
          <h3 className="text-lg font-semibold mb-2 text-gray-900 dark:text-white">Bulk Import Rooms via CSV</h3>
          <p className="text-sm text-gray-600 dark:text-gray-300 mb-4">
            Upload a CSV file with the required headers to add multiple rooms at once.
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
                  href={roomTemplate}
                  download="room_template.csv"
                  className="text-blue-600 dark:text-blue-400 hover:underline"
                >
                  Download sample CSV template
                </a>
                <span>Headers: roomNumber, type, capacity</span>
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
            <li>Room numbers must be unique.</li>
            <li>`type` should match allowed values (e.g., CLASSROOM, LAB).</li>
            <li>Capacity must be a positive integer.</li>
            <li>Keep the header row intact for proper parsing.</li>
          </ul>
        </div>
      </div>

      {editingId && (
        <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow-md mb-6">
          <h3 className="text-lg font-semibold mb-4 text-gray-900 dark:text-white">Edit Room</h3>
          <form onSubmit={(e) => { e.preventDefault(); handleEditSave(); }}>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Room Number</label>
                <input
                  type="text"
                  required
                  value={editData.roomNumber}
                  onChange={(e) => handleEditChange('roomNumber', e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Type</label>
                <select
                  required
                  value={editData.type}
                  onChange={(e) => handleEditChange('type', e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
                >
                  <option value="CLASSROOM">Classroom</option>
                  <option value="LAB">Lab</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Capacity</label>
                <input
                  type="number"
                  required
                  min="1"
                  value={editData.capacity}
                  onChange={(e) => handleEditChange('capacity', e.target.value)}
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
          <h3 className="text-lg font-semibold text-gray-900 dark:text-white">Rooms List</h3>
        </div>
        {loading && !showForm ? (
          <div className="p-4 text-center text-gray-600 dark:text-gray-400">Loading...</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-50 dark:bg-gray-700">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">ID</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">Room Number</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">Type</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">Capacity</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                {rooms.length === 0 ? (
                  <tr>
                    <td colSpan="5" className="px-4 py-4 text-center text-gray-500 dark:text-gray-400">
                      No rooms found
                    </td>
                  </tr>
                ) : (
                  rooms.map((room) => (
                    <tr key={room.id} className="hover:bg-gray-50 dark:hover:bg-gray-700">
                      <td className="px-4 py-3 text-sm text-gray-900 dark:text-white">{room.id}</td>
                      <td className="px-4 py-3 text-sm text-gray-900 dark:text-white">{room.roomNumber}</td>
                      <td className="px-4 py-3 text-sm text-gray-900 dark:text-white">{room.type}</td>
                      <td className="px-4 py-3 text-sm text-gray-900 dark:text-white">{room.capacity}</td>
                      <td className="px-4 py-3 text-sm">
                        <div className="flex gap-2">
                          <button
                            onClick={() => startEdit(room)}
                            className="px-3 py-1 bg-yellow-500 hover:bg-yellow-600 text-white rounded-md"
                          >
                            Edit
                          </button>
                          <button
                            onClick={() => handleDelete(room.id)}
                            disabled={deletingId === room.id}
                            className="px-3 py-1 bg-red-600 hover:bg-red-700 text-white rounded-md disabled:opacity-60"
                          >
                            {deletingId === room.id ? 'Deleting...' : 'Delete'}
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

export default ManageRooms;

