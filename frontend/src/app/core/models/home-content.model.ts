export interface CarouselSlide {
  id: number;
  imageUrl: string;
  title?: string | null;
  caption?: string | null;
  linkUrl?: string | null;
  displayOrder: number;
  active: boolean;
}

export interface CarouselSlideRequest {
  imageUrl: string;
  title?: string;
  caption?: string;
  linkUrl?: string;
  displayOrder?: number;
  active?: boolean;
}

export type MediaType = 'PHOTO' | 'VIDEO';

export interface GymMediaItem {
  id: number;
  mediaType: MediaType;
  mediaUrl: string;
  thumbnailUrl?: string | null;
  title?: string | null;
  displayOrder: number;
  active: boolean;
}

export interface GymMediaItemRequest {
  mediaType: MediaType;
  mediaUrl: string;
  thumbnailUrl?: string;
  title?: string;
  displayOrder?: number;
  active?: boolean;
}

export interface SiteFooter {
  tagline?: string | null;
  address?: string | null;
  phone?: string | null;
  instagramUrl?: string | null;
  facebookUrl?: string | null;
  tiktokUrl?: string | null;
  youtubeUrl?: string | null;
  whatsappUrl?: string | null;
  updatedAt?: string;
}
