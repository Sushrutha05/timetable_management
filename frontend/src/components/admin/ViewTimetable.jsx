import React, { useState, useEffect } from 'react';
import { timetableAPI } from '../../utils/api';

const ViewTimetable = () => {
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
      const data = await timetableAPI.getFull();
      setTimetable(data || []);
    } catch (error) {
      // If endpoint doesn't exist, try using generate endpoint response or show message
      setMessage({ type: 'error', text: `Error loading timetable: ${error.message}. Note: You may need to generate the timetable first.` });
    } finally {
      setLoading(false);
    }
  };

  // Group classes by day and time for better visualization
  const groupByDay = () => {
    const grouped = {};
    timetable.forEach((cls) => {
      const day = cls.dayOfWeek;
      if (!grouped[day]) {
        grouped[day] = [];
      }
      grouped[day].push(cls);
    });

    // Sort each day's classes by start time
    Object.keys(grouped).forEach((day) => {
      grouped[day].sort((a, b) => {
        if (a.startTime < b.startTime) return -1;
        if (a.startTime > b.startTime) return 1;
        return 0;
      });
    });

    return grouped;
  };

  const groupedTimetable = groupByDay();
  const daysOfWeek = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-2xl font-bold text-gray-900 dark:text-white">View Full Timetable</h2>
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
          No timetable data available. Please generate the timetable first.
        </div>
      ) : (
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md overflow-hidden">
          <div className="p-4 border-b border-gray-200 dark:border-gray-700">
            <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
              Master Timetable ({timetable.length} classes)
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
                      {groupedTimetable[day].map((cls) => (
                        <div
                          key={cls.id}
                          className="p-2 bg-blue-50 dark:bg-blue-900/30 rounded text-sm border border-blue-200 dark:border-blue-800"
                        >
                          <div className="font-medium text-gray-900 dark:text-white">
                            {cls.courseOffering?.course?.courseCode || 'N/A'}
                          </div>
                          <div className="text-xs text-gray-600 dark:text-gray-400">
                            {cls.courseOffering?.faculty?.firstName} {cls.courseOffering?.faculty?.lastName}
                          </div>
                          <div className="text-xs text-gray-600 dark:text-gray-400">
                            Room: {cls.room?.roomNumber || 'N/A'}
                          </div>
                          <div className="text-xs text-gray-600 dark:text-gray-400">
                            {cls.startTime} - {cls.endTime}
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
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">Class ID</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">Course</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">Faculty</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">Section</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">Room</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">Day</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">Time</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                {timetable.map((cls) => (
                  <tr key={cls.id} className="hover:bg-gray-50 dark:hover:bg-gray-700">
                    <td className="px-4 py-3 text-sm text-gray-900 dark:text-white">{cls.id}</td>
                    <td className="px-4 py-3 text-sm text-gray-900 dark:text-white">
                      {cls.courseOffering?.course?.courseCode || 'N/A'} - {cls.courseOffering?.course?.courseName || 'N/A'}
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-900 dark:text-white">
                      {cls.courseOffering?.faculty?.firstName} {cls.courseOffering?.faculty?.lastName}
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-900 dark:text-white">
                      {cls.courseOffering?.section?.name || 'N/A'}
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-900 dark:text-white">
                      {cls.room?.roomNumber || 'N/A'}
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-900 dark:text-white">{cls.dayOfWeek}</td>
                    <td className="px-4 py-3 text-sm text-gray-900 dark:text-white">
                      {cls.startTime} - {cls.endTime}
                    </td>
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

export default ViewTimetable;

