# Messenger From Scratch

A full-stack real-time messenger application built with Spring Boot, Next.js, MongoDB and WebSocket/STOMP.

The application supports private conversations, group conversations, real-time messaging, message editing and deletion, disappearing messages, user profiles, avatars and group management.

---

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Local Setup](#local-setup)
  - [Requirements](#requirements)
  - [Environment Variables](#environment-variables)
  - [Backend Configuration](#backend-configuration)
  - [Running the Backend Locally](#running-the-backend-locally)
  - [Running the Backend with Docker](#running-the-backend-with-docker)
  - [Running the Frontend Locally](#running-the-frontend-locally)
- [Running Tests](#running-tests)
  - [Backend Tests](#backend-tests)
  - [Frontend Tests](#frontend-tests)
- [Building the Project](#building-the-project)
  - [Backend Build](#backend-build)
  - [Frontend Build](#frontend-build)
- [Deployment](#deployment)
  - [Backend on Render](#backend-on-render)
  - [Frontend on Vercel](#frontend-on-vercel)
- [Troubleshooting](#troubleshooting)
- [Notes](#notes)

---

## Features

### Authentication and users

- User registration and login
- JWT-based authentication
- User profile management
- Avatar URL support
- Password change

### Conversations

- Private conversations between two users
- Group conversations
- Conversation list with last message preview
- Real-time conversation updates

### Messages

- Real-time text messaging using WebSocket/STOMP
- Message editing
- Message deletion
- Disappearing messages
- Message history
- Live synchronization between browser sessions

### Groups

- Create group conversations
- Add and remove participants
- Leave group
- Transfer group ownership
- Delete group as owner
- Group management modal

### Deployment

- Backend deployed as a Docker Web Service on Render
- Frontend deployed on Vercel
- MongoDB hosted on MongoDB Atlas

---

## Tech Stack

### Backend

- Java 21
- Spring Boot
- Spring Security
- JWT
- Spring Data MongoDB
- WebSocket / STOMP
- Maven
- Docker

### Frontend

- Next.js
- React
- TypeScript
- CSS
- SockJS
- STOMP.js
- Jest
- React Testing Library

### Database

- MongoDB
- MongoDB Atlas

---

## Project Structure

```text
Messenger_From_Scratch/
├── backend/
│   ├── src/
│   ├── Dockerfile
│   ├── pom.xml
│   └── mvnw
│
├── frontend/
│   ├── src/
│   ├── package.json
│   ├── next.config.ts
│   ├── jest.config.js
│   └── jest.setup.js
│
└── README.md
```

---

## Local Setup

The project can be run locally in two ways:

1. with MongoDB Atlas as a remote database
2. with a local MongoDB instance

The frontend always communicates with the backend through environment variables.

---

## Requirements

Before running the project, install:

* Java 21
* Node.js
* npm
* Docker
* MongoDB locally or a MongoDB Atlas account

---

## Environment Variables

### Backend

The backend requires the following environment variables:

```env
MONGODB_URI=
JWT_SECRET=
```

Example for MongoDB Atlas:

```env
MONGODB_URI=mongodb+srv://USER:PASSWORD@cluster0.xxxxx.mongodb.net/messengerdb?retryWrites=true&w=majority
JWT_SECRET=your_jwt_secret
```

Example for local MongoDB:

```env
MONGODB_URI=mongodb://localhost:27017/messengerdb
JWT_SECRET=your_jwt_secret
```

The MongoDB URI must include the database name, for example:

```text
/messengerdb
```

### Frontend

Create a `.env.local` file inside the `frontend` directory:

```env
NEXT_PUBLIC_API_URL=http://localhost:8080
NEXT_PUBLIC_WS_URL=http://localhost:8080
```

These values point the local frontend to the local backend.

---

## Backend Configuration

The backend uses the following configuration in:

```text
backend/src/main/resources/application.properties
```

```properties
spring.application.name=backend
spring.data.mongodb.uri=${MONGODB_URI}

jwt.secret=${JWT_SECRET}
jwt.expiration=43200000

server.port=${PORT:8080}
```

`jwt.expiration=43200000` means that JWT tokens are valid for 12 hours.

---

## Running the Backend Locally

### Option 1: IntelliJ IDEA

1. Open the `backend` directory in IntelliJ IDEA.
2. Add the required environment variables to the run configuration:

```env
MONGODB_URI=mongodb://localhost:27017/messengerdb
JWT_SECRET=your_jwt_secret
```

or, when using MongoDB Atlas:

```env
MONGODB_URI=mongodb+srv://USER:PASSWORD@cluster0.xxxxx.mongodb.net/messengerdb?retryWrites=true&w=majority
JWT_SECRET=your_jwt_secret
```

3. Run the main class:

```text
BackendApplication
```

The backend should be available at:

```text
http://localhost:8080
```

---

## Running the Backend with Docker

Go to the backend directory:

```bash
cd backend
```

Build the Docker image:

```bash
docker build -t messenger-backend .
```

Run the backend with local MongoDB:

```powershell
docker run --rm -p 8080:8080 -e MONGODB_URI="mongodb://host.docker.internal:27017/messengerdb" -e JWT_SECRET="your_jwt_secret" messenger-backend
```

Run the backend with MongoDB Atlas:

```powershell
docker run --rm -p 8080:8080 -e MONGODB_URI="mongodb+srv://USER:PASSWORD@cluster0.xxxxx.mongodb.net/messengerdb?retryWrites=true&w=majority" -e JWT_SECRET="your_jwt_secret" messenger-backend
```

---

## Running the Frontend Locally

Go to the frontend directory:

```bash
cd frontend
```

Install dependencies:

```bash
npm install
```

Create `.env.local`:

```env
NEXT_PUBLIC_API_URL=http://localhost:8080
NEXT_PUBLIC_WS_URL=http://localhost:8080
```

Start the development server:

```bash
npm run dev
```

The frontend should be available at:

```text
http://localhost:3000
```

---

## Running Tests

### Backend tests

Go to the backend directory:

```bash
cd backend
```

Run tests:

```bash
./mvnw test
```

On Windows:

```bash
mvnw test
```

### Frontend tests

Go to the frontend directory:

```bash
cd frontend
```

Run tests:

```bash
npm test
```

Frontend tests use Jest and React Testing Library.

---

## Building the Project

### Backend build

```bash
cd backend
./mvnw clean package
```

On Windows:

```bash
cd backend
mvnw clean package
```

The generated `.jar` file will be available in:

```text
backend/target
```

### Frontend build

```bash
cd frontend
npm run build
```

---

## Deployment

The project can be deployed using:

* Render for the backend
* Vercel for the frontend
* MongoDB Atlas for the database

### Backend on Render

The backend is deployed as a Docker Web Service.

Required Render environment variables:

```env
MONGODB_URI=mongodb+srv://USER:PASSWORD@cluster0.xxxxx.mongodb.net/messengerdb?retryWrites=true&w=majority
JWT_SECRET=your_jwt_secret
```

The backend must also allow the deployed frontend domain in CORS and WebSocket configuration.

### Frontend on Vercel

Required Vercel environment variables:

```env
NEXT_PUBLIC_API_URL=https://your-backend.onrender.com
NEXT_PUBLIC_WS_URL=https://your-backend.onrender.com
```

After changing environment variables on Vercel, redeploy the frontend.

---

## Troubleshooting

### Backend cannot connect to MongoDB Atlas

Check that:

* `MONGODB_URI` contains the database name, for example `/messengerdb`
* the database username and password are correct
* the current IP address is allowed in MongoDB Atlas Network Access
* special characters in the password are URL-encoded
* the MongoDB Atlas cluster is running

### Frontend cannot log in

Check that:

* `NEXT_PUBLIC_API_URL` points to the correct backend URL
* the backend is running
* CORS allows the frontend origin
* the backend is reachable from the browser

### WebSocket does not connect

Check that:

* `NEXT_PUBLIC_WS_URL` points to the correct backend URL
* the `/ws` endpoint is available
* the frontend origin is allowed in `WebSocketConfig`
* the backend is running and not sleeping on the hosting provider

---

## Notes

On free hosting plans, the backend service may go to sleep after a period of inactivity. The first request after inactivity may take longer because the service needs to wake up.