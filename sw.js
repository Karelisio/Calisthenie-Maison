const CACHE_NAME = 'calisthenie-maison-v4';
const ASSETS = ['./index.html', './manifest.json', './icon-192.png', './icon-512.png'];
// Past this delay the cached copy is served, so a weak connection does not
// leave the app loading; the network response still refreshes the cache.
const NETWORK_TIMEOUT_MS = 4000;

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => cache.addAll(ASSETS)).then(() => self.skipWaiting())
  );
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(keys.filter((k) => k !== CACHE_NAME).map((k) => caches.delete(k)))
    ).then(() => self.clients.claim())
  );
});

// Network-first: always fetch the live page when online (so updates show up
// immediately), falling back to the cached copy when offline or when the
// network is too slow.
self.addEventListener('fetch', (event) => {
  const req = event.request;
  if (req.method !== 'GET') return;

  let cachePut = Promise.resolve();
  const fromNetwork = fetch(req).then((response) => {
    // Never cache an error page (404/5xx): it would be served offline
    // instead of the last good copy.
    if (response.ok) {
      const copy = response.clone();
      cachePut = caches.open(CACHE_NAME).then((cache) => cache.put(req, copy));
    }
    return response;
  });
  // Keep the worker alive until the cache is refreshed, even when the cached
  // copy was served first because the network was slow.
  event.waitUntil(fromNetwork.then(() => cachePut).catch(() => {}));

  // A page that was never cached (e.g. "./" or a URL with a query string)
  // still opens the app offline.
  const fromCache = () => caches.match(req)
    .then((cached) => cached || (req.mode === 'navigate' ? caches.match('./index.html') : undefined))
    .catch(() => undefined);

  event.respondWith(new Promise((resolve) => {
    let done = false;
    const finish = (response) => { if (!done && response) { done = true; resolve(response); } };
    const timer = setTimeout(() => fromCache().then(finish), NETWORK_TIMEOUT_MS);
    fromNetwork.then(
      (response) => { clearTimeout(timer); finish(response); },
      () => { clearTimeout(timer); fromCache().then((cached) => finish(cached || Response.error())); }
    );
  }));
});
