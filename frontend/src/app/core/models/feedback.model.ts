export type FeedbackType = 'SUGGESTION' | 'COMPLAINT' | 'PRAISE';
export type FeedbackStatus = 'PENDING' | 'RESOLVED' | 'NOT_RESOLVED';

export interface FeedbackMessage {
  id: number;
  type: FeedbackType;
  typeLabel: string;
  message: string;
  anonymous: boolean;
  authorName?: string | null;
  displayName: string;
  status: FeedbackStatus;
  statusLabel: string;
  adminNote?: string | null;
  createdAt: string;
  resolvedAt?: string | null;
}

export interface FeedbackMessageRequest {
  type: FeedbackType;
  message: string;
  anonymous: boolean;
  authorName?: string;
}

export interface FeedbackStatusUpdateRequest {
  status: 'RESOLVED' | 'NOT_RESOLVED';
  adminNote?: string;
}
