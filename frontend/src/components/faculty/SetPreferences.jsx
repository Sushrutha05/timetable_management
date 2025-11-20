import React, { useState, useEffect } from 'react';
import { facultyPreferenceAPI, courseAPI } from '../../utils/api';

const SetPreferences = () => {
  const facultyId = 1; // Hard-coded as per requirements
  const [courses, setCourses] = useState([]);
  const [preferences, setPreferences] = useState([{ courseId: '', priority: '' }]);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState({ type: '', text: '' });

  useEffect(() => {
    loadCourses();
    loadPreferences();
  }, []);

  const loadCourses = async () => {
    try {
      const data = await courseAPI.getAll();
      setCourses(data);
    } catch (error) {
      setMessage({ type: 'error', text: `Error loading courses: ${error.message}` });
    }
  };

  const loadPreferences = async () => {
    try {
      const data = await facultyPreferenceAPI.getPreferences(facultyId);
      if (data && data.length > 0) {
        setPreferences(
          data.map((pref) => ({
            courseId: pref.course?.id?.toString() || '',
            priority: pref.priority?.toString() || '',
          }))
        );
      }
    } catch (error) {
      // Preferences might not exist yet, that's okay
      console.log('No existing preferences found');
    }
  };

  const handleAddPreference = () => {
    setPreferences([...preferences, { courseId: '', priority: '' }]);
  };

  const handleRemovePreference = (index) => {
    if (preferences.length > 1) {
      setPreferences(preferences.filter((_, i) => i !== index));
    }
  };

  const handlePreferenceChange = (index, field, value) => {
    const newPreferences = [...preferences];
    newPreferences[index][field] = value;
    setPreferences(newPreferences);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setMessage({ type: '', text: '' });

    // Filter out incomplete preferences
    const validPreferences = preferences.filter((pref) => pref.courseId && pref.priority);

    if (validPreferences.length === 0) {
      setMessage({ type: 'error', text: 'Please add at least one preference with both course and priority.' });
      setLoading(false);
      return;
    }

    try {
      const payload = {
        preferences: validPreferences.map((pref) => ({
          courseId: parseInt(pref.courseId),
          priority: parseInt(pref.priority),
        })),
      };

      await facultyPreferenceAPI.setPreferences(facultyId, payload);
      setMessage({ type: 'success', text: 'Preferences saved successfully!' });
    } catch (error) {
      setMessage({ type: 'error', text: `Error: ${error.message}` });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <div className="mb-6">
        <h2 className="text-2xl font-bold text-gray-900 dark:text-white">Set Teaching Preferences</h2>
        <p className="text-gray-600 dark:text-gray-400 mt-2">
          Set your preferences for courses you would like to teach. Lower priority numbers indicate higher preference.
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

      <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow-md">
        <form onSubmit={handleSubmit}>
          <div className="space-y-4">
            {preferences.map((pref, index) => (
              <div key={index} className="flex gap-4 items-end p-4 border border-gray-200 dark:border-gray-700 rounded-lg">
                <div className="flex-1">
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                    Course <span className="text-red-500">*</span>
                  </label>
                  <select
                    required={preferences.length === 1 || pref.courseId !== ''}
                    value={pref.courseId}
                    onChange={(e) => handlePreferenceChange(index, 'courseId', e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
                  >
                    <option value="">Select a course</option>
                    {courses
                      .filter((course) => {
                        // Don't show courses already selected in other preferences
                        const isSelectedElsewhere = preferences.some(
                          (p, i) => i !== index && p.courseId === course.id.toString()
                        );
                        return !isSelectedElsewhere;
                      })
                      .map((course) => (
                        <option key={course.id} value={course.id}>
                          {course.courseCode} - {course.courseName}
                        </option>
                      ))}
                  </select>
                </div>
                <div className="w-32">
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                    Priority <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="number"
                    required={preferences.length === 1 || pref.priority !== ''}
                    min="1"
                    value={pref.priority}
                    onChange={(e) => handlePreferenceChange(index, 'priority', e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
                    placeholder="1 = highest"
                  />
                </div>
                {preferences.length > 1 && (
                  <button
                    type="button"
                    onClick={() => handleRemovePreference(index)}
                    className="px-4 py-2 bg-red-600 hover:bg-red-700 text-white rounded-md transition-colors"
                  >
                    Remove
                  </button>
                )}
              </div>
            ))}
          </div>

          <div className="mt-4 flex gap-4">
            <button
              type="button"
              onClick={handleAddPreference}
              className="px-4 py-2 bg-gray-200 dark:bg-gray-700 hover:bg-gray-300 dark:hover:bg-gray-600 text-gray-800 dark:text-gray-200 rounded-md transition-colors"
            >
              Add Another Preference
            </button>
            <button
              type="submit"
              disabled={loading}
              className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-md transition-colors disabled:opacity-50"
            >
              {loading ? 'Saving...' : 'Save Preferences'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default SetPreferences;

