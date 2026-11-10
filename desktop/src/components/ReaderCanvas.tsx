import React from 'react';
import { PDFDocumentProxy, PDFPageProxy } from 'pdfjs-dist';
import { ZoomMode } from '../types';

interface ReaderCanvasProps {
  activeDoc: PDFDocumentProxy | null;
  currentPage: number;
  zoomMode: ZoomMode;
  zoomScale: number;
  onCalculatedScaleChange: (scale: number) => void;
  onDropFiles: (files: FileList) => void;
}

export const ReaderCanvas: React.FC<ReaderCanvasProps> = ({
  activeDoc,
  currentPage,
  zoomMode,
  zoomScale,
  onCalculatedScaleChange,
  onDropFiles,
}) => {
  const containerRef = React.useRef<HTMLDivElement | null>(null);
  const canvasRef = React.useRef<HTMLCanvasElement | null>(null);
  const [loading, setLoading] = React.useState(false);
  const [dragOver, setDragOver] = React.useState(false);

  // Render current page
  React.useEffect(() => {
    let cancelled = false;
    let renderTask: any = null;

    if (!activeDoc || !canvasRef.current || !containerRef.current) return;

    setLoading(true);

    activeDoc.getPage(currentPage).then((page: PDFPageProxy) => {
      if (cancelled || !canvasRef.current || !containerRef.current) return;

      const container = containerRef.current;
      const canvas = canvasRef.current;
      const context = canvas.getContext('2d');
      if (!context) return;

      const unscaledViewport = page.getViewport({ scale: 1 });
      const containerWidth = container.clientWidth - 48; // padding margin
      const containerHeight = container.clientHeight - 48;

      let effectiveScale = 1.0;
      if (zoomMode === 'fit-width') {
        effectiveScale = Math.max(0.2, containerWidth / unscaledViewport.width);
      } else if (zoomMode === 'fit-page') {
        const scaleW = containerWidth / unscaledViewport.width;
        const scaleH = containerHeight / unscaledViewport.height;
        effectiveScale = Math.max(0.2, Math.min(scaleW, scaleH));
      } else {
        effectiveScale = zoomScale;
      }

      onCalculatedScaleChange(effectiveScale);

      // Hi-DPI scaling for ultra-sharp text
      const dpr = window.devicePixelRatio || 1;
      const viewport = page.getViewport({ scale: effectiveScale });

      canvas.width = Math.floor(viewport.width * dpr);
      canvas.height = Math.floor(viewport.height * dpr);
      canvas.style.width = `${Math.floor(viewport.width)}px`;
      canvas.style.height = `${Math.floor(viewport.height)}px`;

      context.setTransform(dpr, 0, 0, dpr, 0, 0);

      renderTask = page.render({
        canvasContext: context,
        viewport,
      });

      renderTask.promise.then(() => {
        if (!cancelled) setLoading(false);
      }).catch((err: any) => {
        if (err?.name !== 'RenderingCancelledException') {
          console.error('Page render error:', err);
        }
      });
    }).catch((err) => {
      console.error('Failed to get page:', err);
      setLoading(false);
    });

    return () => {
      cancelled = true;
      if (renderTask) {
        renderTask.cancel();
      }
    };
  }, [activeDoc, currentPage, zoomMode, zoomScale]);

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
    <main
      className={`reader-container ${dragOver ? 'drag-over' : ''}`}
      ref={containerRef}
      onDragOver={handleDragOver}
      onDragLeave={handleDragLeave}
      onDrop={handleDrop}
    >
      <div className="canvas-wrapper">
        <canvas ref={canvasRef} className="pdf-canvas" />
      </div>

      {loading && (
        <div className="canvas-spinner">
          <div className="spinner"></div>
          <span>Rendering page...</span>
        </div>
      )}

      {dragOver && (
        <div className="drop-overlay">
          <div className="drop-card">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
              <polyline points="17 8 12 3 7 8" />
              <line x1="12" y1="3" x2="12" y2="15" />
            </svg>
            <h3>Drop PDF to open in NOVA</h3>
          </div>
        </div>
      )}
    </main>
  );
};
