import React, { useState } from 'react';
import { timetableAPI } from '../../utils/api';

const PARITY_OPTIONS = [
  {
    parity: 'EVEN',
    label: 'Generate Even Semester Timetable',
    sublabel: 'Semesters 2, 4, 6, 8',
    color: 'blue',
    icon: '📘',
  },
  {
    parity: 'ODD',
    label: 'Generate Odd Semester Timetable',
    sublabel: 'Semesters 1, 3, 5, 7',
    color: 'indigo',
    icon: '📗',
  },
];

const GenerateTimetable = () => {
  const [loading, setLoading] = useState(null); // 'EVEN' | 'ODD' | null
  const [messages, setMessages] = useState({ EVEN: null, ODD: null });
  const [counts, setCounts] = useState({ EVEN: null, ODD: null });

  const handleGenerate = async (parity) => {
    const label = parity === 'EVEN' ? 'Even (Sem 2, 4, 6, 8)' : 'Odd (Sem 1, 3, 5, 7)';
    if (!window.confirm(
      `This will clear the existing ${label} semester schedule and generate a new one.\nThe other semester's timetable will NOT be affected.\n\nContinue?`
    )) return;

    setLoading(parity);
    setMessages((prev) => ({ ...prev, [parity]: null }));
    setCounts((prev) => ({ ...prev, [parity]: null }));

    try {
      const data = await timetableAPI.generate(parity);
      const count = data?.length || 0;
      setCounts((prev) => ({ ...prev, [parity]: count }));
      setMessages((prev) => ({
        ...prev,
        [parity]: { type: 'success', text: `✅ ${label} timetable generated! ${count} classes scheduled.` },
      }));
    } catch (error) {
      setMessages((prev) => ({
        ...prev,
        [parity]: { type: 'error', text: `❌ ${error.message}` },
      }));
    } finally {
      setLoading(null);
    }
  };

  const colorMap = {
    blue: {
      btn: 'bg-blue-600 hover:bg-blue-700 focus:ring-blue-500',
      badge: 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-300',
    },
    indigo: {
      btn: 'bg-indigo-600 hover:bg-indigo-700 focus:ring-indigo-500',
      badge: 'bg-indigo-100 text-indigo-800 dark:bg-indigo-900 dark:text-indigo-300',
    },
  };

  return (
    <div>
      <div className="mb-6">
        <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-2">
          Generate Timetable
        </h2>
        <p className="text-gray-600 dark:text-gray-400">
          Even and odd semesters run in different halves of the year, so they are
          generated independently. Faculty workloads and room conflicts are resolved
          separately for each parity.
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
        {PARITY_OPTIONS.map(({ parity, label, sublabel, color, icon }) => {
          const c = colorMap[color];
          const msg = messages[parity];
          const isLoading = loading === parity;
          const isDisabled = loading !== null;
          const count = counts[parity];

          return (
            <div
              key={parity}
              className="bg-white dark:bg-gray-800 rounded-lg shadow-md p-6 flex flex-col gap-4"
            >
              <div className="flex items-center gap-3">
                <span className="text-3xl">{icon}</span>
                <div>
                  <h3 className="text-lg font-semibold text-gray-900 dark:text-white">{label}</h3>
                  <span className={`text-xs font-medium px-2 py-0.5 rounded ${c.badge}`}>
                    {sublabel}
                  </span>
                </div>
              </div>

              <p className="text-sm text-gray-500 dark:text-gray-400">
                Clears only the {parity.toLowerCase()} semester schedule. The{' '}
                {parity === 'EVEN' ? 'odd' : 'even'} semester timetable is preserved.
              </p>

              <button
                onClick={() => handleGenerate(parity)}
                disabled={isDisabled}
                className={`px-5 py-2.5 text-white font-medium rounded-md transition-colors
                  focus:outline-none focus:ring-2 focus:ring-offset-2
                  disabled:opacity-50 disabled:cursor-not-allowed ${c.btn}`}
              >
                {isLoading ? (
                  <span className="flex items-center justify-center gap-2">
                    <svg className="animate-spin h-4 w-4" viewBox="0 0 24 24" fill="none">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                      <path className="opacity-75" fill="currentColor"
                        d="M4 12a8 8 0 018-8v8H4z" />
                    </svg>
                    Generating…
                  </span>
                ) : (
                  label
                )}
              </button>

              {msg && (
                <div
                  className={`text-sm rounded-md px-3 py-2 ${msg.type === 'success'
                      ? 'bg-green-50 dark:bg-green-900/30 text-green-700 dark:text-green-300'
                      : 'bg-red-50 dark:bg-red-900/30 text-red-700 dark:text-red-300'
                    }`}
                >
                  {msg.text}
                </div>
              )}

              {count !== null && count > 0 && (
                <p className="text-xs text-gray-500 dark:text-gray-400">
                  {count} scheduled class entries created.
                </p>
              )}
            </div>
          );
        })}
      </div>

      {/* Collapsible info note */}
      <div className="bg-yellow-50 dark:bg-yellow-900/20 border border-yellow-200 dark:border-yellow-700 rounded-md p-4 text-sm text-yellow-800 dark:text-yellow-300">
        <strong>Note:</strong> Both generations share the same room and faculty data,
        but faculty workloads are computed independently per parity. This mirrors how
        the university schedule actually works — even and odd semesters do not run
        simultaneously.
      </div>
    </div>
  );
};

export default GenerateTimetable;
