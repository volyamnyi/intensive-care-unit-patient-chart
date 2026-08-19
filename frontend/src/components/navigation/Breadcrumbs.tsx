import { useLocation, Link as RouterLink } from 'react-router-dom';
import { useMemo } from 'react';

interface BreadcrumbItem {
  label: string;
  to?: string;
}

const ROUTE_BREADCRUMBS: Record<string, BreadcrumbItem[]> = {
  '/icu/doctor': [{ label: 'Пацієнти', to: '/icu/doctor' }],
  '/icu/doctor/department': [
    { label: 'Пацієнти', to: '/icu/doctor' },
    { label: 'Відділення', to: '/icu/doctor/department' },
  ],
  '/icu/doctor/create-card': [
    { label: 'Пацієнти', to: '/icu/doctor' },
    { label: 'Новий пацієнт' },
  ],
  '/icu/doctor/episode': [
    { label: 'Пацієнти', to: '/icu/doctor' },
    { label: 'День' },
  ],
  '/icu/nurse': [{ label: 'Пацієнти', to: '/icu/nurse' }],
  '/icu/nurse/episode': [
    { label: 'Пацієнти', to: '/icu/nurse' },
    { label: 'День' },
  ],
  '/prescriptions/doctor': [{ label: 'Призначення', to: '/prescriptions/doctor' }],
  '/prescriptions/doctor/:id': [
    { label: 'Призначення', to: '/prescriptions/doctor' },
    { label: 'Листок' },
  ],
  '/prescriptions/nurse': [{ label: 'Призначення', to: '/prescriptions/nurse' }],
  '/prescriptions/nurse/:id': [
    { label: 'Призначення', to: '/prescriptions/nurse' },
    { label: 'Листок' },
  ],
  '/admin': [{ label: 'Адмін', to: '/admin' }],
};

function matchBreadcrumbs(pathname: string): BreadcrumbItem[] {
  if (pathname.startsWith('/icu/doctor/episode/')) return ROUTE_BREADCRUMBS['/icu/doctor/episode'];
  if (pathname.startsWith('/icu/nurse/episode/')) return ROUTE_BREADCRUMBS['/icu/nurse/episode'];
  if (pathname.startsWith('/icu/doctor/department')) return ROUTE_BREADCRUMBS['/icu/doctor/department'];
  if (pathname.startsWith('/icu/doctor/create-card')) return ROUTE_BREADCRUMBS['/icu/doctor/create-card'];
  if (pathname.startsWith('/icu/doctor')) return ROUTE_BREADCRUMBS['/icu/doctor'];
  if (pathname.startsWith('/icu/nurse')) return ROUTE_BREADCRUMBS['/icu/nurse'];
  if (pathname.startsWith('/prescriptions/doctor/')) return ROUTE_BREADCRUMBS['/prescriptions/doctor/:id'];
  if (pathname.startsWith('/prescriptions/nurse/')) return ROUTE_BREADCRUMBS['/prescriptions/nurse/:id'];
  if (pathname.startsWith('/prescriptions/doctor')) return ROUTE_BREADCRUMBS['/prescriptions/doctor'];
  if (pathname.startsWith('/prescriptions/nurse')) return ROUTE_BREADCRUMBS['/prescriptions/nurse'];
  if (pathname.startsWith('/admin')) return ROUTE_BREADCRUMBS['/admin'];
  return [];
}

export default function Breadcrumbs() {
  const location = useLocation();

  const items = useMemo(() => matchBreadcrumbs(location.pathname), [location.pathname]);

  if (items.length === 0) return null;

  return (
    <nav aria-label="Breadcrumb" className="flex items-center gap-1 text-xs text-muted-foreground mb-2 overflow-x-auto whitespace-nowrap">
      {items.map((item, idx) => {
        const isLast = idx === items.length - 1;
        return (
          <span key={idx} className="flex items-center gap-1">
            {idx > 0 && <span className="text-muted-foreground/60">/</span>}
            {item.to && !isLast ? (
              <RouterLink to={item.to} className="hover:text-primary transition-colors">
                {item.label}
              </RouterLink>
            ) : (
              <span className={isLast ? 'text-foreground font-medium' : ''} aria-current={isLast ? 'page' : undefined}>
                {item.label}
              </span>
            )}
          </span>
        );
      })}
    </nav>
  );
}
