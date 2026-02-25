import React, { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { FiMoon, FiSun } from 'react-icons/fi';
import Logo from '../common/Logo';
import { useTheme } from '../../context/ThemeContext';

const navItems = [
  { label: 'Features', hash: '#features' },
  { label: 'How It Works', hash: '#how-it-works' },
  { label: 'Benefits', hash: '#benefits' },
  { label: 'Testimonials', hash: '#testimonials' },
];

const Navbar = () => {
  const [menuOpen, setMenuOpen] = useState(false);
  const location = useLocation();
  const { theme, toggleTheme } = useTheme();
  const isDark = theme === 'dark';
  const isHome = location.pathname === '/';
  const isLogin = location.pathname === '/login';
  const toSectionHref = (hash) => (location.pathname === '/' ? hash : `/${hash}`);

  const headerClass = isDark
    ? 'bg-[#011445]/95 border-b border-cyan-300/20'
    : 'bg-[#5a6fb0]/95 border-b border-white/25';

  const navTextClass = 'text-slate-100';
  const brandTextClass = 'text-white';
  const headerLayoutClass = 'sticky top-0 z-50';

  return (
    <header className={`${headerLayoutClass} backdrop-blur-md ${headerClass}`}>
      <nav className="mx-auto flex w-full max-w-7xl items-center justify-between px-4 py-3 sm:px-6">
        <Link to="/" className={`flex items-center gap-2 ${brandTextClass}`}>
          <Logo width={34} height={34} className={isDark ? 'text-teal-300' : 'text-teal-200'} />
          <span className="text-2xl font-semibold leading-none tracking-tight">PathVision</span>
        </Link>

        <ul className={`hidden items-center gap-8 text-[15px] font-semibold md:flex ${navTextClass}`}>
          <li>
            <Link
              to="/"
              className="rounded-lg px-2 py-1.5 transition hover:bg-white/10 hover:text-white"
            >
              Home
            </Link>
          </li>
          {navItems.map((item) => (
            <li key={item.hash}>
              <a
                href={toSectionHref(item.hash)}
                className="rounded-lg px-2 py-1.5 transition hover:bg-white/10 hover:text-white"
              >
                {item.label}
              </a>
            </li>
          ))}
        </ul>

        <div className="hidden items-center gap-3 md:flex">
          <button
            type="button"
            onClick={toggleTheme}
            aria-label="Toggle theme"
            aria-pressed={isDark}
            className={`relative h-9 w-[4.5rem] rounded-full border transition ${
              isDark
                ? 'border-cyan-300/60 bg-[#0a2462]'
                : 'border-white/60 bg-white/20'
            }`}
          >
            <span className={`absolute left-1.5 top-1.5 inline-flex h-6 w-6 items-center justify-center rounded-full ${isDark ? 'bg-slate-700/70' : 'bg-white/80'}`}>
              <FiSun className={`${isDark ? 'text-slate-500' : 'text-amber-500'} text-sm`} />
            </span>
            <span className={`absolute right-1.5 top-1.5 inline-flex h-6 w-6 items-center justify-center rounded-full ${isDark ? 'bg-cyan-500/20' : 'bg-white/25'}`}>
              <FiMoon className={`${isDark ? 'text-cyan-200' : 'text-white'} text-sm`} />
            </span>
            <span className={`absolute top-1.5 h-6 w-6 rounded-full bg-white shadow-lg transition ${isDark ? 'left-9' : 'left-1.5'}`} />
          </button>
          <Link
            to="/login"
            className="rounded-lg px-4 py-2 text-[15px] font-semibold text-slate-100 transition hover:bg-white/10"
          >
            Login
          </Link>
          <Link
            to="/register"
            className={`rounded-lg px-5 py-2.5 text-[15px] font-semibold text-white shadow ${
              isDark ? 'bg-teal-500 hover:bg-teal-600' : 'bg-[#24B5A7] hover:bg-[#1FA296]'
            }`}
          >
            Get Started
          </Link>
        </div>

        <button
          type="button"
          className="inline-flex h-10 w-10 items-center justify-center rounded-lg text-slate-100 hover:bg-white/10 md:hidden"
          onClick={() => setMenuOpen((v) => !v)}
          aria-label="Toggle menu"
        >
          <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 6h16M4 12h16M4 18h16" />
          </svg>
        </button>
      </nav>

      {menuOpen && (
        <div
          className={`mx-4 mb-3 rounded-xl border px-4 py-4 shadow-sm md:hidden sm:mx-6 ${
            isDark
              ? 'border-cyan-400/20 bg-[#0b1f50]/90 text-slate-100'
              : 'border-white/25 bg-[#5a6fb0]/90 text-slate-100'
          }`}
        >
          <div className="mb-3 flex items-center justify-between">
            <span className="text-sm font-semibold">Dark Mode</span>
            <button
              type="button"
              onClick={toggleTheme}
              className={`relative h-9 w-[4.5rem] rounded-full border transition ${
                isDark ? 'border-cyan-300/60 bg-[#0a2462]' : 'border-white/55 bg-white/20'
              }`}
            >
              <span className={`absolute left-1.5 top-1.5 inline-flex h-6 w-6 items-center justify-center rounded-full ${isDark ? 'bg-slate-700/70' : 'bg-white/80'}`}>
                <FiSun className={`${isDark ? 'text-slate-500' : 'text-amber-500'} text-sm`} />
              </span>
              <span className={`absolute right-1.5 top-1.5 inline-flex h-6 w-6 items-center justify-center rounded-full ${isDark ? 'bg-cyan-500/20' : 'bg-white/25'}`}>
                <FiMoon className={`${isDark ? 'text-cyan-200' : 'text-white'} text-sm`} />
              </span>
              <span className={`absolute top-1.5 h-6 w-6 rounded-full bg-white shadow-lg transition ${isDark ? 'left-9' : 'left-1.5'}`} />
            </button>
          </div>
          <ul className="space-y-2 text-base font-medium">
            <li>
              <Link to="/" onClick={() => setMenuOpen(false)} className="block rounded px-1 py-1">
                Home
              </Link>
            </li>
            {navItems.map((item) => (
              <li key={item.hash}>
                <a href={toSectionHref(item.hash)} onClick={() => setMenuOpen(false)} className="block rounded px-1 py-1">
                  {item.label}
                </a>
              </li>
            ))}
            <li>
              <Link to="/login" onClick={() => setMenuOpen(false)} className="block rounded px-1 py-1">
                Login
              </Link>
            </li>
            <li>
              <Link
                to="/register"
                onClick={() => setMenuOpen(false)}
                className={`mt-2 inline-block rounded-lg px-6 py-2.5 font-semibold text-white ${
                  isDark ? 'bg-teal-500' : 'bg-[#24B5A7]'
                }`}
              >
                Get Started
              </Link>
            </li>
          </ul>
        </div>
      )}
    </header>
  );
};

export default Navbar;
