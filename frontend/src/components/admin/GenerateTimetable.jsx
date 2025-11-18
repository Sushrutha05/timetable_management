import React, { useState } from 'react';
import { timetableAPI } from '../../utils/api';

const GenerateTimetable = () => {
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState({ type: '', text: '' });
  const [generatedClasses, setGeneratedClasses] = useState(null);

  const handleGenerate = async () => {
    if (!window.confirm('This will clear the old schedule and generate a new timetable. Continue?')) {
      return;
    }

    setLoading(true);
    setMessage({ type: '', text: '' });
    setGeneratedClasses(null);

    try {
      const data = await timetableAPI.generate();
      setGeneratedClasses(data);
      setMessage({ type: 'success', text: `Timetable generated successfully! ${data?.length || 0} classes scheduled.` });
    } catch (error) {
      setMessage({ type: 'error', text: `Error: ${error.message}` });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <div className="mb-6">
        <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-4">Generate Master Timetable</h2>
        <p className="text-gray-600 dark:text-gray-400 mb-6">
          This will generate a complete timetable based on all course offerings, faculty preferences, room availability, and time slots.
          The previous schedule will be cleared.
        </p>
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

      <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow-md mb-6">
        <button
          onClick={handleGenerate}
          disabled={loading}
          className="px-6 py-3 bg-blue-600 hover:bg-blue-700 text-white font-medium rounded-md transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {loading ? 'Generating...' : 'Generate Master Timetable'}
        </button>
      </div>

      {generatedClasses && generatedClasses.length > 0 && (
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md overflow-hidden">
          <div className="p-4 border-b border-gray-200 dark:border-gray-700">
            <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
              Generated Classes ({generatedClasses.length})
            </h3>
          </div>
          <div className="overflow-x-auto max-h-96">
            <table className="w-full">
              <thead className="bg-gray-50 dark:bg-gray-700 sticky top-0">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">Class ID</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">Course</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">Faculty</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">Room</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">Day</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase">Time</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                {generatedClasses.map((cls) => (
                  <tr key={cls.id} className="hover:bg-gray-50 dark:hover:bg-gray-700">
                    <td className="px-4 py-3 text-sm text-gray-900 dark:text-white">{cls.id}</td>
                    <td className="px-4 py-3 text-sm text-gray-900 dark:text-white">
                      {cls.courseOffering?.course?.courseCode || 'N/A'}
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-900 dark:text-white">
                      {cls.courseOffering?.faculty?.firstName} {cls.courseOffering?.faculty?.lastName}
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

export default GenerateTimetable;

