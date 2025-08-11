import { messaging } from '../firebase';
import { getToken } from 'firebase/messaging';

const VAPID_KEY = process.env.REACT_APP_VAPID_PUBLIC_KEY;

// Rejestracja użytkownika do lokalnych powiadomień
export const registerForPushNotifications = async () => {
  if (!('Notification' in window)) {
    console.warn('[DEBUG] Przeglądarka nie wspiera powiadomień.');
    return false;
  }

  const permission = await Notification.requestPermission();
  if (permission === 'denied') {
    console.warn('[DEBUG] Powiadomienia zablokowane – użytkownik musi zmienić ręcznie w przeglądarce.');
    return false;
  } else if (permission === 'default') {
    console.warn('[DEBUG] Użytkownik zignorował prośbę – spróbuj ponownie później.');
    return false;
  }

  try {
    const fcmToken = await getToken(messaging, {
      vapidKey: VAPID_KEY,
      forceRefresh: true, // wymuszenie nowego tokenu
    });

    if (!fcmToken) {
      console.error('[DEBUG] Nie udało się uzyskać FCM tokena.');
      return false;
    }

    console.log('[DEBUG] Uzyskano FCM token:', fcmToken);
    return true;
  } catch (error) {
    console.error('[FCM] Nie udało się uzyskać tokena:', error);
    return false;
  }
};

// Pokazywanie lokalnych powiadomień
export const showNotification = (title, options) => {
  if ('Notification' in window && Notification.permission === 'granted') {
    new Notification(title, options);
  }
};
