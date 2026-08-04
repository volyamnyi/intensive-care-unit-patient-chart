import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search, ChevronLeft, ChevronRight } from 'lucide-react';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardTitle } from '@/components/ui/card';
import { useProsthetics } from '@/prosthetics/ProstheticsContext';

export default function PatientStep() {
  const navigate = useNavigate();
  const { draft, setDraftField, patient } = useProsthetics();
  const [query, setQuery] = useState('');
  const [searchResults, setSearchResults] = useState<
    { id: string; fullName: string; birthDate: string }[]
  >([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    document.title = 'Вибір пацієнта — Виробництво протезів';
  }, []);

  const handleSearch = async () => {
    if (!query.trim()) return;
    setLoading(true);
    try {
      const res = await fetch(`/api/prosthesis-manufacturing/patients?query=${encodeURIComponent(query)}`, {
        headers: {
          Authorization: `Bearer ${localStorage.getItem('token')}`,
        },
      });
      if (res.ok) {
        const data = await res.json();
        setSearchResults(data);
      }
    } catch {
      // ignore
    } finally {
      setLoading(false);
    }
  };

  const handleSelect = (patientId: string) => {
    setDraftField('patientId', patientId);
    navigate('/prosthetics/new/order');
  };

  return (
    <div className="container mx-auto max-w-2xl py-8">
      <div className="mb-6 flex items-center gap-3">
        <Button
          variant="ghost"
          size="sm"
          onClick={() => navigate('/prosthetics')}
        >
          <ChevronLeft className="size-4" />
          Назад
        </Button>
        <h1 className="font-display text-2xl font-bold">Вибір пацієнта</h1>
      </div>

      <div className="mb-4 flex gap-2">
        <Input
          placeholder="Пошук пацієнта за ПІБ або ідентифікатором..."
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
        />
        <Button onClick={handleSearch} disabled={loading || !query.trim()}>
          <Search className="size-4" />
          Шукати
        </Button>
      </div>

      {patient && (
        <Card className="mb-4 border-mint/30 bg-mint/5">
          <CardContent className="pt-4">
            <CardTitle className="text-base">
              Обраний пацієнт: {patient.fullName}
            </CardTitle>
            <p className="text-sm text-muted-foreground">
              Дата народження: {patient.birthDate}
            </p>
          </CardContent>
        </Card>
      )}

      <div className="space-y-2">
          {searchResults.map((p) => (
            <Card
              key={p.id}
              className="cursor-pointer transition-colors hover:bg-accent"
              onClick={() => handleSelect(p.id)}
            >
            <CardContent className="pt-4">
              <CardTitle className="text-base">{p.fullName}</CardTitle>
              <p className="text-sm text-muted-foreground">
                Дата народження: {p.birthDate}
              </p>
            </CardContent>
          </Card>
        ))}
      </div>

      <div className="mt-6 flex justify-end">
        <Button
          onClick={() => navigate('/prosthetics/new/order')}
          disabled={!draft.patientId}
          className="gap-2"
        >
          Продовжити
          <ChevronRight className="size-4" />
        </Button>
      </div>
    </div>
  );
}
