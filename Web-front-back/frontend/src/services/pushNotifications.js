// src/utils/pushNotifications.js
import { getToken, onMessage } from 'firebase/messaging';
import { messaging } from '../firebase';

// Funkcja do rejestracji tokena FCM
export const registerFCMToken = async () => {
  try {
    console.log('[FCM] Sprawdzam JWT w localStorage...');
    
    // Sprawdź czy JWT token istnieje w localStorage
    const jwtToken = localStorage.getItem('token');
    if (!jwtToken) {
      console.log('[FCM] Pomijam zapis tokena – brak JWT w localStorage');
      return null;
    }

    console.log('[FCM] JWT znaleziony, kontynuuję rejestrację FCM...');

    // Sprawdź czy Service Worker jest gotowy
    if (!('serviceWorker' in navigator)) {
      console.log('[FCM] Service Worker nie jest obsługiwany');
      return null;
    }

    // Poczekaj na aktywny Service Worker
    let registration = await navigator.serviceWorker.getRegistration();
    if (!registration || !registration.active) {
      console.log('[FCM] Oczekiwanie na aktywny Service Worker...');
      await new Promise(resolve => {
        navigator.serviceWorker.addEventListener('controllerchange', resolve, { once: true });
      });
      registration = await navigator.serviceWorker.getRegistration();
    }

    if (!registration || !registration.active) {
      console.log('[FCM] Brak aktywnego Service Worker – przerywam');
      return null;
    }

    // Pobierz token FCM z retry mechanizmem
    let fcmToken = null;
    let retries = 3;
    
    while (retries > 0 && !fcmToken) {
      try {
        fcmToken = await getToken(messaging, {
          vapidKey: 'BJE5Uf31460EwYFtC_FWAl_rbkD1slLgXOUWUe3d8M_wfyx-kuX63c5Xf_Ttmbv7mWiJbPkimOmnAqx4W5hUK6g'
        });
        break;
      } catch (error) {
        retries--;
        console.log(`[FCM] Próba ${4-retries}/3 nieudana:`, error.message);
        if (retries > 0) {
          await new Promise(resolve => setTimeout(resolve, 1000));
        }
      }
    }

    if (!fcmToken) {
      throw new Error('Nie udało się uzyskać tokena FCM po 3 próbach');
    }

    console.log('[FCM] Otrzymano token:', fcmToken.substring(0, 50) + '...');

    // Wyślij token na backend
    const response = await fetch('http://localhost:5000/auth/register_token', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${jwtToken}` // Użyj JWT z localStorage
      },
      body: JSON.stringify({ fcm_token: fcmToken })
    });

    if (response.ok) {
      console.log('[FCM] Token FCM został wysłany do backendu.');
    } else {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }

    return fcmToken;

  } catch (error) {
    console.error('[FCM] Błąd podczas rejestracji tokena:', error);
    return null;
  }
};

// Obsługa wiadomości w foreground
export const setupForegroundMessaging = () => {
  onMessage(messaging, (payload) => {
    console.log('[FCM] Otrzymano wiadomość w foreground:', payload);
    
    // Nie wyświetlaj duplikatu jeśli już pokazuje Service Worker
    // Service Worker obsługuje background, my tylko logujemy w foreground
    if (Notification.permission === 'granted') {
      // Opcjonalnie: pokaż toast/banner w aplikacji zamiast systemowego powiadomienia
      console.log('[FCM] Powiadomienie zostanie obsłużone przez Service Worker');
    }
  });
};

// Alias dla kompatybilności z istniejącym kodem
export const fetchAndStoreFcmToken = registerFCMToken;

// Alias dla setupForegroundMessaging
export const listenForMessages = setupForegroundMessaging;

// Funkcja do żądania uprawnień
export const requestNotificationPermission = async () => {
  try {
    const permission = await Notification.requestPermission();
    if (permission === 'granted') {
      console.log('[FCM] Uprawnienia do powiadomień przyznane');
      return true;
    } else {
      console.log('[FCM] Uprawnienia do powiadomień odrzucone');
      return false;
    }
  } catch (error) {
    console.error('[FCM] Błąd podczas żądania uprawnień:', error);
    return false;
  }
};