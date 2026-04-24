## API Endpoints

### Auth

#### POST `/api/auth/register`
Registers a new user account.

#### POST `/api/auth/login`
Logs in an existing user and returns an authentication token.

---

### Users

#### GET `/api/users/me`
Returns information about the currently authenticated user.

**Requires authentication:** Yes

#### GET `/api/users/search`
Searches users by name.

**Requires authentication:** Yes

---

### Conversations

#### POST `/api/conversations`
Creates a new conversation between the authenticated user and another user.

**Requires authentication:** Yes  
**Required data:** other users ID

#### GET `/api/conversations/my`
Returns conversations of the currently authenticated user.

**Requires authentication:** Yes

---

### Messages

#### POST `/api/messages`
Sends a new message to an existing conversation.

**Requires authentication:** Yes  
**Required data:** conversation ID, message content

#### GET `/api/conversations/{conversation_id}/messages`
Returns messages from a specific conversation.

**Requires authentication:** Yes