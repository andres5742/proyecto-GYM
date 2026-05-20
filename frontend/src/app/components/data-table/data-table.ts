import { NgTemplateOutlet } from '@angular/common';
import {
  Component,
  computed,
  contentChild,
  effect,
  input,
  signal,
  TemplateRef,
} from '@angular/core';
import { DataTableColumn, DATA_TABLE_PAGE_SIZES } from './data-table.model';

@Component({
  selector: 'app-data-table',
  standalone: true,
  imports: [NgTemplateOutlet],
  templateUrl: './data-table.html',
  styleUrl: './data-table.scss',
})
export class DataTableComponent<T> {
  readonly rows = input.required<T[]>();
  readonly columns = input.required<DataTableColumn<T>[]>();
  readonly searchPlaceholder = input('Buscar…');
  readonly emptyMessage = input('Sin registros.');
  readonly pageSizeDefault = input(10);
  readonly showActions = input(false);
  readonly rowClass = input<((row: T) => string | null) | undefined>();

  readonly actionsTemplate = contentChild<TemplateRef<{ $implicit: T }>>('actions');

  protected readonly searchQuery = signal('');
  protected readonly sortColumnId = signal<string | null>(null);
  protected readonly sortAsc = signal(true);
  protected readonly page = signal(1);
  protected readonly pageSize = signal(10);

  protected readonly pageSizeOptions = DATA_TABLE_PAGE_SIZES;

  protected readonly filteredRows = computed(() => {
    const q = this.searchQuery().trim().toLowerCase();
    const list = [...this.rows()];
    if (!q) {
      return list;
    }
    return list.filter((row) =>
      this.columns().some((col) => col.cell(row).toLowerCase().includes(q)),
    );
  });

  protected readonly sortedRows = computed(() => {
    const list = [...this.filteredRows()];
    const colId = this.sortColumnId();
    if (!colId) {
      return list;
    }
    const col = this.columns().find((c) => c.id === colId);
    if (!col?.sortable) {
      return list;
    }
    const dir = this.sortAsc() ? 1 : -1;
    const valueOf = col.sortValue ?? col.cell;
    list.sort((a, b) => {
      const av = valueOf(a);
      const bv = valueOf(b);
      if (typeof av === 'number' && typeof bv === 'number') {
        return (av - bv) * dir;
      }
      return String(av).localeCompare(String(bv), 'es') * dir;
    });
    return list;
  });

  protected readonly totalPages = computed(() =>
    Math.max(1, Math.ceil(this.sortedRows().length / this.pageSize())),
  );

  protected readonly paginatedRows = computed(() => {
    const page = Math.min(this.page(), this.totalPages());
    const start = (page - 1) * this.pageSize();
    return this.sortedRows().slice(start, start + this.pageSize());
  });

  protected readonly pageStart = computed(() =>
    this.sortedRows().length === 0 ? 0 : (Math.min(this.page(), this.totalPages()) - 1) * this.pageSize() + 1,
  );

  protected readonly pageEnd = computed(() =>
    Math.min(this.pageStart() + this.pageSize() - 1, this.sortedRows().length),
  );

  constructor() {
    effect(() => {
      this.pageSize.set(this.pageSizeDefault());
    });
  }

  onSearch(value: string): void {
    this.searchQuery.set(value);
    this.page.set(1);
  }

  onPageSizeChange(value: string): void {
    this.pageSize.set(Number(value));
    this.page.set(1);
  }

  sortBy(columnId: string): void {
    if (this.sortColumnId() === columnId) {
      this.sortAsc.set(!this.sortAsc());
    } else {
      this.sortColumnId.set(columnId);
      this.sortAsc.set(true);
    }
    this.page.set(1);
  }

  sortIndicator(columnId: string): string {
    if (this.sortColumnId() !== columnId) {
      return '';
    }
    return this.sortAsc() ? ' ▲' : ' ▼';
  }

  goToPage(next: number): void {
    this.page.set(Math.max(1, Math.min(next, this.totalPages())));
  }

  cellClass(row: T, col: DataTableColumn<T>): string | null {
    return col.cellClass?.(row) ?? null;
  }
}
