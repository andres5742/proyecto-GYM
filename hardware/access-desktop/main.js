const { app, BrowserWindow, dialog, globalShortcut, ipcMain, session } = require('electron');
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

/** Carpeta turnstile-gateway: C:\SportGym (INSTALAR), junto al .exe, o resources embebido. */
function resolveGatewayDir() {
  const gymEntry = path.join('C:', 'SportGym', 'turnstile-gateway');
  const gymBat = path.join(gymEntry, 'iniciar-lector-tarjeta.bat');
  if (fs.existsSync(gymBat)) {
    return gymEntry;
  }
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

function runGateCommand(action) {
  if (process.platform !== 'win32') {
    return;
  }
  const gwDir = resolveGatewayDir();
  const script = path.join(gwDir, 'turnstile_gate.py');
  if (!fs.existsSync(script)) {
    console.warn('No se encontró turnstile_gate.py en', gwDir);
    return;
  }
  const args = action === 'unlock' ? [script, 'unlock'] : [script, action];
  const child = spawn('python', args, {
    cwd: gwDir,
    detached: false,
    stdio: 'ignore',
    windowsHide: true,
  });
  child.unref();
}

/** Cola lock/unlock para el lector (mismo COM3 @ 19200, letras l/a). */
function queueGateCommand(cmd, data) {
  if (process.platform !== 'win32') {
    return;
  }
  const gwDir = resolveGatewayDir();
  const pending = path.join(gwDir, '.gate-pending.json');
  try {
    fs.writeFileSync(
      pending,
      JSON.stringify({ cmd, data: data || null, ts: Date.now() }),
      'utf8',
    );
  } catch (err) {
    console.warn('gate-pending', err.message);
  }
}

function syncGateFromPayload(payload) {
  const result = payload && String(payload.result || '').toUpperCase();
  const granted = result === 'GRANTED';
  const opened = payload && Boolean(payload.gateOpened);
  const shouldUnlock = granted && opened;
  const deviceUserId = payload && String(payload.deviceUserId || '');
  const isShortcut = deviceUserId.startsWith('F2') || deviceUserId.startsWith('F3');
  const isCardFromReader =
    payload && payload.credentialType === 'CARD' && !isShortcut;

  // Con lector activo, unlock lo maneja serial_card_reader (after_api_response).
  // Solo omitimos unlock duplicado; DENIED siempre debe encolar lock por si el lector falló.
  if (config.spawnCardReader && isCardFromReader) {
    if (!shouldUnlock) {
      queueGateCommand('lock');
    }
    return;
  }

  if (config.spawnCardReader) {
    if (shouldUnlock) {
      queueGateCommand('unlock');
    } else {
      queueGateCommand('lock');
    }
  } else if (shouldUnlock) {
    runGateCommand('unlock');
  } else {
    runGateCommand('lock');
  }
}

function spawnCardReader() {
  if (!config.spawnCardReader || process.platform !== 'win32') {
    return;
  }
  const gwDir = resolveGatewayDir();
  const bat = path.join(gwDir, 'iniciar-lector-tarjeta.bat');
  if (!fs.existsSync(bat)) {
    const msg =
      'No se encontro el lector de tarjeta COM3.\n\n' +
      'Ejecute: ACTUALIZAR-TORNIQUETE-DESDE-GIT.bat\n\n' +
      'O abra manualmente:\n' +
      'C:\\SportGym\\turnstile-gateway\\iniciar-lector-tarjeta.bat';
    console.warn(msg, gwDir);
    if (mainWindow) {
      dialog.showMessageBoxSync(mainWindow, {
        type: 'warning',
        title: 'Sport Gym Acceso',
        message: 'Lector COM3 no encontrado',
        detail: msg,
      });
    } else {
      dialog.showErrorBox('Sport Gym Acceso - Lector COM3', msg);
    }
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

  mainWindow.webContents.on('did-finish-load', () => {
    mainWindow.webContents.insertCSS(`
      .kiosk-hero__logo-wrap, .access-card, .kiosk-motto__icon { display: none !important; }
    `);
  });

  const accesoUrl = new URL(config.accesoUrl);
  accesoUrl.searchParams.set('_v', String(Date.now()));
  mainWindow.loadURL(accesoUrl.toString()).catch((err) => {
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

ipcMain.on('gate-sync', (_event, payload) => {
  if (!payload || typeof payload !== 'object') {
    syncGateFromPayload(null);
    return;
  }
  syncGateFromPayload(payload);
});

app.whenReady().then(async () => {
  const ses = session.defaultSession;
  await ses.clearCache();
  ses.webRequest.onBeforeSendHeaders((details, callback) => {
    details.requestHeaders['Cache-Control'] = 'no-cache';
    callback({ requestHeaders: details.requestHeaders });
  });
  spawnCardReader();
  if (!config.spawnCardReader) {
    runGateCommand('lock');
  }
  createWindow();
  registerExitShortcuts();
});

app.on('will-quit', () => {
  if (config.spawnCardReader) {
    queueGateCommand('lock');
  } else {
    runGateCommand('lock');
  }
  globalShortcut.unregisterAll();
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});
