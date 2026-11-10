import React from 'react';
import { ZoomMode } from '../types';

interface ToolbarProps {
  fileName: string | null;
  currentPage: number;
  numPages: number;
  zoomMode: ZoomMode;
  zoomScale: number;
  darkMode: boolean;
  sidebarOpen: boolean;
  searchOpen: boolean;
  isBookmarked: boolean;
  onOpenPdf: () => void;
  onPageChange: (page: number) => void;
  onZoomChange: (mode: ZoomMode) => void;
  onZoomIn: () => void;
  onZoomOut: () => void;
  onToggleSidebar: () => void;
  onToggleSearch: () => void;
  onToggleBookmark: () => void;
  onToggleTheme: () => void;
  onPrint: () => void;
  onGoHome: () => void;
}

export const Toolbar: React.FC<ToolbarProps> = ({
  fileName,
  currentPage,
  numPages,
  zoomMode,
  zoomScale,
  darkMode,
  sidebarOpen,
  searchOpen,
  isBookmarked,
  onOpenPdf,
  onPageChange,
  onZoomChange,
  onZoomIn,
  onZoomOut,
  onToggleSidebar,
  onToggleSearch,
  onToggleBookmark,
  onToggleTheme,
  onPrint,
  onGoHome,
}) => {
  const [pageInput, setPageInput] = React.useState(String(currentPage));

  React.useEffect(() => {
    setPageInput(String(currentPage));
  }, [currentPage]);

  const handlePageSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const p = parseInt(pageInput, 10);
    if (!isNaN(p) && p >= 1 && p <= numPages) {
      onPageChange(p);
    } else {
      setPageInput(String(currentPage));
    }
  };

  return (
    <header className="app-toolbar">
      <div className="toolbar-left">
        <button className="brand-button" onClick={onGoHome} title="NOVA PDF Home">
          <img
            src={darkMode ? './nova-logo-white.png' : './nova-logo.png'}
            alt="NOVA PDF Logo"
            className="brand-logo"
            onError={(e) => {
              // fallback if relative path issues
              (e.target as HTMLElement).style.display = 'none';
            }}
          />
          <span className="brand-title">NOVA PDF</span>
        </button>

        <button className="toolbar-btn primary" onClick={onOpenPdf} title="Open PDF document (Ctrl+O)">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M12 5v14M5 12h14" />
          </svg>
          <span>Open PDF</span>
        </button>

        {fileName && (
          <button
            className={`toolbar-icon-btn ${sidebarOpen ? 'active' : ''}`}
            onClick={onToggleSidebar}
            title="Toggle Sidebar (Ctrl+B)"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
              <line x1="9" y1="3" x2="9" y2="21" />
            </svg>
          </button>
        )}
      </div>

      {fileName && (
        <div className="toolbar-center">
          <div className="doc-title" title={fileName}>
            {fileName}
          </div>

          <div className="page-nav-controls">
            <button
              className="toolbar-icon-btn"
              disabled={currentPage <= 1}
              onClick={() => onPageChange(currentPage - 1)}
              title="Previous Page (PageUp / Left Arrow)"
            >
              ‹
            </button>

            <form onSubmit={handlePageSubmit} className="page-input-form">
              <input
                type="text"
                className="page-input"
                value={pageInput}
                onChange={(e) => setPageInput(e.target.value)}
                onBlur={handlePageSubmit}
                title="Type page number and press Enter"
              />
              <span className="page-count">/ {numPages}</span>
            </form>

            <button
              className="toolbar-icon-btn"
              disabled={currentPage >= numPages}
              onClick={() => onPageChange(currentPage + 1)}
              title="Next Page (PageDown / Right Arrow)"
            >
              ›
            </button>
          </div>

          <div className="zoom-controls">
            <button className="toolbar-icon-btn" onClick={onZoomOut} title="Zoom Out (Ctrl+Minus)">
              −
            </button>
            <select
              className="zoom-select"
              value={typeof zoomMode === 'number' ? Math.round(zoomScale * 100) : zoomMode}
              onChange={(e) => {
                const val = e.target.value;
                if (val === 'fit-width' || val === 'fit-page') {
                  onZoomChange(val);
                } else {
                  onZoomChange(parseInt(val, 10) / 100);
                }
              }}
            >
              <option value="fit-width">Fit Width</option>
              <option value="fit-page">Fit Page</option>
              <option value="50">50%</option>
              <option value="75">75%</option>
              <option value="100">100%</option>
              <option value="125">125%</option>
              <option value="150">150%</option>
              <option value="200">200%</option>
            </select>
            <button className="toolbar-icon-btn" onClick={onZoomIn} title="Zoom In (Ctrl+Plus)">
              +
            </button>
          </div>
        </div>
      )}

      <div className="toolbar-right">
        {fileName && (
          <>
            <button
              className={`toolbar-icon-btn ${searchOpen ? 'active' : ''}`}
              onClick={onToggleSearch}
              title="Search Document (Ctrl+F)"
            >
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="11" cy="11" r="8" />
                <line x1="21" y1="21" x2="16.65" y2="16.65" />
              </svg>
            </button>

            <button
              className={`toolbar-icon-btn ${isBookmarked ? 'bookmarked' : ''}`}
              onClick={onToggleBookmark}
              title={isBookmarked ? 'Remove Bookmark' : 'Bookmark this page'}
            >
              {isBookmarked ? '★' : '☆'}
            </button>

            <button className="toolbar-icon-btn" onClick={onPrint} title="Print Document (Ctrl+P)">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <polyline points="6 9 6 2 18 2 18 9" />
                <path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2" />
                <rect x="6" y="14" width="12" height="8" />
              </svg>
            </button>
          </>
        )}

        <button className="toolbar-icon-btn" onClick={onToggleTheme} title={darkMode ? 'Light Theme' : 'Dark Theme'}>
          {darkMode ? '☀' : '☾'}
        </button>
      </div>
    </header>
  );
};
