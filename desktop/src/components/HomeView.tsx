import React from 'react';
import { Bookmark, RecentDocument } from '../types';

interface HomeViewProps {
  darkMode: boolean;
  recentDocuments: RecentDocument[];
  bookmarks: Bookmark[];
  lastDocument: RecentDocument | null | undefined;
  onOpenPdf: () => void;
  onOpenRecent: (filePath: string, page?: number) => void;
  onDropFiles: (files: FileList) => void;
}

export const HomeView: React.FC<HomeViewProps> = ({
  darkMode,
  recentDocuments,
  bookmarks,
  lastDocument,
  onOpenPdf,
  onOpenRecent,
  onDropFiles,
}) => {
  const [dragOver, setDragOver] = React.useState(false);

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragOver(true);
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragOver(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragOver(false);
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      onDropFiles(e.dataTransfer.files);
    }
  };

  return (
    <div className="home-view-container">
      <div className="home-content">
        <div className="home-header">
          <div className="home-logo-wrap">
            <img
              src={darkMode ? './nova-logo-white.png' : './nova-logo.png'}
              alt="NOVA PDF"
              className="home-logo-img"
            />
          </div>
          <h1 className="home-title">NOVA PDF</h1>
          <p className="home-subtitle">
            Private, local document reader for Windows. No tracking. No ads. No cloud uploads.
          </p>
          <div className="home-badge">
            <span>OFFLINE</span>
            <span className="dot">•</span>
            <span>PRIVATE</span>
            <span className="dot">•</span>
            <span>FAST RENDERING</span>
          </div>
        </div>

        <div
          className={`home-dropzone ${dragOver ? 'drag-over' : ''}`}
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}
          onDrop={handleDrop}
          onClick={onOpenPdf}
        >
          <div className="dropzone-icon">
            <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
              <polyline points="14 2 14 8 20 8" />
              <line x1="12" y1="18" x2="12" y2="12" />
              <line x1="9" y1="15" x2="15" y2="15" />
            </svg>
          </div>
          <h3>Click to choose a PDF or drag and drop it here</h3>
          <p>Files are read entirely on your computer.</p>
          <button className="home-open-btn" onClick={(e) => { e.stopPropagation(); onOpenPdf(); }}>
            Open PDF (Ctrl+O)
          </button>
        </div>

        {lastDocument && (
          <div className="continue-reading-card" onClick={() => onOpenRecent(lastDocument.filePath, lastDocument.lastPage)}>
            <div className="continue-info">
              <span className="continue-label">Continue reading</span>
              <span className="continue-filename">{lastDocument.fileName}</span>
            </div>
            <div className="continue-page-badge">
              Page {lastDocument.lastPage} ➔
            </div>
          </div>
        )}

        <div className="home-grid">
          <section className="home-section">
            <div className="section-header">
              <h2>Recent Documents</h2>
              <span className="section-count">{recentDocuments.length}</span>
            </div>

            {recentDocuments.length === 0 ? (
              <div className="section-empty">
                <p>Your recent documents will appear here for easy access.</p>
              </div>
            ) : (
              <div className="recent-cards-list">
                {recentDocuments.slice(0, 8).map((doc) => (
                  <div
                    key={doc.filePath}
                    className="doc-card"
                    onClick={() => onOpenRecent(doc.filePath, doc.lastPage)}
                  >
                    <div className="doc-card-icon">
                      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                        <polyline points="14 2 14 8 20 8" />
                      </svg>
                    </div>
                    <div className="doc-card-body">
                      <div className="doc-card-title" title={doc.fileName}>{doc.fileName}</div>
                      <div className="doc-card-sub">
                        Page {doc.lastPage} · {new Date(doc.lastOpened).toLocaleDateString()}
                      </div>
                    </div>
                    <span className="doc-card-arrow">›</span>
                  </div>
                ))}
              </div>
            )}
          </section>

          <section className="home-section">
            <div className="section-header">
              <h2>Bookmarks</h2>
              <span className="section-count">{bookmarks.length}</span>
            </div>

            {bookmarks.length === 0 ? (
              <div className="section-empty">
                <p>Bookmark any page while reading to jump back to it quickly.</p>
              </div>
            ) : (
              <div className="bookmark-cards-list">
                {bookmarks.slice(0, 8).map((b) => (
                  <div
                    key={b.id}
                    className="doc-card"
                    onClick={() => onOpenRecent(b.filePath, b.pageNumber)}
                  >
                    <div className="doc-card-icon star">★</div>
                    <div className="doc-card-body">
                      <div className="doc-card-title" title={b.fileName}>{b.fileName}</div>
                      <div className="doc-card-sub">Bookmarked page {b.pageNumber}</div>
                    </div>
                    <span className="doc-card-arrow">↗</span>
                  </div>
                ))}
              </div>
            )}
          </section>
        </div>
      </div>
    </div>
  );
};
