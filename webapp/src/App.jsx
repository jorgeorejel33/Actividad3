import React, { useEffect, useMemo, useState } from 'react'

const API = import.meta.env.VITE_API_BASE || 'http://localhost:8080'

export default function App() {
  const [q, setQ] = useState('')
  const [items, setItems] = useState([])
  const [facets, setFacets] = useState({})
  const [suggestions, setSuggestions] = useState([])

  // simple debounce
  const debouncedQ = useMemo(() => q, [q])
  useEffect(() => {
    const t = setTimeout(async () => {
      if (!debouncedQ) { setSuggestions([]); return }
      const res = await fetch(`${API}/api/search/suggest?q=${encodeURIComponent(debouncedQ)}`)
      setSuggestions(await res.json())
    }, 200)
    return () => clearTimeout(t)
  }, [debouncedQ])

  async function doSearch(params = {}) {
    const url = new URL(`${API}/api/search/fulltext`)
    url.searchParams.set('q', q || '')
    Object.entries(params).forEach(([k,v]) => v!=null && url.searchParams.set(k, v))
    const res = await fetch(url)
    const data = await res.json()
    setItems(data.items || [])
    setFacets(data.facets || {})
  }

  return (
    <div style={{fontFamily:'system-ui', maxWidth:960, margin:'32px auto'}}>
      <h1>Buscador DWFS</h1>
      <input
        placeholder="Buscar productos…"
        value={q}
        onChange={e=>setQ(e.target.value)}
        style={{padding:'8px 12px', width:'100%', border:'1px solid #ccc', borderRadius:8}}
      />
      {suggestions.length>0 && (
        <div style={{border:'1px solid #eee', marginTop:8, borderRadius:8, padding:8}}>
          <strong>Sugerencias</strong>
          <div style={{display:'flex', gap:8, flexWrap:'wrap', marginTop:6}}>
            {suggestions.map(s => (
              <button key={s} onClick={()=>{ setQ(s); doSearch(); }}
                style={{border:'1px solid #ddd', padding:'4px 8px', borderRadius:16, background:'#fafafa'}}>
                {s}
              </button>
            ))}
          </div>
        </div>
      )}

      <div style={{display:'grid', gridTemplateColumns:'240px 1fr', gap:16, marginTop:16}}>
        <aside>
          <button onClick={()=>doSearch()} style={{padding:'8px 12px'}}>Buscar</button>
          <h3>Facetas</h3>
          <pre style={{whiteSpace:'pre-wrap', background:'#f9f9f9', padding:8, borderRadius:8}}>
            {JSON.stringify(facets, null, 2)}
          </pre>
        </aside>
        <main>
          <h3>Resultados</h3>
          {items.map(p => (
            <div key={p.id} style={{border:'1px solid #eee', padding:12, borderRadius:12, marginBottom:8}}>
              <strong>{p.name}</strong><br/>
              <small>{p.brand} · {p.category} · ${p.price}</small>
              <p>{p.description}</p>
            </div>
          ))}
        </main>
      </div>
    </div>
  )
}
