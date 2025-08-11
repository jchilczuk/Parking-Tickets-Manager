import React from 'react';
import './LoadingSpinner.css';

const LoadingSpinner = ({ small = false }) => {
  return (
    <div className={`spinner-container ${small ? 'small' : ''}`}>
      <div className="loading-spinner"></div>
    </div>
  );
};

export default LoadingSpinner;