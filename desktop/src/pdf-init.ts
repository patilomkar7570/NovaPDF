import * as pdfjsLib from 'pdfjs-dist';
// Vite URL import for offline self-contained worker bundling
import workerUrl from 'pdfjs-dist/build/pdf.worker.min.js?url';

pdfjsLib.GlobalWorkerOptions.workerSrc = workerUrl;

export { pdfjsLib };
