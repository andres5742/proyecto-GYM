const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('sportGymDesktop', {
  isDesktopApp: true,
  requestClose: () => ipcRenderer.send('app-request-quit'),
  /** Pone o quita el seguro del torniquete según resultado del API. */
  syncAccessResult: (result, gateOpened) =>
    ipcRenderer.send('gate-sync', { result, gateOpened }),
});
