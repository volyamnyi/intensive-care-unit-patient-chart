import '@testing-library/jest-dom';

vi.mock('react-i18next', () => {
  const uk: Record<string, unknown> = {
    common: { loading: 'Завантаження...', noData: 'Немає даних', cancel: 'Скасувати', save: 'Зберегти', back: 'Назад', search: 'Пошук', logout: 'Вийти', userMenu: 'Меню користувача', appTitle: 'ВАІТ', appSubtitle: 'Карта інтенсивної терапії', themeToggle: 'Переключити тему', noResults: 'Немає результатів', create: 'Створити', add: 'Додати', open: 'Відкрити' },
    login: { title: 'ВАІТ — Вхід', heading: 'Вхід до системи', username: 'Логін', password: 'Пароль', submit: 'Увійти', error: 'Невірний логін або пароль', superhumansAlt: 'Superhumans' },
    doctor: {
      dashboard: { title: 'ВАІТ — Лікар', heading: 'Активні пацієнти', subtitle: 'Відділення анестезіології та інтенсивної терапії', newCard: 'Нова карта', searchPlaceholder: 'Пошук пацієнта за ПІБ...', noResults: 'Немає пацієнтів за запитом', empty: 'Немає активних пацієнтів' },
      createCard: { title: 'ВАІТ — Нова карта', heading: 'Нова карта інтенсивної терапії', error: 'Помилка створення карти', patientSearch: 'Пошук пацієнта', patientData: 'Дані пацієнта (з МІС)', fullName: 'ПІП', birthDate: 'Дата народження', sex: 'Стать', male: 'Чол', female: 'Жін', height: 'Зріст (см)', weight: 'Маса (кг)', bloodGroup: 'Група крові', rhFactor: 'Rezus', medicalCardNumber: '№ медкарти', createButton: 'Створити карту', cancelButton: 'Скасувати' },
      patientDay: { title: 'ВАІТ — Пацієнт', episodeNotFound: 'Епізод не знайдено', patientFallback: 'Пацієнт', dayPrefix: 'Доба №', dayLabel: 'Доба №{{dayNumber}}', noOpenDay: 'Немає відкритої доби', backButton: 'Назад', signOffButton: 'Підписати добу', reopenButton: 'Перевідкрити', statusOpen: 'Відкрита', statusNurseSigned: 'Підписана медсестрою', statusDoctorSigned: 'Підписана', statusReopened: 'Перевідкрита', statusClosed: 'Закрита', tabs: { vitals: 'Вітальні', orders: 'Призначення', scales: 'Шкали', notes: 'Нотатки', balance: 'Баланс' }, vitalSignsTitle: 'Показники — {{hour}}:00', reopenDialog: { title: 'Перевідкрити добу №{{dayNumber}}', text: 'Ви впевнені, що хочете перевідкрити цю добу? Підписи будуть скасовані.', reasonLabel: 'Причина перевідкриття', cancelButton: 'Скасувати', reopenButton: 'Перевідкрити' }, episodeChipPrefix: '№ {{id}}' },
      layout: { patientsLink: 'Пацієнти', roleHod: 'Завідувач відділення', roleDoctor: 'Лікар' },
    },
    nurse: { dashboard: { title: 'ВАІТ — Медсестра', heading: 'Активні пацієнти', searchPlaceholder: 'Пошук пацієнта за ПІБ...', noResults: 'Немає пацієнтів за запитом', empty: 'Немає активних пацієнтів' }, layout: { role: 'Медсестра' } },
    admin: { title: 'ВАІТ — Адміністратор', heading: 'Користувачі системи', roleDoctor: 'Лікар', roleNurse: 'Медсестра', roleHod: 'Завідувач відділення', roleAdmin: 'Адміністратор', tableHeaders: { fullName: 'ПІБ', login: 'Логін', role: 'Роль', email: 'Email' }, noData: 'Немає даних', sectionDoctors: 'Лікарі', sectionNurses: 'Медсестри' },
    signDialog: { title: 'Підписання доби №{{dayNumber}}', text: 'Після підписання доба стане read-only для {{role}}.', roleNurse: 'медсестри', roleDoctor: 'лікаря', nurseInfo: 'Буде збережено підпис медсестри. Після підписання лікарем буде згенеровано PDF.', doctorInfo: 'Буде згенеровано PDF та відправлено в МІС.', cancelButton: 'Скасувати', signingButton: 'Підписання...', signButton: 'Підписати' },
    vitalSigns: { systolicBP: 'АТ сист (мм.рт.ст)', diastolicBP: 'АТ діас (мм.рт.ст)', heartRate: 'ЧСС (в 1 хв)', spo2: 'SpO2 (%)', temperature: 'Темп. тіла (°С)', cvp: 'ЦВТ (мм.вод.ст)', respiratoryRate: 'ЧД (в 1 хв)', consciousness: 'Свідомість', etco2: 'etCO2 (мм.рт.ст)', fio2: 'FiO2 (%)', urineOutput: 'Діурез (мл/год)', drainOutput: 'Дренаж (мл)', painScore: 'Біль (0-10)', notes: 'Нотатки', validationWarning: '{{label}}: значення виходить за межі норми ({{min}}-{{max}}{{unit}})', saveButton: 'Зберегти показники', savingButton: 'Збереження...' },
    patientSearch: { defaultLabel: 'ПІБ, телефон або № медкарти', minChars: 'Введіть мінімум 2 символи', notFound: 'Пацієнтів не знайдено' },
    hourlyRecordTable: { columns: { hour: 'Година', systolicBP: 'АТ сист', diastolicBP: 'АТ діас', heartRate: 'ЧСС', spo2: 'SpO2', temperature: 'Темп', cvp: 'ЦВТ', respiratoryRate: 'ЧД' }, emptyValue: '-' },
    medicalOrders: { statusDraft: 'Чернетка', statusActive: 'Активне', statusCompleted: 'Виконано', statusCancelled: 'Скасовано', formTitle: 'Нове призначення', category: 'Категорія', drugName: 'Препарат', dose: 'Доза', unit: 'Од.', route: 'Шлях', frequency: 'Частота', startTime: 'Початок', endTime: 'Кінець', createButton: 'Створити', cancelButton: 'Скасувати', newOrderButton: '+ Нове призначення', tableHeaders: { drugName: 'Препарат', dose: 'Доза', route: 'Шлях', status: 'Статус', actions: 'Дії' }, empty: 'Немає призначень', cancelOrderButton: 'Скасувати' },
    medicalNotes: { newNoteLabel: 'Нова нотатка', addNoteButton: 'Додати нотатку', empty: 'Немає нотаток' },
    scaleResults: { scaleLabel: 'Шкала', resultLabel: 'Результат', addButton: 'Додати', empty: 'Немає даних шкал', resultText: 'Результат: {{result}} ({{date}})', notFilled: 'Не заповнено' },
    fluidBalance: { title: 'Баланс рідини', intake: 'Надійшло:', output: 'Виділено:', dailyBalance: 'Добовий баланс:', cumulativeBalance: 'Кумулятивний баланс:', unit: 'мл', recalculateButton: 'Перерахувати', calculatingButton: 'Розрахунок...' },
    episodeTable: { statusDraft: 'Чернетка', statusActive: 'Активний', statusCompleted: 'Завершений', statusArchived: 'Архівний', loading: 'Завантаження...', empty: 'Немає епізодів', tableHeaders: { patient: 'Пацієнт', admissionDate: 'Дата госпіталізації', dischargeDate: 'Дата виписки', status: 'Статус', actions: 'Дії' }, openAction: 'Відкрити', dischargeDateEmpty: '-' },
    clinicalDayTimeline: { empty: 'Немає клінічних днів', dayLabel: 'Доба {{dayNumber}}' },
    auditLog: { loading: 'Завантаження...', empty: 'Немає записів аудиту', tableHeaders: { timestamp: 'Час', user: 'Користувач', entity: 'Сутність', action: 'Дія', changes: 'Зміни' } },
    clinicalRanges: { temperature: 'Температура', heartRate: 'ЧСС', respiratoryRate: 'ЧД', systolicBP: 'АТ сист.', diastolicBP: 'АТ діаст.', spo2: 'SpO2', units: { celsius: '°C', bpm: 'уд/хв', perMin: '/хв', mmhg: 'мм рт.ст.', percent: '%' } },
  };

  function t(key: string, params?: Record<string, unknown>): string {
    const value = key.split('.').reduce((obj: unknown, k: string) => {
      if (obj && typeof obj === 'object') return (obj as Record<string, unknown>)[k];
      return undefined;
    }, uk);
    if (typeof value !== 'string') return key;
    if (!params) return value;
    return value.replace(/\{\{(\w+)\}\}/g, (_: string, p: string) => String(params[p] ?? `{{${p}}}`));
  }

  const initReactI18next = { type: '3rdParty' as const, init: () => {} };

  return {
    useTranslation: () => ({ t, i18n: { language: 'uk', changeLanguage: () => {} } }),
    initReactI18next,
    Trans: ({ i18nKey, children }: { i18nKey?: string; children?: React.ReactNode }) => children ?? null,
    I18nextProvider: ({ children }: { children?: React.ReactNode }) => children,
  };
});
