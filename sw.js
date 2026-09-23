const CACHE_NAME = 'cartelgo-pwa-v4-6-20260923';
const ASSETS = [
  './index.html',
  './manifest.webmanifest',
  './icon-192.png',
  './icon-512.png',
  './icon-maskable-192.png',
  './icon-maskable-512.png',
  './apple-touch-icon.png',
  './favicon-64.png',
  './social-preview.jpg',
  './social-preview.png'
];

self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(CACHE_NAME).then(cache => cache.addAll(ASSETS))
  );
  self.skipWaiting();
});

self.addEventListener('activate', event => {
  // Esta PWA no enumera ni borra cachés de otras apps ni versiones anteriores.
  event.waitUntil(self.clients.claim());
});

self.addEventListener('fetch', event => {
  if(event.request.method !== 'GET') return;

  const url = new URL(event.request.url);
  if(url.origin !== self.location.origin) return;

  // Para la propia página: RED PRIMERO.
  // Así F5 obtiene la versión publicada en GitHub y la caché queda solo como respaldo offline.
  if(event.request.mode === 'navigate' || url.pathname.endsWith('/index.html')){
    event.respondWith((async () => {
      const cache = await caches.open(CACHE_NAME);
      try{
        const response = await fetch(event.request, {cache:'no-store'});
        if(response && response.ok){
          cache.put('./index.html', response.clone());
        }
        return response;
      }catch{
        return (await cache.match('./index.html')) || Response.error();
      }
    })());
    return;
  }

  // Recursos estáticos: caché propia primero, red como respaldo/actualización si faltan.
  event.respondWith((async () => {
    const cache = await caches.open(CACHE_NAME);
    const cached = await cache.match(event.request);
    if(cached) return cached;

    try{
      const response = await fetch(event.request);
      if(response && response.ok && response.type !== 'opaque'){
        cache.put(event.request, response.clone());
      }
      return response;
    }catch{
      return Response.error();
    }
  })());
});
