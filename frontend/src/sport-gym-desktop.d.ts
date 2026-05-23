export {};

declare global {
  interface Window {
    sportGymDesktop?: {
      isDesktopApp: boolean;
      requestClose: () => void;
      syncAccessResult?: (payload: {
        result: string;
        gateOpened: boolean;
        deviceUserId?: string;
        credentialType?: string;
      }) => void;
    };
  }
}
