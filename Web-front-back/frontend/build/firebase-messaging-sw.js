/* eslint-disable no-restricted-globals */
/* eslint-env serviceworker */
/* global firebase importScripts self */

// ✅ Import Firebase SDK (kompatybilna wersja do SW)
importScripts("https://www.gstatic.com/firebasejs/10.10.0/firebase-app-compat.js");
importScripts("https://www.gstatic.com/firebasejs/10.10.0/firebase-messaging-compat.js");

// ✅ Inicjalizacja Firebase
firebase.initializeApp({
  apiKey: "AIzaSyAw7CWdZGvr4VMOxWb8j9viq3Wf0PSfd5g",
  authDomain: "arius-projekt.firebaseapp.com",
  projectId: "arius-projekt",
  storageBucket: "arius-projekt.appspot.com",
  messagingSenderId: "264390002037",
  appId: "1:264390002037:web:2ce7aeca9f91761b0aff3c"
});

const messaging = firebase.messaging();

// ✅ Obsługa powiadomień w tle (z FCM)
messaging.onBackgroundMessage(function (payload) {
  console.log('[firebase-messaging-sw.js] onBackgroundMessage:', payload);

  const { title = 'Powiadomienie', body = 'Masz nowe powiadomienie' } = payload.notification || {};
  const options = {
    body,
    icon: '/icon.png'
  };

  self.registration.showNotification(title, options);
});

// ✅ Web Push fallback – tylko jeśli nie FCM
/*self.addEventListener('push', function (event) {
  if (!event.data) return;

  try {
    const data = event.data.json();

    // 🔁 Jeśli pochodzi z FCM i już było obsłużone — pomiń
    if (data.firebaseMessaging) return;

    const title = data.notification?.title || 'Powiadomienie';
    const options = {
      body: data.notification?.body || 'Masz nowe powiadomienie',
      icon: '/icon.png'
    };

    event.waitUntil(self.registration.showNotification(title, options));
  } catch (err) {
    console.error('[SW] Push parsing error:', err);
  }
});*/

// ✅ Obsługa kliknięcia w powiadomienie
self.addEventListener('notificationclick', function (event) {
  event.notification.close();
  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then(clientList => {
      for (const client of clientList) {
        if (client.url === '/' && 'focus' in client) {
          return client.focus();
        }
      }
      return clients.openWindow('/');
    })
  );
});
