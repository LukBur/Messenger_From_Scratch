export type UserResponse = {
  id: string;
  email: string;
  login: string;
  displayName: string;
  role: string;
  avatarUrl: string | null;
};

export type UserSearchResponse = {
  id: string;
  login: string;
  displayName: string;
  avatarUrl: string | null;
  role: string;
};