# Auth

Модуль отвечает за login, refresh, logout и текущего пользователя. Пароли хранятся в виде хэшей. После login frontend получает access token, refresh token и профиль пользователя с ролями и permissions.

Основные endpoints:

- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `GET /api/auth/me`
