import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import './Navbar.css';
import { logout } from '../../services/auth';

const Navbar = () => {
  const navigate = useNavigate();

  const isLoggedIn = !!localStorage.getItem('token');

  const handleLogout = async() => {
    await logout();
    navigate('/login');
  };

  return (
    <nav className="navbar">
      <div className="navbar-container">
        <Link to="/" className="navbar-brand">
          Parking App
        </Link>

        <div className="navbar-links">
          {isLoggedIn ? (
            <>
              <Link to="/tickets" className="nav-link">
                Bilety
              </Link>
              <Link to="/profile" className="nav-link">
                Profil
              </Link>
              <button onClick={handleLogout} className="nav-button">
                Wyloguj
              </button>
            </>
          ) : (
            <Link to="/login" className="nav-link">
              Logowanie
            </Link>
          )}
        </div>
      </div>
    </nav>
  );
};

export default Navbar;
