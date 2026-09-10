import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ChevronLeft } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { Alert, AlertTitle, AlertDescription } from '@/components/ui/alert';
import { Table, TableHeader, TableBody, TableHead, TableRow, TableCell } from '@/components/ui/table';
import { useProsthetics } from '@/prosthetics/ProstheticsContext';
import { prostheticsOrderApi, prostheticsPatientApi } from '@/api/prosthetics';
import { getErrorMessage } from '@/utils/errorMessage';
import type { MisOrderDocument, ProstheticsCandidateDocument } from '@/prosthetics/types';
import { SetupSteps } from '@/components/prosthetics/SetupSteps';

function formatDocDate(raw: string | undefined): string {
  if (!raw) return '—';
  const d = new Date(raw);
  return isNaN(d.getTime()) ? '—' : d.toLocaleDateString('uk-UA');
}

export default function OrderSelectPage() {
  const navigate = useNavigate();
  const { draft, setDraftField } = useProsthetics();
  const [documents, setDocuments] = useState<ProstheticsCandidateDocument[]>([]);
  const [documentsUnknown, setDocumentsUnknown] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [misDocs, setMisDocs] = useState<MisOrderDocument[]>([]);
  const [misLoading, setMisLoading] = useState(false);
  const [misError, setMisError] = useState<string | null>(null);
  const [provisioningUrl, setProvisioningUrl] = useState<string | null>(null);

  useEffect(() => {
    document.title = 'Вибір замовлення — Виробництво протезів';
  }, []);

  useEffect(() => {
    if (!draft.patientId) {
      navigate('/prosthetics/new/select-patient');
      return;
    }
    // MIS document badges (Phase 9, #262): best-effort enrichment from the
    // candidates worklist — the order list stays usable without it.
    const fetchDocuments = async () => {
      try {
        const candRes = await prostheticsPatientApi.listCandidates();
        const mine = candRes.data.find((c) => c.patient.id === draft.patientId);
        if (mine) {
          setDocuments(mine.documents);
          setDocumentsUnknown(mine.documentsUnknown);
        }
      } catch {
        // ignore — badges are decorative
      }
    };
    // Order selection is MIS-driven only: every limb-prosthesis document
    // (templates 120/121, 404 URLs excluded backend-side) is selectable,
    // regardless of local rows.
    const fetchMisDocs = async () => {
      setMisLoading(true);
      setMisError(null);
      try {
        const res = await prostheticsOrderApi.listMisDocuments(draft.patientId!);
        setMisDocs(res.data);
      } catch {
        setMisError('Не вдалося завантажити замовлення MIS');
        setMisDocs([]);
      } finally {
        setMisLoading(false);
      }
    };
    fetchDocuments();
    fetchMisDocs();
  }, [draft.patientId, navigate]);

  // Picking an MIS document provisions (find-or-create) the local order the
  // rest of the flow runs on — selection never depends on pre-existing
  // local rows.
  const handleSelectMisDoc = async (doc: MisOrderDocument) => {
    if (!doc.documentUrl || doc.documentId == null || provisioningUrl) return;
    setProvisioningUrl(doc.documentUrl);
    setError(null);
    try {
      const res = await prostheticsOrderApi.provisionFromMis({
        patientId: draft.patientId!,
        documentId: doc.documentId,
      });
      setDraftField('orderId', res.data.id);
      setDraftField('templateId', null);
      setDraftField('misDocumentUrl', doc.documentUrl);
      setDraftField('misDocumentId', String(doc.documentId));
      setDraftField('misDocumentTemplateName', doc.documentTemplateName ?? null);
      navigate('/prosthetics/new/review-order');
    } catch (err: unknown) {
      setError(getErrorMessage(err, 'Не вдалося підготувати замовлення'));
    } finally {
      setProvisioningUrl(null);
    }
  };

  return (
    <div className="container mx-auto max-w-2xl py-8">
      <div className="mb-4 flex flex-wrap items-center gap-3">
        <Button variant="ghost" size="sm" onClick={() => navigate('/prosthetics/new/select-patient')}>
          <ChevronLeft className="size-4" />
          Назад
        </Button>
        <div>
          <h1 className="font-display text-2xl font-bold">Вибір замовлення</h1>
          <p className="text-sm text-muted-foreground">Крок 2 з 4</p>
        </div>
        <SetupSteps current={2} className="ml-auto" />
      </div>

      <Card className="mb-4">
        <CardHeader>
          <CardTitle className="text-base">Пацієнт</CardTitle>
        </CardHeader>
        <CardContent className="grid gap-4 text-sm sm:grid-cols-4">
          <div>
            <div className="text-xs text-muted-foreground">ID пацієнта</div>
            <div className="font-medium">{draft.patientId}</div>
          </div>
        </CardContent>
      </Card>

      {error && (
        <Alert variant="destructive" className="mb-4">
          <AlertTitle>Помилка</AlertTitle>
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      {documents.length > 0 && (
        <div className="mb-4 flex flex-wrap items-center gap-1.5" data-testid="mis-documents">
          <span className="text-xs text-muted-foreground">Документи MIS:</span>
          {documents.map((d) => (
            <Badge key={d.documentId ?? d.documentTemplateId} variant="secondary">
              {d.documentTemplateName ?? `Шаблон ${d.documentTemplateId}`}
            </Badge>
          ))}
        </div>
      )}
      {documentsUnknown && (
        <p className="mb-4 text-xs text-muted-foreground">
          Документи MIS недоступні — показано локальні замовлення.
        </p>
      )}

      <Card className="mb-4">
        <CardHeader>
          <CardTitle className="text-base">Замовлення на протези (MIS)</CardTitle>
        </CardHeader>
        <CardContent>
          {misLoading ? (
            <div className="space-y-2">
              <Skeleton className="h-12 w-full" />
              <Skeleton className="h-12 w-full" />
            </div>
          ) : misError ? (
            <Alert variant="destructive">
              <AlertTitle>Помилка</AlertTitle>
              <AlertDescription>{misError}</AlertDescription>
            </Alert>
          ) : misDocs.length === 0 ? (
            <p className="text-sm text-muted-foreground">Замовлень на протези в MIS не знайдено.</p>
          ) : (
            <Table data-testid="mis-order-documents">
              <TableHeader>
                <TableRow>
                  <TableHead>Документ</TableHead>
                  <TableHead>Дата</TableHead>
                  <TableHead>Виріб</TableHead>
                  <TableHead className="text-right">Дія</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {misDocs.map((doc) => (
                  <TableRow key={`${doc.documentTemplateId}-${doc.documentId}-${doc.documentUrl}`}>
                    <TableCell className="font-medium">
                      {doc.documentTemplateName ?? `Шаблон ${doc.documentTemplateId}`}
                    </TableCell>
                    <TableCell className="whitespace-nowrap">
                      {formatDocDate(doc.documentCreationDate ?? doc.orderDate)}
                    </TableCell>
                    <TableCell>{doc.productName || '—'}</TableCell>
                    <TableCell className="text-right">
                      <Button
                        size="sm"
                        variant={draft.misDocumentUrl === doc.documentUrl ? 'default' : 'outline'}
                        disabled={!doc.documentUrl || doc.documentId == null || provisioningUrl != null}
                        onClick={() => void handleSelectMisDoc(doc)}
                      >
                        {provisioningUrl === doc.documentUrl
                          ? 'Підготовка…'
                          : draft.misDocumentUrl === doc.documentUrl
                            ? 'Обрано'
                            : 'Обрати'}
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      <div className="sticky bottom-0 z-10 -mx-4 mt-4 flex flex-col gap-3 border-t bg-background/95 px-4 py-3 pb-[max(0.75rem,env(safe-area-inset-bottom))] backdrop-blur sm:-mx-6 sm:flex-row sm:items-center sm:justify-between sm:px-6 sm:pb-3">
        <Button variant="outline" className="w-full sm:w-auto" onClick={() => navigate('/prosthetics/new/select-patient')}>
          Назад
        </Button>
        <Button
          disabled={!draft.orderId && !draft.misDocumentUrl}
          className="w-full bg-accent text-accent-foreground hover:bg-accent/90 sm:w-auto"
          onClick={() => navigate('/prosthetics/new/review-order')}
        >
          Далі
        </Button>
      </div>
    </div>
  );
}