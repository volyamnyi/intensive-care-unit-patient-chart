import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { Search, Loader2 } from 'lucide-react'
import { Input } from '@/components/ui/input'
import { Alert } from '@/components/ui/alert'
import { episodeApi } from '../../api/icu';
import EpisodeTable from '../../components/common/EpisodeTable'
import type { Episode } from '../../types/icu';

export default function NurseDashboardPage() {
  const navigate = useNavigate()
  const [episodes, setEpisodes] = useState<Episode[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')

  useEffect(() => {
    episodeApi.search({ status: 'ACTIVE' })
      .then((res) => setEpisodes(res.data))
      .catch(() => setEpisodes([]))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => { document.title = 'ВАІТ — Медсестра' }, [])

  const filteredEpisodes = episodes.filter((ep) =>
    (ep.patientName ?? '').toLowerCase().includes(search.toLowerCase())
  )

  if (loading) return <Loader2 role="progressbar" aria-label="Loading" className="mx-auto mt-4 size-6 animate-spin text-primary" />

  return (
    <div>
      <h1 className="font-rubik text-2xl font-extrabold text-foreground mb-3">
        Активні пацієнти
      </h1>
      <div className="relative mb-2">
        <Search className="pointer-events-none absolute top-1/2 left-2.5 size-4 -translate-y-1/2 text-muted-foreground" />
        <Input
          placeholder="Пошук пацієнта за ПІБ..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="pl-8"
        />
      </div>
      {filteredEpisodes.length === 0 && !loading ? (
        <Alert>
          {search ? 'Немає пацієнтів за запитом' : 'Немає активних пацієнтів'}
        </Alert>
      ) : (
        <EpisodeTable
          episodes={filteredEpisodes}
          onSelect={(ep) => navigate('/icu/nurse/episode/' + ep.id)}
        />
      )}
    </div>
  )
}
