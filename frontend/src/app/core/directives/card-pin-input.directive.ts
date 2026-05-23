import { Directive, ElementRef, HostListener, inject } from '@angular/core';

/**
 * Lectores USB y teclados del ZKT envían pulsaciones como teclado (HID).
 * Con teclado en español, el carácter mostrado puede ser una letra aunque se pulse un número.
 * Usamos {@code event.code} (Digit1, Numpad1…) para insertar siempre el dígito correcto.
 */
@Directive({
  selector: 'input[appCardPinInput]',
})
export class CardPinInputDirective {
  private readonly el = inject(ElementRef<HTMLInputElement>);

  private static readonly CODE_TO_CHAR: Record<string, string> = {
    Digit0: '0',
    Digit1: '1',
    Digit2: '2',
    Digit3: '3',
    Digit4: '4',
    Digit5: '5',
    Digit6: '6',
    Digit7: '7',
    Digit8: '8',
    Digit9: '9',
    Numpad0: '0',
    Numpad1: '1',
    Numpad2: '2',
    Numpad3: '3',
    Numpad4: '4',
    Numpad5: '5',
    Numpad6: '6',
    Numpad7: '7',
    Numpad8: '8',
    Numpad9: '9',
  };

  @HostListener('keydown', ['$event'])
  onKeyDown(event: KeyboardEvent): void {
    const fromCode = CardPinInputDirective.CODE_TO_CHAR[event.code];
    if (fromCode) {
      event.preventDefault();
      this.insertAtCursor(fromCode);
      return;
    }
    const hexKey = event.code.match(/^Key([A-F])$/);
    if (hexKey) {
      event.preventDefault();
      this.insertAtCursor(hexKey[1]);
    }
  }

  private insertAtCursor(char: string): void {
    const input = this.el.nativeElement;
    const start = input.selectionStart ?? input.value.length;
    const end = input.selectionEnd ?? start;
    const next = `${input.value.slice(0, start)}${char}${input.value.slice(end)}`;
    input.value = next;
    input.dispatchEvent(new Event('input', { bubbles: true }));
    const pos = start + char.length;
    input.setSelectionRange(pos, pos);
  }
}
