const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('sportGymDesktop', {
  isDesktopApp: true,
  requestClose: () => ipcRenderer.send('app-request-quit'),
});
