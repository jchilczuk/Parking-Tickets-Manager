/* eslint-disable no-restricted-globals */

self.addEventListener('install', function (event) {
  console.log('[Service Worker] Installed');
});

self.addEventListener('activate', function (event) {
  console.log('[Service Worker] Activated');
});

self.addEventListener('fetch', function (event) {
  // Możesz zostawić pusty – niech nie przechwytuje zapytań
});
