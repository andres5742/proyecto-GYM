const { app, BrowserWindow, dialog, globalShortcut, ipcMain } = require('electron');
const { spawn } = require('child_process');
const fs = require('fs');
const path = require('path');

let mainWindow = null;

function loadConfig() {
  const configPath = path.join(__dirname, 'config.json');
  const examplePath = path.join(__dirname, 'config.example.json');
  const raw = fs.existsSync(configPath)
    ? fs.readFileSync(configPath, 'utf8')
    : fs.readFileSync(examplePath, 'utf8');
  return JSON.parse(raw);
}

let config;
try {
  config = loadConfig();
} catch (err) {
  console.error('config', err);
  if (process.platform === 'win32') {
    dialog.showErrorBoxSync(
      'Sport Gym Acceso',
      'No se pudo leer config.json:\n' + err.message,
    );
  }
  process.exit(1);
}

function appBaseDir() {
  return app.isPackaged ? path.dirname(process.execPath) : __dirname;
}

/** Carpeta turnstile-gateway: primero junto al .exe (C:\SportGym), luego resources. */
function resolveGatewayDir() {
  const beside = path.join(appBaseDir(), 'turnstile-gateway');
  const besideBat = path.join(beside, 'iniciar-lector-tarjeta.bat');
  if (fs.existsSync(besideBat)) {
    return beside;
  }
  if (app.isPackaged && process.resourcesPath) {
    const bundled = path.join(process.resourcesPath, 'turnstile-gateway');
    const bat = path.join(bundled, 'iniciar-lector-tarjeta.bat');
    if (fs.existsSync(bat)) {
      return bundled;
    }
  }
  return path.resolve(
    appBaseDir(),
    config.turnstileGatewayDir || 'turnstile-gateway',
  );
}

function spawnCardReader() {
  if (!config.spawnCardReader || process.platform !== 'win32') {
    return;
  }
  const gwDir = resolveGatewayDir();
  const bat = path.join(gwDir, 'iniciar-lector-tarjeta.bat');
  if (!fs.existsSync(bat)) {
    console.warn('No se encontró iniciar-lector-tarjeta.bat en', gwDir);
    return;
  }
  const child = spawn('cmd.exe', ['/c', 'call', bat], {
    cwd: gwDir,
    detached: true,
    stdio: 'ignore',
    windowsHide: false,
  });
  child.unref();
}

function resolveWindowIcon() {
  const candidates = [];
  if (app.isPackaged && process.resourcesPath) {
    candidates.push(
      path.join(process.resourcesPath, 'SportGym.ico'),
      path.join(process.resourcesPath, 'icon.ico'),
    );
  }
  candidates.push(path.join(__dirname, 'build', 'icon.ico'));
  for (const p of candidates) {
    if (fs.existsSync(p)) {
      return p;
    }
  }
  return undefined;
}

function createWindow() {
  const iconPath = resolveWindowIcon();
  mainWindow = new BrowserWindow({
    width: 1400,
    height: 900,
    show: false,
    icon: iconPath,
    fullscreen: Boolean(config.kioskFullscreen),
    autoHideMenuBar: true,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      nodeIntegration: false,
      contextIsolation: true,
    },
  });

  mainWindow.once('ready-to-show', () => {
    mainWindow.show();
    mainWindow.focus();
  });

  mainWindow.webContents.on(
    'did-fail-load',
    (_event, errorCode, errorDescription, validatedURL) => {
      dialog.showErrorBox(
        'Sport Gym Acceso',
        'No se pudo cargar la pantalla:\n' +
          validatedURL +
          '\n\n' +
          errorDescription +
          ' (' +
          errorCode +
          ')\n\nCompruebe internet o la URL en config.json.',
      );
    },
  );

  mainWindow.loadURL(config.accesoUrl).catch((err) => {
    dialog.showErrorBox(
      'Sport Gym Acceso',
      'Error al abrir ' + config.accesoUrl + ':\n' + err.message,
    );
  });

  mainWindow.on('closed', () => {
    mainWindow = null;
  });
}

function confirmQuit() {
  if (!mainWindow) {
    app.quit();
    return;
  }
  const choice = dialog.showMessageBoxSync(mainWindow, {
    type: 'question',
    buttons: ['Cancelar', 'Cerrar aplicación'],
    defaultId: 0,
    cancelId: 0,
    title: 'Sport Gym Acceso',
    message: '¿Cerrar la aplicación de acceso?',
  });
  if (choice === 1) {
    app.quit();
  }
}

function registerExitShortcuts() {
  globalShortcut.register('Control+Shift+Q', confirmQuit);
  globalShortcut.register('Escape', confirmQuit);
}

if (process.platform === 'win32') {
  app.commandLine.appendSwitch('disable-gpu');
}

ipcMain.on('app-request-quit', () => {
  confirmQuit();
});

app.whenReady().then(() => {
  spawnCardReader();
  createWindow();
  registerExitShortcuts();
});

app.on('will-quit', () => {
  globalShortcut.unregisterAll();
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});
