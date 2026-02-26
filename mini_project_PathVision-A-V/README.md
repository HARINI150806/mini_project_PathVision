# PathVision Project Structure

This project is a One-Stop Personalized Career & Education Advisor platform tracked in a monorepo style.

## Folder Structure

### Backend (Spring Boot)
Located in `/backend`

- `src/main/java/com/pathvision/config`: Configuration classes (Security, CORS, Swagger).
- `src/main/java/com/pathvision/controller`: REST API endpoints.
- `src/main/java/com/pathvision/service`: Business logic.
- `src/main/java/com/pathvision/repository`: Data access layer (JPA Repositories).
- `src/main/java/com/pathvision/entity`: formatting database models.
- `src/main/java/com/pathvision/dto`: Data Transfer Objects for API requests/responses.
- `src/main/java/com/pathvision/exception`: Global exception handling.
- `src/main/java/com/pathvision/util`: Utility classes (e.g., AI integration helpers).

### Frontend (React + Vite)
Located in `/frontend`

- `src/assets`: Static assets (images, icons).
- `src/components`: Reusable UI components.
  - `common`: Buttons, inputs, modals.
  - `layout`: Header, footer, sidebar.
- `src/pages`: Main application views (Home, Dashboard, Career Path).
- `src/services`: API calls to the backend.
- `src/context`: React Context for state management (Auth, Theme).
- `src/hooks`: Custom React hooks.
- `src/utils`: Helper functions.

## Getting Started

### Backend
1. Navigate to `/backend`.
2. Configure database in `src/main/resources/application.properties`.
3. Run `mvn spring-boot:run`.

### Frontend
1. Navigate to `/frontend`.
2. Run `npm install`.
3. Run `npm run dev`.
