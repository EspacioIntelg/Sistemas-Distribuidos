import { useCallback, useEffect, useMemo, useState } from 'react'
import { API_BASE_URL } from './api/config'
import './App.css'

async function request(path, options) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  })

  if (!response.ok) {
    const message = await response.text()
    throw new Error(message || `Error HTTP ${response.status}`)
  }

  if (response.status === 204) return null
  return response.json()
}

function describeError(error) {
  try {
    const parsed = JSON.parse(error.message)
    return parsed.message || error.message
  } catch {
    return error.message
  }
}

function App() {
  const [page, setPage] = useState('libros')
  const [books, setBooks] = useState([])
  const [loans, setLoans] = useState([])
  const [selectedBook, setSelectedBook] = useState(null)
  const [filters, setFilters] = useState({ titulo: '', autor: '', anio: '', disponible: '' })
  const [bookForm, setBookForm] = useState({
    titulo: '',
    autor: '',
    anioPublicacion: '',
    isbn: '',
    sinopsis: '',
  })
  const [loanForm, setLoanForm] = useState({ libroId: '', usuario: '' })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  const loadBooks = useCallback(async (searchFilters) => {
    const params = new URLSearchParams()
    Object.entries(searchFilters).forEach(([key, value]) => {
      if (value !== '') params.set(key, value)
    })
    const query = params.size ? `?${params.toString()}` : ''
    setBooks(await request(`/libros${query}`))
  }, [])

  const loadLoans = useCallback(async () => {
    setLoans(await request('/prestamos'))
  }, [])

  const refreshData = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      await Promise.all([loadBooks(filters), loadLoans()])
    } catch (loadError) {
      setError(describeError(loadError))
    } finally {
      setLoading(false)
    }
  }, [filters, loadBooks, loadLoans])

  useEffect(() => {
    let active = true
    setLoading(true)
    Promise.all([request('/libros'), request('/prestamos')])
      .then(([bookData, loanData]) => {
        if (active) {
          setBooks(bookData)
          setLoans(loanData)
        }
      })
      .catch((loadError) => {
        if (active) setError(describeError(loadError))
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [])

  const availableBooks = useMemo(() => books.filter((book) => book.disponible), [books])
  const activeLoans = useMemo(() => loans.filter((loan) => loan.estado === 'ACTIVO'), [loans])

  async function submitSearch(event) {
    event.preventDefault()
    setLoading(true)
    setError('')
    try {
      await loadBooks(filters)
      setSelectedBook(null)
    } catch (loadError) {
      setError(describeError(loadError))
    } finally {
      setLoading(false)
    }
  }

  async function submitBook(event) {
    event.preventDefault()
    setLoading(true)
    setError('')
    setNotice('')
    try {
      const payload = {
        ...bookForm,
        anioPublicacion: bookForm.anioPublicacion ? Number(bookForm.anioPublicacion) : null,
        disponible: true,
      }
      await request('/libros', { method: 'POST', body: JSON.stringify(payload) })
      setBookForm({ titulo: '', autor: '', anioPublicacion: '', isbn: '', sinopsis: '' })
      await loadBooks(filters)
      setNotice('Libro añadido al catálogo.')
    } catch (submitError) {
      setError(describeError(submitError))
    } finally {
      setLoading(false)
    }
  }

  async function submitLoan(event) {
    event.preventDefault()
    setLoading(true)
    setError('')
    setNotice('')
    try {
      await request('/prestamos', {
        method: 'POST',
        body: JSON.stringify({ ...loanForm, libroId: Number(loanForm.libroId) }),
      })
      setLoanForm({ libroId: '', usuario: '' })
      await Promise.all([loadBooks(filters), loadLoans()])
      setNotice('Préstamo registrado.')
    } catch (submitError) {
      setError(describeError(submitError))
    } finally {
      setLoading(false)
    }
  }

  async function returnLoan(loanId) {
    setLoading(true)
    setError('')
    setNotice('')
    try {
      await request(`/prestamos/${loanId}`, { method: 'PUT' })
      await Promise.all([loadBooks(filters), loadLoans()])
      setNotice('Libro devuelto correctamente.')
    } catch (returnError) {
      setError(describeError(returnError))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <a className="brand" href="/" onClick={(event) => event.preventDefault()}>
          <span className="brand-mark" aria-hidden="true">B</span>
          <span>Biblioteca<span className="brand-light">Online</span></span>
        </a>
        <nav className="navigation" aria-label="Navegación principal">
          <button className={page === 'libros' ? 'nav-link active' : 'nav-link'} onClick={() => setPage('libros')}>
            Catálogo
          </button>
          <button className={page === 'prestamos' ? 'nav-link active' : 'nav-link'} onClick={() => setPage('prestamos')}>
            Préstamos
            {activeLoans.length > 0 && <span className="nav-count">{activeLoans.length}</span>}
          </button>
        </nav>
      </header>

      <main className="page-content">
        <section className="page-heading">
          <div>
            <p className="eyebrow">LECTURA PARA TODOS</p>
            <h1>{page === 'libros' ? 'Explora el catálogo' : 'Gestión de préstamos'}</h1>
            <p className="subtitle">
              {page === 'libros'
                ? 'Encuentra tu próxima historia entre los libros disponibles.'
                : 'Registra préstamos y mantén el estado de cada libro actualizado.'}
            </p>
          </div>
          <div className="heading-stat">
            <span>{page === 'libros' ? books.length : activeLoans.length}</span>
            <small>{page === 'libros' ? 'libros mostrados' : 'préstamos activos'}</small>
          </div>
        </section>

        {error && <div className="alert error-alert" role="alert">{error}</div>}
        {notice && <div className="alert success-alert" role="status">{notice}</div>}

        {page === 'libros' ? (
          <>
            <form className="search-panel" onSubmit={submitSearch}>
              <label>
                Título
                <input value={filters.titulo} onChange={(event) => setFilters({ ...filters, titulo: event.target.value })} placeholder="Ej. Cien años de soledad" />
              </label>
              <label>
                Autor
                <input value={filters.autor} onChange={(event) => setFilters({ ...filters, autor: event.target.value })} placeholder="Nombre del autor" />
              </label>
              <label className="year-filter">
                Año
                <input type="number" min="0" value={filters.anio} onChange={(event) => setFilters({ ...filters, anio: event.target.value })} placeholder="Cualquiera" />
              </label>
              <label>
                Disponibilidad
                <select value={filters.disponible} onChange={(event) => setFilters({ ...filters, disponible: event.target.value })}>
                  <option value="">Todos</option>
                  <option value="true">Disponible</option>
                  <option value="false">Prestado</option>
                </select>
              </label>
              <button className="button primary search-button" type="submit" disabled={loading}>Buscar</button>
            </form>

            <div className="catalog-layout">
              <section className="book-section" aria-label="Libros del catálogo">
                {loading && books.length === 0 ? (
                  <div className="empty-state">Cargando catálogo…</div>
                ) : books.length ? (
                  <div className="book-grid">
                    {books.map((book) => (
                      <article className={`book-card ${selectedBook?.id === book.id ? 'selected' : ''}`} key={book.id}>
                        <button className="book-card-button" onClick={() => setSelectedBook(book)} aria-label={`Ver detalles de ${book.titulo}`}>
                          <span className="book-cover" aria-hidden="true"><span>LIBRO</span><strong>{book.titulo?.slice(0, 1)}</strong></span>
                          <span className="book-card-content">
                            <span className={`availability ${book.disponible ? 'available' : 'unavailable'}`}>
                              <span aria-hidden="true" />{book.disponible ? 'Disponible' : 'Prestado'}
                            </span>
                            <strong className="book-title">{book.titulo}</strong>
                            <span className="book-author">{book.autor}</span>
                            <span className="book-year">{book.anioPublicacion || 'Año no especificado'}</span>
                          </span>
                        </button>
                      </article>
                    ))}
                  </div>
                ) : (
                  <div className="empty-state">
                    <strong>No encontramos libros</strong>
                    <span>Prueba a cambiar o limpiar los filtros de búsqueda.</span>
                  </div>
                )}
              </section>

              <aside className="side-panel">
                {selectedBook ? (
                  <div className="detail-card">
                    <div className="detail-topline">
                      <p className="eyebrow">DETALLE DEL LIBRO</p>
                      <button className="icon-button" onClick={() => setSelectedBook(null)} aria-label="Cerrar detalle">×</button>
                    </div>
                    <h2>{selectedBook.titulo}</h2>
                    <p className="detail-author">por {selectedBook.autor}</p>
                    <dl className="book-metadata">
                      <div><dt>Año</dt><dd>{selectedBook.anioPublicacion || 'No especificado'}</dd></div>
                      <div><dt>ISBN</dt><dd>{selectedBook.isbn || 'No especificado'}</dd></div>
                    </dl>
                    <p className="synopsis">{selectedBook.sinopsis || 'Este libro aún no tiene sinopsis.'}</p>
                    <span className={`availability ${selectedBook.disponible ? 'available' : 'unavailable'}`}>
                      <span aria-hidden="true" />{selectedBook.disponible ? 'Disponible para préstamo' : 'Actualmente prestado'}
                    </span>
                  </div>
                ) : (
                  <div className="detail-placeholder">
                    <span className="placeholder-icon" aria-hidden="true">▤</span>
                    <strong>Tu próxima lectura</strong>
                    <p>Selecciona un libro para consultar sus detalles.</p>
                  </div>
                )}

                <details className="add-book">
                  <summary>Añadir un libro</summary>
                  <form className="stacked-form" onSubmit={submitBook}>
                    <label>Título<input required value={bookForm.titulo} onChange={(event) => setBookForm({ ...bookForm, titulo: event.target.value })} /></label>
                    <label>Autor<input required value={bookForm.autor} onChange={(event) => setBookForm({ ...bookForm, autor: event.target.value })} /></label>
                    <label>Año de publicación<input type="number" min="0" value={bookForm.anioPublicacion} onChange={(event) => setBookForm({ ...bookForm, anioPublicacion: event.target.value })} /></label>
                    <label>ISBN<input value={bookForm.isbn} onChange={(event) => setBookForm({ ...bookForm, isbn: event.target.value })} /></label>
                    <label>Sinopsis<textarea rows="3" value={bookForm.sinopsis} onChange={(event) => setBookForm({ ...bookForm, sinopsis: event.target.value })} /></label>
                    <button className="button primary" disabled={loading}>Guardar libro</button>
                  </form>
                </details>
              </aside>
            </div>
          </>
        ) : (
          <div className="loans-layout">
            <section className="panel loan-form-panel">
              <p className="eyebrow">NUEVO MOVIMIENTO</p>
              <h2>Registrar préstamo</h2>
              <p className="panel-description">El libro se marcará como no disponible hasta que se registre su devolución.</p>
              <form className="stacked-form" onSubmit={submitLoan}>
                <label>
                  Libro disponible
                  <select required value={loanForm.libroId} onChange={(event) => setLoanForm({ ...loanForm, libroId: event.target.value })}>
                    <option value="">Selecciona un libro</option>
                    {availableBooks.map((book) => <option key={book.id} value={book.id}>{book.titulo} — {book.autor}</option>)}
                  </select>
                </label>
                <label>Nombre del usuario<input required value={loanForm.usuario} onChange={(event) => setLoanForm({ ...loanForm, usuario: event.target.value })} placeholder="Nombre y apellidos" /></label>
                <button className="button primary" disabled={loading || !availableBooks.length}>Crear préstamo</button>
                {!availableBooks.length && <small className="form-hint">No hay libros disponibles en el catálogo actual.</small>}
              </form>
            </section>

            <section className="panel loan-list-panel">
              <div className="list-heading">
                <div><p className="eyebrow">HISTORIAL</p><h2>Todos los préstamos</h2></div>
                <button className="button secondary" onClick={refreshData} disabled={loading}>Actualizar</button>
              </div>
              {loans.length ? (
                <div className="loan-list">
                  {loans.slice().sort((a, b) => b.id - a.id).map((loan) => (
                    <article className="loan-row" key={loan.id}>
                      <div className="loan-icon" aria-hidden="true">▤</div>
                      <div className="loan-info">
                        <strong>{loan.tituloLibro || `Libro #${loan.libroId}`}</strong>
                        <span>{loan.usuario} · Préstamo #{loan.id}</span>
                      </div>
                      <div className="loan-dates">
                        <span>Desde {loan.fechaPrestamo || '—'}</span>
                        {loan.fechaDevolucion && <span>Devuelto {loan.fechaDevolucion}</span>}
                      </div>
                      <span className={`status-pill ${loan.estado === 'ACTIVO' ? 'status-active' : 'status-returned'}`}>
                        {loan.estado === 'ACTIVO' ? 'Activo' : 'Devuelto'}
                      </span>
                      {loan.estado === 'ACTIVO' && (
                        <button className="button secondary return-button" onClick={() => returnLoan(loan.id)} disabled={loading}>
                          Registrar devolución
                        </button>
                      )}
                    </article>
                  ))}
                </div>
              ) : (
                <div className="empty-state compact-empty">Todavía no hay préstamos registrados.</div>
              )}
            </section>
          </div>
        )}
      </main>
      <footer className="footer">Biblioteca Online <span>·</span> Catálogo y préstamos</footer>
    </div>
  )
}

export default App
