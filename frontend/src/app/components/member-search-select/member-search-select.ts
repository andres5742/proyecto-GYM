import {
  Component,
  effect,
  ElementRef,
  forwardRef,
  HostListener,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Member } from '../../core/models/member.model';
import {
  MemberAccessMap,
  memberAccessSummary,
} from '../../core/utils/member-access-status';
import { filterMembersByQuery, memberDisplayName } from '../../core/utils/member-search';
import { MemberAccessBadgesComponent } from '../member-access-badges/member-access-badges';

@Component({
  selector: 'app-member-search-select',
  standalone: true,
  imports: [MemberAccessBadgesComponent],
  templateUrl: './member-search-select.html',
  styleUrl: './member-search-select.scss',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => MemberSearchSelectComponent),
      multi: true,
    },
  ],
})
export class MemberSearchSelectComponent implements ControlValueAccessor {
  readonly members = input<Member[]>([]);
  readonly accessByMemberId = input<MemberAccessMap>({});
  readonly label = input('Afiliado');
  readonly placeholder = input('Escribe nombre o cédula…');
  /** Sincronización explícita (p. ej. con signal del padre). */
  readonly memberId = input<number | null | undefined>(undefined);

  readonly memberIdChange = output<number | null>();

  protected readonly searchQuery = signal('');
  protected readonly open = signal(false);
  protected readonly disabled = signal(false);
  protected readonly selectedId = signal<number | null>(null);

  private readonly rootRef = viewChild<ElementRef<HTMLElement>>('root');
  private onChange: (value: number | null) => void = () => {};
  private onTouched: () => void = () => {};
  private ignoreNextDocumentClose = false;

  constructor() {
    effect(() => {
      const external = this.memberId();
      if (external !== undefined && external !== this.selectedId()) {
        this.writeValue(external);
      }
    });
  }

  protected filteredMembers(): Member[] {
    return filterMembersByQuery(this.members(), this.searchQuery());
  }

  protected selectedMember(): Member | undefined {
    const id = this.selectedId();
    if (id == null) {
      return undefined;
    }
    return this.members().find((m) => m.id === id);
  }

  protected accessFlags(memberId: number) {
    return this.accessByMemberId()[memberId];
  }

  protected accessSummary(memberId: number): string {
    return memberAccessSummary(this.accessFlags(memberId));
  }

  writeValue(value: number | null): void {
    this.selectedId.set(value);
    const member = this.selectedMember();
    if (member) {
      this.searchQuery.set(memberDisplayName(member));
    } else if (value == null) {
      this.searchQuery.set('');
    }
  }

  registerOnChange(fn: (value: number | null) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled.set(isDisabled);
  }

  onSearchInput(value: string): void {
    this.searchQuery.set(value);
    this.open.set(true);
    const selected = this.selectedMember();
    if (selected && memberDisplayName(selected) !== value.trim()) {
      this.emitValue(null);
    }
    if (!value.trim()) {
      this.emitValue(null);
    }
  }

  onFocus(): void {
    this.open.set(true);
    this.onTouched();
  }

  pickMember(member: Member, event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    this.ignoreNextDocumentClose = true;
    this.selectMember(member);
  }

  selectMember(member: Member | null): void {
    this.searchQuery.set(member ? memberDisplayName(member) : '');
    this.open.set(false);
    this.emitValue(member?.id ?? null);
  }

  clearSelection(event: Event): void {
    event.stopPropagation();
    this.selectMember(null);
    this.searchQuery.set('');
  }

  protected touch(): void {
    this.onTouched();
  }

  @HostListener('document:pointerdown', ['$event'])
  onDocumentPointerDown(event: PointerEvent): void {
    if (this.ignoreNextDocumentClose) {
      this.ignoreNextDocumentClose = false;
      return;
    }
    const root = this.rootRef()?.nativeElement;
    if (root && !root.contains(event.target as Node)) {
      this.open.set(false);
      const member = this.selectedMember();
      if (member) {
        this.searchQuery.set(memberDisplayName(member));
      }
    }
  }

  private emitValue(id: number | null): void {
    this.selectedId.set(id);
    this.onChange(id);
    this.memberIdChange.emit(id);
    this.onTouched();
  }
}
