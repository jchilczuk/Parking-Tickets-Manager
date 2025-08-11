import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { login } from '../../services/auth';
import { registerForPushNotifications } from '../../utils/notifications';
import { fetchAndStoreFcmToken } from '../../services/pushNotifications'; 
import LoadingSpinner from '../common/LoadingSpinner';
import './Auth.css';

const Login = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      const response = await login({ email, password });

      if (response && response.access_token) {
        // NAJPIERW zapisz token JWT
        localStorage.setItem('token', response.access_token);
        
        // Poczekaj chwilę żeby auth się zaktualizował
        setTimeout(async () => {
          try {
            // Teraz dopiero rejestruj FCM (z JWT)
            await fetchAndStoreFcmToken();
            console.log('[LOGIN] FCM token zarejestrowany po logowaniu');
            
            // I stary system notyfikacji
            await registerForPushNotifications();
          } catch (fcmError) {
            console.log('[LOGIN] Błąd FCM (nie krytyczny):', fcmError);
          }
        }, 500); // 500ms delay
        
        // Przejdź do tickets od razu
        navigate('/tickets');
      }
    } catch (err) {
      // Obsługa błędów HTTP
      if (err.response?.status === 401) {
        setError('Niepoprawny e-mail lub hasło.');
      } else if (err.response?.status === 400) {
        setError('Nieprawidłowe dane logowania.');
      } else if (!err.response) {
        setError('Brak połączenia z serwerem.');
      } else {
        setError('Wystąpił nieznany błąd.');
      }

      console.error('[LOGIN] Błąd logowania:', err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-container">
      <h2>Logowanie</h2>
      {error && <div className="error-message">{error}</div>}
      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label>Email</label>
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder='Adres e-mail'
            required
          />
        </div>
        <div className="form-group">
          <label>Hasło</label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder='Hasło'
            required
          />
        </div>
        <button type="submit" disabled={loading}>
          {loading ? <LoadingSpinner small /> : 'Zaloguj się'}
        </button>
      </form>
      <p className="auth-switch">
        Nie masz konta? <a href="/register">Zarejestruj się</a>
      </p>
    </div>
  );
};

export default Login;