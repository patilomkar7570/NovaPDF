import { app, BrowserWindow, dialog, ipcMain, Menu, MenuItemConstructorOptions } from 'electron';
import * as path from 'path';
import * as fs from 'fs';

let mainWindow: BrowserWindow | null = null;

function getIconPath(): string {
  const icoPath = path.join(__dirname, '../resources/icon.ico');
  if (fs.existsSync(icoPath)) return icoPath;
  const pngPath = path.join(__dirname, '../resources/icon.png');
  if (fs.existsSync(pngPath)) return pngPath;
  return path.join(__dirname, '../public/icon.ico');
}

function getPrefsPath(): string {
  const userDataDir = app.getPath('userData');
  return path.join(userDataDir, 'nova-pdf-settings.json');
}

function createWindow(): void {
  mainWindow = new BrowserWindow({
    width: 1200,
    height: 850,
    minWidth: 780,
    minHeight: 560,
    title: 'NOVA PDF',
    icon: getIconPath(),
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true,
      webSecurity: true,
    },
    backgroundColor: '#0F1115',
    show: false,
  });

  // Security: block unwanted navigation and popups
  mainWindow.webContents.on('will-navigate', (event, url) => {
    if (!url.startsWith('file://') && !url.startsWith('http://localhost:5173')) {
      event.preventDefault();
    }
  });

  mainWindow.webContents.setWindowOpenHandler(() => ({ action: 'deny' }));

  const isDev = !app.isPackaged && process.env.NODE_ENV !== 'production' && !!process.env.VITE_DEV_SERVER_URL;

  if (isDev) {
    mainWindow.loadURL(process.env.VITE_DEV_SERVER_URL || 'http://localhost:5173');
  } else {
    mainWindow.loadFile(path.join(__dirname, '../dist/index.html'));
  }

  mainWindow.once('ready-to-show', () => {
    if (mainWindow) {
      mainWindow.show();
    }
  });

  mainWindow.on('closed', () => {
    mainWindow = null;
  });

  buildMenu();
}

function buildMenu(): void {
  const template: MenuItemConstructorOptions[] = [
    {
      label: 'File',
      submenu: [
        {
          label: 'Open PDF...',
          accelerator: 'CmdOrCtrl+O',
          click: () => {
            if (mainWindow) mainWindow.webContents.send('menu:action', 'open');
          },
        },
        {
          label: 'Print...',
          accelerator: 'CmdOrCtrl+P',
          click: () => {
            if (mainWindow) mainWindow.webContents.send('menu:action', 'print');
          },
        },
        { type: 'separator' },
        {
          label: 'Close Document',
          accelerator: 'CmdOrCtrl+W',
          click: () => {
            if (mainWindow) mainWindow.webContents.send('menu:action', 'close');
          },
        },
        { type: 'separator' },
        { role: 'quit', label: 'Exit NOVA PDF' },
      ],
    },
    {
      label: 'View',
      submenu: [
        {
          label: 'Toggle Search',
          accelerator: 'CmdOrCtrl+F',
          click: () => {
            if (mainWindow) mainWindow.webContents.send('menu:action', 'search');
          },
        },
        {
          label: 'Toggle Sidebar',
          accelerator: 'CmdOrCtrl+B',
          click: () => {
            if (mainWindow) mainWindow.webContents.send('menu:action', 'toggleSidebar');
          },
        },
        { type: 'separator' },
        {
          label: 'Zoom In',
          accelerator: 'CmdOrCtrl+Plus',
          click: () => {
            if (mainWindow) mainWindow.webContents.send('menu:action', 'zoomIn');
          },
        },
        {
          label: 'Zoom Out',
          accelerator: 'CmdOrCtrl+-',
          click: () => {
            if (mainWindow) mainWindow.webContents.send('menu:action', 'zoomOut');
          },
        },
        {
          label: 'Reset Zoom (100%)',
          accelerator: 'CmdOrCtrl+0',
          click: () => {
            if (mainWindow) mainWindow.webContents.send('menu:action', 'zoomReset');
          },
        },
        {
          label: 'Fit to Width',
          accelerator: 'CmdOrCtrl+Shift+W',
          click: () => {
            if (mainWindow) mainWindow.webContents.send('menu:action', 'fitWidth');
          },
        },
        {
          label: 'Fit to Page',
          accelerator: 'CmdOrCtrl+Shift+P',
          click: () => {
            if (mainWindow) mainWindow.webContents.send('menu:action', 'fitPage');
          },
        },
        { type: 'separator' },
        {
          label: 'Toggle Theme',
          accelerator: 'CmdOrCtrl+T',
          click: () => {
            if (mainWindow) mainWindow.webContents.send('menu:action', 'toggleTheme');
          },
        },
      ],
    },
    {
      label: 'Help',
      submenu: [
        {
          label: 'About NOVA PDF',
          click: () => {
            dialog.showMessageBox(mainWindow!, {
              type: 'info',
              title: 'About NOVA PDF',
              message: 'NOVA PDF Desktop v1.0.0',
              detail: 'A private, offline-first, local document reader for Windows.\nNo ads, no tracking, no accounts.\n\nBuilt by patilomkar7570.',
              icon: getIconPath(),
            });
          },
        },
      ],
    },
  ];

  const menu = Menu.buildFromTemplate(template);
  Menu.setApplicationMenu(menu);
}

// IPC Handlers
ipcMain.handle('dialog:openPdf', async () => {
  if (!mainWindow) return null;
  const result = await dialog.showOpenDialog(mainWindow, {
    title: 'Select a PDF document',
    properties: ['openFile'],
    filters: [
      { name: 'PDF Documents (*.pdf)', extensions: ['pdf'] },
      { name: 'All Files', extensions: ['*'] },
    ],
  });

  if (result.canceled || result.filePaths.length === 0) {
    return null;
  }

  const filePath = result.filePaths[0];
  try {
    const buffer = await fs.promises.readFile(filePath);
    return {
      filePath,
      fileName: path.basename(filePath),
      data: Array.from(buffer),
    };
  } catch (err: any) {
    dialog.showErrorBox('Could not read PDF', err?.message || 'Failed to open the selected PDF file.');
    return null;
  }
});

ipcMain.handle('file:readPdf', async (_event, filePath: string) => {
  try {
    if (!filePath || typeof filePath !== 'string') return null;
    const resolvedPath = path.resolve(filePath);
    if (!fs.existsSync(resolvedPath)) return null;

    const buffer = await fs.promises.readFile(resolvedPath);
    return {
      filePath: resolvedPath,
      fileName: path.basename(resolvedPath),
      data: Array.from(buffer),
    };
  } catch (err) {
    return null;
  }
});

ipcMain.handle('prefs:get', async () => {
  try {
    const prefsPath = getPrefsPath();
    if (!fs.existsSync(prefsPath)) {
      return {
        darkMode: false,
        recentDocuments: [],
        bookmarks: [],
        lastDocument: null,
      };
    }
    const data = await fs.promises.readFile(prefsPath, 'utf-8');
    return JSON.parse(data);
  } catch (e) {
    return {
      darkMode: false,
      recentDocuments: [],
      bookmarks: [],
      lastDocument: null,
    };
  }
});

ipcMain.handle('prefs:save', async (_event, prefs: any) => {
  try {
    const prefsPath = getPrefsPath();
    await fs.promises.writeFile(prefsPath, JSON.stringify(prefs, null, 2), 'utf-8');
    return true;
  } catch (e) {
    return false;
  }
});

ipcMain.handle('window:print', async () => {
  if (mainWindow) {
    mainWindow.webContents.print({ silent: false, printBackground: true });
  }
});

ipcMain.handle('app:version', async () => {
  return app.getVersion();
});

app.whenReady().then(createWindow);

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});

app.on('activate', () => {
  if (BrowserWindow.getAllWindows().length === 0) {
    createWindow();
  }
});
