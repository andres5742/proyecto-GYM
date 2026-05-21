const { contextBridge } = require('electron');

contextBridge.exposeInMainWorld('sportGymDesktop', {
  isDesktopApp: true,
});
