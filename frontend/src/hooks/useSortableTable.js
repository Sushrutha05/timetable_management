import { useState, useMemo } from 'react';

/**
 * Generic sortable-table hook.
 *
 * @param {Array}  data       - Raw array to sort
 * @param {string} defaultKey - Initial sort column key
 *
 * @returns {{ sortedData, sortKey, sortDir, handleSort }}
 *
 * Usage:
 *   const { sortedData, sortKey, sortDir, handleSort } =
 *     useSortableTable(facultyList, 'lastName');
 *
 *   // In JSX:
 *   <th onClick={() => handleSort('lastName')}>
 *     Last Name {sortKey === 'lastName' ? (sortDir === 'asc' ? '▲' : '▼') : '⇅'}
 *   </th>
 *
 *   // Render sortedData instead of raw list.
 *
 * Note: For nested values (e.g. faculty.department.name) pass a dot path:
 *   handleSort('department.name')
 */
function getNestedValue(obj, path) {
    return path.split('.').reduce((acc, part) => acc?.[part], obj);
}

export default function useSortableTable(data = [], defaultKey = '') {
    const [sortKey, setSortKey] = useState(defaultKey);
    const [sortDir, setSortDir] = useState('asc');

    const handleSort = (key) => {
        if (sortKey === key) {
            setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'));
        } else {
            setSortKey(key);
            setSortDir('asc');
        }
    };

    const sortedData = useMemo(() => {
        if (!sortKey || !data.length) return data;
        return [...data].sort((a, b) => {
            const aVal = getNestedValue(a, sortKey) ?? '';
            const bVal = getNestedValue(b, sortKey) ?? '';
            const cmp =
                typeof aVal === 'number' && typeof bVal === 'number'
                    ? aVal - bVal
                    : String(aVal).localeCompare(String(bVal), undefined, { numeric: true });
            return sortDir === 'asc' ? cmp : -cmp;
        });
    }, [data, sortKey, sortDir]);

    /** Render a sort indicator for a column header */
    const sortIcon = (key) => {
        if (sortKey !== key) return ' ⇅';
        return sortDir === 'asc' ? ' ▲' : ' ▼';
    };

    return { sortedData, sortKey, sortDir, handleSort, sortIcon };
}
