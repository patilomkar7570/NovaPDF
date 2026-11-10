import React from 'react';
import { Bookmark, RecentDocument } from '../types';
import { PDFDocumentProxy } from 'pdfjs-dist';

interface SidebarProps {
  isOpen: boolean;
  activeDoc: PDFDocumentProxy | null;
  numPages: number;
  currentPage: number;
  currentFilePath: string | null;
  bookmarks: Bookmark[];
  recentDocs: RecentDocument[];
  onSelectPage: (page: number) => void;
  onOpenRecent: (filePath: string) => void;
  onRemoveBookmark: (id: string) => void;
  onClose: () => void;
}

export const Sidebar: React.FC<SidebarProps> = ({
  isOpen,
  activeDoc,
  numPages,
  currentPage,
  currentFilePath,
  bookmarks,
  recentDocs,
  onSelectPage,
  onOpenRecent,
  onRemoveBookmark,
  onClose,
}) => {
  const [tab, setTab] = React.useState<'thumbs' | 'bookmarks' | 'recents'>('thumbs');

  if (!isOpen) return null;

  return (
    <aside className="app-sidebar">
      <div className="sidebar-header">
        <div className="sidebar-tabs">
          <button
            className={`sidebar-tab ${tab === 'thumbs' ? 'active' : ''}`}
            onClick={() => setTab('thumbs')}
          >
            Pages
          </button>
          <button
            className={`sidebar-tab ${tab === 'bookmarks' ? 'active' : ''}`}
            onClick={() => setTab('bookmarks')}
          >
            Bookmarks ({bookmarks.filter(b => b.filePath === currentFilePath).length})
          </button>
          <button
            className={`sidebar-tab ${tab === 'recents' ? 'active' : ''}`}
            onClick={() => setTab('recents')}
          >
            Recent
          </button>
        </div>
        <button className="sidebar-close-btn" onClick={onClose} title="Close sidebar">
          ×
        </button>
      </div>

      <div className="sidebar-content">
        {tab === 'thumbs' && (
          <div className="thumbnail-list">
            {Array.from({ length: numPages }).map((_, i) => (
              <ThumbnailItem
                key={i + 1}
                pageNumber={i + 1}
                activeDoc={activeDoc}
                isActive={currentPage === i + 1}
                onSelect={() => onSelectPage(i + 1)}
              />
            ))}
          </div>
        )}

        {tab === 'bookmarks' && (
          <div className="sidebar-list">
            {bookmarks.filter(b => b.filePath === currentFilePath).length === 0 ? (
              <div className="empty-state">
                <p>No bookmarks in this document yet.</p>
                <small>Click the star button on the toolbar or press Ctrl+B to add one.</small>
              </div>
            ) : (
              bookmarks
                .filter(b => b.filePath === currentFilePath)
                .map((b) => (
                  <div key={b.id} className="bookmark-item">
                    <button
                      className="bookmark-jump-btn"
                      onClick={() => onSelectPage(b.pageNumber)}
                    >
                      <span className="bookmark-icon">★</span>
                      <span className="bookmark-text">Page {b.pageNumber}</span>
                    </button>
                    <button
                      className="bookmark-delete-btn"
                      onClick={() => onRemoveBookmark(b.id)}
                      title="Remove bookmark"
                    >
                      ×
                    </button>
                  </div>
                ))
            )}
          </div>
        )}

        {tab === 'recents' && (
          <div className="sidebar-list">
            {recentDocs.length === 0 ? (
              <div className="empty-state">
                <p>No recent documents.</p>
              </div>
            ) : (
              recentDocs.map((rec) => (
                <div
                  key={rec.filePath}
                  className={`recent-item ${rec.filePath === currentFilePath ? 'current' : ''}`}
                  onClick={() => onOpenRecent(rec.filePath)}
                >
                  <div className="recent-name" title={rec.fileName}>
                    {rec.fileName}
                  </div>
                  <div className="recent-meta">
                    Page {rec.lastPage} · {new Date(rec.lastOpened).toLocaleDateString()}
                  </div>
                </div>
              ))
            )}
          </div>
        )}
      </div>
    </aside>
  );
};

interface ThumbnailItemProps {
  pageNumber: number;
  activeDoc: PDFDocumentProxy | null;
  isActive: boolean;
  onSelect: () => void;
}

const ThumbnailItem: React.FC<ThumbnailItemProps> = ({
  pageNumber,
  activeDoc,
  isActive,
  onSelect,
}) => {
  const canvasRef = React.useRef<HTMLCanvasElement | null>(null);
  const [rendered, setRendered] = React.useState(false);

  React.useEffect(() => {
    let cancelled = false;
    if (!activeDoc || !canvasRef.current || rendered) return;

    activeDoc.getPage(pageNumber).then((page) => {
      if (cancelled || !canvasRef.current) return;
      const canvas = canvasRef.current;
      const context = canvas.getContext('2d');
      if (!context) return;

      const viewport = page.getViewport({ scale: 1 });
      const targetWidth = 140;
      const scale = targetWidth / viewport.width;
      const scaledViewport = page.getViewport({ scale });

      canvas.width = scaledViewport.width;
      canvas.height = scaledViewport.height;

      page.render({
        canvasContext: context,
        viewport: scaledViewport,
      }).promise.then(() => {
        if (!cancelled) setRendered(true);
      }).catch(() => {});
    }).catch(() => {});

    return () => {
      cancelled = true;
    };
  }, [activeDoc, pageNumber, rendered]);

  return (
    <div
      className={`thumbnail-card ${isActive ? 'active' : ''}`}
      onClick={onSelect}
    >
      <div className="thumbnail-canvas-wrap">
        <canvas ref={canvasRef} />
      </div>
      <div className="thumbnail-label">{pageNumber}</div>
    </div>
  );
};
