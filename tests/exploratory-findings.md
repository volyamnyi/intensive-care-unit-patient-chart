# Exploratory Testing Findings — Full Interactive Session

**Date:** 2026-07-14T18:12:57.090Z
**Total interactions: 323**

**Severity breakdown:**
- INFO: 323

## Interaction Log

| # | Role | Page | Element | Action | Result | Severity |
|---|------|------|---------|--------|--------|----------|
| 1 | doctor1 | Login Page | page | navigate | http://localhost:5173/login | INFO |
| 2 | doctor1 | Login Page | login+password | fill | doctor1 | INFO |
| 3 | doctor1 | Dashboard | page | loaded | http://localhost:5173/doctor | INFO |
| 4 | doctor1 | Dashboard | menu item[0] | read | "Олександр Мельник" | INFO |
| 5 | doctor1 | Dashboard | menu item[1] | read | "Лікар" | INFO |
| 6 | doctor1 | Dashboard | menu item[2] | read | "Вийти" | INFO |
| 7 | doctor1 | Create Card | page | loaded | http://localhost:5173/doctor/create-card | INFO |
| 8 | doctor1 | Create Card | autocomplete option | select | Петренко Іван СергійовичМК-001234 · 1978-03-15 · м | INFO |
| 9 | doctor1 | Dashboard | menu item[0] | read | "Олександр Мельник" | INFO |
| 10 | doctor1 | Dashboard | menu item[1] | read | "Лікар" | INFO |
| 11 | doctor1 | Dashboard | menu item[2] | read | "Вийти" | INFO |
| 12 | doctor1 | Dashboard | Відкрити | click | opening episode | INFO |
| 13 | doctor1 | Episode | page | loaded | http://localhost:5173/doctor/episode/a1111111-1111-1111-1111-111111111111 | INFO |
| 14 | doctor1 | Episode | header "Петренко" | click | OK | INFO |
| 15 | doctor1 | Episode | header "Статус:" | click | OK | INFO |
| 16 | doctor1 | Episode | header "№ a" | click | OK | INFO |
| 17 | doctor1 | Episode | header "Доба №" | click | OK | INFO |
| 18 | doctor1 | Episode | clinical day "Доба 1" | click | selected (header: "Карта інтенсивної те") | INFO |
| 19 | doctor1 | Episode | clinical day "Доба 2" | click | selected (header: "Карта інтенсивної те") | INFO |
| 20 | doctor1 | Episode | tab "Вітальні показники" | click | switched | INFO |
| 21 | doctor1 | Vitals Tab | hour pill 8:00 | click | selected | INFO |
| 22 | doctor1 | Vitals Tab | hour pill 9:00 | click | selected | INFO |
| 23 | doctor1 | Vitals Tab | hour pill 10:00 | click | selected | INFO |
| 24 | doctor1 | Vitals Tab | hour pill 11:00 | click | selected | INFO |
| 25 | doctor1 | Vitals Tab | hour pill 12:00 | click | selected | INFO |
| 26 | doctor1 | Vitals Tab | hour pill 13:00 | click | selected | INFO |
| 27 | doctor1 | Vitals Tab | hour pill 14:00 | click | selected | INFO |
| 28 | doctor1 | Vitals Tab | hour pill 15:00 | click | selected | INFO |
| 29 | doctor1 | Vitals Tab | hour pill 16:00 | click | selected | INFO |
| 30 | doctor1 | Vitals Tab | hour pill 17:00 | click | selected | INFO |
| 31 | doctor1 | Vitals Tab | hour pill 18:00 | click | selected | INFO |
| 32 | doctor1 | Vitals Tab | hour pill 19:00 | click | selected | INFO |
| 33 | doctor1 | Vitals Tab | hour pill 20:00 | click | selected | INFO |
| 34 | doctor1 | Vitals Tab | hour pill 21:00 | click | selected | INFO |
| 35 | doctor1 | Vitals Tab | hour pill 22:00 | click | selected | INFO |
| 36 | doctor1 | Vitals Tab | hour pill 23:00 | click | selected | INFO |
| 37 | doctor1 | Vitals Tab | hour pill 24:00 | click | selected | INFO |
| 38 | doctor1 | Vitals Tab | hour pill 1:00 | click | selected | INFO |
| 39 | doctor1 | Vitals Tab | hour pill 2:00 | click | selected | INFO |
| 40 | doctor1 | Vitals Tab | hour pill 3:00 | click | selected | INFO |
| 41 | doctor1 | Vitals Tab | hour pill 4:00 | click | selected | INFO |
| 42 | doctor1 | Vitals Tab | hour pill 5:00 | click | selected | INFO |
| 43 | doctor1 | Vitals Tab | hour pill 6:00 | click | selected | INFO |
| 44 | doctor1 | Vitals Tab | hour pill 7:00 | click | selected | INFO |
| 45 | doctor1 | Episode | tab "Призначення" | click | switched | INFO |
| 46 | doctor1 | Orders Tab | empty state | click | OK | INFO |
| 47 | doctor1 | Episode | tab "Шкали" | click | switched | INFO |
| 48 | doctor1 | Scales Tab | empty state | click | OK | INFO |
| 49 | doctor1 | Episode | tab "Нотатки" | click | switched | INFO |
| 50 | doctor1 | Notes Tab | add note | complete | note added | INFO |
| 51 | doctor1 | Episode | tab "Баланс рідини" | click | switched | INFO |
| 52 | doctor2 | Login Page | page | navigate | http://localhost:5173/login | INFO |
| 53 | doctor2 | Login Page | login+password | fill | doctor2 | INFO |
| 54 | doctor2 | Dashboard | page | loaded | http://localhost:5173/doctor | INFO |
| 55 | doctor2 | Dashboard | menu item[0] | read | "Наталія Бойко" | INFO |
| 56 | doctor2 | Dashboard | menu item[1] | read | "Лікар" | INFO |
| 57 | doctor2 | Dashboard | menu item[2] | read | "Вийти" | INFO |
| 58 | doctor2 | Create Card | page | loaded | http://localhost:5173/doctor/create-card | INFO |
| 59 | doctor2 | Create Card | autocomplete option | select | Петренко Іван СергійовичМК-001234 · 1978-03-15 · м | INFO |
| 60 | doctor2 | Dashboard | menu item[0] | read | "Наталія Бойко" | INFO |
| 61 | doctor2 | Dashboard | menu item[1] | read | "Лікар" | INFO |
| 62 | doctor2 | Dashboard | menu item[2] | read | "Вийти" | INFO |
| 63 | doctor2 | Dashboard | Відкрити | click | opening episode | INFO |
| 64 | doctor2 | Episode | page | loaded | http://localhost:5173/doctor/episode/a1111111-1111-1111-1111-111111111111 | INFO |
| 65 | doctor2 | Episode | header "Петренко" | click | OK | INFO |
| 66 | doctor2 | Episode | header "Статус:" | click | OK | INFO |
| 67 | doctor2 | Episode | header "№ a" | click | OK | INFO |
| 68 | doctor2 | Episode | header "Доба №" | click | OK | INFO |
| 69 | doctor2 | Episode | clinical day "Доба 1" | click | selected (header: "Карта інтенсивної те") | INFO |
| 70 | doctor2 | Episode | clinical day "Доба 2" | click | selected (header: "Карта інтенсивної те") | INFO |
| 71 | doctor2 | Episode | tab "Вітальні показники" | click | switched | INFO |
| 72 | doctor2 | Vitals Tab | hour pill 8:00 | click | selected | INFO |
| 73 | doctor2 | Vitals Tab | hour pill 9:00 | click | selected | INFO |
| 74 | doctor2 | Vitals Tab | hour pill 10:00 | click | selected | INFO |
| 75 | doctor2 | Vitals Tab | hour pill 11:00 | click | selected | INFO |
| 76 | doctor2 | Vitals Tab | hour pill 12:00 | click | selected | INFO |
| 77 | doctor2 | Vitals Tab | hour pill 13:00 | click | selected | INFO |
| 78 | doctor2 | Vitals Tab | hour pill 14:00 | click | selected | INFO |
| 79 | doctor2 | Vitals Tab | hour pill 15:00 | click | selected | INFO |
| 80 | doctor2 | Vitals Tab | hour pill 16:00 | click | selected | INFO |
| 81 | doctor2 | Vitals Tab | hour pill 17:00 | click | selected | INFO |
| 82 | doctor2 | Vitals Tab | hour pill 18:00 | click | selected | INFO |
| 83 | doctor2 | Vitals Tab | hour pill 19:00 | click | selected | INFO |
| 84 | doctor2 | Vitals Tab | hour pill 20:00 | click | selected | INFO |
| 85 | doctor2 | Vitals Tab | hour pill 21:00 | click | selected | INFO |
| 86 | doctor2 | Vitals Tab | hour pill 22:00 | click | selected | INFO |
| 87 | doctor2 | Vitals Tab | hour pill 23:00 | click | selected | INFO |
| 88 | doctor2 | Vitals Tab | hour pill 24:00 | click | selected | INFO |
| 89 | doctor2 | Vitals Tab | hour pill 1:00 | click | selected | INFO |
| 90 | doctor2 | Vitals Tab | hour pill 2:00 | click | selected | INFO |
| 91 | doctor2 | Vitals Tab | hour pill 3:00 | click | selected | INFO |
| 92 | doctor2 | Vitals Tab | hour pill 4:00 | click | selected | INFO |
| 93 | doctor2 | Vitals Tab | hour pill 5:00 | click | selected | INFO |
| 94 | doctor2 | Vitals Tab | hour pill 6:00 | click | selected | INFO |
| 95 | doctor2 | Vitals Tab | hour pill 7:00 | click | selected | INFO |
| 96 | doctor2 | Episode | tab "Призначення" | click | switched | INFO |
| 97 | doctor2 | Orders Tab | empty state | click | OK | INFO |
| 98 | doctor2 | Episode | tab "Шкали" | click | switched | INFO |
| 99 | doctor2 | Scales Tab | empty state | click | OK | INFO |
| 100 | doctor2 | Episode | tab "Нотатки" | click | switched | INFO |
| 101 | doctor2 | Notes Tab | add note | complete | note added | INFO |
| 102 | doctor2 | Episode | tab "Баланс рідини" | click | switched | INFO |
| 103 | head1 | Login Page | page | navigate | http://localhost:5173/login | INFO |
| 104 | head1 | Login Page | login+password | fill | head1 | INFO |
| 105 | head1 | Dashboard | page | loaded | http://localhost:5173/doctor | INFO |
| 106 | head1 | Dashboard | menu item[0] | read | "Василь Гончарук" | INFO |
| 107 | head1 | Dashboard | menu item[1] | read | "Лікар" | INFO |
| 108 | head1 | Dashboard | menu item[2] | read | "Вийти" | INFO |
| 109 | head1 | Create Card | page | loaded | http://localhost:5173/doctor/create-card | INFO |
| 110 | head1 | Create Card | autocomplete option | select | Петренко Іван СергійовичМК-001234 · 1978-03-15 · м | INFO |
| 111 | head1 | Dashboard | menu item[0] | read | "Василь Гончарук" | INFO |
| 112 | head1 | Dashboard | menu item[1] | read | "Лікар" | INFO |
| 113 | head1 | Dashboard | menu item[2] | read | "Вийти" | INFO |
| 114 | head1 | Dashboard | Відкрити | click | opening episode | INFO |
| 115 | head1 | Episode | page | loaded | http://localhost:5173/doctor/episode/a1111111-1111-1111-1111-111111111111 | INFO |
| 116 | head1 | Episode | header "Петренко" | click | OK | INFO |
| 117 | head1 | Episode | header "Статус:" | click | OK | INFO |
| 118 | head1 | Episode | header "№ a" | click | OK | INFO |
| 119 | head1 | Episode | header "Доба №" | click | OK | INFO |
| 120 | head1 | Episode | clinical day "Доба 1" | click | selected (header: "Карта інтенсивної те") | INFO |
| 121 | head1 | Episode | clinical day "Доба 2" | click | selected (header: "Карта інтенсивної те") | INFO |
| 122 | head1 | Episode | tab "Вітальні показники" | click | switched | INFO |
| 123 | head1 | Vitals Tab | hour pill 8:00 | click | selected | INFO |
| 124 | head1 | Vitals Tab | hour pill 9:00 | click | selected | INFO |
| 125 | head1 | Vitals Tab | hour pill 10:00 | click | selected | INFO |
| 126 | head1 | Vitals Tab | hour pill 11:00 | click | selected | INFO |
| 127 | head1 | Vitals Tab | hour pill 12:00 | click | selected | INFO |
| 128 | head1 | Vitals Tab | hour pill 13:00 | click | selected | INFO |
| 129 | head1 | Vitals Tab | hour pill 14:00 | click | selected | INFO |
| 130 | head1 | Vitals Tab | hour pill 15:00 | click | selected | INFO |
| 131 | head1 | Vitals Tab | hour pill 16:00 | click | selected | INFO |
| 132 | head1 | Vitals Tab | hour pill 17:00 | click | selected | INFO |
| 133 | head1 | Vitals Tab | hour pill 18:00 | click | selected | INFO |
| 134 | head1 | Vitals Tab | hour pill 19:00 | click | selected | INFO |
| 135 | head1 | Vitals Tab | hour pill 20:00 | click | selected | INFO |
| 136 | head1 | Vitals Tab | hour pill 21:00 | click | selected | INFO |
| 137 | head1 | Vitals Tab | hour pill 22:00 | click | selected | INFO |
| 138 | head1 | Vitals Tab | hour pill 23:00 | click | selected | INFO |
| 139 | head1 | Vitals Tab | hour pill 24:00 | click | selected | INFO |
| 140 | head1 | Vitals Tab | hour pill 1:00 | click | selected | INFO |
| 141 | head1 | Vitals Tab | hour pill 2:00 | click | selected | INFO |
| 142 | head1 | Vitals Tab | hour pill 3:00 | click | selected | INFO |
| 143 | head1 | Vitals Tab | hour pill 4:00 | click | selected | INFO |
| 144 | head1 | Vitals Tab | hour pill 5:00 | click | selected | INFO |
| 145 | head1 | Vitals Tab | hour pill 6:00 | click | selected | INFO |
| 146 | head1 | Vitals Tab | hour pill 7:00 | click | selected | INFO |
| 147 | head1 | Episode | tab "Призначення" | click | switched | INFO |
| 148 | head1 | Orders Tab | empty state | click | OK | INFO |
| 149 | head1 | Episode | tab "Шкали" | click | switched | INFO |
| 150 | head1 | Scales Tab | empty state | click | OK | INFO |
| 151 | head1 | Episode | tab "Нотатки" | click | switched | INFO |
| 152 | head1 | Notes Tab | add note | complete | note added | INFO |
| 153 | head1 | Episode | tab "Баланс рідини" | click | switched | INFO |
| 154 | nurse1 | Login Page | page | navigate | http://localhost:5173/login | INFO |
| 155 | nurse1 | Login Page | login+password | fill | nurse1 | INFO |
| 156 | nurse1 | Nurse Dashboard | page | loaded | http://localhost:5173/nurse | INFO |
| 157 | nurse1 | Nurse Dashboard | menu item[0] | read | "Олена Ткаченко" | INFO |
| 158 | nurse1 | Nurse Dashboard | menu item[1] | read | "Медсестра" | INFO |
| 159 | nurse1 | Nurse Dashboard | menu item[2] | read | "Вийти" | INFO |
| 160 | nurse1 | Nurse Episode | page | loaded | http://localhost:5173/nurse/episode/a1111111-1111-1111-1111-111111111111 | INFO |
| 161 | nurse1 | Nurse Episode | tab "Вітальні показники" | click | switched | INFO |
| 162 | nurse1 | Vitals Tab | hour pill 8:00✓ | click | selected | INFO |
| 163 | nurse1 | Vitals Tab | hour pill 9:00 | click | selected | INFO |
| 164 | nurse1 | Vitals Tab | hour pill 10:00 | click | selected | INFO |
| 165 | nurse1 | Vitals Tab | hour pill 11:00 | click | selected | INFO |
| 166 | nurse1 | Vitals Tab | hour pill 12:00 | click | selected | INFO |
| 167 | nurse1 | Vitals Tab | hour pill 13:00 | click | selected | INFO |
| 168 | nurse1 | Vitals Tab | hour pill 14:00 | click | selected | INFO |
| 169 | nurse1 | Vitals Tab | hour pill 15:00 | click | selected | INFO |
| 170 | nurse1 | Vitals Tab | hour pill 16:00 | click | selected | INFO |
| 171 | nurse1 | Vitals Tab | hour pill 17:00 | click | selected | INFO |
| 172 | nurse1 | Vitals Tab | hour pill 18:00 | click | selected | INFO |
| 173 | nurse1 | Vitals Tab | hour pill 19:00 | click | selected | INFO |
| 174 | nurse1 | Vitals Tab | hour pill 20:00 | click | selected | INFO |
| 175 | nurse1 | Vitals Tab | hour pill 21:00 | click | selected | INFO |
| 176 | nurse1 | Vitals Tab | hour pill 22:00 | click | selected | INFO |
| 177 | nurse1 | Vitals Tab | hour pill 23:00 | click | selected | INFO |
| 178 | nurse1 | Vitals Tab | hour pill 24:00 | click | selected | INFO |
| 179 | nurse1 | Vitals Tab | hour pill 1:00 | click | selected | INFO |
| 180 | nurse1 | Vitals Tab | hour pill 2:00 | click | selected | INFO |
| 181 | nurse1 | Vitals Tab | hour pill 3:00 | click | selected | INFO |
| 182 | nurse1 | Vitals Tab | hour pill 4:00 | click | selected | INFO |
| 183 | nurse1 | Vitals Tab | hour pill 5:00 | click | selected | INFO |
| 184 | nurse1 | Vitals Tab | hour pill 6:00 | click | selected | INFO |
| 185 | nurse1 | Vitals Tab | hour pill 7:00 | click | selected | INFO |
| 186 | nurse1 | Vitals Tab | field "АТ сист" | fill | 120 | INFO |
| 187 | nurse1 | Vitals Tab | field "АТ діас" | fill | 80 | INFO |
| 188 | nurse1 | Vitals Tab | field "ЧСС" | fill | 72 | INFO |
| 189 | nurse1 | Vitals Tab | field "SpO2" | fill | 98 | INFO |
| 190 | nurse1 | Vitals Tab | field "Темп. тіла" | fill | 36.6 | INFO |
| 191 | nurse1 | Vitals Tab | field "ЦВТ" | fill | 8 | INFO |
| 192 | nurse1 | Vitals Tab | field "ЧД" | fill | 16 | INFO |
| 193 | nurse1 | Vitals Tab | field "Свідомість" | fill | Ясна, задовільний стан | INFO |
| 194 | nurse1 | Vitals Tab | field "etCO2" | fill | 35 | INFO |
| 195 | nurse1 | Vitals Tab | field "FiO2" | fill | 21 | INFO |
| 196 | nurse1 | Vitals Tab | field "Діурез" | fill | 100 | INFO |
| 197 | nurse1 | Vitals Tab | field "Дренаж" | fill | 50 | INFO |
| 198 | nurse1 | Vitals Tab | field "Біль" | fill | 0 | INFO |
| 199 | nurse1 | Vitals Tab | field "Нотатки" | fill | Стан пацієнта стабільний, гемодинаміка в нормі | INFO |
| 200 | nurse1 | Vitals Tab | field "АТ сист" | fill | 120 | INFO |
| 201 | nurse1 | Vitals Tab | field "АТ діас" | fill | 80 | INFO |
| 202 | nurse1 | Vitals Tab | field "ЧСС" | fill | 72 | INFO |
| 203 | nurse1 | Vitals Tab | field "SpO2" | fill | 98 | INFO |
| 204 | nurse1 | Vitals Tab | field "Темп. тіла" | fill | 36.6 | INFO |
| 205 | nurse1 | Vitals Tab | field "ЦВТ" | fill | 8 | INFO |
| 206 | nurse1 | Vitals Tab | field "ЧД" | fill | 16 | INFO |
| 207 | nurse1 | Vitals Tab | field "Свідомість" | fill | Ясна, задовільний стан | INFO |
| 208 | nurse1 | Vitals Tab | field "etCO2" | fill | 35 | INFO |
| 209 | nurse1 | Vitals Tab | field "FiO2" | fill | 21 | INFO |
| 210 | nurse1 | Vitals Tab | field "Діурез" | fill | 100 | INFO |
| 211 | nurse1 | Vitals Tab | field "Дренаж" | fill | 50 | INFO |
| 212 | nurse1 | Vitals Tab | field "Біль" | fill | 0 | INFO |
| 213 | nurse1 | Vitals Tab | field "Нотатки" | fill | Стан пацієнта стабільний, гемодинаміка в нормі | INFO |
| 214 | nurse1 | Nurse Episode | tab "Призначення" | click | switched | INFO |
| 215 | nurse1 | Nurse Episode | tab "Шкали" | click | switched | INFO |
| 216 | nurse1 | Scales Tab | empty state | click | confirmed no create controls | INFO |
| 217 | nurse1 | Nurse Episode | tab "Нотатки" | click | switched | INFO |
| 218 | nurse1 | Nurse Notes | add note | complete | note added | INFO |
| 219 | nurse1 | Nurse Episode | tab "Баланс рідини" | click | switched | INFO |
| 220 | nurse2 | Login Page | page | navigate | http://localhost:5173/login | INFO |
| 221 | nurse2 | Login Page | login+password | fill | nurse2 | INFO |
| 222 | nurse2 | Nurse Dashboard | page | loaded | http://localhost:5173/nurse | INFO |
| 223 | nurse2 | Nurse Dashboard | menu item[0] | read | "Марія Кравчук" | INFO |
| 224 | nurse2 | Nurse Dashboard | menu item[1] | read | "Медсестра" | INFO |
| 225 | nurse2 | Nurse Dashboard | menu item[2] | read | "Вийти" | INFO |
| 226 | nurse2 | Nurse Episode | page | loaded | http://localhost:5173/nurse/episode/a1111111-1111-1111-1111-111111111111 | INFO |
| 227 | nurse2 | Nurse Episode | tab "Вітальні показники" | click | switched | INFO |
| 228 | nurse2 | Vitals Tab | hour pill 8:00✓ | click | selected | INFO |
| 229 | nurse2 | Vitals Tab | hour pill 9:00 | click | selected | INFO |
| 230 | nurse2 | Vitals Tab | hour pill 10:00 | click | selected | INFO |
| 231 | nurse2 | Vitals Tab | hour pill 11:00 | click | selected | INFO |
| 232 | nurse2 | Vitals Tab | hour pill 12:00 | click | selected | INFO |
| 233 | nurse2 | Vitals Tab | hour pill 13:00 | click | selected | INFO |
| 234 | nurse2 | Vitals Tab | hour pill 14:00 | click | selected | INFO |
| 235 | nurse2 | Vitals Tab | hour pill 15:00 | click | selected | INFO |
| 236 | nurse2 | Vitals Tab | hour pill 16:00 | click | selected | INFO |
| 237 | nurse2 | Vitals Tab | hour pill 17:00 | click | selected | INFO |
| 238 | nurse2 | Vitals Tab | hour pill 18:00 | click | selected | INFO |
| 239 | nurse2 | Vitals Tab | hour pill 19:00 | click | selected | INFO |
| 240 | nurse2 | Vitals Tab | hour pill 20:00 | click | selected | INFO |
| 241 | nurse2 | Vitals Tab | hour pill 21:00 | click | selected | INFO |
| 242 | nurse2 | Vitals Tab | hour pill 22:00 | click | selected | INFO |
| 243 | nurse2 | Vitals Tab | hour pill 23:00 | click | selected | INFO |
| 244 | nurse2 | Vitals Tab | hour pill 24:00 | click | selected | INFO |
| 245 | nurse2 | Vitals Tab | hour pill 1:00 | click | selected | INFO |
| 246 | nurse2 | Vitals Tab | hour pill 2:00 | click | selected | INFO |
| 247 | nurse2 | Vitals Tab | hour pill 3:00 | click | selected | INFO |
| 248 | nurse2 | Vitals Tab | hour pill 4:00 | click | selected | INFO |
| 249 | nurse2 | Vitals Tab | hour pill 5:00 | click | selected | INFO |
| 250 | nurse2 | Vitals Tab | hour pill 6:00 | click | selected | INFO |
| 251 | nurse2 | Vitals Tab | hour pill 7:00 | click | selected | INFO |
| 252 | nurse2 | Vitals Tab | field "АТ сист" | fill | 120 | INFO |
| 253 | nurse2 | Vitals Tab | field "АТ діас" | fill | 80 | INFO |
| 254 | nurse2 | Vitals Tab | field "ЧСС" | fill | 72 | INFO |
| 255 | nurse2 | Vitals Tab | field "SpO2" | fill | 98 | INFO |
| 256 | nurse2 | Vitals Tab | field "Темп. тіла" | fill | 36.6 | INFO |
| 257 | nurse2 | Vitals Tab | field "ЦВТ" | fill | 8 | INFO |
| 258 | nurse2 | Vitals Tab | field "ЧД" | fill | 16 | INFO |
| 259 | nurse2 | Vitals Tab | field "Свідомість" | fill | Ясна, задовільний стан | INFO |
| 260 | nurse2 | Vitals Tab | field "etCO2" | fill | 35 | INFO |
| 261 | nurse2 | Vitals Tab | field "FiO2" | fill | 21 | INFO |
| 262 | nurse2 | Vitals Tab | field "Діурез" | fill | 100 | INFO |
| 263 | nurse2 | Vitals Tab | field "Дренаж" | fill | 50 | INFO |
| 264 | nurse2 | Vitals Tab | field "Біль" | fill | 0 | INFO |
| 265 | nurse2 | Vitals Tab | field "Нотатки" | fill | Стан пацієнта стабільний, гемодинаміка в нормі | INFO |
| 266 | nurse2 | Vitals Tab | field "АТ сист" | fill | 120 | INFO |
| 267 | nurse2 | Vitals Tab | field "АТ діас" | fill | 80 | INFO |
| 268 | nurse2 | Vitals Tab | field "ЧСС" | fill | 72 | INFO |
| 269 | nurse2 | Vitals Tab | field "SpO2" | fill | 98 | INFO |
| 270 | nurse2 | Vitals Tab | field "Темп. тіла" | fill | 36.6 | INFO |
| 271 | nurse2 | Vitals Tab | field "ЦВТ" | fill | 8 | INFO |
| 272 | nurse2 | Vitals Tab | field "ЧД" | fill | 16 | INFO |
| 273 | nurse2 | Vitals Tab | field "Свідомість" | fill | Ясна, задовільний стан | INFO |
| 274 | nurse2 | Vitals Tab | field "etCO2" | fill | 35 | INFO |
| 275 | nurse2 | Vitals Tab | field "FiO2" | fill | 21 | INFO |
| 276 | nurse2 | Vitals Tab | field "Діурез" | fill | 100 | INFO |
| 277 | nurse2 | Vitals Tab | field "Дренаж" | fill | 50 | INFO |
| 278 | nurse2 | Vitals Tab | field "Біль" | fill | 0 | INFO |
| 279 | nurse2 | Vitals Tab | field "Нотатки" | fill | Стан пацієнта стабільний, гемодинаміка в нормі | INFO |
| 280 | nurse2 | Nurse Episode | tab "Призначення" | click | switched | INFO |
| 281 | nurse2 | Nurse Episode | tab "Шкали" | click | switched | INFO |
| 282 | nurse2 | Scales Tab | empty state | click | confirmed no create controls | INFO |
| 283 | nurse2 | Nurse Episode | tab "Нотатки" | click | switched | INFO |
| 284 | nurse2 | Nurse Notes | add note | complete | note added | INFO |
| 285 | nurse2 | Nurse Episode | tab "Баланс рідини" | click | switched | INFO |
| 286 | admin | Login Page | page | navigate | http://localhost:5173/login | INFO |
| 287 | admin | Login Page | login+password | fill | admin | INFO |
| 288 | admin | Admin Page | page | loaded | http://localhost:5173/admin | INFO |
| 289 | admin | Admin Page | menu item[0] | read | "Адмін Системи" | INFO |
| 290 | admin | Admin Page | menu item[1] | read | "Вийти" | INFO |
| 291 | admin | Admin Page | heading "Користувачі системи" | click | OK | INFO |
| 292 | admin | Admin Page | heading "Лікарі" | click | OK | INFO |
| 293 | admin | Admin Page | heading "Медсестри" | click | OK | INFO |
| 294 | admin | Admin Page | Doctors header[0] "ПІБ" | click | OK | INFO |
| 295 | admin | Admin Page | Doctors header[1] "Логін" | click | OK | INFO |
| 296 | admin | Admin Page | Doctors header[2] "Роль" | click | OK | INFO |
| 297 | admin | Admin Page | Doctors header[3] "Email" | click | OK | INFO |
| 298 | admin | Admin Page | Doctors row[0] cell[0] | click | "Олександр Мельник" | INFO |
| 299 | admin | Admin Page | Doctors row[0] cell[1] | click | "doctor1" | INFO |
| 300 | admin | Admin Page | Doctors row[0] cell[2] | click | "Лікар" | INFO |
| 301 | admin | Admin Page | Doctors row[0] cell[3] | click | "melnyk@hospital.ua" | INFO |
| 302 | admin | Admin Page | Doctors row[1] cell[0] | click | "Наталія Бойко" | INFO |
| 303 | admin | Admin Page | Doctors row[1] cell[1] | click | "doctor2" | INFO |
| 304 | admin | Admin Page | Doctors row[1] cell[2] | click | "Лікар" | INFO |
| 305 | admin | Admin Page | Doctors row[1] cell[3] | click | "boyko@hospital.ua" | INFO |
| 306 | admin | Admin Page | Nurses header[0] "ПІБ" | click | OK | INFO |
| 307 | admin | Admin Page | Nurses header[1] "Логін" | click | OK | INFO |
| 308 | admin | Admin Page | Nurses header[2] "Роль" | click | OK | INFO |
| 309 | admin | Admin Page | Nurses header[3] "Email" | click | OK | INFO |
| 310 | admin | Admin Page | Nurses row[0] cell[0] | click | "Олена Ткаченко" | INFO |
| 311 | admin | Admin Page | Nurses row[0] cell[1] | click | "nurse1" | INFO |
| 312 | admin | Admin Page | Nurses row[0] cell[2] | click | "Медсестра" | INFO |
| 313 | admin | Admin Page | Nurses row[0] cell[3] | click | "tkachenko@hospital.ua" | INFO |
| 314 | admin | Admin Page | Nurses row[1] cell[0] | click | "Марія Кравчук" | INFO |
| 315 | admin | Admin Page | Nurses row[1] cell[1] | click | "nurse2" | INFO |
| 316 | admin | Admin Page | Nurses row[1] cell[2] | click | "Медсестра" | INFO |
| 317 | admin | Admin Page | Nurses row[1] cell[3] | click | "kravchuk@hospital.ua" | INFO |
| 318 | anon | Login Page | page | loaded | http://localhost:5173/login | INFO |
| 319 | anon | Login Page | error alert | read | "Невірний логін або пароль" | INFO |
| 320 | anon | /doctor | redirect | navigate | http://localhost:5173/login | INFO |
| 321 | anon | /nurse | redirect | navigate | http://localhost:5173/login | INFO |
| 322 | anon | /admin | redirect | navigate | http://localhost:5173/login | INFO |
| 323 | anon | /nonexistent | 404 page | loaded | http://localhost:5173/nonexistent | INFO |

## Bugs / Issues

| # | Severity | Description | Location |
|---|----------|-------------|----------|
| — | — | No issues found | — |
