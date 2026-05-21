import { CashShortfall } from './cash-shortfall.model';
import { BillingCashRegister } from './billing.model';
import { InventoryMissingLine } from './cash-shortfall.model';

export interface ProductInventoryLine {
  productId: number;
  productName: string;
  category: string;
  expectedQuantity: number;
  unitPrice: number;
}

export interface BillingCashRegisterClosePreview {
  cashInDrawer: number;
  fiadoCashCollected: number;
  expectedCashTotal: number;
  products: ProductInventoryLine[];
}

export interface ProductInventoryCountItem {
  productId: number;
  countedQuantity: number;
}

export interface CloseBillingCashRegisterRequest {
  cashCount: {
    bill2000: number;
    bill5000: number;
    bill10000: number;
    bill20000: number;
    bill50000: number;
    bill100000: number;
    coin1000: number;
    coin500: number;
    coin200: number;
    coin100: number;
    coin50: number;
  };
  inventoryCounts: ProductInventoryCountItem[];
  notes?: string;
}

export interface BillingCashRegisterCloseResult {
  register: BillingCashRegister;
  declaredCashTotal: number;
  expectedCashTotal: number;
  cashShortfall?: CashShortfall | null;
  inventoryShortfall?: CashShortfall | null;
  inventoryMissingLines: InventoryMissingLine[];
}
