export interface TrainerRatingParticipant {
  id: number;
  fullName: string;
  photoUrl?: string;
}

export interface TrainerRatingSubmitRequest {
  employeeId: number;
  identificationNumber: string;
  score: number;
}

export interface RatingParticipantAdmin {
  id: number;
  fullName: string;
  role: string;
  roleLabel: string;
  active: boolean;
  ratingEligible: boolean;
  photoUrl?: string;
}

export interface RatingParticipantUpdateRequest {
  ratingEligible?: boolean;
  photoUrl?: string;
}

export interface TrainerRatingMonthlySummary {
  employeeId: number;
  fullName: string;
  photoUrl?: string;
  averageScore: number;
  ratingCount: number;
  rank: number;
}
