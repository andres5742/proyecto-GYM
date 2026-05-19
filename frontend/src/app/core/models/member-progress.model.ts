export interface MemberProgressEntry {
  id: number;
  recordedAt: string;
  weightKg: number | null;
  chestCm: number | null;
  waistCm: number | null;
  hipsCm: number | null;
  armRightCm: number | null;
  armLeftCm: number | null;
  thighUpperRightCm: number | null;
  thighUpperLeftCm: number | null;
  thighLowerRightCm: number | null;
  thighLowerLeftCm: number | null;
  calfRightCm: number | null;
  calfLeftCm: number | null;
  bodyFatPercent: number | null;
  notes: string | null;
  imageUrls: string[];
  createdAt: string;
}

export interface MemberProgressEntryRequest {
  recordedAt: string;
  weightKg?: number | null;
  chestCm?: number | null;
  waistCm?: number | null;
  hipsCm?: number | null;
  armRightCm?: number | null;
  armLeftCm?: number | null;
  thighUpperRightCm?: number | null;
  thighUpperLeftCm?: number | null;
  thighLowerRightCm?: number | null;
  thighLowerLeftCm?: number | null;
  calfRightCm?: number | null;
  calfLeftCm?: number | null;
  bodyFatPercent?: number | null;
  notes?: string | null;
  imageUrls?: string[];
}

export interface ProgressMetricGroup {
  title: string;
  icon: string;
  hint?: string;
  items: { label: string; value: string }[];
}

export interface ProgressEntryChip {
  label: string;
  kind: 'weight' | 'measures' | 'notes' | 'photos';
}
