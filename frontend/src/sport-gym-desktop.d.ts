export {};

declare global {
  interface Window {
    sportGymDesktop?: {
      isDesktopApp: boolean;
      requestClose: () => void;
      syncAccessResult?: (result: string, gateOpened: boolean) => void;
    };
  }
}
