export interface DataTableColumn<T> {
  id: string;
  header: string;
  sortable?: boolean;
  sortValue?: (row: T) => string | number;
  cell: (row: T) => string;
  /** Clases del &lt;th&gt; del encabezado */
  headerClass?: string;
  /** Clases del &lt;td&gt; */
  cellClass?: (row: T) => string | null;
  /** Clases de un &lt;span&gt; interior (p. ej. badges) */
  cellInnerClass?: (row: T) => string | null;
}

export const DATA_TABLE_PAGE_SIZES = [10, 20, 30, 50] as const;
