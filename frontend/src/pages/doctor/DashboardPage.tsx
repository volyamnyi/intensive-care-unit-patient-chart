import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { Search, Plus, Loader2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Alert } from '@/components/ui/alert'
import { episodeApi } from '../../api/icu';
import EpisodeTable from '../../components/icu/EpisodeTable'
import type { Episode } from '../../types/icu';

export default function DashboardPage() {
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

  useEffect(() => { document.title = 'ВАІТ — Лікар' }, [])

  const filteredEpisodes = episodes.filter((ep) =>
    (ep.patientName ?? '').toLowerCase().includes(search.toLowerCase())
  )

  if (loading) return <Loader2 role="progressbar" aria-label="Loading" className="mx-auto mt-4 size-6 animate-spin text-primary" />

  return (
    <div>
      <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
        <div>
          <h1 className="font-rubik text-2xl font-extrabold text-foreground">
            Активні пацієнти
          </h1>
          <p className="mt-0.5 text-sm text-muted-foreground">
            Відділення анестезіології та інтенсивної терапії
          </p>
        </div>
        <Button onClick={() => navigate('/icu/doctor/create-card')}>
          <Plus />
          Нова карта
        </Button>
      </div>
      <div className="relative mb-3">
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
          onSelect={(ep) => navigate('/icu/doctor/episode/' + ep.id)}
        />
      )}
    </div>
  )
}
