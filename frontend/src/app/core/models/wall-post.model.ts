export type PostCategory = 'AVISO' | 'PROMO' | 'HORARIO' | 'MOTIVACION';

export interface WallPost {
  id: number;
  title: string;
  body: string;
  emoji?: string | null;
  category: PostCategory;
  categoryLabel: string;
  authorId: number;
  authorName: string;
  authorRoleLabel: string;
  publishedAt: string;
  permanent: boolean;
  displayDays?: number | null;
  expiresAt?: string | null;
  createdAt: string;
  imageUrls?: string[];
}

export interface WallPostRequest {
  title: string;
  body: string;
  emoji?: string;
  category: PostCategory;
  permanent: boolean;
  displayDays?: number;
  imageUrls?: string[];
}
