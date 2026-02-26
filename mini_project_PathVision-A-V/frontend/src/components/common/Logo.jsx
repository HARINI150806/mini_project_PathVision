import React from 'react';

const Logo = ({ width = 40, height = 40, className = '' }) => {
  return (
    <svg
      width={width}
      height={height}
      viewBox="0 0 100 100"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className={className}
      role="img"
      aria-label="PathVision Logo"
    >
      <defs>
        <linearGradient id="logoGradient" x1="0%" y1="100%" x2="100%" y2="0%">
          <stop offset="0%" stopColor="currentColor" stopOpacity="0.8" />
          <stop offset="100%" stopColor="currentColor" stopOpacity="1" />
        </linearGradient>
      </defs>

      {/* The Eye Shape (Vision) */}
      <path
        d="M10 50 Q 50 20, 90 50 Q 50 80, 10 50 Z"
        stroke="currentColor"
        strokeWidth="5"
        fill="none"
        strokeLinecap="round"
        strokeLinejoin="round"
      />

      {/* The Iris/Path (The Journey) */}
      <circle cx="50" cy="50" r="18" fill="none" stroke="currentColor" strokeWidth="2" opacity="0.6"/>
      
      {/* The Pupil/Destination (Path focused inside the eye) */}
      <path
        d="M50 50 L 65 35 L 70 30"
        stroke="currentColor"
        strokeWidth="4"
        strokeLinecap="round"
      />
      <circle cx="50" cy="50" r="6" fill="currentColor" />

      {/* Graduation Cap Tassel element hanging from the eye corner potentially, or just keep it simple as Vision */}
      {/* Let's add a subtle graduation cap hint above the eye */}
      <path 
        d="M30 20 L 50 10 L 70 20 L 50 30 Z" 
        fill="currentColor" 
        opacity="0.8"
      />


    </svg>
  );
};


export default Logo;
