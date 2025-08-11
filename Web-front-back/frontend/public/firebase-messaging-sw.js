/* eslint-disable no-restricted-globals */
/* eslint-env serviceworker */
/* global firebase importScripts self */

// public/firebase-messaging-sw.js
importScripts('https://www.gstatic.com/firebasejs/9.0.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/9.0.0/firebase-messaging-compat.js');

// Firebase config - MUSI się zgadzać z .env!
const firebaseConfig = {
  apiKey: "AIzaSyAw7CWdZGvr4VMOxWb8j9viq3Wf0PSfd5g",
  authDomain: "arius-projekt.firebaseapp.com", 
  projectId: "arius-projekt",
  storageBucket: "arius-projekt.firebasestorage.app",
  messagingSenderId: "264390002037",
  appId: "1:264390002037:web:2ce7aeca9f91761b0aff3c"
};

firebase.initializeApp(firebaseConfig);
const messaging = firebase.messaging();

// Set do śledzenia już wyświetlonych powiadomień
const displayedNotifications = new Set();

// Background message handler
messaging.onBackgroundMessage((payload) => {
  console.log('[SW] Otrzymano background message:', payload);
  
  // Utwórz unikalny ID dla powiadomienia
  const notificationId = `${payload.data?.title || 'notification'}-${Date.now()}`;
  
  // Sprawdź czy już wyświetlono
  if (displayedNotifications.has(notificationId)) {
    console.log('[SW] Powiadomienie już wyświetlone, pomijam');
    return;
  }
  
  // Dodaj do zbioru wyświetlonych
  displayedNotifications.add(notificationId);
  
  // Wyczyść stare wpisy (starsze niż 5 minut)
  setTimeout(() => {
    displayedNotifications.delete(notificationId);
  }, 5 * 60 * 1000);

  const notificationTitle = payload.notification?.title || payload.data?.title || 'Nowe powiadomienie';
  const notificationOptions = {
    body: payload.notification?.body || payload.data?.body || 'Masz nowe powiadomienie',
    icon: '/icon-192x192.png',
    badge: '/icon-192x192.png',
    tag: 'parking-notification', // Zapobiega duplikatom o tym samym tagu
    requireInteraction: true,
    data: {
      url: payload.data?.url || '/',
      timestamp: Date.now()
    }
  };

  console.log('[SW] Wyświetlam powiadomienie:', notificationTitle);
  return self.registration.showNotification(notificationTitle, notificationOptions);
});

// Obsługa kliknięcia w powiadomienie
self.addEventListener('notificationclick', (event) => {
  console.log('[SW] Kliknięto powiadomienie:', event.notification.tag);
  event.notification.close();

  event.waitUntil(
    clients.matchAll({ type: 'window' }).then((clientList) => {
      // Sprawdź czy aplikacja jest już otwarta
      for (const client of clientList) {
        if (client.url === self.location.origin + '/' && 'focus' in client) {
          return client.focus();
        }
      }
      // Jeśli nie ma otwartej aplikacji, otwórz nową kartę
      if (clients.openWindow) {
        return clients.openWindow('/');
      }
    })
  );
});