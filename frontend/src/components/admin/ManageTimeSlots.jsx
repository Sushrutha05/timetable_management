import React, { useState, useEffect, useCallback } from 'react';
import { timeSlotAPI } from '../../utils/api';

const SEMESTER_GROUPS = [
  { key: 'SEM_3_4', label: 'Sem 3 & 4' },
  { key: 'SEM_5_6_7', label: 'Sem 5, 6 & 7' },
  { key: 'SEM_1_2', label: 'Sem 1 & 2 (future)' },
];

const DAY_ORDER = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];

const formatTime = (t) => (t || '').toString().substring(0, 5);

const ManageTimeSlots = () => {
  const [activeGroup, setActiveGroup] = useState('SEM_5_6_7');
  const [timeSlots, setTimeSlots] = useState([]);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState({ type: '', text: '' });

  // Edit state
  const [editingId, setEditingId] = useState(null);
  const [editData, setEditData] = useState({});

  // Add form state
  const [showForm, setShowForm] = useState(false);
  const [formData, setFormData] = useState({
    dayOfWeek: 'MONDAY',
    startTime: '',
    endTime: '',
    isBreak: false,
    semesterGroup: 'SEM_5_6_7',
  });

  const [saving, setSaving] = useState(false);
  const [deletingId, setDeletingId] = useState(null);
  const [togglingId, setTogglingId] = useState(null);

  const loadTimeSlots = useCallback(async () => {
    setLoading(true);
    try {
      const data = await timeSlotAPI.getAll(activeGroup);
      setTimeSlots(data);
      setMessage({ type: '', text: '' });
    } catch (error) {
      setMessage({ type: 'error', text: `Error loading time slots: ${error.message}` });
    } finally {
      setLoading(false);
    }
  }, [activeGroup]);

  useEffect(() => {
    loadTimeSlots();
    setShowForm(false);
    setEditingId(null);
  }, [loadTimeSlots]);

  // ── Inline break toggle ──────────────────────────────────────────────────
  const handleToggleBreak = async (slot) => {
    setTogglingId(slot.id);
    try {
      await timeSlotAPI.update(slot.id, {
        dayOfWeek: slot.dayOfWeek,
        startTime: formatTime(slot.startTime) + ':00',
        endTime: formatTime(slot.endTime) + ':00',
        isBreak: !slot.breakSlot,
        semesterGroup: slot.semesterGroup,
      });
      setTimeSlots((prev) =>
        prev.map((s) => s.id === slot.id ? { ...s, breakSlot: !s.breakSlot } : s)
      );
    } catch (error) {
      setMessage({ type: 'error', text: `Toggle failed: ${error.message}` });
    } finally {
      setTogglingId(null);
    }
  };

  // ── Add form ─────────────────────────────────────────────────────────────
  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setMessage({ type: '', text: '' });
    try {
      await timeSlotAPI.create({
        ...formData,
        startTime: formData.startTime + ':00',
        endTime: formData.endTime + ':00',
      });
      setMessage({ type: 'success', text: 'Time slot created successfully!' });
      setShowForm(false);
      setFormData({ dayOfWeek: 'MONDAY', startTime: '', endTime: '', isBreak: false, semesterGroup: activeGroup });
      loadTimeSlots();
    } catch (error) {
      setMessage({ type: 'error', text: `Error: ${error.message}` });
    } finally {
      setLoading(false);
    }
  };

  // ── Delete ────────────────────────────────────────────────────────────────
  const handleDelete = async (id) => {
    if (!window.confirm('Delete this time slot?')) return;
    setDeletingId(id);
    try {
      await timeSlotAPI.delete(id);
      setTimeSlots((prev) => prev.filter((s) => s.id !== id));
      setMessage({ type: 'success', text: 'Time slot deleted.' });
    } catch (error) {
      setMessage({ type: 'error', text: `Error: ${error.message}` });
    } finally {
      setDeletingId(null);
    }
  };

  // ── Inline edit ───────────────────────────────────────────────────────────
  const startEdit = (slot) => {
    setEditingId(slot.id);
    setEditData({
      dayOfWeek: slot.dayOfWeek || 'MONDAY',
      startTime: formatTime(slot.startTime),
      endTime: formatTime(slot.endTime),
      isBreak: !!slot.breakSlot,
      semesterGroup: slot.semesterGroup || activeGroup,
    });
  };

  const handleEditSave = async () => {
    setSaving(true);
    try {
      await timeSlotAPI.update(editingId, {
        dayOfWeek: editData.dayOfWeek,
        startTime: editData.startTime + ':00',
        endTime: editData.endTime + ':00',
        isBreak: editData.isBreak,
        semesterGroup: editData.semesterGroup,
      });
      setEditingId(null);
      setMessage({ type: 'success', text: 'Time slot updated.' });
      loadTimeSlots();
    } catch (error) {
      setMessage({ type: 'error', text: `Error: ${error.message}` });
    } finally {
      setSaving(false);
    }
  };

  // ── Group slots by day for display ────────────────────────────────────────
  const slotsByDay = DAY_ORDER.reduce((acc, day) => {
    const daySlots = timeSlots
      .filter((s) => s.dayOfWeek === day)
      .sort((a, b) => (a.startTime > b.startTime ? 1 : -1));
    if (daySlots.length) acc[day] = daySlots;
    return acc;
  }, {});

  const firstDaySlots = Object.values(slotsByDay)[0] || [];

  return (
    <div>
      {/* ── Header ───────────────────────────────────────────────────────── */}
      <div className="flex flex-wrap justify-between items-center mb-4 gap-3">
        <h2 className="text-2xl font-bold text-gray-900 dark:text-white">Manage Time Slots</h2>
        <button
          onClick={() => { setShowForm(!showForm); setEditingId(null); }}
          className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-md transition-colors text-sm"
        >
          {showForm ? 'Cancel' : '+ Add Time Slot'}
        </button>
      </div>

      {/* ── Semester group tabs ───────────────────────────────────────────── */}
      <div className="flex gap-1 mb-6 border-b border-gray-200 dark:border-gray-700">
        {SEMESTER_GROUPS.map((g) => (
          <button
            key={g.key}
            onClick={() => setActiveGroup(g.key)}
            className={`px-4 py-2 text-sm font-medium rounded-t-md transition-colors ${activeGroup === g.key
                ? 'bg-blue-600 text-white border-b-2 border-blue-600'
                : 'text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700'
              }`}
          >
            {g.label}
          </button>
        ))}
      </div>

      {/* ── Flash messages ────────────────────────────────────────────────── */}
      {message.text && (
        <div
          className={`mb-4 p-3 rounded-md text-sm ${message.type === 'success'
              ? 'bg-green-100 dark:bg-green-900 text-green-700 dark:text-green-300'
              : 'bg-red-100 dark:bg-red-900 text-red-700 dark:text-red-300'
            }`}
        >
          {message.text}
        </div>
      )}

      {/* ── Add form ──────────────────────────────────────────────────────── */}
      {showForm && (
        <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow-md mb-6">
          <h3 className="text-lg font-semibold mb-4 text-gray-900 dark:text-white">Create New Time Slot</h3>
          <form onSubmit={handleSubmit}>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Semester Group <span className="text-red-500">*</span>
                </label>
                <select
                  required
                  value={formData.semesterGroup}
                  onChange={(e) => setFormData({ ...formData, semesterGroup: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
                >
                  {SEMESTER_GROUPS.map((g) => (
                    <option key={g.key} value={g.key}>{g.label}</option>
                  ))}
                </select>
              </div>
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
                  {DAY_ORDER.map((d) => <option key={d} value={d}>{d}</option>)}
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Start Time <span className="text-red-500">*</span>
                </label>
                <input
                  type="time" required value={formData.startTime}
                  onChange={(e) => setFormData({ ...formData, startTime: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  End Time <span className="text-red-500">*</span>
                </label>
                <input
                  type="time" required value={formData.endTime}
                  onChange={(e) => setFormData({ ...formData, endTime: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
                />
              </div>
              <div className="flex items-center col-span-2">
                <label className="flex items-center gap-2 text-sm font-medium text-gray-700 dark:text-gray-300 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={formData.isBreak}
                    onChange={(e) => setFormData({ ...formData, isBreak: e.target.checked })}
                    className="w-4 h-4 rounded"
                  />
                  Mark as Break Slot
                </label>
              </div>
            </div>
            <button
              type="submit" disabled={loading}
              className="mt-4 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-md transition-colors disabled:opacity-50 text-sm"
            >
              {loading ? 'Creating…' : 'Create Time Slot'}
            </button>
          </form>
        </div>
      )}

      {/* ── Slot table ────────────────────────────────────────────────────── */}
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md overflow-hidden">
        <div className="p-4 border-b border-gray-200 dark:border-gray-700 flex items-center justify-between">
          <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
            {SEMESTER_GROUPS.find((g) => g.key === activeGroup)?.label} — Time Slots
          </h3>
          <span className="text-sm text-gray-500 dark:text-gray-400">
            {firstDaySlots.length} slots/day × 6 days
          </span>
        </div>

        {loading ? (
          <div className="p-6 text-center text-gray-500 dark:text-gray-400">Loading…</div>
        ) : timeSlots.length === 0 ? (
          <div className="p-6 text-center text-gray-500 dark:text-gray-400">
            No slots found for this group.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-50 dark:bg-gray-700">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">Day</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">Start</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">End</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">Break?</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                {Object.entries(slotsByDay).map(([day, slots]) =>
                  slots.map((slot, idx) => (
                    <tr
                      key={slot.id}
                      className={`hover:bg-gray-50 dark:hover:bg-gray-700 ${slot.breakSlot ? 'bg-amber-50 dark:bg-amber-900/20' : ''
                        }`}
                    >
                      {/* Day cell only on first row of each day */}
                      <td className="px-4 py-2 text-sm font-medium text-gray-900 dark:text-white">
                        {idx === 0 ? day.charAt(0) + day.slice(1).toLowerCase() : ''}
                      </td>

                      {editingId === slot.id ? (
                        <>
                          <td className="px-2 py-1">
                            <input type="time" value={editData.startTime}
                              onChange={(e) => setEditData({ ...editData, startTime: e.target.value })}
                              className="px-2 py-1 border rounded text-sm dark:bg-gray-700 dark:text-white w-28"
                            />
                          </td>
                          <td className="px-2 py-1">
                            <input type="time" value={editData.endTime}
                              onChange={(e) => setEditData({ ...editData, endTime: e.target.value })}
                              className="px-2 py-1 border rounded text-sm dark:bg-gray-700 dark:text-white w-28"
                            />
                          </td>
                          <td className="px-2 py-1">
                            <input type="checkbox" checked={editData.isBreak}
                              onChange={(e) => setEditData({ ...editData, isBreak: e.target.checked })}
                              className="w-4 h-4"
                            />
                          </td>
                          <td className="px-2 py-1">
                            <div className="flex gap-2">
                              <button onClick={handleEditSave} disabled={saving}
                                className="px-3 py-1 bg-green-600 hover:bg-green-700 text-white rounded text-xs disabled:opacity-50">
                                {saving ? '…' : 'Save'}
                              </button>
                              <button onClick={() => setEditingId(null)}
                                className="px-3 py-1 bg-gray-400 hover:bg-gray-500 text-white rounded text-xs">
                                Cancel
                              </button>
                            </div>
                          </td>
                        </>
                      ) : (
                        <>
                          <td className="px-4 py-2 text-sm text-gray-900 dark:text-white">{formatTime(slot.startTime)}</td>
                          <td className="px-4 py-2 text-sm text-gray-900 dark:text-white">{formatTime(slot.endTime)}</td>
                          <td className="px-4 py-2">
                            <button
                              onClick={() => handleToggleBreak(slot)}
                              disabled={togglingId === slot.id}
                              className={`px-2 py-0.5 rounded-full text-xs font-medium transition-colors ${slot.breakSlot
                                  ? 'bg-amber-200 text-amber-800 hover:bg-amber-300 dark:bg-amber-800 dark:text-amber-100'
                                  : 'bg-gray-100 text-gray-600 hover:bg-gray-200 dark:bg-gray-700 dark:text-gray-300'
                                } disabled:opacity-50`}
                            >
                              {togglingId === slot.id ? '…' : slot.breakSlot ? 'Break ✓' : 'Class'}
                            </button>
                          </td>
                          <td className="px-4 py-2">
                            <div className="flex gap-2">
                              <button onClick={() => startEdit(slot)}
                                className="px-3 py-1 bg-yellow-500 hover:bg-yellow-600 text-white rounded text-xs">
                                Edit
                              </button>
                              <button onClick={() => handleDelete(slot.id)} disabled={deletingId === slot.id}
                                className="px-3 py-1 bg-red-600 hover:bg-red-700 text-white rounded text-xs disabled:opacity-60">
                                {deletingId === slot.id ? '…' : 'Delete'}
                              </button>
                            </div>
                          </td>
                        </>
                      )}
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
