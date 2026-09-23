const CACHE_NAME = 'cartelgo-pwa-v4-20260923';
const ASSETS = [
  './',
  './index.html',
  './manifest.webmanifest',
  './icon-192.png',
  './icon-512.png'
];

self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(CACHE_NAME).then(cache => cache.addAll(ASSETS))
  );
  self.skipWaiting();
});

self.addEventListener('activate', event => {
  // No se enumeran ni se borran caches ajenas.
  event.waitUntil(self.clients.claim());
});

self.addEventListener('fetch', event => {
  if(event.request.method !== 'GET') return;

  const url = new URL(event.request.url);
  if(url.origin !== self.location.origin) return;

  event.respondWith(
    caches.open(CACHE_NAME).then(async cache => {
      const cached = await cache.match(event.request);
      if(cached) return cached;

      try{
        const response = await fetch(event.request);
        if(response && response.status === 200 && response.type !== 'opaque'){
          cache.put(event.request, response.clone());
        }
        return response;
      }catch{
        if(event.request.mode === 'navigate'){
          return (await cache.match('./index.html')) || Response.error();
        }
        return Response.error();
      }
    })
  );
});
