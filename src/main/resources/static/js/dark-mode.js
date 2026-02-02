/* ═══════════════════════════════════════════════════════════════════════════
   dark-mode.js – QuickLink Dark Mode Controller
   ═══════════════════════════════════════════════════════════════════════════

   Features:
   - Respects user's system preference (prefers-color-scheme)
   - Persists theme choice in localStorage
   - Prevents flash of unstyled content (FOUC) on page load
   - Provides toggle button component

   Usage:
   1. Include this script at the END of <body> in all templates:
      <script src="/js/dark-mode.js"></script>

   2. Add the toggle button to your navbar (see HTML snippet below)
   ═══════════════════════════════════════════════════════════════════════════ */

(function() {
  'use strict';

  // ───────────────────────────────────────────────────────────────────────────
  // Constants
  // ───────────────────────────────────────────────────────────────────────────
  const STORAGE_KEY = 'quicklink-theme';
  const THEME_LIGHT = 'light';
  const THEME_DARK = 'dark';

  // ───────────────────────────────────────────────────────────────────────────
  // Core Theme Management
  // ───────────────────────────────────────────────────────────────────────────

  /**
   * Get the user's preferred theme
   * Priority: localStorage > system preference > default (light)
   */
  function getPreferredTheme() {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored) {
      return stored;
    }

    // Check system preference
    if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
      return THEME_DARK;
    }

    return THEME_LIGHT;
  }

  /**
   * Apply theme to the document
   */
  function setTheme(theme) {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem(STORAGE_KEY, theme);

    // Update Chart.js charts if they exist
    updateChartColors(theme);
  }

  /**
   * Toggle between light and dark themes
   */
  function toggleTheme() {
    const current = document.documentElement.getAttribute('data-theme') || THEME_LIGHT;
    const newTheme = current === THEME_LIGHT ? THEME_DARK : THEME_LIGHT;
    setTheme(newTheme);
  }

  // ───────────────────────────────────────────────────────────────────────────
  // Chart.js Dark Mode Support
  // ───────────────────────────────────────────────────────────────────────────

  /**
   * Update Chart.js chart colors when theme changes
   */
  function updateChartColors(theme) {
    if (typeof Chart === 'undefined') return;

    const isDark = theme === THEME_DARK;
    const textColor = isDark ? '#e9ecef' : '#212529';
    const gridColor = isDark ? 'rgba(255, 255, 255, 0.1)' : 'rgba(0, 0, 0, 0.1)';

    // Update all existing charts
    Chart.instances.forEach(chart => {
      if (chart.options.scales) {
        // Update scales text color
        Object.keys(chart.options.scales).forEach(scaleKey => {
          const scale = chart.options.scales[scaleKey];
          if (scale.ticks) scale.ticks.color = textColor;
          if (scale.grid) scale.grid.color = gridColor;
        });
      }

      // Update legend text color
      if (chart.options.plugins && chart.options.plugins.legend) {
        chart.options.plugins.legend.labels.color = textColor;
      }

      chart.update();
    });
  }

  // ───────────────────────────────────────────────────────────────────────────
  // System Preference Change Listener
  // ───────────────────────────────────────────────────────────────────────────

  /**
   * Listen for system theme changes (only if user hasn't manually set a preference)
   */
  function listenForSystemChanges() {
    if (!window.matchMedia) return;

    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (e) => {
      // Only auto-switch if user hasn't manually set a preference
      if (!localStorage.getItem(STORAGE_KEY)) {
        setTheme(e.matches ? THEME_DARK : THEME_LIGHT);
      }
    });
  }

  // ───────────────────────────────────────────────────────────────────────────
  // Initialize on Page Load
  // ───────────────────────────────────────────────────────────────────────────

  /**
   * Initialize theme immediately to prevent FOUC
   * This runs synchronously before DOMContentLoaded
   */
  function init() {
    const theme = getPreferredTheme();
    setTheme(theme);
  }

  /**
   * Set up event listeners and toggle button after DOM loads
   */
  function setupUI() {
    // Listen for system preference changes
    listenForSystemChanges();

    // Set up toggle buttons (multiple may exist across pages)
    const toggleButtons = document.querySelectorAll('.theme-toggle');
    toggleButtons.forEach(button => {
      button.addEventListener('click', toggleTheme);
    });
  }

  // ───────────────────────────────────────────────────────────────────────────
  // Run Initialization
  // ───────────────────────────────────────────────────────────────────────────

  // Apply theme immediately (prevents FOUC)
  init();

  // Set up UI after DOM loads
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', setupUI);
  } else {
    setupUI();
  }

  // ───────────────────────────────────────────────────────────────────────────
  // Global API (optional - for manual control)
  // ───────────────────────────────────────────────────────────────────────────

  window.QuickLinkTheme = {
    get: () => document.documentElement.getAttribute('data-theme'),
    set: setTheme,
    toggle: toggleTheme,
    getPreferred: getPreferredTheme
  };

})();

/* ═══════════════════════════════════════════════════════════════════════════
   HTML SNIPPET: Theme Toggle Button for Navbar
   ═══════════════════════════════════════════════════════════════════════════

   Add this button to your navbar, typically in the right-side section:

   <button class="theme-toggle" aria-label="Toggle dark mode" title="Toggle theme">
     <svg class="sun-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24">
       <path d="M12 17q-2.075 0-3.537-1.463T7 12q0-2.075 1.463-3.537T12 7q2.075 0 3.538 1.463T17 12q0 2.075-1.463 3.538T12 17Zm0-2q1.25 0 2.125-.875T15 12q0-1.25-.875-2.125T12 9q-1.25 0-2.125.875T9 12q0 1.25.875 2.125T12 15Zm0-3Zm0 9q-.425 0-.712-.288T11 20v-1.5q0-.425.288-.712T12 17.5q.425 0 .713.288T13 18.5V20q0 .425-.288.713T12 21Zm0-17q-.425 0-.712-.288T11 3V1.5q0-.425.288-.712T12 .5q.425 0 .713.288T13 1.5V3q0 .425-.288.713T12 4Zm8 8q-.425 0-.712-.288T19 11q0-.425.288-.712T20 10h1.5q.425 0 .713.288T22.5 11q0 .425-.288.713T21.5 12H20Zm-17 0q-.425 0-.712-.288T2 11q0-.425.288-.712T3 10h1.5q.425 0 .713.288T5.5 11q0 .425-.288.713T4.5 12H3Zm14.95 7.05q-.3-.3-.3-.712t.3-.713l1.05-1.05q.3-.3.713-.3t.712.3q.3.3.3.713t-.3.712l-1.05 1.05q-.3.3-.712.3t-.713-.3Zm-12.9-12.9q-.3-.3-.3-.712t.3-.713l1.05-1.05q.3-.3.713-.3t.712.3q.3.3.3.713t-.3.712L6.475 5.35q-.3.3-.712.3T5.05 5.35Zm12.9 0l-1.05-1.05q-.3-.3-.3-.713t.3-.712q.3-.3.713-.3t.712.3l1.05 1.05q.3.3.3.713t-.3.712q-.3.3-.712.3T17.95 5.35Zm-12.9 12.9l-1.05-1.05q-.3-.3-.3-.712t.3-.713q.3-.3.713-.3t.712.3l1.05 1.05q.3.3.3.713t-.3.712q-.3.3-.712.3T5.05 18.25Z"/>
     </svg>
     <svg class="moon-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24">
       <path d="M12 21q-3.75 0-6.375-2.625T3 12q0-3.75 2.625-6.375T12 3q.35 0 .688.025t.662.075q-1.025.725-1.638 1.888T11.1 7.5q0 2.25 1.575 3.825T16.5 12.9q1.375 0 2.525-.613T20.9 10.65q.05.325.075.663T21 12q0 3.75-2.625 6.375T12 21Zm0-2q2.2 0 3.95-1.213t2.55-3.162q-.5.125-1 .2t-1 .075q-3.075 0-5.238-2.163T9.1 7.5q0-.5.075-1t.2-1q-1.95.8-3.163 2.55T5 12q0 2.9 2.05 4.95T12 19Zm-.25-6.75Z"/>
     </svg>
   </button>

   Or for a more descriptive button with text:

   <button class="btn btn-sm btn-outline-secondary theme-toggle d-flex align-items-center gap-2">
     <svg class="sun-icon" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24">
       <path d="M12 17q-2.075 0-3.537-1.463T7 12q0-2.075 1.463-3.537T12 7q2.075 0 3.538 1.463T17 12q0 2.075-1.463 3.538T12 17Z"/>
     </svg>
     <svg class="moon-icon" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24">
       <path d="M12 21q-3.75 0-6.375-2.625T3 12q0-3.75 2.625-6.375T12 3q.35 0 .688.025t.662.075q-1.025.725-1.638 1.888T11.1 7.5q0 2.25 1.575 3.825T16.5 12.9q1.375 0 2.525-.613T20.9 10.65q.05.325.075.663T21 12q0 3.75-2.625 6.375T12 21Z"/>
     </svg>
     <span class="d-none d-md-inline">Theme</span>
   </button>

   ═══════════════════════════════════════════════════════════════════════════ */