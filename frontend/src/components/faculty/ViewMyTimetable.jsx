import React, { useState, useEffect, useMemo } from 'react';
import { timetableAPI, timeSlotAPI } from '../../utils/api';

const daysOfWeek = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];

function fmt(t) {
  if (!t) return '';
  const [h, m] = t.split(':');
  return `${h}:${m}`;
}

const ViewMyTimetable = ({ facultyId }) => {
  const [timeSlots, setTimeSlots] = useState([]);
  const [timetable, setTimetable] = useState([]);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState({ type: '', text: '' });

  useEffect(() => {
    if (facultyId) {
      loadTimeSlots();
      loadTimetable();
    }
  }, [facultyId]);

  const loadTimeSlots = async () => {
    try {
      const data = await timeSlotAPI.getAll();
      setTimeSlots(Array.isArray(data) ? data : []);
    } catch (e) {
      setMessage({ type: 'error', text: `Error loading time slots: ${e.message}` });
    }
  };

  const loadTimetable = async () => {
    setLoading(true);
    setMessage({ type: '', text: '' });
    try {
      const data = await timetableAPI.getForFaculty(facultyId);
      setTimetable(Array.isArray(data) ? data : []);
      if (data && data.length === 0) {
        setMessage({ type: 'info', text: 'No scheduled classes found. The timetable may not have been published yet.' });
      }
    } catch (error) {
      setMessage({ type: 'error', text: `Error loading timetable: ${error.message}` });
    } finally {
      setLoading(false);
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

  const classByKey = useMemo(() => {
    const map = new Map();
    for (const sc of timetable) {
      const day = (sc.dayOfWeek || '').toUpperCase();
      const key = `${day}|${sc.startTime}`;
      map.set(key, sc);
    }
    return map;
  }, [timetable]);

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
          const contiguous = prevEnd === nslot.startTime;
          if (!(sameOffering && sameRoom && contiguous)) break;
          span += 1;
          prevEnd = nslot.endTime;
        }

        cells.push(
          <td key={`${day}-class-${start}`} colSpan={span}
            className="align-top px-2 py-2 border border-gray-200 dark:border-gray-700 bg-blue-50 dark:bg-blue-900/30"
          >
            <div className="text-xs font-semibold text-blue-800 dark:text-blue-200">
              {sc?.courseOffering?.course?.courseCode || 'COURSE'}
            </div>
            <div className="text-[11px] text-gray-700 dark:text-gray-300">
              {sc?.courseOffering?.section?.name || 'Section'} (Sem {sc?.courseOffering?.section?.semester || '-'})
            </div>
            <div className="text-[11px] text-gray-500 dark:text-gray-400">Room: {sc?.room?.roomNumber || '-'}</div>
          </td>
        );
        i += span;
        continue;
      }

      cells.push(
        <td key={`${day}-empty-${start}`}
          className="px-2 py-6 text-center text-xs border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800">
        </td>
      );
      i += 1;
    }
    return cells;
  };

  if (!facultyId) {
    return <div className="p-4">Loading faculty details...</div>;
  }

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-2xl font-bold text-gray-900 dark:text-white">My Timetable</h2>
        <button
          onClick={loadTimetable}
          disabled={loading}
          className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-md transition-colors disabled:opacity-50"
        >
          {loading ? 'Loading...' : 'Refresh'}
        </button>
      </div>

      {message.text && (
        <div
          className={`mb-4 p-3 rounded-md ${message.type === 'success'
            ? 'bg-green-100 dark:bg-green-900 text-green-700 dark:text-green-300'
            : message.type === 'info'
              ? 'bg-blue-100 dark:bg-blue-900 text-blue-700 dark:text-blue-300'
              : 'bg-red-100 dark:bg-red-900 text-red-700 dark:text-red-300'
            }`}
        >
          {message.text}
        </div>
      )}

      {loading ? (
        <div className="text-center py-8 text-gray-600 dark:text-gray-400">Loading timetable...</div>
      ) : timetable.length === 0 ? (
        <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow-md text-center text-gray-600 dark:text-gray-400">
          No scheduled classes found. Please check back later or contact the administrator.
        </div>
      ) : (
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
      )}
    </div>
  );
};

export default ViewMyTimetable;

