import React from 'react';
import { pdfjsLib } from './pdf-init';
import { PDFDocumentProxy } from 'pdfjs-dist';
import { AppPreferences, Bookmark, RecentDocument, ZoomMode } from './types';
import { Toolbar } from './components/Toolbar';
import { Sidebar } from './components/Sidebar';
import { ReaderCanvas } from './components/ReaderCanvas';
import { SearchBar } from './components/SearchBar';
import { HomeView } from './components/HomeView';
import './App.css';

const DEFAULT_PREFS: AppPreferences = {
  darkMode: false,
  recentDocuments: [],
  bookmarks: [],
  lastDocument: null,
};

export const App: React.FC = () => {
  const [prefs, setPrefs] = React.useState<AppPreferences>(DEFAULT_PREFS);
  const [activeDoc, setActiveDoc] = React.useState<PDFDocumentProxy | null>(null);
  const [currentFilePath, setCurrentFilePath] = React.useState<string | null>(null);
  const [currentFileName, setCurrentFileName] = React.useState<string | null>(null);
  const [numPages, setNumPages] = React.useState(0);
  const [currentPage, setCurrentPage] = React.useState(1);

  const [zoomMode, setZoomMode] = React.useState<ZoomMode>('fit-width');
  const [zoomScale, setZoomScale] = React.useState(1.0);
  const [calculatedScale, setCalculatedScale] = React.useState(1.0);

  const [sidebarOpen, setSidebarOpen] = React.useState(false);
  const [searchOpen, setSearchOpen] = React.useState(false);

  // Load preferences on startup
  React.useEffect(() => {
    const loadPrefs = async () => {
      try {
        if (window.electronAPI) {
          const loaded = await window.electronAPI.getPreferences();
          if (loaded) setPrefs(loaded);
        } else {
          const stored = localStorage.getItem('nova_pdf_prefs');
          if (stored) setPrefs(JSON.parse(stored));
        }
      } catch (e) {
        console.error('Failed to load preferences:', e);
      }
    };
    loadPrefs();
  }, []);

  // Save preferences helper
  const updatePrefs = (updater: (prev: AppPreferences) => AppPreferences) => {
    setPrefs((prev) => {
      const next = updater(prev);
      if (window.electronAPI) {
        window.electronAPI.savePreferences(next).catch(console.error);
      } else {
        localStorage.setItem('nova_pdf_prefs', JSON.stringify(next));
      }
      return next;
    });
  };

  // Listen to menu actions and file open from Electron
  React.useEffect(() => {
    if (!window.electronAPI) return;
    const cleanupMenu = window.electronAPI.onMenuAction((action) => {
      switch (action) {
        case 'open':
          handleOpenPdf();
          break;
        case 'print':
          handlePrint();
          break;
        case 'close':
          handleCloseDocument();
          break;
        case 'search':
          setSearchOpen((prev) => !prev);
          break;
        case 'toggleSidebar':
          setSidebarOpen((prev) => !prev);
          break;
        case 'zoomIn':
          handleZoomIn();
          break;
        case 'zoomOut':
          handleZoomOut();
          break;
        case 'zoomReset':
          handleZoomChange(1.0);
          break;
        case 'fitWidth':
          handleZoomChange('fit-width');
          break;
        case 'fitPage':
          handleZoomChange('fit-page');
          break;
        case 'toggleTheme':
          handleToggleTheme();
          break;
      }
    });

    const cleanupFile = window.electronAPI.onOpenFile((filePath) => {
      if (filePath) {
        handleOpenRecent(filePath, 1);
      }
    });

    return () => {
      cleanupMenu();
      cleanupFile();
    };
  }, [activeDoc, currentPage, numPages]);

  // Keyboard shortcuts
  React.useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement) {
        if (e.key === 'Escape') {
          setSearchOpen(false);
          (e.target as HTMLElement).blur();
        }
        return;
      }

      if (e.ctrlKey || e.metaKey) {
        if (e.key.toLowerCase() === 'o') {
          e.preventDefault();
          handleOpenPdf();
        } else if (e.key.toLowerCase() === 'f') {
          e.preventDefault();
          setSearchOpen((prev) => !prev);
        } else if (e.key.toLowerCase() === 'p') {
          e.preventDefault();
          handlePrint();
        } else if (e.key.toLowerCase() === 'b') {
          e.preventDefault();
          setSidebarOpen((prev) => !prev);
        } else if (e.key === '=' || e.key === '+') {
          e.preventDefault();
          handleZoomIn();
        } else if (e.key === '-') {
          e.preventDefault();
          handleZoomOut();
        } else if (e.key === '0') {
          e.preventDefault();
          handleZoomChange(1.0);
        }
      } else {
        if (activeDoc) {
          if (e.key === 'ArrowLeft' || e.key === 'PageUp') {
            e.preventDefault();
            handlePageChange(currentPage - 1);
          } else if (e.key === 'ArrowRight' || e.key === 'PageDown' || e.key === ' ') {
            e.preventDefault();
            handlePageChange(currentPage + 1);
          } else if (e.key === 'Home') {
            e.preventDefault();
            handlePageChange(1);
          } else if (e.key === 'End') {
            e.preventDefault();
            handlePageChange(numPages);
          } else if (e.key === 'Escape') {
            setSearchOpen(false);
          }
        }
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [activeDoc, currentPage, numPages, zoomScale]);

  const loadPdfFromData = async (data: Uint8Array | ArrayBuffer, filePath: string, fileName: string, initialPage = 1) => {
    try {
      const loadingTask = pdfjsLib.getDocument({
        data: data instanceof Uint8Array ? data : new Uint8Array(data),
      });

      const doc = await loadingTask.promise;
      setActiveDoc(doc);
      setCurrentFilePath(filePath);
      setCurrentFileName(fileName);
      setNumPages(doc.numPages);

      const targetPage = Math.max(1, Math.min(initialPage, doc.numPages));
      setCurrentPage(targetPage);

      // Record in recent documents and lastDocument
      updatePrefs((prev) => {
        const existing = prev.recentDocuments.filter((d) => d.filePath !== filePath);
        const updatedRecent: RecentDocument = {
          filePath,
          fileName,
          lastPage: targetPage,
          lastOpened: Date.now(),
        };
        return {
          ...prev,
          recentDocuments: [updatedRecent, ...existing].slice(0, 20),
          lastDocument: updatedRecent,
        };
      });
    } catch (err: any) {
      alert(`Could not open PDF: ${err?.message || 'Unsupported format or encrypted document.'}`);
    }
  };

  const handleOpenPdf = async () => {
    if (window.electronAPI) {
      const result = await window.electronAPI.openPdfDialog();
      if (result) {
        await loadPdfFromData(new Uint8Array(result.data), result.filePath, result.fileName, 1);
      }
    } else {
      // Browser fallback (for preview/dev)
      const input = document.createElement('input');
      input.type = 'file';
      input.accept = 'application/pdf';
      input.onchange = async (e) => {
        const file = (e.target as HTMLInputElement).files?.[0];
        if (file) {
          const buffer = await file.arrayBuffer();
          await loadPdfFromData(buffer, file.name, file.name, 1);
        }
      };
      input.click();
    }
  };

  const handleOpenRecent = async (filePath: string, page = 1) => {
    if (window.electronAPI) {
      const result = await window.electronAPI.readPdfFile(filePath);
      if (result) {
        await loadPdfFromData(new Uint8Array(result.data), result.filePath, result.fileName, page);
      } else {
        alert('Could not find that file. It may have been moved or deleted.');
      }
    }
  };

  const handleDropFiles = async (files: FileList) => {
    for (let i = 0; i < files.length; i++) {
      const file = files[i];
      if (file.name.toLowerCase().endsWith('.pdf')) {
        const buffer = await file.arrayBuffer();
        // file.path is available in Electron File object
        const fullPath = (file as any).path || file.name;
        await loadPdfFromData(buffer, fullPath, file.name, 1);
        break;
      }
    }
  };

  const handlePageChange = (page: number) => {
    if (page < 1 || page > numPages) return;
    setCurrentPage(page);

    // Update last page in preferences
    if (currentFilePath) {
      updatePrefs((prev) => {
        const recents = prev.recentDocuments.map((doc) =>
          doc.filePath === currentFilePath ? { ...doc, lastPage: page, lastOpened: Date.now() } : doc
        );
        const lastDoc = prev.lastDocument?.filePath === currentFilePath
          ? { ...prev.lastDocument, lastPage: page, lastOpened: Date.now() }
          : prev.lastDocument;
        return {
          ...prev,
          recentDocuments: recents,
          lastDocument: lastDoc,
        };
      });
    }
  };

  const handleZoomChange = (mode: ZoomMode) => {
    setZoomMode(mode);
    if (typeof mode === 'number') {
      setZoomScale(mode);
    }
  };

  const handleZoomIn = () => {
    const current = typeof zoomMode === 'number' ? zoomScale : calculatedScale;
    const next = Math.min(3.0, current + 0.25);
    setZoomMode(next);
    setZoomScale(next);
  };

  const handleZoomOut = () => {
    const current = typeof zoomMode === 'number' ? zoomScale : calculatedScale;
    const next = Math.max(0.25, current - 0.25);
    setZoomMode(next);
    setZoomScale(next);
  };

  const handleToggleBookmark = () => {
    if (!currentFilePath || !currentFileName) return;
    const isBookmarked = prefs.bookmarks.some(
      (b) => b.filePath === currentFilePath && b.pageNumber === currentPage
    );

    updatePrefs((prev) => {
      if (isBookmarked) {
        return {
          ...prev,
          bookmarks: prev.bookmarks.filter(
            (b) => !(b.filePath === currentFilePath && b.pageNumber === currentPage)
          ),
        };
      } else {
        const newBm: Bookmark = {
          id: `${Date.now()}-${Math.random()}`,
          filePath: currentFilePath,
          fileName: currentFileName,
          pageNumber: currentPage,
          createdAt: Date.now(),
        };
        return {
          ...prev,
          bookmarks: [newBm, ...prev.bookmarks],
        };
      }
    });
  };

  const handleRemoveBookmark = (id: string) => {
    updatePrefs((prev) => ({
      ...prev,
      bookmarks: prev.bookmarks.filter((b) => b.id !== id),
    }));
  };

  const handleToggleTheme = () => {
    updatePrefs((prev) => ({
      ...prev,
      darkMode: !prev.darkMode,
    }));
  };

  const handlePrint = () => {
    if (window.electronAPI) {
      window.electronAPI.print();
    } else {
      window.print();
    }
  };

  const handleCloseDocument = () => {
    setActiveDoc(null);
    setCurrentFilePath(null);
    setCurrentFileName(null);
    setSearchOpen(false);
  };

  const isCurrentBookmarked = prefs.bookmarks.some(
    (b) => b.filePath === currentFilePath && b.pageNumber === currentPage
  );

  return (
    <div className={`nova-app ${prefs.darkMode ? 'dark-theme' : 'light-theme'}`}>
      <Toolbar
        fileName={currentFileName}
        currentPage={currentPage}
        numPages={numPages}
        zoomMode={zoomMode}
        zoomScale={typeof zoomMode === 'number' ? zoomScale : calculatedScale}
        darkMode={prefs.darkMode}
        sidebarOpen={sidebarOpen}
        searchOpen={searchOpen}
        isBookmarked={isCurrentBookmarked}
        onOpenPdf={handleOpenPdf}
        onPageChange={handlePageChange}
        onZoomChange={handleZoomChange}
        onZoomIn={handleZoomIn}
        onZoomOut={handleZoomOut}
        onToggleSidebar={() => setSidebarOpen((prev) => !prev)}
        onToggleSearch={() => setSearchOpen((prev) => !prev)}
        onToggleBookmark={handleToggleBookmark}
        onToggleTheme={handleToggleTheme}
        onPrint={handlePrint}
        onGoHome={handleCloseDocument}
      />

      <div className="nova-workspace">
        {activeDoc && (
          <Sidebar
            isOpen={sidebarOpen}
            activeDoc={activeDoc}
            numPages={numPages}
            currentPage={currentPage}
            currentFilePath={currentFilePath}
            bookmarks={prefs.bookmarks}
            recentDocs={prefs.recentDocuments}
            onSelectPage={handlePageChange}
            onOpenRecent={(path) => handleOpenRecent(path, 1)}
            onRemoveBookmark={handleRemoveBookmark}
            onClose={() => setSidebarOpen(false)}
          />
        )}

        <div className="nova-main-canvas-area">
          {activeDoc ? (
            <>
              <SearchBar
                isOpen={searchOpen}
                activeDoc={activeDoc}
                numPages={numPages}
                onNavigateToPage={handlePageChange}
                onClose={() => setSearchOpen(false)}
              />
              <ReaderCanvas
                activeDoc={activeDoc}
                currentPage={currentPage}
                zoomMode={zoomMode}
                zoomScale={zoomScale}
                onCalculatedScaleChange={setCalculatedScale}
                onDropFiles={handleDropFiles}
                onZoomIn={handleZoomIn}
                onZoomOut={handleZoomOut}
              />
            </>
          ) : (
            <HomeView
              darkMode={prefs.darkMode}
              recentDocuments={prefs.recentDocuments}
              bookmarks={prefs.bookmarks}
              lastDocument={prefs.lastDocument}
              onOpenPdf={handleOpenPdf}
              onOpenRecent={handleOpenRecent}
              onDropFiles={handleDropFiles}
            />
          )}
        </div>
      </div>
    </div>
  );
};
