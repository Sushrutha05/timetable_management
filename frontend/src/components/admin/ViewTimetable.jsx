import React, { useEffect, useMemo, useState } from 'react';
import { API_BASE_URL, sectionAPI, timeSlotAPI, timetableAPI, facultyAPI } from '../../utils/api';

const daysOfWeek = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];

function fmt(t) {
  if (!t) return '';
  const [h, m] = t.split(':');
  return `${h}:${m}`;
}

const ViewTimetable = () => {
  const [viewMode, setViewMode] = useState('SECTION'); // 'SECTION' or 'FACULTY'
  const [sections, setSections] = useState([]);
  const [selectedSection, setSelectedSection] = useState('');
  const [faculties, setFaculties] = useState([]);
  const [selectedFaculty, setSelectedFaculty] = useState('');
  const [timeSlots, setTimeSlots] = useState([]);
  const [timetable, setTimetable] = useState([]);
  const [status, setStatus] = useState('DRAFT');
  const [loading, setLoading] = useState(false);
  const [isPublishing, setIsPublishing] = useState(false);
  const [message, setMessage] = useState({ type: '', text: '' });
  const [dragging, setDragging] = useState(null); // { id, roomId }

  useEffect(() => {
    loadSections();
    loadFaculties();
    loadTimeSlots();
    loadStatus();
  }, []);

  const loadSections = async () => {
    try {
      const data = await sectionAPI.getAll();
      setSections(data || []);
    } catch (e) {
      setMessage({ type: 'error', text: `Error loading sections: ${e.message}` });
    }
  };

  const loadFaculties = async () => {
    try {
      const data = await facultyAPI.getAll();
      setFaculties(data || []);
    } catch (e) {
      setMessage({ type: 'error', text: `Error loading faculties: ${e.message}` });
    }
  };

  const loadTimeSlots = async () => {
    try {
      const data = await timeSlotAPI.getAll();
      setTimeSlots(Array.isArray(data) ? data : []);
    } catch (e) {
      setMessage({ type: 'error', text: `Error loading time slots: ${e.message}` });
    }
  };

  const loadStatus = async () => {
    try {
      const data = await timetableAPI.getStatus();
      setStatus(data || 'DRAFT');
    } catch (e) {
      console.error('Failed to fetch status', e);
    }
  };

  const loadTimetable = async () => {
    setLoading(true);
    try {
      let data = [];
      if (viewMode === 'SECTION' && selectedSection) {
        data = await timetableAPI.getForSection(selectedSection);
      } else if (viewMode === 'FACULTY' && selectedFaculty) {
        data = await timetableAPI.getForFaculty(selectedFaculty);
      } else {
        // If nothing selected, maybe get full or empty. Let's get empty to enforce filtering.
        setTimetable([]);
        setLoading(false);
        return;
      }
      setTimetable(Array.isArray(data) ? data : []);
    } catch (e) {
      setMessage({ type: 'error', text: `Error loading timetable: ${e.message}` });
    } finally {
      setLoading(false);
    }
  };

  const publishTimetable = async () => {
    if (!window.confirm("Are you sure you want to publish the timetable? This will freeze edits for this generation.")) return;
    setIsPublishing(true);
    try {
      await timetableAPI.publish();
      setStatus('PUBLISHED');
      setMessage({ type: 'success', text: 'Timetable published successfully!' });
    } catch (e) {
      setMessage({ type: 'error', text: `Error publishing: ${e.message}` });
    } finally {
      setIsPublishing(false);
    }
  };

  const slotsByDay = useMemo(() => {
    const map = {};
    for (const d of daysOfWeek) map[d] = [];
    const sorted = [...timeSlots].sort((a, b) => {
      const da = (a.dayOfWeek || '').toUpperCase();
      const db = (b.dayOfWeek || '').toUpperCase();
      if (da !== db) return daysOfWeek.indexOf(da) - daysOfWeek.indexOf(db);
      return (a.startTime || '').localeCompare(b.startTime || '');
    });
    for (const ts of sorted) {
      const day = (ts.dayOfWeek || '').toUpperCase();
      if (!map[day]) map[day] = [];
      map[day].push(ts);
    }
    return map;
  }, [timeSlots]);

  const columnTimes = useMemo(() => {
    const set = new Set();
    for (const ts of timeSlots) {
      if (!ts.isBreakSlot) set.add(ts.startTime);
    }
    return Array.from(set).sort();
  }, [timeSlots]);

  const filteredClasses = timetable; // Already filtered from the backend based on viewMode

  const classByKey = useMemo(() => {
    const map = new Map();
    for (const sc of filteredClasses) {
      const day = (sc.dayOfWeek || '').toUpperCase();
      const key = `${day}|${sc.startTime}`;
      map.set(key, sc);
    }
    return map;
  }, [filteredClasses]);

  const slotMapByDay = useMemo(() => {
    const map = {};
    for (const d of daysOfWeek) map[d] = {};
    for (const d of daysOfWeek) {
      const arr = slotsByDay[d] || [];
      for (const ts of arr) {
        map[d][ts.startTime] = ts;
      }
    }
    return map;
  }, [slotsByDay]);

  const onDragStart = (sc) => (e) => {
    if (status === 'PUBLISHED') return; // Prevent drag if published
    if (viewMode === 'FACULTY') return; // Prevent drag if in faculty view to avoid context confusion
    setDragging({ id: sc.id, roomId: sc.room?.id });
    e.dataTransfer.effectAllowed = 'move';
  };

  const onDragEnd = () => setDragging(null);

  const canDrop = (day, startTime) => {
    if (status === 'PUBLISHED') return false;
    if (viewMode === 'FACULTY') return false;
    const ts = slotMapByDay[day]?.[startTime];
    if (!ts || ts.isBreakSlot) return false;
    const exists = classByKey.has(`${day}|${startTime}`);
    return !exists;
  };

  const handleDrop = (day, startTime) => async (e) => {
    e.preventDefault();
    if (!dragging) return;
    if (!canDrop(day, startTime)) return;

    const payload = {
      scheduledClassId: dragging.id,
      newRoomId: dragging.roomId,
      newDayOfWeek: day,
      newStartTime: startTime,
    };

    try {
      const resp = await fetch(`${API_BASE_URL}/api/admin/timetable/update-slot`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });
      if (!resp.ok) {
        const txt = await resp.text();
        throw new Error(txt || `HTTP ${resp.status}`);
      }
      setMessage({ type: 'success', text: 'Updated successfully' });
      await loadTimetable();
    } catch (err) {
      setMessage({ type: 'error', text: String(err.message || err) });
    } finally {
      setDragging(null);
    }
  };

  const allowDrop = (day, startTime) => (e) => {
    if (dragging && canDrop(day, startTime)) {
      e.preventDefault();
    }
  };

  const renderDayRow = (day) => {
    const cells = [];
    let i = 0;
    while (i < columnTimes.length) {
      const start = columnTimes[i];
      const key = `${day}|${start}`;
      const slot = slotMapByDay[day]?.[start];
      const hasClass = classByKey.has(key);

      if (!slot) {
        cells.push(
          <td key={`${day}-${start}`} className="px-2 py-3 text-center text-xs text-gray-400 bg-gray-50 dark:bg-gray-800">
            —
          </td>
        );
        i += 1;
        continue;
      }

      if (slot.isBreakSlot) {
        let span = 1;
        for (let j = i + 1; j < columnTimes.length; j++) {
          const nextStart = columnTimes[j];
          const next = slotMapByDay[day]?.[nextStart];
          if (!next || !next.isBreakSlot) break;
          if (slot.endTime !== next.startTime) break;
          span += 1;
          slot.endTime = next.endTime;
        }
        cells.push(
          <td key={`${day}-break-${start}`} colSpan={span}
            className="px-2 py-3 text-center text-xs font-semibold bg-yellow-50 dark:bg-yellow-900/30 text-yellow-700 dark:text-yellow-300 border border-yellow-200 dark:border-yellow-800">
            BREAK
          </td>
        );
        i += span;
        continue;
      }

      if (hasClass) {
        const sc = classByKey.get(key);
        let span = 1;
        let prevEnd = slot.endTime;
        for (let j = i + 1; j < columnTimes.length; j++) {
          const ns = columnTimes[j];
          const nslot = slotMapByDay[day]?.[ns];
          const nk = `${day}|${ns}`;
          const nsc = classByKey.get(nk);
          if (!nslot || !nsc) break;
          const sameOffering = nsc?.courseOffering?.id === sc?.courseOffering?.id;
          const sameRoom = nsc?.room?.id === sc?.room?.id;
          const sameFaculty = nsc?.courseOffering?.faculty?.id === sc?.courseOffering?.faculty?.id;
          const contiguous = prevEnd === nslot.startTime;
          if (!(sameOffering && sameRoom && sameFaculty && contiguous)) break;
          span += 1;
          prevEnd = nslot.endTime;
        }

        cells.push(
          <td key={`${day}-class-${start}`} colSpan={span}
            className="align-top px-2 py-2 border border-gray-200 dark:border-gray-700 bg-blue-50 dark:bg-blue-900/30"
            draggable
            onDragStart={onDragStart(sc)}
            onDragEnd={onDragEnd}
          >
            <div className="text-xs font-semibold text-blue-800 dark:text-blue-200">
              {sc?.courseOffering?.course?.courseCode || 'COURSE'}
            </div>
            <div className="text-[11px] text-gray-700 dark:text-gray-300">
              {sc?.courseOffering?.faculty?.firstName} {sc?.courseOffering?.faculty?.lastName}
            </div>
            <div className="text-[11px] text-gray-500 dark:text-gray-400">Room: {sc?.room?.roomNumber || '-'}</div>
          </td>
        );
        i += span;
        continue;
      }

      const droppable = canDrop(day, start);
      cells.push(
        <td key={`${day}-empty-${start}`} data-day={day} data-start={start}
          onDragOver={allowDrop(day, start)} onDrop={handleDrop(day, start)}
          className={`px-2 py-6 text-center text-xs border border-dashed ${droppable ? 'border-green-400 dark:border-green-700 bg-white dark:bg-gray-900' : 'border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800'}`}>
          {droppable ? 'Drop here' : ''}
        </td>
      );
      i += 1;
    }
    return cells;
  };

  const handleDownload = () => {
    if (viewMode === 'SECTION') {
      if (!selectedSection) {
        setMessage({ type: 'error', text: 'Please select a section to download.' });
        return;
      }
      const url = `${API_BASE_URL}/api/admin/timetable/download/${selectedSection}?type=xlsx`;
      window.open(url, '_blank', 'noopener,noreferrer');
    } else {
      setMessage({ type: 'error', text: 'Downloads are currently only supported in Section view.' });
    }
  };

  return (
    <div>
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between mb-6">
        <div className="flex items-center gap-3">
          <h2 className="text-2xl font-bold text-gray-900 dark:text-white">Timetable Views</h2>
          <span className={`px-2 py-1 text-xs font-semibold rounded-full ${status === 'PUBLISHED' ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'}`}>
            {status}
          </span>
        </div>
        <div className="flex flex-col sm:flex-row gap-3 w-full md:w-auto">
          {status !== 'PUBLISHED' && (
            <button
              type="button"
              onClick={publishTimetable}
              disabled={isPublishing}
              className="px-4 py-2 bg-green-600 hover:bg-green-700 text-white rounded-md transition-colors disabled:opacity-50 font-medium"
            >
              {isPublishing ? 'Publishing...' : 'Publish Timetable'}
            </button>
          )}
        </div>
      </div>

      <div className="bg-white dark:bg-gray-800 p-4 rounded-lg shadow-sm mb-6 flex flex-col md:flex-row gap-4 items-end">
        <div className="w-full md:w-1/3">
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">View Mode</label>
          <select
            value={viewMode}
            onChange={(e) => {
              setViewMode(e.target.value);
              setTimetable([]);
              setSelectedSection('');
              setSelectedFaculty('');
            }}
            className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
          >
            <option value="SECTION">Section View (Student Focus)</option>
            <option value="FACULTY">Faculty View (Teacher Focus)</option>
          </select>
        </div>

        <div className="w-full md:w-1/3">
          {viewMode === 'SECTION' ? (
            <>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Select Section</label>
              <select
                value={selectedSection}
                onChange={(e) => setSelectedSection(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
              >
                <option value="">-- Choose Section --</option>
                {sections.map((section) => (
                  <option key={section.id} value={section.id}>
                    {section.name} ({section.department?.name || 'Dept'})
                  </option>
                ))}
              </select>
            </>
          ) : (
            <>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Select Faculty</label>
              <select
                value={selectedFaculty}
                onChange={(e) => setSelectedFaculty(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
              >
                <option value="">-- Choose Faculty --</option>
                {faculties.map((fac) => (
                  <option key={fac.id} value={fac.id}>
                    {fac.firstName} {fac.lastName} ({fac.department?.name || 'Dept'})
                  </option>
                ))}
              </select>
            </>
          )}
        </div>

        <div className="flex gap-2">
          <button
            onClick={loadTimetable}
            disabled={loading || (viewMode === 'SECTION' ? !selectedSection : !selectedFaculty)}
            className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-md transition-colors disabled:opacity-50"
          >
            {loading ? 'Loading...' : 'Load Timetable'}
          </button>
          {viewMode === 'SECTION' && (
            <button
              type="button"
              onClick={handleDownload}
              className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-md transition-colors disabled:opacity-50"
              disabled={!timetable.length}
            >
              Export
            </button>
          )}
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

      <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md overflow-x-auto">
        <table className="min-w-[900px] w-full border-collapse">
          <thead className="bg-gray-50 dark:bg-gray-700">
            <tr>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-700 dark:text-gray-300 uppercase sticky left-0 bg-gray-50 dark:bg-gray-700 z-10">Day</th>
              {columnTimes.map((t) => (
                <th key={`hdr-${t}`} className="px-4 py-3 text-left text-xs font-medium text-gray-700 dark:text-gray-300 uppercase">
                  {fmt(t)}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
            {daysOfWeek.map((day) => (
              <tr key={`row-${day}`}>
                <td className="px-4 py-3 text-sm font-semibold text-gray-900 dark:text-white sticky left-0 bg-white dark:bg-gray-800 z-10">
                  {day}
                </td>
                {renderDayRow(day)}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="mt-3 text-xs text-gray-500 dark:text-gray-400">
        Tip: Drag a class to an empty slot to move it. Invalid targets are non-droppable. Conflicts will be reported and the class will snap back.
      </div>
    </div>
  );
};

export default ViewTimetable;
