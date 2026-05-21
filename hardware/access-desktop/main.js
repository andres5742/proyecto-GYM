const { app, BrowserWindow } = require('electron');
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

const config = loadConfig();

/** Carpeta del .exe portable (no la temp interna de Electron). */
function appBaseDir() {
  return app.isPackaged ? path.dirname(process.execPath) : __dirname;
}

function spawnCardReader() {
  if (!config.spawnCardReader || process.platform !== 'win32') {
    return;
  }
  const gwDir = path.resolve(
    appBaseDir(),
    config.turnstileGatewayDir || 'turnstile-gateway',
  );
  const bat = path.join(gwDir, 'iniciar-lector-tarjeta.bat');
  if (!fs.existsSync(bat)) {
    console.warn(
      'No se encontró iniciar-lector-tarjeta.bat en',
      gwDir,
      '- copie turnstile-gateway junto al .exe',
    );
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

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1400,
    height: 900,
    fullscreen: Boolean(config.kioskFullscreen),
    autoHideMenuBar: true,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      nodeIntegration: false,
      contextIsolation: true,
    },
  });

  mainWindow.loadURL(config.accesoUrl);
  mainWindow.on('closed', () => {
    mainWindow = null;
  });
}

app.whenReady().then(() => {
  spawnCardReader();
  createWindow();
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});
