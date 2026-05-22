export type BirthdayPersonType = 'MEMBER' | 'EMPLOYEE';

export interface UpcomingBirthday {
  personType: BirthdayPersonType;
  personTypeLabel: string;
  personId: number;
  fullName: string;
  celebrationDate: string;
  daysUntil: number;
  turningAge: number;
}
