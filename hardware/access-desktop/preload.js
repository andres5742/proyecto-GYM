const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('sportGymDesktop', {
  isDesktopApp: true,
  requestClose: () => ipcRenderer.send('app-request-quit'),
  /** Pone o quita el seguro del torniquete según resultado del API. */
  syncAccessResult: (payload) => {
    if (payload && typeof payload === 'object') {
      ipcRenderer.send('gate-sync', payload);
      return;
    }
    ipcRenderer.send('gate-sync', { result: payload, gateOpened: false });
  },
});
