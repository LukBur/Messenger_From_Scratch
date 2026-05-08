export type LoginRequest = {
  login: string;
  password: string;
};

export type RegisterRequest = {
  email: string;
  login: string;
  displayName: string;
  password: string;
};

export type LoginResponse = {
  token: string;
};

export type MessageResponse = {
  message: string;
};