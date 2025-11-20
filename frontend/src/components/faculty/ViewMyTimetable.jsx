import React, { useState, useEffect } from 'react';
import { facultyPreferenceAPI } from '../../utils/api';

const ViewMyTimetable = () => {
  const facultyId = 1; // Hard-coded as per requirements
  const [timetable, setTimetable] = useState([]);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState({ type: '', text: '' });

  useEffect(() => {
    loadTimetable();
  }, []);

  const loadTimetable = async () => {
    setLoading(true);
    setMessage({ type: '', text: '' });

    try {
      // Note: The endpoint /api/faculty/{facultyId}/timetable currently returns preferences
      // In a real system, this should return ScheduledClass objects filtered by faculty
      // For now, we'll try to fetch and display what we can
      const data = await facultyPreferenceAPI.getTimetable(facultyId);
      
      // If the endpoint returns preferences instead of scheduled classes, we'll handle that
      // Otherwise, we expect it to return ScheduledClass objects
      setTimetable(Array.isArray(data) ? data : []);
      
      if (data && data.length === 0) {
        setMessage({ type: 'info', text: 'No scheduled classes found. The timetable may not have been generated yet.' });
      }
    } catch (error) {
      setMessage({ type: 'error', text: `Error loading timetable: ${error.message}` });
    } finally {
      setLoading(false);
    }
  };

  // Group classes by day for better visualization
  const groupByDay = () => {
    const grouped = {};
    timetable.forEach((cls) => {
      // Handle both ScheduledClass objects and preference objects
      const day = cls.dayOfWeek || cls.scheduledClass?.dayOfWeek || 'UNKNOWN';
      if (!grouped[day]) {
        grouped[day] = [];
      }
      grouped[day].push(cls);
    });

    // Sort each day's classes by start time
    Object.keys(grouped).forEach((day) => {
      grouped[day].sort((a, b) => {
        const timeA = a.startTime || a.scheduledClass?.startTime || '';
        const timeB = b.startTime || b.scheduledClass?.startTime || '';
        if (timeA < timeB) return -1;
        if (timeA > timeB) return 1;
        return 0;
      });
    });

    return grouped;
  };

  const groupedTimetable = groupByDay();
  const daysOfWeek = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];

  // Helper to extract course info from different object structures
  const getCourseInfo = (item) => {
    if (item.courseOffering?.course) {
      return item.courseOffering.course.courseCode || 'N/A';
    }
    if (item.course) {
      return item.course.courseCode || 'N/A';
    }
    if (item.scheduledClass?.courseOffering?.course) {
      return item.scheduledClass.courseOffering.course.courseCode || 'N/A';
    }
    return 'N/A';
  };

  const getFacultyInfo = (item) => {
    if (item.courseOffering?.faculty) {
      return `${item.courseOffering.faculty.firstName} ${item.courseOffering.faculty.lastName}`;
    }
    if (item.scheduledClass?.courseOffering?.faculty) {
      return `${item.scheduledClass.courseOffering.faculty.firstName} ${item.scheduledClass.courseOffering.faculty.lastName}`;
    }
    return 'N/A';
  };

  const getRoomInfo = (item) => {
    return item.room?.roomNumber || item.scheduledClass?.room?.roomNumber || 'N/A';
  };

  const getTimeInfo = (item) => {
    const startTime = item.startTime || item.scheduledClass?.startTime || '';
    const endTime = item.endTime || item.scheduledClass?.endTime || '';
    return startTime && endTime ? `${startTime} - ${endTime}` : 'N/A';
  };

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
          className={`mb-4 p-3 rounded-md ${
            message.type === 'success'
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
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md overflow-hidden">
          <div className="p-4 border-b border-gray-200 dark:border-gray-700">
            <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
              My Schedule ({timetable.length} classes)
            </h3>
          </div>
          <div className="overflow-x-auto p-4">
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
              {daysOfWeek.map((day) => (
                <div key={day} className="border border-gray-200 dark:border-gray-700 rounded-lg p-4">
                  <h4 className="font-semibold text-gray-900 dark:text-white mb-3 pb-2 border-b border-gray-200 dark:border-gray-700">
                    {day}
                  </h4>
                  {groupedTimetable[day] && groupedTimetable[day].length > 0 ? (
                    <div className="space-y-2">
                      {groupedTimetable[day].map((cls, idx) => (
                        <div
                          key={cls.id || idx}
                          className="p-2 bg-blue-50 dark:bg-blue-900/30 rounded text-sm border border-blue-200 dark:border-blue-800"
                        >
                          <div className="font-medium text-gray-900 dark:text-white">
                            {getCourseInfo(cls)}
                          </div>
                          <div className="text-xs text-gray-600 dark:text-gray-400">
                            Room: {getRoomInfo(cls)}
                          </div>
                          <div className="text-xs text-gray-600 dark:text-gray-400">
                            {getTimeInfo(cls)}
                          </div>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <div className="text-sm text-gray-500 dark:text-gray-400">No classes</div>
                  )}
                </div>
              ))}
            </div>
          </div>

          {/* Table View */}
          <div className="overflow-x-auto mt-4">
            <table className="w-full">
              <thead className="bg-gray-50 dark:bg-gray-700">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">Course</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">Room</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">Day</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">Time</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                {timetable.map((cls, idx) => (
                  <tr key={cls.id || idx} className="hover:bg-gray-50 dark:hover:bg-gray-700">
                    <td className="px-4 py-3 text-sm text-gray-900 dark:text-white">{getCourseInfo(cls)}</td>
                    <td className="px-4 py-3 text-sm text-gray-900 dark:text-white">{getRoomInfo(cls)}</td>
                    <td className="px-4 py-3 text-sm text-gray-900 dark:text-white">
                      {cls.dayOfWeek || cls.scheduledClass?.dayOfWeek || 'N/A'}
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-900 dark:text-white">{getTimeInfo(cls)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
};

export default ViewMyTimetable;

