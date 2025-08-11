import api from './api';
import { getMessaging, getToken, deleteToken } from 'firebase/messaging';
import { firebaseApp, messaging } from '../firebase';
import { fetchAndStoreFcmToken } from './pushNotifications';

const VAPID_KEY = process.env.REACT_APP_VAPID_PUBLIC_KEY;

// Rejestracja użytkownika
export const register = async (userData) => {
  try {
    const response = await api.post('/auth/register', userData);
    return response.data;
  } catch (error) {
    throw new Error(error.response?.data?.msg || 'Nie udało się zarejestrować');
  }
};

// Rejestracja tokena push na backendzie
const registerPushToken = async (access_token) => {
  try {
    const messaging = getMessaging(firebaseApp);
    const fcmToken = await getToken(messaging, { vapidKey: VAPID_KEY });

    if (fcmToken) {
      await api.post(
        '/auth/register_token',
        { fcm_token: fcmToken },
        {
          headers: {
            Authorization: `Bearer ${access_token}`,
          },
        }
      );
      console.log('[DEBUG] Token FCM zarejestrowany u backendu.');
    } else {
      console.warn('[DEBUG] Nie udało się uzyskać FCM tokena.');
    }
  } catch (err) {
    console.error('[FCM] Rejestracja tokena nie powiodła się:', err);
  }
};

// Logowanie użytkownika
export const login = async (credentials) => {
  const response = await api.post('/auth/login', credentials);
  const { access_token, name, surname } = response.data;

  localStorage.setItem('token', access_token);
  localStorage.setItem('user', JSON.stringify({ name, surname, email: credentials.email }));

  await fetchAndStoreFcmToken();

  return response.data;
};

// Wylogowanie
export const logout = async () => {
  try {
    await deleteToken(messaging);
    console.log('[DEBUG] FCM token usunięty lokalnie');
  } catch (err) {
    console.error('[DEBUG] Błąd przy usuwaniu tokena:', err);
  }

  localStorage.removeItem('token');
  localStorage.removeItem('user');
};
