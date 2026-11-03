# Portfolio Dashboard

A simple personal portfolio / CV dashboard.

- **Backend:** Java 21 + Spring Boot 3 — serves your profile data, your public GitHub repos, and your CV PDF.
- **Frontend:** React (Vite) — a single-page dashboard with *Overview*, *Projects* and *Experience* tabs, light/dark mode, mobile friendly.

## Make it yours

1. **Your details** — edit [`backend/src/main/resources/portfolio.json`](backend/src/main/resources/portfolio.json)
   (name, title, summary, links, highlights, skills, experience, education, featured projects).
   Set `avatarUrl` to e.g. `https://github.com/<your-username>.png`.
2. **Your GitHub repos** — set `GITHUB_USERNAME`. Public, non-archived, non-fork repos are shown, sorted by stars.
   Add *topics* to your repos on GitHub and they show up as tags.
3. **Your CV** — drop a PDF at `backend/src/main/resources/cv.pdf` and a *Download CV* button appears.

## Run locally

Requirements: Java 21, Maven, Node 20+.

One jar with everything:

```bash
cd backend && mvn -Pfullstack package
```

```bash
GITHUB_USERNAME=your-username java -jar backend/target/portfolio-dashboard-0.1.0.jar
```

Open http://localhost:8080.

Dev mode with hot reload (two terminals):

```bash
cd backend && GITHUB_USERNAME=your-username mvn spring-boot:run
```

```bash
cd frontend && npm install && npm run dev
```

Open http://localhost:5173 (Vite proxies `/api` to Spring on :8080).

## Docker

```bash
docker build -t portfolio-dashboard .
```

```bash
docker run -p 8080:8080 -e GITHUB_USERNAME=your-username portfolio-dashboard
```

## Configuration

| Env var | Default | Purpose |
|---|---|---|
| `GITHUB_USERNAME` | *(empty — section hidden)* | Whose public repos to show |
| `GITHUB_TOKEN` | *(empty)* | Optional; raises the API rate limit from 60/h (responses are cached anyway) |
| `GITHUB_MAX_REPOS` | `9` | Number of repos shown |
| `GITHUB_INCLUDE_FORKS` | `false` | Show forked repos too |
| `GITHUB_CACHE_MINUTES` | `30` | How long GitHub results are cached |
| `PORTFOLIO_PROFILE` | `classpath:portfolio.json` | Use `file:/path/portfolio.json` to edit without rebuilding |
| `PORTFOLIO_CV` | `classpath:cv.pdf` | Location of your CV PDF |
| `PORT` | `8080` | HTTP port |

## API

- `GET /api/profile` — contents of `portfolio.json`
- `GET /api/repos` — `{ enabled, profileUrl, hasCv, repos: [...] }`
- `GET /api/cv` — the CV PDF (404 if none)
- `GET /actuator/health`
