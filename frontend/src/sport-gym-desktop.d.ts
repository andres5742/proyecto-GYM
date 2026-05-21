export {};

declare global {
  interface Window {
    sportGymDesktop?: {
      isDesktopApp: boolean;
      requestClose: () => void;
    };
  }
}
