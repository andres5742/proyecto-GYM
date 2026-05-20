import { Component, input } from '@angular/core';
import { MemberAccessFlags } from '../../core/utils/member-access-status';

@Component({
  selector: 'app-member-access-badges',
  standalone: true,
  templateUrl: './member-access-badges.html',
  styleUrl: './member-access-badges.scss',
})
export class MemberAccessBadgesComponent {
  readonly flags = input<MemberAccessFlags | undefined>();
  readonly compact = input(false);
}
