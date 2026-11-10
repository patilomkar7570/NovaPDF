import { contextBridge, ipcRenderer } from 'electron';

export interface ElectronAPI {
  openPdfDialog: () => Promise<{ filePath: string; fileName: string; data: number[] } | null>;
  readPdfFile: (filePath: string) => Promise<{ filePath: string; fileName: string; data: number[] } | null>;
  getPreferences: () => Promise<any>;
  savePreferences: (prefs: any) => Promise<boolean>;
  print: () => Promise<void>;
  onMenuAction: (callback: (action: string) => void) => () => void;
  getAppVersion: () => Promise<string>;
}

const api: ElectronAPI = {
  openPdfDialog: () => ipcRenderer.invoke('dialog:openPdf'),
  readPdfFile: (filePath: string) => ipcRenderer.invoke('file:readPdf', filePath),
  getPreferences: () => ipcRenderer.invoke('prefs:get'),
  savePreferences: (prefs: any) => ipcRenderer.invoke('prefs:save', prefs),
  print: () => ipcRenderer.invoke('window:print'),
  onMenuAction: (callback: (action: string) => void) => {
    const handler = (_event: any, action: string) => callback(action);
    ipcRenderer.on('menu:action', handler);
    return () => {
      ipcRenderer.removeListener('menu:action', handler);
    };
  },
  getAppVersion: () => ipcRenderer.invoke('app:version'),
};

contextBridge.exposeInMainWorld('electronAPI', api);
