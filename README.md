<div align="center">

# ⚙️ TaleMind — Backend

### The API powering [TaleMind](https://www.talemindai.com/): AI story generation, auth, and vocabulary management.

[![Website](https://img.shields.io/badge/Website-talemindai.com-4F86F7?style=flat-square&logo=googlechrome&logoColor=white)](https://www.talemindai.com/)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-JPA-4479A1?style=flat-square&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![MongoDB](https://img.shields.io/badge/MongoDB-NoSQL-47A248?style=flat-square&logo=mongodb&logoColor=white)](https://www.mongodb.com/)
[![Deploy](https://img.shields.io/badge/Deployed%20on-Railway-0B0D0E?style=flat-square&logo=railway&logoColor=white)](https://railway.app/)

</div>

---

## ✨ Overview

This is the backend for **TaleMind**, an AI‑powered language‑learning platform where users generate short stories from the vocabulary they want to learn — seeing words used naturally in context instead of memorizing them from flashcards.

The service exposes a REST API consumed by the [TaleMind mobile app](https://github.com/MehmetKaTR/translate_easy) and the website. It handles authentication, LLM‑based story generation, per‑user token budgeting, word/word‑list management, and a full admin layer for monitoring and moderation.

---

## 🚀 Features

- 🤖 **AI story generation** — builds coherent stories from a user's selected words via **Google Gemini**, with automatic model fallback
- 🔤 **Word validation & translation suggestions** — validates input words and suggests translations
- 🔐 **Full auth suite** — registration, email verification, password reset, JWT access tokens + **refresh‑token rotation**, and **Google Sign‑In**
- 🎟️ **Token budgeting** — per‑user daily token limits and daily story limits, split across **free** and **premium** tiers
- 🗂️ **Vocabulary management** — words, word lists, starred/favorite words (many‑to‑many)
- 🛡️ **Rate limiting** — throttling on sensitive auth endpoints (login, forgot/reset password)
- 👤 **Account lifecycle** — soft‑delete with a configurable grace period + cancel‑deletion
- 🧑‍💼 **Admin panel API** — user management, action logs, LLM model configuration, and import/export logs
- 📊 **Usage logging** — token‑usage and import/export audit trails
- 📧 **Email delivery** — SMTP and Resend providers for verification & reset codes
- ❤️ **Health checks** — Spring Actuator (`/actuator/health`)

---

## 🛠️ Tech Stack

| Area | Technology |
|------|-----------|
| Language | **Java 21** |
| Framework | **Spring Boot 3.5** (Web, Data JPA, Data MongoDB, Validation, Mail, Security) |
| Relational DB | **MySQL** (via Spring Data JPA / Hibernate) |
| NoSQL DB | **MongoDB** |
| Auth | **JWT** (jjwt) + Google OAuth token verification |
| AI | **Google Gemini** API (configurable model + fallbacks) |
| Email | SMTP / **Resend** |
| Build | **Maven** |
| Hosting | **Railway** |

---

## 🧱 Architecture

Classic layered architecture with a clean separation of concerns:

```
controller/     # REST endpoints (auth, story, words, admin, public, logs)
  auth/         # Registration, login, verification, Google sign-in
  admin/        # Admin auth & management
service/        # Business logic (story generation, tokens, rate limiting,
                # account lifecycle, email, Google verification, ...)
repository/     # Spring Data JPA repositories
entity/         # JPA entities (User, Story, Word, WordList, RefreshToken,
                # LlmModel, TokenUsageLog, AdminActionLog, ...)
dto/            # Request/response DTOs (auth, story, admin)
security/       # JWT filter & authenticated-user resolution
exception/      # Custom exceptions + global exception handler
config/         # Security config, filters
```

---

## 📡 API Overview

Base path: `/api`

| Group | Base | Highlights |
|-------|------|-----------|
| **Auth** | `/api/auth` | `register`, `verify-email`, `login`, `social/google`, `refresh`, `logout`, `forgot-password`, `reset-password`, account deletion & onboarding |
| **Story generation** | `/api/story` | `generate`, `validate-words`, `limit-status` |
| **Stories** | `/api/stories` | Story history / retrieval |
| **Words** | `/api/words` | `all`, `byWordList`, `starred`, `add`, `update`, `delete`, `updateStarred` |
| **Word lists** | `/api/word_lists` | Folder / list management |
| **Users** | `/api/users` | User profile & preferences |
| **Public** | `/api/public` | Unauthenticated public data |
| **Admin** | `/api/admin`, `/api/admin/auth` | Admin auth, user management, logs, LLM models |

---

## 🏁 Getting Started

> Requires **JDK 21**, **Maven**, a **MySQL** instance, and (optionally) **MongoDB**.

```bash
# 1. Clone
git clone https://github.com/MehmetKaTR/project-easy-translate.git
cd project-easy-translate

# 2. Configure environment
cp .env.example .env      # then fill in the values below

# 3. Run (dev profile)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

The API starts on `http://localhost:8080` (or `SERVER_PORT`).

### Configuration

All secrets are injected via **environment variables** — nothing sensitive is committed. Key variables:

| Variable | Description |
|----------|-------------|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | MySQL connection |
| `MONGO_URI` | MongoDB connection |
| `JWT_SECRET`, `JWT_EXPIRATION_MS` | JWT signing secret & TTL |
| `GEMINI_API_KEY`, `GEMINI_MODEL` | Google Gemini config |
| `GOOGLE_OAUTH_CLIENT_IDS` | Accepted Google OAuth client IDs |
| `ALLOWED_ORIGINS` | CORS allow‑list |
| `MAIL_*` / `RESEND_API_KEY` | Email provider config |
| `FREE_DAILY_TOKEN_LIMIT`, `FREE_DAILY_STORY_LIMIT`, `PREMIUM_DAILY_STORY_LIMIT` | Usage limits |

See [`.env.example`](.env.example) for the full list.

---

## 🔗 Related

- 🌐 **Website:** [talemindai.com](https://www.talemindai.com/)
- 📱 **Mobile app:** [translate_easy](https://github.com/MehmetKaTR/translate_easy)

---

## 📄 License

See repository license.

<div align="center">
<sub>Built by <a href="https://github.com/MehmetKaTR">Mehmet Kaan Genç</a></sub>
</div>
