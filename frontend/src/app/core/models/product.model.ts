export interface Product {
  id: number;
  name: string;
  description?: string;
  category?: string;
  quantity: number;
  unitPrice: number;
  stockValue: number;
  minStock: number;
  lowStock: boolean;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ProductRequest {
  name: string;
  description?: string;
  category?: string;
  quantity: number;
  unitPrice: number;
  minStock?: number;
  active?: boolean;
}

export interface StockAdjustmentRequest {
  delta: number;
  reason?: string;
}
