import React, { useState, useEffect } from 'react';
import { timeSlotAPI } from '../../utils/api';

const ManageTimeSlots = () => {
  const [timeSlots, setTimeSlots] = useState([]);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState({ type: '', text: '' });
  const [editingId, setEditingId] = useState(null);
  const [editData, setEditData] = useState({
    dayOfWeek: 'MONDAY',
    startTime: '',
    endTime: '',
    isBreak: false,
  });
  const [saving, setSaving] = useState(false);
  const [deletingId, setDeletingId] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [formData, setFormData] = useState({
    dayOfWeek: 'MONDAY',
    startTime: '',
    endTime: '',
    isBreak: false,
  });

  useEffect(() => {
    loadTimeSlots();
  }, []);

  const loadTimeSlots = async () => {
    setLoading(true);
    try {
      const data = await timeSlotAPI.getAll();
      setTimeSlots(data);
      setMessage({ type: '', text: '' });
    } catch (error) {
      setMessage({ type: 'error', text: `Error loading time slots: ${error.message}` });
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
        startTime: formData.startTime + ':00',
        endTime: formData.endTime + ':00',
      };
      await timeSlotAPI.create(payload);
      setMessage({ type: 'success', text: 'Time slot created successfully!' });
      setShowForm(false);
      setFormData({ dayOfWeek: 'MONDAY', startTime: '', endTime: '', isBreak: false });
      loadTimeSlots();
    } catch (error) {
      setMessage({ type: 'error', text: `Error: ${error.message}` });
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this time slot?')) {
      return;
    }

    setDeletingId(id);
    setMessage({ type: '', text: '' });

    try {
      await timeSlotAPI.delete(id);
      setMessage({ type: 'success', text: 'Time slot deleted successfully!' });
      loadTimeSlots();
    } catch (error) {
      setMessage({ type: 'error', text: `Error: ${error.message}` });
    } finally {
      setDeletingId(null);
    }
  };

  const startEdit = (slot) => {
    setEditingId(slot.id);
    const formatTime = (t) => (t || '').toString().substring(0,5);
    setEditData({
      dayOfWeek: slot.dayOfWeek || 'MONDAY',
      startTime: formatTime(slot.startTime),
      endTime: formatTime(slot.endTime),
      isBreak: !!slot.breakSlot,
    });
  };

  const cancelEdit = () => {
    setEditingId(null);
    setEditData({ dayOfWeek: 'MONDAY', startTime: '', endTime: '', isBreak: false });
  };

  const handleEditChange = (field, value) => {
    setEditData((prev) => ({ ...prev, [field]: value }));
  };

  const handleEditSave = async () => {
    if (!editingId) return;
    setSaving(true);
    try {
      const payload = {
        dayOfWeek: editData.dayOfWeek,
        startTime: editData.startTime + ':00',
        endTime: editData.endTime + ':00',
        break: editData.isBreak,
      };
      await timeSlotAPI.update(editingId, payload);
      setSaving(false);
      setEditingId(null);
      setMessage({ type: 'success', text: 'Time slot updated successfully!' });
      loadTimeSlots();
    } catch (error) {
      setSaving(false);
      setMessage({ type: 'error', text: `Error: ${error.message}` });
    }
  };

  const daysOfWeek = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-2xl font-bold text-gray-900 dark:text-white">Manage Time Slots</h2>
        <button
          onClick={() => setShowForm(!showForm)}
          className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-md transition-colors"
        >
          {showForm ? 'Cancel' : 'Add New Time Slot'}
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
          <h3 className="text-lg font-semibold mb-4 text-gray-900 dark:text-white">Create New Time Slot</h3>
          <form onSubmit={handleSubmit}>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Day of Week <span className="text-red-500">*</span>
                </label>
                <select
                  required
                  value={formData.dayOfWeek}
                  onChange={(e) => setFormData({ ...formData, dayOfWeek: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
                >
                  {daysOfWeek.map((day) => (
                    <option key={day} value={day}>
                      {day}
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Start Time <span className="text-red-500">*</span>
                </label>
                <input
                  type="time"
                  required
                  value={formData.startTime}
                  onChange={(e) => setFormData({ ...formData, startTime: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  End Time <span className="text-red-500">*</span>
                </label>
                <input
                  type="time"
                  required
                  value={formData.endTime}
                  onChange={(e) => setFormData({ ...formData, endTime: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
                />
              </div>
              <div className="flex items-center">
                <label className="flex items-center text-sm font-medium text-gray-700 dark:text-gray-300">
                  <input
                    type="checkbox"
                    checked={formData.isBreak}
                    onChange={(e) => setFormData({ ...formData, isBreak: e.target.checked })}
                    className="mr-2"
                  />
                  Is Break Slot
                </label>
              </div>
            </div>
            <button
              type="submit"
              disabled={loading}
              className="mt-4 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-md transition-colors disabled:opacity-50"
            >
              {loading ? 'Creating...' : 'Create Time Slot'}
            </button>
          </form>
        </div>
      )}

      <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md overflow-hidden">
        <div className="p-4 border-b border-gray-200 dark:border-gray-700">
          <h3 className="text-lg font-semibold text-gray-900 dark:text-white">Time Slots List</h3>
        </div>
        {loading && !showForm ? (
          <div className="p-4 text-center text-gray-600 dark:text-gray-400">Loading...</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-50 dark:bg-gray-700">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">ID</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">Day</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">Start Time</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">End Time</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">Is Break</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                {timeSlots.length === 0 ? (
                  <tr>
                    <td colSpan="6" className="px-4 py-4 text-center text-gray-500 dark:text-gray-400">
                      No time slots found
                    </td>
                  </tr>
                ) : (
                  timeSlots.map((slot) => (
                    <tr key={slot.id} className="hover:bg-gray-50 dark:hover:bg-gray-700">
                      <td className="px-4 py-3 text-sm text-gray-900 dark:text-white">{slot.id}</td>
                      <td className="px-4 py-3 text-sm text-gray-900 dark:text-white">{slot.dayOfWeek}</td>
                      <td className="px-4 py-3 text-sm text-gray-900 dark:text-white">{slot.startTime}</td>
                      <td className="px-4 py-3 text-sm text-gray-900 dark:text-white">{slot.endTime}</td>
                      <td className="px-4 py-3 text-sm text-gray-900 dark:text-white">
                        {slot.breakSlot ? 'Yes' : 'No'}
                      </td>
                      <td className="px-4 py-3 text-sm">
                        <div className="flex gap-2">
                          <button
                            onClick={() => startEdit(slot)}
                            className="px-3 py-1 bg-yellow-500 hover:bg-yellow-600 text-white rounded-md text-xs"
                          >
                            Edit
                          </button>
                          <button
                            onClick={() => handleDelete(slot.id)}
                            disabled={deletingId === slot.id}
                            className="px-3 py-1 bg-red-600 hover:bg-red-700 text-white rounded-md transition-colors text-xs disabled:opacity-60"
                          >
                            {deletingId === slot.id ? 'Deleting...' : 'Delete'}
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

export default ManageTimeSlots;

