import { Component, CUSTOM_ELEMENTS_SCHEMA, forwardRef, signal } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import esI18n from 'emoji-picker-element/i18n/es';
import 'emoji-picker-element/picker.js';

@Component({
  selector: 'app-emoji-picker',
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  templateUrl: './emoji-picker.html',
  styleUrl: './emoji-picker.scss',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => EmojiPickerComponent),
      multi: true,
    },
  ],
})
export class EmojiPickerComponent implements ControlValueAccessor {
  protected readonly open = signal(false);
  protected readonly selected = signal('📌');
  protected readonly esI18n = esI18n;

  private onChange: (value: string) => void = () => {};
  private onTouched: () => void = () => {};

  toggle(): void {
    this.open.update((v) => !v);
    if (this.open()) {
      this.onTouched();
    }
  }

  close(): void {
    this.open.set(false);
  }

  writeValue(value: string | null): void {
    this.selected.set(value?.trim() || '📌');
  }

  registerOnChange(fn: (value: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(_isDisabled: boolean): void {
    // El web component no expone disabled de forma uniforme; el formulario padre puede ignorarlo.
  }

  onEmojiPick(event: Event): void {
    const detail = (event as CustomEvent<{ unicode?: string }>).detail;
    const emoji = detail?.unicode?.trim();
    if (!emoji) {
      return;
    }
    this.selected.set(emoji);
    this.onChange(emoji);
    this.close();
  }
}
