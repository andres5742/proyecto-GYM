export interface DataTableColumn<T> {
  id: string;
  header: string;
  sortable?: boolean;
  sortValue?: (row: T) => string | number;
  cell: (row: T) => string;
  cellClass?: (row: T) => string | null;
}

export const DATA_TABLE_PAGE_SIZES = [10, 25, 50, 100] as const;
