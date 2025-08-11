import React, { useEffect } from 'react';
import { Routes, Route } from 'react-router-dom';
import Layout from './components/Layout/Layout';
import Login from './components/Auth/Login';
import Register from './components/Auth/Register';
import TicketsPage from './pages/TicketsPage';
import Profile from './pages/Profile';
import AddTicketForm from './pages/AddTicketForm';
import ProtectedRoute from './utils/ProtectedRoute';
import { fetchAndStoreFcmToken, listenForMessages } from './services/pushNotifications';

function App() {
  useEffect(() => {
    const setupFirebase = async () => {
      const jwt = localStorage.getItem('token');

      // Rejestruj Service Worker tylko dla zalogowanych użytkowników
      if (jwt && 'serviceWorker' in navigator) {
        try {
          const registration = await navigator.serviceWorker.register('/firebase-messaging-sw.js');
          console.log('[DEBUG] Service worker zarejestrowany:', registration);
          
          // Czekaj aż SW będzie gotowy
          await navigator.serviceWorker.ready;
          
          // Rejestruj FCM token
          await fetchAndStoreFcmToken(registration);
          
        } catch (err) {
          console.error('[DEBUG] Błąd rejestracji SW:', err);
        }
      }

      // Zawsze nasłuchuj na wiadomości
      if ('Notification' in window) {
        listenForMessages();
      }
    };

    setupFirebase();
  }, []);

  return (
    <Layout>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/tickets" element={<ProtectedRoute><TicketsPage /></ProtectedRoute>} />
        <Route path="/tickets/new" element={<ProtectedRoute><AddTicketForm /></ProtectedRoute>} />
        <Route path="/profile" element={<ProtectedRoute><Profile /></ProtectedRoute>} />
      </Routes>
    </Layout>
  );
}

export default App;