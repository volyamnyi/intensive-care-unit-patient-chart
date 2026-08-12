import { createFileRoute } from "@tanstack/react-router";
import { Facebook, Instagram, Globe, Mail } from "lucide-react";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "Замовлення на протези верхніх кінцівок № ПВ-26-4748-43" },
      {
        name: "description",
        content:
          "Бланк замовлення на протези верхніх кінцівок Superhumans Center: загальні відомості про особу, причина та рівень порушень кінцівки, стан кукси.",
      },
      { property: "og:title", content: "Замовлення на протези верхніх кінцівок" },
      {
        property: "og:description",
        content:
          "Бланк замовлення БО «БФ «Суперлюди» на протези верхніх кінцівок — частина 1.",
      },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary_large_image" },
    ],
  }),
  component: Index,
});

function Val({ children }: { children?: React.ReactNode }) {
  return <span className="doc-val">{children}</span>;
}

function Index() {
  return (
    <div className="doc-page-wrap">
      <main className="doc-page">
        {/* Logo */}
        <div className="doc-logo">
          <span className="doc-logo-main">superhumans</span>
          <span className="doc-logo-sub">Center</span>
        </div>

        {/* Header table */}
        <table className="doc-head-table">
          <tbody>
            <tr>
              <td className="doc-head-org">
                <b>МЕДИЧНИЙ ЦЕНТР БЛАГОДІЙНОЇ ОРГАНІЗАЦІЇ</b>
                <br />
                <b>"БЛАГОДІЙНИЙ ФОНД "СУПЕРЛЮДИ"</b>
                <br />
                <b>Ліцензія: МОЗ № 926 від 18.05.2023 р.</b>
                <br />
                <b>Медичний центр БО «БФ «СУПЕРЛЮДИ»</b>
                <br />
                <b>79495, Львівська область, Львівський р-н, м.</b>
                <br />
                <b>Винники, вул. Івасюка, буд. 31</b>
                <br />
                <b>Код за ЄДРПОУ: 44803597</b>
              </td>
              <td className="doc-head-logo">
                <div className="doc-logo doc-logo--sm">
                  <span className="doc-logo-main">superhumans</span>
                  <span className="doc-logo-sub">Center</span>
                </div>
              </td>
              <td className="doc-head-approve">
                <div className="doc-approve-title">
                  Замовлення на протези верхніх кінцівок
                </div>
                <div className="doc-approve-bold">ЗАТВЕРДЖЕНО</div>
                <div className="doc-approve-bold">БО «БФ «СУПЕРЛЮДИ»</div>
                <table className="doc-code-table">
                  <tbody>
                    <tr>
                      <td>0</td>
                      <td>2</td>
                      <td>.</td>
                      <td>1</td>
                      <td>2</td>
                      <td>.</td>
                      <td>2</td>
                      <td>0</td>
                      <td>2</td>
                      <td>5</td>
                      <td>р.</td>
                      <td className="doc-code-no">№</td>
                      <td>4</td>
                      <td>2</td>
                    </tr>
                  </tbody>
                </table>
              </td>
            </tr>
          </tbody>
        </table>

        {/* Title */}
        <h1 className="doc-title">
          ЗАМОВЛЕННЯ № ПВ-26- 4748 - 43
        </h1>
        <div className="doc-subtitle">на протези верхніх кінцівок</div>

        <div className="doc-row-between">
          <span>Дата 20.02.2026</span>
          <span>До особової картки особи № 4748</span>
        </div>

        <div className="doc-section-label">Загальні відомості про особу</div>
        <div className="doc-lines">
          <div>
            Прізвище, ім'я, по батькові <Val>Сніжко Іван Петрович</Val>
          </div>
          <div>
            Дата народження <Val>15.03.1987</Val>
          </div>
          <div>
            Паспорт (інший документ, що посвідчує особу): <Val>СЕ956231</Val>
          </div>
          <div>
            Зареєстроване або задекларооване місце проживання (перебування){" "}
            <Val>
              Івано-Франківська область, с. Микитинці, вул.
            </Val>
          </div>
          <div>
            Юність, буд. <Val>46</Val>
          </div>
          <div>
            Контактні телефони <Val>380953312911</Val>
          </div>
          <div>
            Електронна пошта <Val>lisni.jadriando@gmail.com</Val>
          </div>
          <div>
            Стать: <Val>Чоловіча</Val> Зріст <Val>173 см</Val>
          </div>
          <div>
            Вага <Val>83 кг</Val>
          </div>
          <div>
            Соціальний статус: <Val>ветеран</Val>
          </div>
        </div>

        <h2 className="doc-h2">Причина та рівень порушень кінцівки, стан кукси</h2>

        <div className="doc-lines">
          <div>
            Причина ураження: <Val>травма</Val>
          </div>
          <div>
            Дата ампутації <Val>19.01.2025</Val>
          </div>
          <div>
            Уражена кінцівка <Val>Ліва</Val>
          </div>
          <div>Рівень ушкодження:</div>
          <div>
            Вичленення в променезап'ястковому суглобі
          </div>
          <div>
            Довжина кукси: <Val>довга (&gt;2/3)</Val>
          </div>
          <div>
            Стан кукси: Форма: <Val>з кістковими виступами</Val>
          </div>
          <div>
            Стан м'яких тканин та шкіри: <Val>М'які тканини - звичайні Стан шкіри - у нормі</Val>
          </div>
          <div>
            Післяопераційний рубець: <Val>загоєний</Val>
          </div>
          <div>
            Чутливість: <Val>нормальна</Val>
          </div>
          <div>
            Наявність болю: <Val />
          </div>
          <div>
            Об'єм рухів у суглобах ураженої кінцівки: <Val>у нормі</Val>
          </div>
          <div>
            У разі наявності обмежень чи контрактури вказати суглоб і порушення рухомості (згинання / розгинання,
          </div>
          <div>приведення / відведення та інші)</div>
          <div>
            Сила м'язів ураженої верхньої кінцівки: <Val>знижена</Val>
          </div>
          <div>
            У разі наявності зниження сили м'язів вказати певні порушення рухливості
          </div>
          <div className="doc-strong-line">Загальний стан здоров'я:</div>
          <div>
            Стан опорно-рухової системи: <Val />
          </div>
          <div>
            Стан контрлатеральної верхньої кінцівки: <Val>нормальний</Val>
          </div>
          <div>
            Стан тулуба: <Val>нормальний</Val>
          </div>
          <div>
            Стан інших систем організму: <Val>відсутні супутні захворювання</Val>
          </div>
          <div>
            Порушення інших систем організму та/або органів, які можуть вплинути на протезування: <Val>відсутні</Val>
          </div>
          <div className="doc-strong-line">
            Активність і залучення до життєвих ситуацій особи
          </div>
          <div>
            Діяльність особи пов'язана з важкою фізичною працею
          </div>
          <div>
            Залучення до життєвих ситуацій, додаткова діяльність і хобі спорт активне дозвілля (туризм, активні ігри,
          </div>
          <div>
            рибальство тощо) садівництво, городництво, фермерство керування автомобілем
          </div>
          <div>Здатність до самообслуговування</div>
          <div>
            спроможність самостійно митися, доглядати за частинами тіла <Val>Так</Val>
          </div>
          <div>
            спроможність самостійно одягатися <Val>Так</Val>
          </div>
          <div>
            спроможність самостійно вживати їжу, напої <Val>Так</Val>
          </div>
          <div>
            <b>Діагноз по типу конструкції протезу:</b>{" "}
            <Val>Ампутація верхньої лівої кінцівки н/3 передпліччя</Val>
          </div>
        </div>

        <div className="doc-lines doc-lines--gap">
          <div>
            <b>Найменування виробу (засобу реабілітації) та код з згідно ISO 9999:2016, IDT):</b>{" "}
            <Val>06 18 09</Val>
          </div>
          <div>
            <b>06 18 09.B-TR.c - протези передпліччя з тяговим керуванням комбіновані</b>
          </div>
        </div>

        <table className="doc-sign-table">
          <tbody>
            <tr>
              <td className="doc-sign-role">Лікар</td>
              <td className="doc-sign-name">
                <span className="doc-val">Ходирєва І.</span>
                <span className="doc-sign-hint">(Власне ім'я ПРІЗВИЩЕ)</span>
              </td>
              <td className="doc-sign-line">(підпис)_____________</td>
            </tr>
            <tr>
              <td className="doc-sign-role">Технік</td>
              <td className="doc-sign-name">
                <span className="doc-val">Нагорний Д.В</span>
                <span className="doc-sign-hint">(Власне ім'я ПРІЗВИЩЕ)</span>
              </td>
              <td className="doc-sign-line">(підпис)_____________</td>
            </tr>
          </tbody>
        </table>

        <div className="doc-lines doc-lines--gap">
          <div>
            Із призначенням ознайомлений(на) <Val>Сніжко Іван Петрович</Val>
            <span className="doc-sign-inline">(підпис)_____________</span>
          </div>
          <div className="doc-sign-hint doc-sign-hint--indent">
            (Власне ім'я ПРІЗВИЩЕ замовника)
          </div>
        </div>

        <div className="doc-lines doc-lines--gap">
          <div>
            Дата передання виробу у виробництво <Val>20.02.2026</Val>
          </div>
        </div>

        <footer className="doc-footer">
          <div className="doc-footer-col">
            <div className="doc-footer-item">
              <span className="doc-ico">
                <Facebook size={11} />
              </span>
              Superhumans.Center
            </div>
            <div className="doc-footer-item">
              <span className="doc-ico">
                <Instagram size={11} />
              </span>
              superhumans.center
            </div>
          </div>
          <div className="doc-footer-col">
            <div className="doc-footer-item">
              <span className="doc-ico">
                <Globe size={11} />
              </span>
              www.superhumans.com
            </div>
            <div className="doc-footer-item">
              <span className="doc-ico">
                <Mail size={11} />
              </span>
              help@superhumans.com
            </div>
          </div>
        </footer>
      </main>

      <main className="doc-page doc-page--2">
        <h2 className="doc-p2-title">До замовлення №</h2>

        <div className="doc-blue doc-p2-date">Дата прийняття виробу у роботу :</div>

        <div className="doc-p2-caption">Комплектувальні вироби та матеріали</div>
        <table className="doc-p2-table">
          <thead>
            <tr>
              <th>Найменування</th>
              <th>Артикул</th>
              <th>Одиниця виміру</th>
              <th>Кількість</th>
            </tr>
          </thead>
        </table>

        <div className="doc-p2-tech">
          <span className="doc-blue">Технік</span>
          <span className="doc-p2-tech-name">
            <span className="doc-p2-rule" />
            <span className="doc-p2-hint">(Власне ім'я ПРІЗВИЩЕ)</span>
          </span>
          <span className="doc-p2-tech-sign">(підпис)</span>
        </div>

        <div className="doc-p2-caption doc-p2-caption--fit">Примірки</div>
        <table className="doc-p2-table doc-p2-table--fit">
          <thead>
            <tr>
              <th className="c1">№</th>
              <th className="c2">Дата примірки</th>
              <th className="c3">Послуга</th>
            </tr>
            <tr>
              <th className="c1">1</th>
              <th className="c2">2</th>
              <th className="c3">3</th>
            </tr>
          </thead>
          <tbody>
            {[
              ["1", "12.01.2026", "B-TR", " зняття мірок"],
              ["2", "15.01.2026", "B-TR", " примірка тестової гільзи"],
              ["3", "16.01.2026", "B-TR", " примірка тестової гільзи"],
              ["4", "19.01.2026", "B-TR", " примірка постійної внутрішньої гільзи"],
              ["5", "19.01.2026", "B-TR", " примірка постійної внутрішньої гільзи"],
              ["6", "21.01.2026", "B-TR", " примірка постійного протезу"],
              ["7", "21.01.2026", "B-TR", " налаштування кріплення протезу"],
              ["8", "21.01.2026", "B-TR", " ввідне навчання керуванню протезом"],
              ["9", "21.01.2026", "a/B-TR", " видача протезу"],
            ].map(([n, d, code, text]) => (
              <tr key={n}>
                <td className="c1">{n}</td>
                <td className="c2">{d}</td>
                <td className="c3">
                  <span className="doc-blue">{code}</span>
                  {text}
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        <div className="doc-p2-issue">Дата видачі протезу</div>
        <div className="doc-p2-box">
          <div>Виріб отримав, <b>Сніжко Іван Петрович</b></div>
          <div className="doc-p2-box-hint">
            (Власне ім'я ПРІЗВИЩЕ замовника)&nbsp;&nbsp;&nbsp;&nbsp;(підпис)
          </div>
        </div>

        <div className="doc-p2-chief">
          Начальник відділу протезування верхніх кінцівок :{" "}
          <span className="doc-blue doc-underline">Нагорний Денис Віталійович</span>
        </div>

        <div className="doc-p2-final">
          <span className="doc-blue">(Дата) :</span>
          <span className="doc-blue">(Підпис) __________________</span>
        </div>

        <div className="doc-p2-pager">
          Сторінка <span className="doc-blue">1</span> з <span className="doc-blue">1</span>
        </div>
      </main>
    </div>
  );
}
