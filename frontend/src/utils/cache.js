/**
 * Lightweight in-memory TTL cache for the SPA session.
 *
 * Usage:
 *   import cache from './cache';
 *
 *   // On load  — returns cached data if fresh, otherwise fetches:
 *   const data = await cache.getOrFetch('faculty', () => facultyAPI.getAll(), 120_000);
 *
 *   // After mutation — force next load to re-fetch:
 *   cache.invalidate('faculty');
 */

const store = {}; // { key: { data, expiresAt } }

const cache = {
    /**
     * Return cached data if it exists and hasn't expired, otherwise call
     * `fetcher()`, store the result, and return it.
     *
     * @param {string}   key       - Cache key (e.g. 'faculty', 'courses')
     * @param {Function} fetcher   - Async function that returns the fresh data
     * @param {number}   ttl       - Time-to-live in ms (default: 2 minutes)
     */
    async getOrFetch(key, fetcher, ttl = 120_000) {
        const entry = store[key];
        if (entry && Date.now() < entry.expiresAt) {
            return entry.data;
        }
        const data = await fetcher();
        store[key] = { data, expiresAt: Date.now() + ttl };
        return data;
    },

    /** Force the next getOrFetch for `key` to re-fetch. */
    invalidate(key) {
        delete store[key];
    },

    /** Invalidate multiple keys at once. */
    invalidateMany(...keys) {
        keys.forEach((k) => delete store[k]);
    },

    /** Clear everything (e.g. on logout). */
    invalidateAll() {
        Object.keys(store).forEach((k) => delete store[k]);
    },
};

export default cache;
