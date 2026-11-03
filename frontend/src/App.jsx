import { useEffect, useState } from 'react'

const TABS = ['Overview', 'Projects', 'Experience']

function useJson(url) {
  const [state, setState] = useState({ data: null, error: null, loading: true })
  useEffect(() => {
    let alive = true
    fetch(url)
      .then((r) => (r.ok ? r.json() : Promise.reject(new Error(`${r.status} ${r.statusText}`))))
      .then((data) => alive && setState({ data, error: null, loading: false }))
      .catch((error) => alive && setState({ data: null, error, loading: false }))
    return () => { alive = false }
  }, [url])
  return state
}

function initials(name = '') {
  return name.split(/\s+/).filter(Boolean).slice(0, 2).map((w) => w[0].toUpperCase()).join('')
}

function timeAgo(iso) {
  if (!iso) return ''
  const days = Math.floor((Date.now() - new Date(iso).getTime()) / 86400000)
  if (days < 1) return 'today'
  if (days < 30) return `${days}d ago`
  if (days < 365) return `${Math.floor(days / 30)}mo ago`
  return `${Math.floor(days / 365)}y ago`
}

function Sidebar({ profile, hasCv, githubUrl }) {
  const links = [...(profile.links ?? [])]
  if (githubUrl && !links.some((l) => l.url.includes('github.com'))) {
    links.unshift({ label: 'GitHub', url: githubUrl })
  }
  return (
    <aside className="sidebar card">
      {profile.avatarUrl
        ? <img className="avatar" src={profile.avatarUrl} alt={profile.name} />
        : <div className="avatar avatar-fallback">{initials(profile.name)}</div>}
      <h1>{profile.name}</h1>
      <p className="title">{profile.title}</p>
      {profile.location && <p className="muted">📍 {profile.location}</p>}
      {hasCv && (
        <a className="btn primary" href="/api/cv" target="_blank" rel="noreferrer">Download CV</a>
      )}
      <nav className="links">
        {links.map((l) => (
          <a key={l.url} href={l.url} target="_blank" rel="noreferrer">{l.label}</a>
        ))}
      </nav>
    </aside>
  )
}

function Overview({ profile }) {
  return (
    <>
      <section className="card intro">
        <p>{profile.summary}</p>
      </section>
      <section>
        <h2 className="section-title">What I focus on</h2>
        <div className="focus-grid">
          {profile.highlights?.map((h) => (
            <div className="card focus" key={h.title}>
              <span className="focus-icon" aria-hidden="true">{h.icon}</span>
              <h3>{h.title}</h3>
              <p className="muted">{h.text}</p>
            </div>
          ))}
        </div>
      </section>
      <section className="card">
        <h2>What I work with</h2>
        <div className="skills">
          {profile.skills?.map((g) => (
            <div key={g.category}>
              <h3>{g.category}</h3>
              <div className="tags">{g.items.map((i) => <span className="tag" key={i}>{i}</span>)}</div>
            </div>
          ))}
        </div>
      </section>
    </>
  )
}

function Projects({ profile, repos }) {
  return (
    <>
      {profile.featuredProjects?.length > 0 && (
        <section>
          <h2 className="section-title">Things I've built</h2>
          <div className="grid">
            {profile.featuredProjects.map((p) => {
              const body = (
                <>
                  <h3>{p.name}</h3>
                  <p className="muted">{p.description}</p>
                  <div className="tags">{p.tags?.map((t) => <span className="tag" key={t}>{t}</span>)}</div>
                </>
              )
              return p.url
                ? <a className="card project" key={p.name} href={p.url} target="_blank" rel="noreferrer">{body}</a>
                : <div className="card project static" key={p.name}>{body}</div>
            })}
          </div>
        </section>
      )}
      <section>
        <h2 className="section-title">
          On GitHub
          {repos.data?.profileUrl && (
            <a className="small-link" href={repos.data.profileUrl} target="_blank" rel="noreferrer">view all →</a>
          )}
        </h2>
        {repos.loading && <p className="muted">Loading repositories…</p>}
        {repos.error && <p className="muted">Couldn't load repositories.</p>}
        {repos.data && !repos.data.enabled && (
          <p className="muted card">Set <code>GITHUB_USERNAME</code> on the backend to show your public repos here.</p>
        )}
        {repos.data?.enabled && repos.data.repos.length === 0 && <p className="muted">No public repositories yet.</p>}
        <div className="grid">
          {repos.data?.repos.map((r) => (
            <a className="card project" key={r.id} href={r.url} target="_blank" rel="noreferrer">
              <h3>{r.name}</h3>
              <p className="muted">{r.description || 'No description'}</p>
              <div className="tags">{r.topics.slice(0, 4).map((t) => <span className="tag" key={t}>{t}</span>)}</div>
              <div className="meta">
                {r.language && <span>● {r.language}</span>}
                <span>★ {r.stars}</span>
                <span>⑂ {r.forks}</span>
                <span>{timeAgo(r.pushedAt)}</span>
              </div>
            </a>
          ))}
        </div>
      </section>
    </>
  )
}

function Experience({ profile }) {
  return (
    <>
      <section className="card">
        <h2>Where I've worked</h2>
        <ol className="timeline">
          {profile.experience?.map((e) => (
            <li key={`${e.company}-${e.start}`}>
              <div className="row">
                <strong>{e.role}</strong>
                <span className="muted">{e.start} – {e.end}</span>
              </div>
              <div className="muted">{e.company}{e.location ? ` · ${e.location}` : ''}</div>
              <ul>{e.bullets?.map((b) => <li key={b}>{b}</li>)}</ul>
            </li>
          ))}
        </ol>
      </section>
      <section className="card">
        <h2>Education</h2>
        {profile.education?.map((e) => (
          <div className="row edu" key={e.school}>
            <div><strong>{e.degree}</strong><div className="muted">{e.school}</div></div>
            <span className="muted">{e.start} – {e.end}</span>
          </div>
        ))}
      </section>
    </>
  )
}

export default function App() {
  const profile = useJson('/api/profile')
  const repos = useJson('/api/repos')
  const [tab, setTab] = useState('Overview')

  useEffect(() => {
    if (profile.data?.name) document.title = `${profile.data.name} — Portfolio`
  }, [profile.data])

  if (profile.loading) return <div className="center muted">Loading…</div>
  if (profile.error) return <div className="center">Couldn't load profile: {profile.error.message}</div>

  const p = profile.data
  return (
    <div className="layout">
      <Sidebar profile={p} hasCv={repos.data?.hasCv} githubUrl={repos.data?.profileUrl} />
      <main>
        <div className="tabs" role="tablist">
          {TABS.map((t) => (
            <button key={t} role="tab" aria-selected={tab === t} className={tab === t ? 'active' : ''} onClick={() => setTab(t)}>
              {t}
            </button>
          ))}
        </div>
        {tab === 'Overview' && <Overview profile={p} />}
        {tab === 'Projects' && <Projects profile={p} repos={repos} />}
        {tab === 'Experience' && <Experience profile={p} />}
      </main>
    </div>
  )
}
