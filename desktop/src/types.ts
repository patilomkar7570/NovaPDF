export interface RecentDocument {
  filePath: string;
  fileName: string;
  lastPage: number;
  lastOpened: number;
}

export interface Bookmark {
  id: string;
  filePath: string;
  fileName: string;
  pageNumber: number;
  createdAt: number;
}

export interface AppPreferences {
  darkMode: boolean;
  recentDocuments: RecentDocument[];
  bookmarks: Bookmark[];
  lastDocument?: RecentDocument | null;
}

export interface ActiveDocument {
  filePath: string;
  fileName: string;
  data: Uint8Array;
  numPages: number;
  currentPage: number;
}

export interface SearchMatch {
  pageNumber: number;
  matchIndex: number;
}

export type ZoomMode = 'fit-width' | 'fit-page' | number;

declare global {
  interface Window {
    electronAPI?: {
      openPdfDialog: () => Promise<{ filePath: string; fileName: string; data: number[] } | null>;
      readPdfFile: (filePath: string) => Promise<{ filePath: string; fileName: string; data: number[] } | null>;
      getPreferences: () => Promise<AppPreferences>;
      savePreferences: (prefs: AppPreferences) => Promise<boolean>;
      print: () => Promise<void>;
      onMenuAction: (callback: (action: string) => void) => () => void;
      getAppVersion: () => Promise<string>;
    };
  }
}
