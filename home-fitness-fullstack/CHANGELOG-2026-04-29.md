# 2026-04-29 Change Log

## Scope

This record covers the Anthropic-style frontend visual refresh, related cleanup, and the latest frontend/backend integration check.

## Frontend Visual Update

- Restyled the site with Anthropic-inspired brand language without changing page content or business flow.
- Unified the visual system around:
  - dark ink / warm cream neutrals
  - accent orange, blue, and green
  - `Poppins` for headings
  - `Lora` for body text
- Reduced the original neon/glow feel and pushed the UI toward a calmer editorial tone.
- Main update areas:
  - global fonts and theme tokens
  - heading hierarchy
  - nav/header/tab surfaces
  - login, records, plans, leaderboard, feed, settings, admin, and training HUD styling

## Files Adjusted For The Visual Refresh

- `frontend/index.html`
- `frontend/src/assets/css/base.css`
- `frontend/src/assets/css/themes.css`
- `frontend/src/assets/css/animations.css`
- `frontend/src/stores/config.js`
- `frontend/src/components/layout/SideNav.vue`
- `frontend/src/components/layout/TopBar.vue`
- `frontend/src/components/layout/TabBar.vue`
- `frontend/src/components/training/HUD.vue`
- `frontend/src/views/Login.vue`
- `frontend/src/views/Records.vue`
- `frontend/src/views/Plans.vue`
- `frontend/src/views/Leaderboard.vue`
- `frontend/src/views/Feed.vue`
- `frontend/src/views/Settings.vue`
- `frontend/src/views/Admin.vue`

## Build And Deployment Verification

- Local frontend build passed with `npm.cmd run build`.
- Docker frontend was rebuilt and restarted with `docker compose up --build -d frontend`.
- Current service ports:
  - frontend: `http://localhost:5173`
  - backend: `http://localhost:8080`

## Cleanup

- Temporary rollback backup directories created during the style migration were removed after confirming the current version should be kept:
  - `frontend/backup-anthropic-20260429`
  - `frontend/src/assets/css/backup-anthropic-20260429`

## Integration Check Summary

The infrastructure path is working, but the application is only partially integrated end-to-end.

### Confirmed Working

- Frontend Nginx proxy to backend `/api`
- Backend health and OpenAPI endpoint
- Email login and JWT issuance
- Authenticated API access
- Exercise list API
- Official plans API
- Plan adoption API
- Session create/list API
- Social post publish/feed API
- Admin dashboard/analytics API

### Not Fully Integrated In The Frontend Yet

- Login page still uses frontend mock auth instead of calling backend auth APIs.
- Records page reads from IndexedDB/local storage rather than backend session APIs.
- Profile page statistics and badges are still derived from local storage.
- Feed page still renders mock data.
- Plans page still renders local mock data.
- Leaderboard page still renders mock data.
- Admin page is still a UI placeholder.

## Backend Completion Assessment

The backend is usable for demo integration, but it is not production-complete yet.

### Current Gaps

- Docker currently runs the backend with the default `dev` profile.
- `dev` profile uses in-memory H2, so backend data is lost after restart.
- SMS login, WeChat login, SMS send, and email send endpoints are placeholder `501` responses.
- AI coach endpoints are still `mock`-backed rather than calling a real provider.
- Challenge join/rank logic is still placeholder-level.
- `/api/posts/feed` is documented as public in the controller, but the current security config does not permit anonymous access.
- No backend test classes were found during the review.

## Notes

- No commit was created in this step.
- The working tree still contains other pre-existing uncommitted changes outside this specific record.
