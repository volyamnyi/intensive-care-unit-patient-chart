# **Методи API** 

## **Зміст** 

|**Зміст**||
|---|---|
|**Таблиця версійності API методів**|**5**|
|**Функціонали, які не підтримуються методами API**|**7**|
|**Вебхуки**|**8**|
|**Сценарії використання методів АРІ**|**10**|
|Онлайн запис|11|
|Чат-бот|13|
|Інтеграція з платіжним терміналом (ПТКС)|15|
|Інтеграція із зовнішніми CRM-системами, сайтом чи іншими ресурсами|16|
|Інтеграція із бухгалтерськими системами|17|
|**Авторизація**|**18**|
|**Лікарі (персонал)**|**20**|
|Метод spzIBUserDetails|20|
|Метод spzIBUserDetailsExtended|22|
|Метод spzIBSpecialityDetails|24|
|Метод spzIBUserSpecialityUserDetails|26|
|**Послуги**|**28**|
|Метод spzIBServiceDetails|28|
|Метод spzIBServiceUserDetails|30|
|Метод spzIBTariffPlanList|32|
|Метод spzIBBookingDocumentList|34|
|**Розклад**|**36**|
|Метод spzIBFreeSeanceDetails|36|
|Метод spzIBFreeDateDetails|38|
|**Інфраструктура**|**40**|
|Метод spzIBCompanyDetails|40|
|Метод spzIBVenueDetails|42|
|Метод spzIBAgentList|44|
|Метод spzIBAgentSave|46|
|Метод spzIBInstitutionList|48|
|Метод spzIBCompanyList|50|
|Метод spzIBInstitutionSave|52|



2 

|**Пацієнти**|**54**|
|---|---|
|Метод spzIBScheduleCreate|54|
|Метод spiAPIReservationCreate|56|
|Метод spzIBScheduleCancel|58|
|Метод spzIBScheduleBookingDetails|59|
|Метод spzIBPatientCreate|61|
|Метод spzIBDocumentList|63|
|Метод spzIBScheduleList|65|
|Метод spzIBCommunicationCreate|67|
|Метод spzIBPatientSearch|68|
|Метод spzIBAccountList|70|
|Метод spzIBAccountTypeList|72|
|Метод spzIBTaskCreate|74|
|Метод spzIBPatientSave|75|
|Метод spiIBUTMSave|78|
|Метод spzIBPatientIDByLogin|79|
|**Послуги та оплати**|**80**|
|Метод splСpaymentDetails|80|
|Метод sp1CbookingDetails|81|
|Метод spzIBBookingList|83|
|Метод spzIBPaymentList|86|
|Метод spzIBServiceSave|88|
|Метод spzIBServiceList|90|
|Метод spzIBServicePriceList|92|
|Метод spzIBServicePriceSave|94|
|Метод spzIBPaymentBookingInfo|96|
|**Методи для роботи з даними пацієнта**|**98**|
|Метод spzIBPatientScheduleList|98|
|Метод spzIBPatientDocumentList|100|
|Метод spzIBPatientServiceList|102|
|Метод spzIBPatientFreeSeanceList|104|
|Метод spzIBPatientUserList|106|
|Метод spzIBPatientDataFileList|108|



3 

|Метод spzIBPatientCompanyList|109|
|---|---|
|Метод spzIBPatientScheduleCreate|111|
|Метод spzIBPatientScheduleCancel|113|
|Метод spzIBServiceRequestValidate|114|
|Метод spzIBPatientServiceRequestValidate|116|
|Метод spzIBPatientRoleList|118|
|**Методи для роботи із складською системою**|**119**|
|Метод spiOutputDeliveryDetails|119|
|Метод spiItemKindDetails|121|
|Метод spiDeliveryChange|123|
|Метод spiDeliveryKindList|124|
|**Статичні словники**|**125**|
|Метод spzIBDocumentApproveStatusDictionary|125|
|Метод spzIBGuarantorStatusDictionary|126|
|Метод spzIBBookingStatusDictionary|127|
|Метод spzIBBookingPaymentStatusDictionary|128|
|Метод spzIBScheduleStatusDictionary|129|
|**Методи АРІ для інтеграції з платіжним терміналом (ПТКС)**|**131**|
|Метод spzIBPatientDebt|131|
|Метод spzBalanceBillInput|133|
|Метод spzIBPatientInfo|135|
|Метод spzIBPaymentStatus|137|
|**Опис стандартних помилок**|**138**|
|Помилки при авторизації|138|
|Стандартні помилки при виконанні методів|139|



4 

## **Таблиця версійності API методів** 

|**Дата**|**Версія**|**Зміни**|
|---|---|---|
|15.01.2023|v1.0|Створено методи та документацію. Початкова версія.|
|||l Додано нові параметри методів.|
|08.03.2023|v1.1|l Додано метод для скасування візитів.|
|||l Додано методи для (ПТКС) програмно-|
|||технічних комплексів самообслуговування.|
|||l Додано статичні словники.|
|||l Додано нові поля.|
|||l Додано нові методи:|
|||l spzIBBookingList|
|||l spzIBPaymentList|
|||l spzIBAgentList|
|15.06.2023|v1.2|l spzIBAgentSave|
|||l spzIBServiceSave|
|||l spzIBInstitutionList|
|||l spzIBInstitutionSave|
|||l spzIBAccountTypeList|
|||l spzIBServiceList|
|||l spzIBServicePriceList|
|||l spzIBServicePriceSave|
|||l Додано короткий опис методів АРІ.|
|||l Додано сценарії використання методів АРІ.|
|||l Додано опис вебхуків.|
|||l Додано нові методи:|
|01.10.2023|v1.3|l spzIBPatientScheduleList|
|||l spzIBPatientDocumentList|
|||l spzIBPatientServiceList|
|||l spzIBPatientUserList|
|||l spzIBPatientDataFileList|
|||l spzIBPatientFreeSeanceList|



5 

|**Дата**|**Версія**|**Зміни**||
|---|---|---|---|
|||l spzIBPatientScheduleCreate||
|||l spzIBPatientScheduleCancel||
|||l spzIBCompanyList||
|||l<br>spzIBPatientCompanyList||
|||l spzIBServiceRequestValidate||
|||l spzIBPatientServiceRequestValidate||
|||l spzIBPatientRoleList||
|||l spzIBTaskCreate||
|||l spzIBPatientSave||
|||l<br>spiIBUTMSave||
|||l spzIBPaymentStatus||
|||l spiItemKindDetails||
|||l spiDeliveryChange||
|||l spiDeliveryKindList||
|||l spiOutputDeliveryDetails||
|||l spzIBPaymentBookingInfo||
|||l Додано нові параметри методів.||
|28.02.2024|v1.4|l Додано метод spiAPIReservationCreate<br>.||
|||l Додано нові параметри методів.||
|||l Додано нові методи:||
|30.12.2024|v.1.6|||
|||l spzIBBookingDocumentList||
|||l spzIBPatientIDByLogin||
|||l Додано нові параметри методів:||
|||l spzIBPatientSearch||
|||l spzIBBookingList||
|||l spzIBScheduleList||
|03.02.2025|v.1.7|l spzIBFreeSeanceDetails||
|||l spzIBSpecialityDetails||
|||l spzIBPaymentList||
|||l spzIBPatientDebt||
|||l spzIBServiceList||
|||l spzIBDocumentList||



6 

## **не Функціонали, які підтримуються методами API** 

Методи API не будуть підтримувати наступні функціонали: 

1. Створення, редагування, видалення документів. 

2. Створення, редагування, видалення користувачів та ролей, прив’язки користувачів до ролей. 

3. Управління доступом до документів, пацієнтів, елементів інфраструктури, звітів, шаблонів документів, журналів. 

4. Налаштування підсистеми словників. 

5. Налаштування шаблонів документів. 

6. Налаштування розкладів. 

7. Налаштування звітів. 

8. Налаштування інфраструктури. 

9. Сутності для роботи із підсистемою eHealth. 

10. Редагування і видалення записів у таблицях історії. 

7 

## **Вебхуки** 

Система передбачає автоматичну нотифікацію третьої сторони про зміни у системі за допомогою механізму вебхуків. 

Використання вебхуків реалізується за допомогою таких параметрів: 

- l **url** — метод, який виконуємо; 

- l **source** — InstallationGUID (константа); 

- l **entity** — сутність, над якою дія виконується; 

- l **key** — ідентифікатор (ключ дії); 

- l **action** — дія, яку виконуємо. 

Приклад: 

https://{{url}}?source={{source}}&entity={{entity}}&key={{key}}&action= {{action}} 

- Є 3 типи дій (action), які можемо виконувати, а саме: 

- l **Create** — створення сутностей; 

- l **Edit** — редагування сутностей; 

- l **Delete** — видалення сутностей. 

- Типи entity (сутностей): 

- l **Schedule** — візити. 

   - Метод для отримання даних — **spzIBScheduleList** . 

- l **Patient** — пацієнти. 

   - Метод для отримання даних — **spzIBPatientSearch** . 

- l **Booking** — бронювання. 

   - Метод для отримання даних — **spzIBBookingList** . 

- l **Payment** — оплати. 

   - Метод для отримання даних — **spzIBPaymentList** . 

- l **User** — користувачі. 

Метод для отримання даних — **spzIBUserDetails** . 

- l **Agent** — агенти. 

Метод для отримання даних — **spzIBAgentList** . 

- l **Institution** — установи (заклади). 

Метод для отримання даних — **spzIBInstitutionList** . 

- l **AccountType** — тип рахунку. 

- Метод для отримання даних — **spzIBAccountTypeList** . 

- l **Service** — послуги. 

Метод для отримання даних — **spzIBServiceList** . 

8 

- l **ServicePrice** — вартість послуг. 

## Метод для отримання даних — **spzIBServicePriceList** . 

- l **TariffPlan** — тарифні плани. 

## Метод для отримання даних — **spzIBTariffPlanList** . 

На стороні третьої системи має бути реалізовано вебінтерфейс, який прийматиме виклики відповідно до описаного. 

9 

## **Сценарії використання методів АРІ** 

## **Базові реалізації функціонала методами АРІ:** 

- l Онлайн запис 

- l Чат-бот 

- l Інтеграція з платіжним терміналом (ПТКС) 

- l Інтеграція із зовнішніми СРМ-системами, сайтом чи іншими ресурсами 

l Інтеграція із бухгалтерськими системами 

10 

## **Онлайн запис** 

## **Авторизація системи** 

- l Список лікарів — **spzIBUserDetails** . 

- l Список послуг вибраного лікаря — **spzIBServiceUserDetails** із параметром обраного лікаря. 

Вхідний параметр: 

   - l **@UserLogin** — логін лікаря. 

- l Розклад лікаря — **spzIBFreeSeanceDetails** із параметрами вибраного лікаря. 

Вхідні параметри: 

   - l **@UserLogin** — логін лікаря; 

   - l **@ServiceID** — ідентифікатор послуги; 

   - l **@DayCount** — кількість днів для відображення; 

- l Пошук або створення нового пацієнта — **spzIBPatientCreate** . Вхідні параметри: 

   - l **@PatientName1** — прізвище пацієнта; 

   - l **@PatientName2** — ім’я пацієнта; 

   - l **@PatientName3** — по батькові пацієнта; 

   - l **@PatientBirthDate** — дата народження пацієнта; 

   - l **@PatientSexCode** — стать пацієнта; 

   - l **@PatientPhone** — номер телефону пацієнта; 

   - l **@PatientEmail** — електронна адреса пацієнта. 

- l Ідентифікація наявного пацієнта за номером телефону — **spzIBPatientSearch** . 

   - Якщо за вказаним параметром пацієнт існує в системі, то метод виводить ID пацієнта для подальшої роботи. 

- l Створення запису для пацієнта у системі Doctor Eleks — **spzIBScheduleCreate** . 

11 

Вхідні параметри: 

   - l **@PatientID** — ідентифікатор пацієнта; 

   - l **@ServiceID** — ідентифікатор послуги; 

   - l **@VenueID** — номер кабінету; 

   - l **@StartTime datetime** — обраний час початку візиту; 

   - l **@EndTime datetime** — обраний час завершення візиту. 

- Додаткові можливості: 

- l Фільтрування за клініками — **spzIBCompanyDetails** . 

- l Фільтрування за спеціальностями лікарів — **spzIBSpecialityDetails** . Реалізація алгоритму із кроками запису на візит: _послуга_ > _лікар_ > _розклад лікаря_ > _запис_ . 

12 

## **Чат-бот** 

## **Основний функціонал чат-бота:** 

## l **Авторизація системи** 

- l Запис на візит для авторизованого чи неавторизованого користувача (описано у функціоналі Онлайн запис). 

- l Інформація про клініки медичного закладу. 

- l Виведення деталей клінік — **spzIBCompanyDetails** . 

   - Вихідні параметри: 

   - l **companyName** — назва клініки; 

   - l **companyAddress** — адреса клініки; 

   - l **companyEmail** — електронна пошта клініки; 

   - l **companyPhone** — номер телефону клініки; 

- l Перегляд списку лікарів і списку послуг, які вони виконують — **spzIBUserDetailsExtended** . 

Додаткова можливість фільтрування за клінікою, послугою чи спеціальністю, пошук за ПІБ лікаря. 

## l **Авторизація пацієнта** 

- l Авторизація за логіном і паролем, отриманими у медичному закладі. 

- l Ідентифікація наявного пацієнта за номером телефону — **spzIBPatientSearch** . 

   - Якщо за вказаним параметром пацієнт існує в системі, то метод виводить ID пацієнта для подальшої роботи. 

- l Ідентифікація чи створення пацієнта за розширеними даними пацієнта — **spzIBPatientCreate** . 

Вхідні параметри: 

   - l **@PatientName1** — прізвище пацієнта; 

   - l **@PatientName2** — ім’я пацієнта; 

   - l **@PatientName3** — по батькові пацієнта; 

   - l **@PatientBirthDate** — дата народження пацієнта; 

   - l **@PatientSexCode** — стать пацієнта; 

   - l **@PatientPhone** — номер телефону; 

   - l **@PatientEmail** — електронна адреса пацієнта; 

- l Перегляд списку запланованих і минулих візитів — **spzIBScheduleList** . 

13 

Вхідні параметри: 

   - l **@StartDate** — дата, від якої виводяться візити; 

   - l **@EndDate** — дата, до якої виводяться візити. 

- l Деталі візиту із виводом інформації про послуги — **spzIBScheduleBookingDetails** . 

   - Вхідний параметр: 

   - l **@ScheduleID** — ідентифікатор візиту. 

- l Перегляд списку документів пацієнта — **spzIBDocumentList** . Вхідні параметри: 

   - l **@StartDate** — дата, від якої виводяться документи; 

   - l **@EndDate** — дата, до якої виводяться документи. 

14 

## **Інтеграція з платіжним терміналом (ПТКС)** 

## **Авторизація системи** 

- l Отримання інформації про послуги для оплати за номером рахунку, який було видано у клініці, — **spzIBPatientInfo** . 

Вхідний параметр: 

- l **@DocumentSequenceNumber** — номер рахунку. 

Для використання цього підходу має бути **Примітка.** налаштований необхідний шаблон **Рахунок** , який містить номер рахунку. 

- l Отримання інформації про послуги для оплати за ID пацієнта — **spzIBPatientDebt** . 

- l Оплата вибраних послуг — **spzBalanceBillInput** . 

У метод передається список послуг до оплати, номер чека та дата оплати. Інформація про оплату автоматично зберігається та відображається у системі Doctor Eleks. 

15 

## **Інтеграція із зовнішніми CRM-системами, сайтом чи іншими ресурсами** 

## **Авторизація системи** 

- l Синхронізація списку лікарів — **spzIBUserDetails** . 

- l Синхронізація списку послуг — **spzIBServiceList** . 

- l Створення чи редагування послуги — **spzIBServiceSave** . 

- l Синхронізація цін на послуги — **spzIBServicePriceList** . 

- l Створення чи редагування вартості послуги — **spzIBServicePriceSave** . 

- l Синхронізація оплат — **spzIBPaymentList** . 

- l Синхронізація бронювань — **spzIBBookingList** . 

- l Синхронізація типів рахунків — **spzIBAccountTypeList** . 

- l Синхронізація рахунків пацієнтів — **spzIBAccountList** . 

- l Синхронізація тарифних планів — **spzIBTariffPlanList** . 

- l Синхронізація списку клінік — **spzIBCompanyDetails** . 

- l Синхронізація інфраструктури кабінетів клініки — **spzIBVenueDetails** . 

- l Синхронізація списку агентів — **spzIBAgentList** . 

- l Створення чи редагування агентів — **spzIBAgentSave** . 

- l Синхронізація списку установ — **spzIBInstitutionList** . 

- l Створення чи редагування списку установ — **spzIBInstitutionSave** . 

16 

## **Інтеграція із бухгалтерськими системами** 

## **Авторизація системи** 

- l Синхронізація списку виконавців — **spzIBUserDetails** . 

- l Синхронізація списку послуг — **spzIBServiceList** . 

- l Синхронізація прайсу послуг — **spzIBServicePriceList** . 

- l Синхронізація оплат — **spzIcpaymentDetails** . 

- l Синхронізація бронювань — **spzIcbookingDetails** . 

- l Синхронізація типів рахунків — **spzIBAccountTypeList** . 

- l Синхронізація рахунків пацієнтів — **spzIBAccountList** . 

- l Синхронізація тарифних планів — **spzIBTariffPlanList** . 

- l Синхронізація списку клінік — **spzIBCompanyDetails** . 

- l Синхронізація інфраструктури кабінетів клініки — **spzIBVenueDetails** . 

- l Синхронізація списку агентів — **spzIBAgentList** . 

- l Синхронізація списку установ — **spzIBInstitutionList** . 

- l Синхронізація довідника статусів гарантації — **spzIBGuarantorStatusDictionary** . 

- l Синхронізація довідника статусів візитів — **spzIBScheduleStatusDictionary** . 

- l Синхронізація довідника статусів призначень — **spzIBBookingStatusDictionary** . 

- l Синхронізація довідника статусів оплат призначень — **spzIBBookingPaymentStatusDictionary** . 

17 

## **Авторизація** 

Інтеграція з системою Doctor Eleks здійснюється через інтеграційну шину, яка підключена до сайту **portal-doctor.eleks.com** . Існує два способи виклику методу: без двофакторної авторизації та з двофакторною авторизацією. 

**Без двофакторної авторизації** . Результатом цього способу є **access_ token** , який треба передавати в наступні методи. 

## **Із двофакторною авторизацією** . Якщо ж для облікового запису 

ввімкнено двофакторну авторизацію, то на телефон користувача надійде sms-повідомлення з кодом. Авторизація у такому випадку виконується повторним викликом методу. На перший виклик без токену система поверне код помилки **TOKEN_REQUIRED** . 

У наступний виклик, окрім логіну, потрібно переслати токен (код із smsповідомлення). Пароль у такому випадку не обов’язковий, адже буде перевірка відповідності токена до логіну користувача. 

Method:Post 

Url:https://portal-doctor.eleks.com/api/token Headers: Content-Type: application/x-www-form-urlencoded Body: grant_type password username <user>@@@<installationGUID> password <password> token <token> 

- l **<user>** і **<password>** — логін і пароль облікового запису інтеграції. 

- l **<token>** — код із sms-повідомлення. 

- l **<installationGUID>** — унікальний ідентифікатор інсталяції, зареєстрованої на portal-doctor.eleks.com. 

Вхідні дані, які потрібні для авторизації (надаються окремо для кожної клініки): 

- l **Login** — логін користувача, під яким будуть запускатися методи. 

- l **Password** — пароль користувача. 

- l **InstallationGUID** — GUID клініки. 

Результатом виконання методу авторизації стане **access_token** , який потім необхідно буде передавати у всіх наступних запитах у секції **Authorization Bearer** : 

Url: https://portal-doctor.eleks.com/api/run Headers Content-Type application/json Authorization Bearer <token> 

У полі **Body** методів потрібно передавати відповідний JSON методу. 

18 

У всіх методах потрібно передавати обов’язковий параметр — **@Login** . Якщо цей параметр не відповідає тому, для якого було отримано токен, у відповідь повернеться помилка. Приклад запиту: 

- Приклад відповіді (отримання токену): 

**Access_token** , який було отримано у відповідь, потрібен для запуску всіх наступних методів. 

19 

## **Лікарі (персонал)** 

## **Метод spzIBUserDetails** 

Метод **spzIBUserDetails** призначений для виводу списку лікарів. Вхідні параметри: 

- l **@UserSpecialityCode** nvarchar(200) = null — фільтрування за головною спеціальністю лікаря (необов’язковий параметр); 

- l **@UserLogin** nvarchar(200) = null — фільтрування за логіном лікаря (необов’язковий параметр); 

- l **@Search** nvarchar(200) = null — фільтрування за ПІБ лікаря 

   - (необов’язковий параметр). Пошук співпадінь за введеним текстом; 

- l **@Login** nvarchar(200) — логін користувача, під яким запускається метод. 

Приклад виклику: 

{ "name": "spzIBUserDetails", "params": [ {"name": "Login","value": "{{Login}}"} ], "installationId": "{{InstallationGUID}}" } 

20 

У результаті на екрані з’являється розширена інформація про лікарів та послуги, які вони надають: 

- l **userLogin** — логін користувача; 

- l **userShortName** — ПІБ скорочено; 

- l **userName** — ПІБ повністю; 

- l **userSpecialityCode** — код спеціальності; 

- l **userSpecialityName** — назва спеціальності; 

- l **userEmail** — електронна скринька користувача; 

- l **userPhone** — номер телефону користувача; 

- l **userPhoto** — фото користувача; 

- l **userSpecialityList** — список додаткових спеціальностей користувача. 

21 

## **Метод spzIBUserDetailsExtended** 

Метод **spzIBUserDetailsExtended** призначений для виводу розширеної інформації про користувачів. Вхідні параметри: 

- l **@CompanyCity** nvarchar(200) = null — додатковий параметр (місто, у межах якого проводиться пошук); 

- l **@CompanyGUID** uniqueidentifier — додатковий параметр (компанія, для якої потрібно знайти лікаря); 

- l **@UserLogin** nvarchar(200) = null — додатковий параметр (логін лікаря, якого ви шукаєте); 

- l **@Search** nvarchar(200) = null — додатковий параметр (фільтрування за ПІБ лікаря, пошук збігів за введеним текстом); 

- l **@ServiceID** int = null — додатковий параметр (фільтрування за послугою); 

- l **@UserSpecialityCode** nvarchar(200) = null — додатковий параметр (спеціальність лікаря, за якою фільтруватимуться лікарі); 

- l **@Login** nvarchar(200) — логін користувача, під яким запущено метод. 

Приклад виклику: 

{ "name": "spzIBUserDetails", "params": [ {"name": "Login","value": "{{Login}}"} ], "installationId": "{{InstallationGUID}}" } 

У результаті на екрані з’являється розширена інформація про лікарів та послуги, які вони надають: 

- l **userLogin** — логін лікаря; 

- l **userShortName** — ПІБ скорочено; 

- l **userName** — ПІБ повністю; 

- l **userSpecialityCode** — код спеціальності; 

- l **userSpecialityName** — назва спеціальності; 

- l **serviceID** — ідентифікатор послуги, яку лікар надає; 

- l **serviceName** — назва послуги; 

- l **serviceDuration** — тривалість виконання послуги у хвилинах; 

- l **servicePriceRate** — вартість послуги у базовому тарифному плані; 

- l **userPhoto** — фото користувача; 

22 

## l **companyGUID** — GUID клініки; 

l **companyCity** — населений пункт, в якому розташована клініка. 

23 

## **Метод spzIBSpecialityDetails** 

Метод **spzIBSpecialityDetails** призначений для виводу спеціальностей. Вхідні параметри: 

- l **@UserSpecialityCode** nvarchar(200) = null — код спеціальності; 

- l **@UserSpecialityExternalID** nvarchar(200) = null — зовнішнійй ідентифікатор спеціальності; 

- l **@Search** nvarchar(200) = null — додатковий параметр (фільтрування за назвою спеціальності, пошук зібігів за введеним текстом) 

- l **@SpecialityisInternet** bit = null — спеціальності лікарів, які доступні для запису онлайн; 

- l **@Login** nvarchar(200) — логін користувача, під яким запущено метод. 

Приклад виклику: 

{ "name": "spzIBSpecialityDetails", "params": [ {"name": "Login","value": "{{Login}}"} ], "installationId": "{{InstallationGUID}}" } 

24 

У результаті на екрані з’являються спеціальності, їхній код, коротка та повна назви: 

- l **userSpecialityCode** — код спеціальності; 

- l **userSpecialityName** — назва спеціальності; 

- l **userSpecialityShortName** — коротка назва спеціальності; 

- l **userSpecialityExternalID** — зовнішній ідентифікатор спеціальності; 

- l **specialityisInternet** — біт, що вказує на те, що лікаря даної спеціальності доступні для запису онлайн. 

25 

## **Метод spzIBUserSpecialityUserDetails** 

Метод **spzIBUserSpecialityUserDetails** призначений для виводу списку додаткових спеціальностей лікаря. Вхідні параметри: 

- l **@UserLogin** nvarchar(200) = null — додатковий параметр (логін лікаря, якого ви шукаєте); 

- l **@UserSpecialityCode** nvarchar(200) = null — додатковий параметр (спеціальність лікаря, за якою ви фільтруєте лікарів); 

- l **@Login** nvarchar(200) — логін користувача, під яким запущено метод. 

Приклад виклику: 

{ "name": "spzIBUserSpecialityUserDetails", "params": [ {"name": "Login","value": "{{Login}}"} ], "installationId": "{{InstallationGUID}}" } 

26 

У результаті на екрані з’являється список додаткових спеціальностей лікаря: 

- l **userLogin** — логін лікаря; 

## l **userSpecialityCode** — код спеціальності. 

27 

## **Послуги** 

## **Метод spzIBServiceDetails** 

Метод **spzIBServiceDetails** призначений для виводу послуг та їхньої вартості у базовому тарифному плані. Вхідні параметри: 

- l **@Login** nvarchar(200) — логін користувача, під яким запущено метод; 

- l **@ServiceID** int = null — додатковий параметр (фільтрування за послугою); 

- l **@TariffPlanID** int = null — ідентифікатор тарифного плану; 

- l **@CompanyGUID** nvarchar(200) = null — ідентифікатор компанії. 

Приклад виклику: 

{ "name": "spzIBServiceDetails ", "params": [ {"name": "Login","value": "{{Login}}"} ], "installationId": "{{InstallationGUID}}" } 

28 

У результаті на екрані з’являється список послуг та їхня вартість у базовому тарифному плані: 

- l **serviceID** — ідентифікатор послуги; 

- l **serviceName** — назва послуги; 

- l **servicePriceRate** — вартість послуги у базовому тарифному плані; 

- l **serviceDesc** — опис послуги; 

- l **serviceCode** nvarchar(200) — код послуги; 

- l **tariffPlanID** — ідентифікатор тарифного плану. 

29 

## **Метод spzIBServiceUserDetails** 

Метод **spzIBServiceUserDetails** призначений для виводу списку послуг, які надають лікарі, та їхньої тривалості. Вхідні параметри: 

- l **@ServiceID** int = null — додатковий параметр (фільтрування за послугою); 

- l **@ServiceUserExternalID** nvarchar(255) = null — додатковий параметр (зовнішній ідентифікатор послуги користувача); 

- l **@UserLogin** nvarchar(200) = null — додатковий параметр (логін лікаря, якого ви шукаєте); 

- l **@Login** nvarchar(200) — логін користувача, під яким запущено метод. 

Приклад виклику: 

{ "name": "spzIBServiceUserDetails", "params": [ {"name": "Login","value": "{{Login}}"} ], "installationId": "{{InstallationGUID}}" } 

30 

У результаті на екрані з’являється список послуг, які надають лікарі, та їхня тривалість: 

- l **serviceID** — ідентифікатор послуги; 

- l **userLogin** — логін лікаря; 

- l **serviceDuration** — тривалість послуги у хвилинах; 

- l **serviceCode** nvarchar(200) — код послуги; 

- l **serviceUserExternalID** — зовнішній ідентифікатор послуги користувача. 

31 

## **Метод spzIBTariffPlanList** 

Метод **spzIBTariffPlanList** призначений для виводу всіх тарифних планів. Вхідні параметри: 

- l **@TariffPlanID** int — ідентифікатор тарифного плану; 

- l **@TariffPlanExternalID** nvarchar(200) = null — додатковий параметр (зовнішній ідентифікатор тарифного плану); 

- l **@Search** nvarchar(200) = null — додатковий параметр (фільтрування за назвою спеціальності). Пошук співпадінь за введеним текстом; 

- l **@CompanyGUID** uniqueidentifier — додатковий параметр (GUID компанії); 

- l **@Login** nvarchar(200) — логін користувача, під яким запущено метод. 

Приклад виклику: 

{ "name": "spzIBTariffPlanList", "params": [ {"name": "Login","value": "{{Login}}"} ], "installationId": "{{InstallationGUID}}" } 

32 

У результаті на екрані з’являються всі тарифні плани, якщо не задано компанію. Якщо компанію задано, то з’являються тарифні плани для вибраної компанії: 

- l **tariffPlanID** — ідентифікатор тарифного плану; 

- l **tariffPlanName** — назва тарифного плану; 

- l **tariffPlanMedicineIsDefault** — перевірка на наявність тарифного плану за замовчуванням; 

- l **tariffPlanDesc** — опис тарифного плану; 

- l **tariffPlanIsDisabled** — перевірка на наявність активного тарифного плану; 

- l **tariffPlanExternalID** — зовнішній ідентифікатор тарифного плану. 

33 

## **Метод spzIBBookingDocumentList** 

Метод **spzIBBookingDocumentList** призначений для виводу списку послуг та прив'язаних документів до послуг. Вхідні параметри: 

- l **@PatientID** int — ідентифікатор пацієнта (обов’язковий параметр); 

- l **@Login** nvarchar(200) — логін користувача, під яким запущено метод; 

- l **@DocumentID** int — ідентифікатор документа (необов'язковий параметр); 

- l **@BookingID** int — ідентифікатор букінга (необов'язковий параметр); 

- l **@PaymentStatus** — статус оплати букінга (необов'язковий параметр); 

- l **@StartDate** datetime — дата, від якої виводяться букінги (необов'язковий параметр); 

- l **@EndDate** datetime — дата, до якої виводяться букінги (необов'язковий параметр). 

Приклад виклику: 

{ "name": "spzIBBookingDocumentList", "params": [ {"name": "PatientID","value": "24"}, {"name": "DocumentID","value": "37234"}, {"name": "BookingID","value": "24"}, {"name": "PaymentStatus","value": "PAY"}, {"name": "StartDate","value": "2020-12-09T14:11:24.353"}, {"name": "EndDate","value": "2021-05-21 14:59:19.947"}, {"name": "Login","value": "{{Login}}"} ], "installationId": "{{InstallationGUID}}" } 

Вихідні параметри: 

- l **DocumentID** — ідентифікатор документа; 

- l **DocumentName** — назва документа; 

- l **DocumentCreationDate** — дата створення документа; 

- l **BookingID** — ідентифікатор букінга; 

- l **BookingName** — назва букінга; 

- l **BookingPaymentStatusName** — статус оплати; 

- l **BookingCreationDate** — дата створення букінга; 

- l **BookingCreationUserRef** — користувач, який створив букінг; 

34 

- l **BookingDate** — дата букінга; 

- l **GuarantorStatus** — статус гарантування. 

Приклад відповіді: 

{ "bookingDocumentList": [ { "documentID": 37234, "documentName": "Загальний аналіз сечі", "documentCreationDate": "2019-01-15T20:00:39.45", "bookingID": 106275, "bookingName": "Аналіз сечі мінімальний", "bookingPaymentStatusName": "Оплачено", "bookingCreationDate": "2019-01-15T14:56:57.193", "bookingCreationUserRef": "Doctor", "bookingDate": "2019-01-15T10:15:00", "guarantirStatus": "Requested" } 

35 

## **Розклад** 

## **Метод spzIBFreeSeanceDetails** 

Метод **spzIBFreeSeanceDetails** призначений для виводу списків вільних слотів відповідно до тривалості послуг або сеансів лікаря (якщо такі сеанси використовуються). Вхідні параметри: 

- l **@CompanyGUID** uniqueidentifier = null — ідентифікатор компанії, додатковий параметр; 

- l **@ServiceID** int — ідентифікатор послуги; 

- l **@DayCount** int — кількість днів, у діапазоні яких шукається розклад; 

- l **@UserLogin** nvarchar(200) — логін користувача, для якого шукаються вільні слоти; 

- l **@CompanyCity** nvarchar(200) = null — місто, додатковий параметр; 

- l **@Login** nvarchar(200) — логін користувача, під яким запущено метод. 

Приклад виклику: 

{ "name": "spzIBFreeSeanceDetails", "params": [ { "name": "ServiceID", "value": "205"}, { "name": "DayCount ", "value": "50"}, { "name": "UserLogin ", "value": "Miller"}, { "name": "Login", "value": "{{Login}}"} ], "installationId": "{{InstallationGUID}}" } 

36 

У результаті на екрані з’являються списки вільних слотів відповідно до тривалості послуг або сеансів лікаря (якщо такі сеанси використовуються): 

- l **сompanyGUID** — ідентифікатор компанії; 

- l **userLogin** — логін лікаря; 

- l **venueID** — ідентифікатор кабінету; 

- l **timeTableStartTime** — час початку прийому; 

- l **timeTableEndTime** — час завершення прийому. 

37 

## **Метод spzIBFreeDateDetails** 

Метод **spzIBFreeDateDetails** призначений для виводу дат, на які можна записатися на візит. 

Вхідні параметри: 

- l **@ServiceID** int — ідентифікатор послуги; 

- l **@DayCount** int — кількість днів (проміжок днів), серед яких шукаються вільні для запису дні; 

- l **@UserLogin** nvarchar(200) — логін лікаря, для якого шукаються вільні для запису дні; 

- l **@CompanyGUID** uniqueidentifier — додатковий параметрі (GUID клініки); 

- l **@UserSpecialityCode** nvarchar(200) — додатковий параметрі (код спеціальності); 

- l **@Login** nvarchar(200) — логін користувача, під яким запущено метод. 

Приклад виклику: 

{ "name": "spzIBFreeDateDetails", "params": [ {"name": "ServiceID","value": "205"}, {"name": "DayCount","value": "50"}, {"name": "UserLogin","value": "Miller"}, {"name": "CompanyGUID","value": "null"}, {"name": "UserSpecialityCode","value": "null"}, {"name": "Login","value": "{{Login}}"} ], "installationId": "{{InstallationGUID}}" } 

38 

- У результаті на екрані з’являються дати, на які можна записатися на візит: l **userLogin** — логін лікаря; 

- l **timeTableDate** — дата запису. 

39 

## **Інфраструктура** 

## **Метод spzIBCompanyDetails** 

Метод **spzIBCompanyDetails** призначений для виводу таких даних про клініку: GUID, назва, поле та тип клініки. Вхідні параметри: 

- l **@CompanyExternalID1** nvarchar(200) — зовнішній ідентифікатор компанії №1; 

- l **@CompanyExternalID2** nvarchar(200) — зовнішній ідентифікатор компанії №2; 

- l **@Login** nvarchar(200) — логін користувача, під яким запущено метод. 

Приклад виклику: 

{ "name": "spzIBCompanyDetails", "params": [ {"name": "Login","value": "{{Login}}"} ], "installationId": "{{InstallationGUID}}" } 

40 

У результаті на екрані з’являються такі дані про клініку: GUID, назва, поле та тип клініки: 

- l **companyGUID** — GUID клініки; 

- l **сompanyName** — назва клініки; 

- l **companyShortName** — коротка назва клініки; 

- l **companyAddress** — адреса клініки; 

- l **companyEmail** — електронна пошта клініки; 

- l **companyPhone** — номер телефону клініки; 

- l **companyExternalID1** — зовнішній ідентифікатор компанії №1; 

- l **companyExternalID2** — зовнішній ідентифікатор компанії №2. 

41 

## **Метод spzIBVenueDetails** 

Метод **spzIBVenueDetails** призначений для виводу кабінетів (на які створений розклад у лікарів), доступних на запис із вебу. Вхідні параметри: 

- l **@VenueExternalID** nvarchar(200) — зовнішній ідентифікатор кабінета; 

- l **@Login** nvarchar(200) — логін користувача, під яким запущено метод. 

Приклад виклику: 

{ "name": "spzIBVenueDetails", "params": [ {"name": "Login","value": "{{Login}}"} ], "installationId": "{{InstallationGUID}}" } 

42 

У результаті на екрані з’являються кабінети (на які створений розклад у лікарів), доступних на запис із вебу: 

- l **venueID** — ідентифікатор кабінету; 

- l **venueName** — назва кабінету; 

- l **companyGUID** — GUID клініки; 

- l **companyName** — назва клініки; 

- l **companyAddress** — адреса клініки; 

- l **companyCity** — населений пункт, в якому розташована клініка; 

- l **venueExternalID** — зовнішній ідентифікатор кабінета. 

43 

## **Метод spzIBAgentList** 

Метод **spzIBAgentList** призначений для виводу інформації про агентів. Вхідні параметри: 

- l **@AgentID** int — ідентифікатор агента (додатковий параметр); 

- l **@InstitutionID** int — ідентифікатор установи (додатковий параметр); 

- l **@AgentExternalID1** int — зовнішній ідентифікатор агента №1 (додатковий параметр); 

- l **@AgentExternalID2** int — зовнішній ідентифікатор агента №2 (додатковий параметр); 

- l **@Login** nvarchar(200) — логін користувача, під яким запускається метод. 

Приклад запиту: 

{ "name": "spzIBAgentList", "params": [ {"name": "AgentID","value": "15"}, {"name": "Login","value": "{{Login}}"} ], "installationId": "{{InstallationGUID}}" } 

44 

У результаті отримуємо інформацію про агентів: 

- l **AgentID** — ідентифікатор агента; 

- l **AgentName** — ПІБ агента; 

- l **AgentDesc** — опис агента; 

- l **AgentPhone** — номер телефону агента; 

- l **AgentAddress** — адреса агента; 

- l **AgentEmail** — електронна пошта агента; 

- l **InstitutionID** — ідентифікатор установи; 

- l **AgentExternalID1** — зовнішній ідентифікатор агента №1; 

- l **AgentExternalID2** — зовнішній ідентифікатор агента №2. 

45 

## **Метод spzIBAgentSave** 

Метод **spzIBAgentSave** призначений для створення або редагування агентів. Якщо вказати ідентифікатор агента, то відбудеться редагування даних вибраного агента, а якщо ідентифікатор агента не вказувати, то створимо нового агента. Вхідні параметри: 

- l **@AgentID** int — ідентифікатор агента; 

- l **@AgentName** nvarchar(200) — ПІБ агента; 

- l **@AgentDesc** nvarchar(max) — опис агента (додатковий параметр); 

- l **@AgentPhone** nvarchar(255) — номер телефону агента (додатковий параметр); 

- l **@AgentAddress** nvarchar(max) — адреса агента (додатковий параметр); 

- l **@AgentEmail** nvarchar(255) — електронна пошта агента (додатковий параметр); 

- l **@InstitutionID** int — ідентифікатор установи (додатковий параметр); 

- l **@AgentExternalID1** int — зовнішній ідентифікатор агента №1 (додатковий параметр); 

- l **@AgentExternalID2** int — зовнішній ідентифікатор агента №2 (додатковий параметр); 

- l **@Login** nvarchar(max) — логін користувача, під яким запускається метод. 

Приклад запиту: 

{ "name": "spzIBAgentSave", "params": [ {"name": "AgentID","value": "8"}, {"name": "AgentName","value": "Петренко Сергій Анатолійович"}, {"name": "AgentDesc","value": "Опис тестового агента"}, {"name": "AgentPhone","value": "380678012345"}, {"name": "AgentAddress","value": "Місто Львів"}, {"name": "AgentEmail","value": "test.agent@gmail.com"}, {"name": "Login","value": "{{login}}"} ], "installationId": "{{InstallationGUID}}" } 

46 

У результаті отримаємо ідентифікатор новоствореного або наявного агента. 

47 

## **Метод spzIBInstitutionList** 

Метод **spzIBInstitutionList** призначений для виводу списку установ та інформації про них. 

Вхідні параметри: 

- l **@InstitutionID** int — ідентифікатор установи (додатковий параметр); 

- l **@InstitutionExternalID1** int — зовнішній ідентифікатор установи (додатковий параметр); 

- l **@Login** nvarchar(200) — логін користувача, під яким запускається метод. 

Приклад запиту: 

{ "name": "spzIBInstitutionList", "params": [ {"name": "InstitutionID","value": "10"}, {"name": "Login","value": "{{Login}}"} ], "installationId": "{{InstallationGUID}}" } 

48 

У результаті отримуємо список установ та інформацію по них: 

- l **InstitutionID** — ідентифікатор установи; 

- l **InstitutionCode** — код установи; 

- l **InstitutionName** — назва установи; 

- l **InstitutionAddress** — адреса установи; 

- l **InstitutionPhone1** — контактний номер установи; 

- l **InstitutionEmail** — електронна пошта установи; 

- l **InstitutionAccountNumber** — номер рахунку установи; 

- l **InstitutionTaxNumber** — податковий номер установи; 

- l **InstitutionKindCode** — код виду установи; 

- l **InstitutionExternalID1** — зовнішній ідентифікатор установи; 

- l **AgentList** — список агентів, які належать до установи. 

49 

## **Метод spzIBCompanyList** 

Метод **spzIBCompanyList** призначений для виведення списку клінік. Вхідні параметри: 

- l **@CompanyExternalID1** nvarchar(200) — зовнішній ідентифікатор компанії №1; 

- l **@CompanyExternalID2** nvarchar(200) — зовнішній ідентифікатор компанії №2; 

- l **@Login** nvarchar(200) — логін користувача, під яким запущено метод. 

- Приклад виклику: 

{ "name": "spzIBCompanyList", "params": [ { "name": "Login", "value": "{{Login}}" } ], "installationId": "{{InstallationGUID}}" } 

Вихідні параметри: 

- l **companyGUID** — GUID клініки; 

- l **сompanyName** — назва клініки; 

- l **companyShortName** — коротка назва клініки; 

- l **companyAddress** — адреса клініки; 

- l **companyEmail** — електронна пошта клініки; 

- l **companyPhone** — номер телефону клініки; 

- l **companyExternalID1** — зовнішній ідентифікатор компанії №1; 

- l **companyExternalID2** — зовнішній ідентифікатор компанії №2. 

50 

У результаті на екрані з’являється список клінік. 

51 

## **Метод spzIBInstitutionSave** 

Метод **spzIBInstitutionSave** призначений для створення або редагування установи, в залежності від того чи вказаний ідентифікатор установи. Якщо ідентифікатор установи вказаний, то відбувається редагування установи. Якщо ідентифікатор установи не вказаний, то створюється нова установа. Вхідні параметри: 

- l **@InstitutionID** int — Ідентифікатор установи; 

- l **@InstitutionCode** nvarchar(200) — код установи; 

- l **@InstitutionName** nvarchar(200) — назва установи; 

- l **@InstitutionAddress** nvarchar(max) — адреса установи (додатковий параметр); 

- l **@InstitutionPhone1** nvarchar(200) — контактний номер установи; 

- l **@InstitutionEmail** nvarchar(max) — електронна пошта установи; 

- l **@InstitutionAccountNumber** nvarchar(200) — номер рахунку установи (додатковий параметр); 

- l **@InstitutionKindRef** nvarchar(200) — вид установи (додатковий параметр); 

- l **@InstitutionExternalID1** int — зовнішній ідентифікатор установи (додатковий параметр); 

- l **@Login** nvarchar(200) — логін користувача, під яким запускається метод. 

Приклад запиту: 

{ "name": "spzIBInstitutionSave", "params": [ {"name": "InstitutionID","value": "10"}, {"name": "InstitutionCode","value": "1234"}, {"name": "InstitutionName","value": "Тестована назва"}, {"name": "InstitutionAddress","value": "Україна, Львівська область, львів"}, {"name": "InstitutionPhone1","value": "380673412321"}, {"name": "InstitutionEmail","value": "test.institution@gmail.com"}, {"name": "InstitutionAccountNumber","value": "12345678"}, {"name": "Login","value": "{{login}}"} ], "installationId": "{{InstallationGUID}}" } 

52 

У результаті отримаємо ідентифікатор новоствореної або наявної установи. 

53 

## **Пацієнти** 

## **Метод spzIBScheduleCreate** 

Метод **spzIBScheduleCreate** призначений для створення візиту. Вхідні параметри: 

- l **@PatientID** int 

- l **@ServiceID** int 

- l **@VenueID** int 

- l **@UserLogin** nvarchar(200) 

- l **@StartTime** datetime 

- l **@EndTime** datetime 

- l **@AccountID** int = null — додатковий параметр; 

- l **@FranchisePercent** decimal (20, 10) = null — додатковий параметр; 

- l **@ScheduleDesc** nvarchcra(max) — опис візиту (додатковий параметр); 

- l **@ScheduleDublicatesRulesIsDisabled** — за замовчуванням = **0** , метод перевіряє зайняті слоти (додатковий параметр). 

   - Якщо **@ScheduleDublicatesRulesIsDisabled** = **1** , то метод ігнорує дублювання слотів. 

- l **@Login** nvarchar(200) — логін користувача, під яким запущено метод. 

Приклад виклику: 

{ "name": "spzIBScheduleCreate", "params": [ {"name": "PatientID","value": "24"}, {"name": "ServiceID","value": "205"}, {"name": "VenueID","value": "155"}, {"name": "UserLogin","value": "Miller"}, {"name": "StartTime","value": "2022-09-12T10:00:00.000"}, {"name": "EndTime","value": "2022-09-12T10:30:00.000"}, {"name": "Login","value": "{{Login}}"} ], "installationId": "{{InstallationGUID}}" } 

54 

У результаті на екрані з’являється створений візит. У відповідь отримуємо ідентифікатор створеного візиту: 

- l **scheduleID** — ідентифікатор створеного візиту. 

55 

## **Метод spiAPIReservationCreate** 

Метод **spiAPIReservationCreate** призначений для створення резервації. Вхідні параметри: 

- l **@PatientID int** 

- l **@ServiceID int** 

- l **@VenueID int** 

- l **@UserLogin** nvarchar(200) 

- l **@StartTime** datetime 

- l **@EndTime** datetime 

- l **@AccountID** int = null — додатковий параметр 

- l **@FranchisePercent** decimal (20, 10) = null — додатковий параметр 

- l **@Login** nvarchar(200) — логін користувача, під яким запущено метод. 

Приклад виклику: 

{ "name": "spiAPIReservationCreate ", "params": [ {"name": "PatientID","value": "24"} , {"name": "VenueID","value": "155"} , {"name": "UserLogin","value": "Miller"} , {"name": "StartTime","value": "2022-09-12T10:00:00.000"} , {"name": "EndTime","value": "2022-09-12T10:30:00.000"} , {"name": "Login","value": "Login"} ], "installationId": "{{InstallationGUID}}" } 

56 

У результаті на екрані з’являється створений візит (зі статусом **Зарезервовано** ). У відповідь отримуємо ідентифікатор створеного візиту: 

l **scheduleID** — ідентифікатор створеного візиту. 

57 

## **Метод spzIBScheduleCancel** 

Метод **spzIBScheduleCancel** призначений для скасування візиту. Вхідні параметри: 

- l **@ScheduleID** int — обов’язковий параметр (ідентифікатор візиту); 

- l **@Login** nvarchar(200) — логін користувача, під яким запущено метод. 

Приклад виклику: 

{ "name": "spzIBScheduleCancel", "params": [ {"name": "ScheduleID ","value": "65809"}, {"name": "Login","value": "{{Login}}"} ], "installationId": "{{InstallationGUID}}" } 

У результаті на екрані з’являються дані про скасування візиту. З'являється статус **200 ОК** . 

58 

## **Метод spzIBScheduleBookingDetails** 

Метод **spzIBScheduleBookingDetails** призначений для виводу даних про візит та бронювання, які були додані до цього візиту. Вхідні параметри: 

- l **@ScheduleID** int — ID візиту; 

- l **@BookingID** int — ID бронювання; 

- l **@Login** nvarchar(200) — логін користувача, під яким запущено метод. 

Приклад виклику: 

{ "name": "spzIBScheduleBookingDetails", "params": [ {"name": "ScheduleID","value": "65050"}, {"name": "Login","value": "{{Login}}"} ], "installationId": "{{InstallationGUID}}" } 

У результаті на екрані з’являються дані про візит та бронювання, які були додані до цього візиту: 

- l **scheduleID** — ідентифікатор візиту; 

- l **scheduleStartTime** — дата і час початку візиту; 

- l **scheduleEndTime** — дата і час закінчення візиту; 

- l **scheduleStatusCode** — код статусу візиту; 

- l **bookingID** — ідентифікатор послуги; 

- l **bookingGuarantorStatusCode** — код статусу гарантування; 

- l **bookingFranchisePercent** — відсоток франшизи; 

- l **bookingPaymentValue** — вартість оплати; 

- l **serviceID** — ідентифікатор послуги; 

- l **bookingStatusCode** — код статусу послуги; 

- l **bookingServicePriceRate** — вартість послуги; 

- l **bookingServicePriceValue** — вартість послуги; 

- l **bookingPaymentStatusCode** — код статусу оплати; 

- l **scheduleExternalID** — зовнішній ідентифікатор візиту; 

- l **scheduleDesc** — опис візиту; 

- l **patientID** — ідентифікатор пацієнта; 

- l **userLogin** — логін лікаря, який виконує візит; 

- l **venueID** — ідентифікатор кабінета; 

59 

- l **bookingName** — назва послуги (бронювання); 

- l **bookingDate** — дата та час бронювання; 

- l **bookingQuantity** — кількість бронювань; 

- l **bookingStatusCode** — код статусу бронювання; 

- l **bookingGuarantorID** — ідентифікатор гарантувальника бронювання; 

- l **bookingAgentID** — ідентифікатор агента; 

- l **bookingExecutionUserLogin** — логін користувача, який виконує бронювання; 

- l **bookingExecutionVenueID** — ідентифікатор кабінету, в якому відбудеться надання послуги; 

- l **accountID** — ідентифікатор рахунку; 

- l **accountTypeID** — ідентифікатор типу рахунку; 

- l **bookingExternalID1** — зовнішній ідентифікатор послуги (бронювання) №1; 

- l **bookingExternalID2** — зовнішній ідентифікатор послуги (бронювання) №2; 

- l **serviceCode** nvarchar(200) — код послуги. 

60 

## **Метод spzIBPatientCreate** 

Метод **spzIBPatientCreate** призначений для виводу ID новоствореного або вже наявного пацієнта, за умови, що була ідентифікація за допомогою введених даних. 

Вхідні параметри: 

- l **@PatientName1** nvarchar(max) = null 

- l **@PatientName2** nvarchar(max) = null 

- l **@PatientName3** nvarchar(max) = null 

- l **@PatientBirthDate** datetime 

- l **@PatientSexCode** nvarchar(max) = N'MAL' 

- l **@PatientIsTwin** 

- l **@PatientPhone** nvarchar(max) 

- l **@PatientEmail** nvarchar(max) = N'' 

- l **@PatientAddress** nvarchar(max) = null — додатковий параметр; 

- l **@PatientIsVIP** bit = null — додатковий параметр; 

- l **@Login** nvarchar(200) — логін користувача, під яким запущено метод. 

Приклад виклику: 

{ "name": "spzIBPatientCreate", "params": [ {"name": "PatientName1","value": "Тестовий"}, {"name": "PatientName2","value": "Василь"}, {"name": "PatientName3","value": "Миколайович"}, {"name": "PatientBirthDate","value": "2000-01-01T00:00:00.000"}, {"name": "PatientSexCode","value": "MAL"}, {"name": "PatientPhone","value": "380678008023"}, {"name": "PatientEmail","value": "vasyl.andrusiak@eleks.com"}, {"name": "PatientAddress","value": "м. Івано-Франківськ"}, {"name": "Login","value": "{{Login}}"} ], "installationId": "{{InstallationGUID}}" } 

61 

У результаті на екрані з’являється ID новоствореного або вже наявного пацієнта, за умови, що ідентифікація була за допомогою введених даних. 

62 

## **Метод spzIBDocumentList** 

Метод **spzIBDocumentList** призначений для виводу списків документів пацієнта. Списки посортовані від нових до старих. Вхідні параметри: 

- l **@PatientID** int — ідентифікатор пацієнта 

- l **@DocumentKindCode** nvarchar(3) = null — код типу документа; 

- l **@DocumentID** int = null — ID конкретного документа; 

- l **@DocumentExternalID** nvarchar(200) — зовнішній ідентифікатор документа; 

- l **@StartDate** datetime — дата, від якої виводяться документи; 

- l **@EndDate** nvarchar(200) — дата, до якої виводяться документи; 

- l **@Login** nvarchar(200) — логін користувача, під яким запущено метод; 

Приклад виклику: 

{ "name": "spzIBDocumentList", "params": [ {"name": "PatientRef","value": "24"}, {"name": "StartDate","value": "2020-12-09T14:11:24.353"}, {"name": "EndDate","value": "2021-05-21 14:59:19.947"}, {"name": "Login","value": "{{Login}}"} ], "installationId": "{{InstallationGUID}}" } 

У результаті на екрані з’являються списки документів пацієнта, посортовані від нових до старих. 

З’являються такі поля: 

- l **DocumentID** — ідентифікатор документа; 

- l **DocumentCreationDate** — дата створення документа; 

- l **DocumentUserLogin** — користувач, який створив документ; 

- l **ScheduleID** — ідентифікатор візиту, якщо такий прикріплений до документа; 

- l **DocumentTemplateID** — ідентифікатор шаблону документа; 

- l **DocumentTemplateName** — назва шаблону документа; 

- l **DocumentKindCode** — код виду документа; 

- l **DocumentKindName** — назва виду документа; 

- l **DocumentExternalID** — зовнішній ідентифікатор документу; 

- l **DocumentAttachmentList** — JSON посилання на прикріплені документи та назви цих документів; 

63 

l **DocumentApproveStatusCode** — код статусу затвердження документу; l **DocumentApproveStatusName** — назва статусу затвердження. 

64 

## **Метод spzIBScheduleList** 

Метод **spzIBScheduleList** призначений для виводу списків візитів пацієнта у діапазоні заданих дат. Вхідні параметри: 

- l **@PatientID** int — ідентифікатор пацієнта; 

- l **@StartDate** datetime — дата, від якої виводяться візити; 

- l **@EndDate** nvarchar(200) — дата, до якої виводяться візити; 

- l **@ScheduleID** int — ідентифікатор візиту; 

- l **@ScheduleExternalID** nvarchar(4000) — зовнішній ідентифікатор візиту; 

- l **@Login** nvarchar(200) — логін користувача, під яким запущено метод. 

Приклад виклику: 

{ "name": "spzIBScheduleList", "params": [ {"name": "PatientID","value": "24"}, {"name": "StartDate","value": "2022-05-02T00:00:00.000"}, {"name": "EndDate","value": "2022-09-22T00:00:00.000"}, {"name": "Login","value": "{{Login}}"} ], "installationId": "{{InstallationGUID}}" } 

65 

У результаті на екрані з’являються списки візитів пацієнта у діапазоні заданих дат: 

- l **scheduleID** — ідентифікатор візиту; 

- l **scheduleStartTime** — дата і час початку візиту; 

- l **scheduleEndTime** — дата і час завершення візиту; 

- l **scheduleStatusCode** — код статусу візиту; 

- l **scheduleCreationDate** — дата і час створення візиту; 

- l **scheduleCreationUser** — логін користувача, який створив візит; 

- l **userLogin** — логін лікаря; 

- l **venueID** — ідентифікатор кабінету; 

- l **serviceCode** nvarchar(200) — код послуги; 

- l **scheduleExternalID** — зовнішній ідентифікатор візиту; 

- l **сompanyAddress** — адреса відділення / клініки. 

66 

## **Метод spzIBCommunicationCreate** 

Метод **spzIBCommunicationCreate** призначений для створення комунікації та отримання ідентифікатора створеної комунікації. Вхідні параметри: 

- l **@PatientID** int — ідентифікатор пацієнта; 

- l **@Text** nvarchar(200) — текст повідомлення; 

- l **@Type** nvarchar(200) = null — тип комунікації; 

- l **@Login** nvarchar(200) — логін користувача, під яким запущено метод. 

Приклад виклику: 

{ "name": "spzIBCommunicationCreate", "params": [ {"name": "PatientRef","value": "24"}, {"name": "Text","value": "Створення комунікації через API метод"}, {"name": "Type ","value": "MSG"}, {"name": "Login","value": "{{Login}}"} ], "installationId": "{{InstallationGUID}}" 

У результаті на екрані з’являється створена комунікація та ідентифікатор створеної комунікації — **communicationID** . 

67 

## **Метод spzIBPatientSearch** 

Метод **spzIBPatientSearch** призначений для пошуку пацієнта за допомогою введених ПІБ та номера телефону. Вхідні параметри: 

- l **@PatientID** int — ідентифікатор пацієнта; 

- l **@PatientExternalID1** nvarchar(255) — зовнішній ідентифікатор пацієнта №1; 

- l **@PatientExternalID2** nvarchar(255) — зовнішній ідентифікатор пацієнта №2; 

- l **@PatientName** nvarchar(200) — ПІБ пацієнта; 

- l **@PatientPhone** nvarchar(200) — номер телефону пацієнта; 

- l **@StartDate** ; 

- l **@EndDate** ; 

- l **@Login** nvarchar(200) — логін користувача, під яким запущено метод. 

Приклад виклику: 

{ "name": "spzIBPatientSearch", "params": [ {"name": "PatientName","value": "Demo Maria"}, {"name": "PatientPhone","value": "380983418373"}, {"name": "Login","value": "{{Login}}"} ], "installationId": "{{InstallationGUID}}" } 

68 

## У результаті на екрані з’являється потрібний пацієнт: 

- l **patientID** — ідентифікатор пацієнта; 

- l **patientName** — ПІБ пацієнта; 

- l **patientBirthDate** — дата народження; 

- l **patientSexCode** — стать пацієнта; 

- l **patientAddress** — адреса пацієнта; 

- l **patientIsVIP** — тип пацієнта; 

- l **patientEmail** — електронна пошта пацієнта; 

- l **patientPhone** — номер телефону; 

- l **patientDefaultAccountID** — рахунок пацієнта за замовчуванням; 

- l **patientSMSPhone** — номер телефону для sms-повідомлень; 

- l **patientPhoto** — фото пацієнта; 

- l **patientExternalID1** — зовнішній ідентифікатор пацієнта №1; 

- l **patientExternalID2** — зовнішній ідентифікатор пацієнта №2; 

- l **рatientNote** — нотатка пацієнта. 

69 

## **Метод spzIBAccountList** 

Метод **spzIBAccountList** призначений для виводу списку рахунків пацієнтів. 

Вхідні параметри: 

- l **@AccountID** int — ідентифікатор рахунку; 

- l **@PatientID** int — ідентифікатор пацієнта; 

- l **@AccountNumber** nvarchar(200) — номер рахунку; 

- l **@Login** nvarchar(200) — логін користувача, під яким запускається метод. 

Приклад запиту: 

{ "name": "spzIBAccountList", "params": [ {"name": "PatientID","value": "244"} {"name": "Login","value": "{{Login}}"} ], "installationId": "{{InstallationGUID}}" } 

У результаті отримуємо інформацію список рахунків пацієнтів та інформацію по них, а саме такі поля: 

- l **AccountID** — ідентифікатор рахунку пацієнта; 

- l **AccountValue** — баланс на рахунку; 

- l **AccountStartDate** — дата та час початку рахунку; 

- l **AccountEndDate** — дата та час закінчення рахунку; 

- l **AccountIsActive** — бітове поле, яке показує, чи рахунок активний; 

- l **CurrencyCode** — код валюти; 

- l **AccountTypeID** — ідентифікатор типу рахунку; 

- l **AccountDesc** — опис рахунку; 

- l **AccountFranchiseDesc** — опис франшизи; 

- l **AccountFranchiseValue** — значення франшизи; 

- l **AccountTypeName** — назва типу рахунку; 

- l **AccountTypeIsActive** — бітове поле, яке показує, чи тип рахунку активний; 

- l **AccountTypeDebtIsIndicated** — чи вказаний борг типу рахунку; 

- l **AccountTypeIsAutopaid** — відображає, чи тип рахунку є автооплачуваний; 

70 

## l **AccountTypeShortName** — коротка назва типу рахунку. 

71 

## **Метод spzIBAccountTypeList** 

Метод **spzIBAccountTypeList** призначений для виводу списку типу рахунків. У методі підтримується можливість фільтрування за установою або пошук за конкретним ідентифікатором типу рахунку або зовнішнім ідентифікатором типу рахунку. 

Вхідні параметри: 

- l **@AccountTypeID** int — ідентифікатор типу рахунку; 

- l **@AccountTypeExternalID** int — зовнішній ідентифікатор типу рахунку; 

- l **@AccountTypeInstitutionID** int — ідентифікатор установи; 

- l **@Login** nvarchar(200) — логін користувача, під яким запускається метод. 

Приклад запиту: 

{ "name": "spzIBAccountTypeList", "params": [ {"name": "Login","value": "{{Login}}"} ], "installationId": "{{InstallationGUID}}" } 

72 

У результаті отримуємо інформацію список типу рахунків та інформацію по них, а саме такі поля: 

- l **AccountTypeID** — ідентифікатор типу рахунку; 

- l **AccountTypeName** — назва типу рахунку; 

- l **AccountTypeDesc** — опис типу рахунку; 

- l **AccountTypeCurrencyCode** — код валюти; 

- l **AccountTypeCompanyGUID** — GUID компанії (установи); 

- l **AccountTypeInstitutionID** — ідентифікатор установи (закладу); 

- l **AccountTypeExternalID** — зовнішній ідентифікатор типу рахунку; 

- l **AccountTypeGuarantorIsRequired** — чи тип рахунку є страховим (1 – так, 0 – ні); 

- l **AccountTypeShortName** — коротка назва типу рахунку; 

- l **AccountTypeTarrifPlanList** — список тарифних планів типу рахунку; 

- l **InstitutionList** — список установ тарифного плану. 

73 

## **Метод spzIBTaskCreate** 

Метод **spzIBTaskCreate** призначений для виведення ідентифікатора 

завдання. 

Тип запиту — **POST** . 

Вхідні параметри: 

- l **Login** nvarchar(200) — логін інтеграції, під яким здійснено авторизацію (обов'язковий параметр); 

- l **TaskStartDate** datetime = null — дата початку виконання завдання (додатковий параметр); 

- l **TaskDesc** nvarchar (max) — опис завдання (додатковий параметр); 

- l **TaskName** nvarchar(200) — назва завдання (додатковий параметр). 

Вихідний параметр: 

- l **TaskID** — ідентифікатор завдання. 

У результаті на екрані з’являється ідентифікатор завдання. 

74 

## **Метод spzIBPatientSave** 

Метод **Метод spzIBPatientSave** призначений для виведення ідентифікатора нового або зміненого пацієнта. Тип запиту — **POST** . Вхідні параметри: 

- l **PatientID** int — ідентифікатор пацієнта; 

- l **PatientName1** nvarchar(200) — прізвище пацієнта; 

- l **PatientName2** nvarchar(200) — ім'я пацієнта; 

- l **PatientName3** nvarchar(200) — по батькові пацієнта; 

- l **PatientBirthDate** datetime — дата народження пацієнта; 

- l **PatientSexCode** nvarchar(3) — код статі ( **MAL** або **FEM** ); 

- l **PatientPhone** nvarchar(200) — номер телефону пацієнта; 

- l **PatientEmail** nvarchar(max) — електронна пошта пацієнта; 

- l **PatientAddress** nvarchar(max) — адреса проживання пацієнта; 

- l **PatientIsTwin** bit — наявність близнюка; 

- l **PatientExternalID1** nvarchar(255) — зовнішній ідентифікатор пацієнта №1; 

- l **PatientExternalID2** nvarchar(max) — зовнішній ідентифікатор пацієнта №2. 

Приклад body запиту: 

{ "name": "spzIBPatientSave", "params": [ { "name": "PatientName1", "value": "Тестовий" }, { "name": "PatientName2", "value": "Василь" }, { "name": "PatientName3", "value": "Миколайович" }, { "name": "PatientBirthDate", "value": "2000-01-01T00:00:00.000" }, { "name": "PatientSexCode", "value": "MAL" 

75 

}, { "name": "PatientPhone", "value": "380672345123" }, { "name": "PatientEmail", "value": "test.test@eleks.com" }, { "name": "PatientAddress", "value": "м. Івано-Франківськ" }, { "name": "Login", "value": "{{Login}}" } ], "installationId": "{{InstallationGUID}}" } Вихідний параметр: 

l **patientID** — ідентифікатор нового або зміненого пацієнта. Приклад відповіді: 

{ "incrementSequence": [ { "sequenceID": 2, "sequenceName": "Patient History Number", "sequenceCurrentNumber": 503, "sequenceCurrentNumberString": "2023.000503", "sequenceResetString": "2023", "sequenceNumberStringPattern": "SRS.06SN" } ], "patient": [ { "patientID": 1268 } ] } 

76 

У результаті на екрані з’являється ідентифікатор нового або зміненого пацієнта. 

77 

## **Метод spiIBUTMSave** 

Метод **spiIBUTMSave** призначений для виведення ідентифікатора нової мітки. 

Тип запиту — **POST** . 

Вхідні параметри: 

- l **UTMXml** int — xml із інформацією про мітку; 

- l **Login** nvarchar(200) — логін користувача, під яким виконується метод (обов'язковий параметр). 

Вихідний параметр: 

- l **UTMID** — ідентифікатор нової мітки. 

У результаті на екрані з’являється ідентифікатор нової мітки. 

78 

## **Метод spzIBPatientIDByLogin** 

Метод **spzIBPatientIDByLogin** призначений для виводу ID пацієнта за його логіном. 

Вхідний параметр: 

l **@Login** nvarchar(200) — логін користувача, під яким запущено метод. Приклад виклику: 

{ "name": "spzIBPatientIDByLogin", "params": [ { "name": "Login", "value": "{{Login}}" } ], "installationId": "{{InstallationGUID}}" } Вихідний параметр: l **PatientID** int — ідентифікатор пацієнта. Приклад відповіді: { "patientList": [ { "patientID": 24 } ] } 

79 

## **та оплати Послуги** 

## **Метод splСpaymentDetails** 

Метод **splСpaymentDetails** призначений для отримання даних про оплати. 

Вхідні параметри: 

- l **@StartTime** datetime 

- l **@EndTime** datetime 

- l **@Login** nvarchar(200) — логін користувача, під яким запущено метод. 

Приклад виклику: 

{ "name": "sp1СPaymentDetails", "params": [ {"name": "Login", "value": "{{Login}}"}, {"name": "StartDate","value": "2022-05-02T00:00:00.000"}, {"name": "EndDate","value": "2022-09-22T00:00:00.000"} ], "installationId": "{{InstallationGUID}}" } 

У результаті на екрані з’являються необхідні дані про оплати. 

80 

## **Метод sp1CbookingDetails** 

Метод **sp1CbookingDetails** призначений для отримання даних про послуги, які було назначено. 

Вхідні параметри: 

- l **@StartTime** datetime. 

- l **@EndTime** datetime. 

- l **@Login** nvarchar(200) — логін користувача, під яким запущено метод. 

Приклад виклику: 

{ "name": "sp1CBookingDetails", "params": [ {"name": "Login","value": "{{Login}}"} {"name": "StartDate","value": "2022-05-02T00:00:00.000"}, {"name": "EndDate","value": "2022-09-22T00:00:00.000"} ], "installationId": "{{InstallationGUID}}" } 

У результаті на екрані з’являються дані про послуги, які було призначено: 

- l **BookingID** — ідентифікатор бронювання (послуги); 

- l **BookingName** — назва бронювання (послуги); 

- l **PatientRef** — ідентифікатор пацієнта; 

- l **ServiceRef** — ідентифікатор послуги; 

- l **ServiceCode** — код послуги; 

- l **ServicePriceRef** — ідентифікатор вартості послуги; 

- l **BookingServicePriceRate** — ставка вартості послуги; 

- l **BookingServicePriceValue** — вартість послуги; 

- l **BookingQuantity** — кількість бронювань (послуг); 

- l **BookingStatusRef** — код статусу бронювань (послуг); 

- l **BookingPaymentStatusRef** — код статусу оплати бронювання (послуги); 

- l **BookingDiscountPercent** — відсоток знижки; 

- l **BookingDiscountValue** — знижка; 

- l **BookingPaymentValue** — сума оплати; 

- l **BookingDate** — дата бронювання (послуги); 

- l **AccountRef** — ідентифікатор рахунку; 

- l **AccountTypeID** — ідентифікатор типу рахунку; 

- l **AccountTypeName** — назва типу рахунку; 

81 

- l **BookingServiceAmount** — кількість послуг; 

- l **BookingGuarantorRef** — ідентифікатор гарантора; 

- l **BookingGuarantorStatusRef** — код статусу гарантування; 

- l **BookingExecutionUserRef** — логін виконавця бронювання (послуги); 

- l **BookingExecutionVenueRef** — ідентифікатор бронювання (послуги); 

- l **BookingActualPaymentValue** — фактична сума оплати; 

- l **BookingGuarantorPriceValue** — сума гарантування; 

- l **BookingMoneybackValue** — сума повернення оплати. 

82 

## **Метод spzIBBookingList** 

Метод **spzIBBookingList** призначений для виводу списку бронювань. Цей метод має можливість фільтрування бронювань по ідентифікатору пацієнта, по конкретних ідентифікаторах візиту або бронювання та фільтрування по зовнішніх ідентифікаторах бронювання. Вхідні параметри: 

- l **@PatientID** int — ідентифікатор пацієнта (додатковий параметр); 

- l **@ScheduleID** int — ідентифікатор візиту (додатковий параметр); 

- l **@BookingID** int — ідентифікатор бронювання (додатковий параметр); 

- l **@BookingExternalID1** nvarchar(255) — зовнішній ідентифікатор бронювання №1 (додатковий параметр); 

- l **@BookingExternalID2** nvarchar(max) — зовнішній ідентифікатор бронювання №2 (додатковий параметр); 

- l **@StartDate** datetime = null; 

- l **@EndDate** datetime = null; 

- l **@Login** nvarchar(200) — логін користувача, під яким запускається метод. 

Приклад запиту: 

{ "name": "spzIBBookingList", "params": [ {"name": "PatientID","value": "50"}, {"name": "StartDate","value": "2023-01-01T00:00:00"}, {"name": "EndDate","value": "2023-02-01T00:00:00"}, {"name": "Login","value": "{{Login}}"} ], "installationId": "{{InstallationGUID}}" } 

У результаті отримуємо список бронювань, який включає таку інформацію: 

- l **BookingID** — ідентифікатор бронювання; 

- l **BookingName** — назва послуги (бронювання); 

- l **BookingDate** — дата бронювання; 

- l **PatientID** — ідентифікатор пацієнта; 

- l **ScheduleID** — ідентифікатор візиту; 

- l **ServiceID** — ідентифікатор послуги; 

- l **BookingServicePriceRate** — ставка вартості бронювання; 

- l **BookingServicePriceValue** — вартість бронювання; 

83 

- l **BookingQuantity** — кількість послуг; 

- l **BookingStatusCode** — код статусу бронювання; 

- l **BookingPaymentStatusCode** — код статусу оплати; 

- l **BookingCreationDate** — дата створення бронювання; 

- l **BookingCreationUserLogin** — логін користувача, який створив бронювання; 

- l **BookingDiscountPercent** — знижка у відсотках; 

- l **BookingDiscountValue** — вартість знижки; 

- l **BookingAgentID** — ідентифікатор агента бронювання; 

- l **AccountID** — ідентифікатор рахунку; 

- l **AccountTypeID** — ідентифікатор типу рахунку; 

- l **BookingExternalID1** — зовнішній ідентифікатор бронювання №1; 

- l **BookingExternalID2** — зовнішній ідентифікатор бронювання №2; 

- l **BookingExecutionUserLogin** — логін користувача (виконавця) бронювання; 

- l **BookingExecutionVenueID** — ідентифікатор кабінету, в якому призначено бронювання; 

- l **BookingGuarantorStatusCode** — код статусу гарантування; 

- l **BookingGuarantorID** — ідентифікатор гарантувальника; 

- l **BookingFranchisePercent** — відсоток франшизи; 

84 

- l **ServiceCode** nvarchar(200) — код послуги; 

l **BookingPaymentValue** — вартість оплати. 

85 

## **Метод spzIBPaymentList** 

Метод **spzIBPaymentList** призначений для виводу списку оплат. Метод має можливість фільтрації оплат по ідентифікатору пацієнта, по ідентифікатору оплати та по зовнішньому ідентифікатору оплати. Вхідні параметри: 

- l **@PatientID** int — ідентифікатор пацієнта (додатковий параметр); 

- l **@PaymentID** int — ідентифікатор оплати (додатковий параметр); 

- l **@PaymentExternalID** int — зовнішній ідентифікатор оплати (додатковий параметр); 

- l **@Login** nvarchar(200) — логін користувача, під яким запускається метод. 

Приклад виконання запиту: 

{ "name": "spzIBPaymentList", "params": [ {"name": "PatientID","value": "50"}, {"name": "PaymentID","value": "20030"}, {"name": "Login","value": "{{Login}}"} ], "installationId": "{{InstallationGUID}}" } 

86 

- У результаті отримуємо список оплат, який включає таку інформацію: 

- l **PaymentID** — ідентифікатор оплати; 

- l **AccountID** — ідентифікатор рахунку; 

- l **AccountTypeID** — ідентифікатор типу рахунку; 

- l **PaymentDate** — дата оплати; 

- l **PaymentUserLogin** — користувач, який здійснив оплату; 

- l **PaymentValue** — вартість оплати; 

- l **PatientID** — ідентифікатор пацієнта; 

- l **PaymentTypeCode** — код типу оплат; 

- l **PaymentKindCode** — вид типу оплат; 

- l **PaymentExternalID** — зовнішній ідентифікатор оплати; 

- l **BookingID** — ідентифікатор бронювання; 

- l **BookingList** — масив ідентифікаторів бронювань, якщо оплата містить декілька послуг; 

## l **PaymentCheck** — чек оплати. 

87 

## **Метод spzIBServiceSave** 

Метод **spzIBServiceSave** призначений для створення або редагування послуги, в залежності від того чи вказаний ідентифікатор послуги. Якщо ідентифікатор послуги вказаний, то відбувається редагування послуги. Якщо ідентифікатор послуги не вказаний, то створюється нова послуга. Вхідні параметри: 

- l **@ServiceID** int — ідентифікатор послуги (додатковий параметр); 

- l **@ServiceExternalID** int — зовнішній ідентифікатор послуги (додатковий параметр); 

- l **@ServiceName** nvarchar(200) — назва послуги (додатковий параметр; 

- l **@ServiceDesc** nvarchar(max) — опис послуги (додатковий параметр); 

- l **@ServiceCode** nvarchar(200) — код послуги; 

- l **@ServiceParentID** int— ідентифікатор батьківської послуги (категорії послуг); 

- l **@ServiceDuration** int — тривалість виконання послуги; 

- l **@Login** nvarchar(200) — логін користувача, під яким запускається метод. 

Приклад запиту: 

{ "name": "spzIBServiceSave", "params": [ {"name": " ServiceID ","value": "12"}, {"name": "ServiceName","value": "Тестова послуга"}, {"name": "ServiceCode","value": "123"}, {"name": "ServiceDesc","value": "Тестовий опис послуги"}, {"name": "ServiceParentID","value": "5"}, {"name": "ServiceDuration","value": "30"}, {"name": "Login","value": "{{login}}"} ], "installationId": "{{InstallationGUID}}" } 

88 

У результаті отримаємо ідентифікатор новоствореної або наявної послуги. 

89 

## **Метод spzIBServiceList** 

Метод **spzIBServiceList** призначений для виводу списку послуг та інформації про них. Додатково можна використати фільтр для пошуку по ідентифікатору послуги або зовнішньому ідентифікатору послуги. Вхідні параметри: 

- l **@ServiceID** int — ідентифікатор послуги (додатковий параметр); 

- l **@ServiceExternalID** int — зовнішній ідентифікатор послуги (додатковий параметр); 

- l **@ServiceIsInternetSevice** bit = null — послуги, які є інтернет послугами, доступні для запису онлайн; 

- l **@Login** nvarchar(200) — логін користувача, під яким запускається метод. 

Приклад запиту: 

{ "name": "spzIBServiceList", "params": [ {"name": "Login","value": "{{Login}}"} ], "installationId": "{{InstallationGUID}}" } 

90 

У результаті отримуємо список послуг із такими полями: 

- l **ServiceID** — ідентифікатор послуги; 

- l **ServiceName** — назва послуги; 

- l **ServiceDesc** — опис послуги; 

- l **ServiceCode** — код послуги; 

- l **ServiceParentID** — ідентифікатор вартості послуги; 

- l **ServiceDuration** — тривалість послуги; 

- l **ServiceExternalID** — зовнішній ідентифікатор послуги; 

- l **ServicePrice** — вартість послуги; 

- l **ServiceIsInternetService** — послуги, які є інтернет послугами, доступні для запису онлайн. 

91 

## **Метод spzIBServicePriceList** 

Метод **spzIBServicePriceList** призначений для виводу списку вартостей послуг. У методі є можливість використання фільтрів по ідентифікатору послуги, по тарифному плану, по ідентифікатору та зовнішньому ідентифікатору вартості послуги. Вхідні параметри: 

- l **@ServicePriceID** int — ідентифікатор вартості послуги (додатковий параметр); 

- l **@ServiceID** int — ідентифікатор послуги (додатковий параметр); 

- l **@TariffPlanID** int — ідентифікатор тарифного плану (додатковий параметр); 

- l **@ServicePriceExternalID** int — зовнішній ідентифікатор вартості послуги (додатковий параметр); 

- l **@Login** nvarchar(200) — логін користувача, під яким запускається метод. 

Приклад запиту: 

{ "name": "spzIBServicePriceList", "params": [ {"name": "Login","value": "{{Login}}"} ], "installationId": "{{InstallationGUID}}" } 

92 

- У результаті отримаємо список вартостей послуг, а саме такі поля: 

- l **ServicePriceID** — ідентифікатор вартості послуги; 

- l **ServiceID** — ідентифікатор послуги; 

- l **ServicePriceRate** — вартість послуги; 

- l **ServicePriceStartDate** — початок терміну вартості послуги; 

- l **ServicePriceEndDate** — закінчення терміну вартості послуги; 

- l **TariffPlanID** — ідентифікатор тарифного плану; 

- l **ServicePriceExternalID** — зовнішній ідентифікатор вартості послуги; 

- l **ServiceCode** nvarchar(200) — код послуги. 

93 

## **Метод spzIBServicePriceSave** 

Метод **spzIBServicePriceSave** призначений для створення або редагування вартостей послуг. Якщо переданий параметр ідентифікатора вартості послуг, то відбувається редагування для заданого ідентифікатора послуги. Якщо ідентифікатор вартості послуги не заданий, то створюється нова вартість послуги. Вхідні параметри: 

- l **@ServicePriceID** int — ідентифікатор вартості послуги; 

- l **@ServiceID** int — ідентифікатор послуги; 

- l **@ServicePriceRate** decimal (20, 10) — вартість послуги; 

- l **@ServicePriceStartDate** datetime — початок терміну вартості послуги; 

- l **@ServicePriceEndDate** datetime — закінчення терміну вартості послуги; 

- l **@TariffPlanID** int — ідентифікатор тарифного плану; 

- l **@ServicePriceExternalID** nvarchar(200) — зовнішній ідентифікатор вартості послуги (додатковий параметр); 

- l **@Login** nvarchar(200) — логін користувача, під яким запускається метод. 

Приклад запиту: 

{ "name": "spzIBServicePriceSave", "params": [ {"name": " ServicePriceID ","value": "1000"}, {"name": "ServiceRef","value": "100"}, {"name": "ServicePriceRate","value": "350"}, {"name": "ServicePriceStartDate","value": "2023-01-01T00:00:00"}, {"name": "ServicePriceEndDate","value": "2023-05-01T00:00:00"}, {"name": "TariffPlanRef","value": "25"}, {"name": "Login","value": "{{login}}"} ], "installationId": "{{InstallationGUID}}" } 

94 

У результаті отримуємо ідентифікатор нової або наявної вартості послуг. 

95 

## **Метод spzIBPaymentBookingInfo** 

Метод **spzIBPaymentBookingInfo** призначений для виведення інформації про оплати послуг. 

Тип запиту — **POST** . Вхідні параметри: 

- l **@Login** nvarchar(200) — логін користувача, під яким запускається метод; 

- l **UserListXML** xml — перелік логінів користувачів; 

- l **StartDate** datetime — дата і час початку виведення інформації про оплати послуг (додатковий параметр); 

- l **EndDate** datetime — дата і час завершення виведення інформації про оплати послуг (додатковий параметр). 

Вихідні параметри: 

- l **BookingID** — ідентифікатор бронювання; 

- l **BookingDate** — дата бронювання (виконання послуги); 

- l **BookingName** — назва послуги; 

- l **ServiceID** — ідентифікатор послуги; 

- l **PatientID** — ідентифікатор пацієнта; 

- l **BookingServiceCode** — код послуги; 

- l **PatientName** — ПІБ пацієнта; 

- l **PatientHistoryNumber** — номер історії пацієнта; 

- l **UserShortName** — ініціали лікаря; 

- l **VenueName** — назва кабінету; 

- l **BookingServicePriceValue** — вартість наданої послуги; 

- l **ServicePriceRate** — вартість послуги; 

- l **TariffPlanName** — назва тарифного плану; 

- l **AssignmentServicePackageGUID** — GUID пакету сервіс-ліста; 

- l **TotalPackPaymentValue** — сума оплати послуги; 

- l **DebtByBook** — значення боргу послуги; 

- l **DebtByPackBook** — значення боргу пакетної послуги. 

96 

Приклад запиту: 

|{||
|---|---|
||"name": "spzIBPaymentBookingInfo",|
||"params": [|
||"name": "UserListXML",|
||"value": "<UserList><User>Miller</User>|
||<User>Alice</User><User>Bob</User></UserList>"|
||},|
||{|
||"name": "Login",|
||"value": "{{Login}}"|
||],|
||"installationId": "{{InstallationGUID}}"|
|}||



## У результаті на екрані з'являється інформація про оплати послуг. 

97 

## **Методи для роботи з даними пацієнта** 

## **Метод spzIBPatientScheduleList** 

Метод **spzIBPatientScheduleList** призначений для виведення візитів авторизованого пацієнта або його споріднених пацієнтів. Тип запиту — **POST** . 

Вхідні параметри: 

- l **Login** nvarchar(200) — логін користувача, під яким пацієнт авторизувався; 

- l **PatientID** int — ідентифікатор пацієнта (обов'язковий параметр). Потрібно вказати **PatientID** пацієнта, під яким запускається метод, або **PatientID** споріднених пацієнтів; 

- l **StartDate** datetime — дата створення візитів, від якої потрібно починати пошук; 

- l **EndDate** datetime — дата створення візитів, до якої потрібно шукати. 

- Вихідні параметри: 

- l **ScheduleID** — ідентифікатор візиту; 

- l **ScheduleStartTime** — дата і час початку візиту; 

- l **ScheduleEndTime** — дата і час закінчення візиту; 

- l **ScheduleStatusCode** — код статусу візиту; 

- l **ScheduleCreationDate** — дата створення візиту; 

- l **ScheduleCreationUserLogin** — логін користувача, який створив візит; 

- l **UserLogin** — логін користувача, який виконує візит; 

- l **VenueID** — ідентифікатор кабінету; 

- l **ScheduleStatusName** — назва статусу візиту; 

- l **BookingList** — список послуг у візиті у форматі JSON. 

98 

Приклад body запиту: 

{ "name": "spzIBPatientScheduleList", "params": [ "name": "PatientID", "value": "1302" }, { "name": "StartDate", "value": "2020-01-09T14:11:24.353" }, { "name": "EndDate", "value": "2023-12-21 14:59:19.947" }, { "name": "Login", "value": "{{Login}}" } ], "installationId": "{{InstallationGUID}}" } 

У результаті на екрані з’являються візити авторизованого пацієнта або його споріднених пацієнтів. 

99 

## **Метод spzIBPatientDocumentList** 

Метод **spzIBPatientDocumentList** призначений для виведення списку документів для авторизованого пацієнта або когось зі споріднених пацієнтів за вибраним логіном. 

Вхідні параметри: 

- l **PatientID** int — ідентифікатор пацієнта (обов'язковий параметр). Потрібно вказати **PatientID** пацієнта, під яким запускається метод, або **PatientID** споріднених пацієнтів; 

- l **StartDate** datetime — дата створення документів, від якої потрібно починати пошук; 

- l **EndDate** datetime — дата створення документів, до якої потрібно шукати; 

- l **Login** nvarchar(200) — логін користувача, під яким пацієнт авторизувався. 

Вихідні параметри: 

- l **DocumentID** — ідентифікатор документа; 

- l **DocumentCreationDate** — дата створення документа; 

- l **DocumentUserLogin** — логін користувача, який створив документ; 

- l **DocumentTemplateID** — ідентифікатор шаблону документа; 

- l **DocumentTemplateName** — назва шаблону документа; 

- l **DocumentKindCode** — код виду документа; 

- l **DocumentKindName** — назва виду документа; 

- l **DocumentApproveStatusCode** — код статусу затвердження документа; 

- l **DocumentApproveStatusName** — назва статусу затвердження документа; 

- l **DocumentUrl** — посилання на документ. 

100 

Приклад body запиту: 

{ "name": "spzIBPatientDocumentList", "params": [ "name": "PatientID", "value": "1302" }, { "name": "StartDate", "value": "2020-01-09T14:11:24.353" }, { "name": "EndDate", "value": "2023-12-21 14:59:19.947" }, { "name": "Login", "value": "{{Login}}" } ], "installationId": "{{InstallationGUID}}" } 

У результаті на екрані з’являється список документів для авторизованого пацієнта або когось зі споріднених пацієнтів за вибраним логіном. 

101 

## **Метод spzIBPatientServiceList** 

Метод **spzIBPatientServiceList** призначений для виведення списку послуг для авторизованих пацієнтів. Послуги виводяться відповідно до віку пацієнта та тільки ті послуги, які додані до інтернет-послуг. Тип запиту — **POST** . 

Вхідні параметри: 

- l **Login** nvarchar(200) — логін пацієнта, під яким здійснено авторизацію (обов'язковий параметр); 

- l **ServiceID** int — ідентифікатор послуги (додатковий параметр); 

- l **ServiceRequestCode** nvarchar(200) — номер направлення (додатковий параметр); 

- l **ServiceExternalID** nvarchar(200) — зовнішній ідентифікатор послуги (додатковий параметр). 

Вихідні параметри: 

- l **ServiceID** — ідентифікатор послуги; 

- l **ServiceName** — назва послуги; 

- l **ServiceDesc** — опис послуги; 

- l **ServiceCode** — код послуги; 

- l **ServiceParentID** — ідентифікатор батьківської послуги; 

- l **ServiceDuration** — тривалість послуги; 

- l **ServiceExternalID** — зовнішній ідентифікатор послуги; 

- l **ServicePrice** — вартість послуги; 

- l **ServiceInstruction** — інструкція до послуги. 

102 

У результаті на екрані з’являється список послуг для авторизованих пацієнтів. 

103 

## **Метод spzIBPatientFreeSeanceList** 

Метод **spzIBPatientFreeSeanceList** призначений для виведення графіка лікарів. 

Вхідні параметри: 

- l **Login** nvarchar(200) — логін пацієнта, під яким виконується метод; 

- l **ServiceID** int — ідентифікатор послуги (обов'язковий параметр); 

- l **DayCount** int — кількість днів від поточної дати, на яку потрібно вивести розклад (обов'язковий параметр); 

- l **UserLogin** nvarchar(200) — логін лікаря (обов'язковий параметр); 

- l **CompanyCity** nvarchar(200) — місто (додатковий параметр); 

- l **CompanyGUID** uniqueidentifier — ідентифікатор компанії (додатковий параметр). 

Вихідні параметри: 

- l **UserLogin** — логін лікаря; 

- l **VenueID** — ідентифікатор кабінету; 

- l **TimeTableStartTime** — дата і час початку слота часу; 

- l **TimeTableEndTime** — дата і час закінчення слота часу; 

- l **CompanyGUID** — GUID компанії. 

Приклад body запиту: 

{ "name": "spzIBPatientFreeSeanceList", "params": [ "name": "PatientID", "value": "98" }, { "name": "DayCount", "value": "50" }, { "name": "UserLogin", "value": "Petrenko" }, { "name": "Login", "value": "{{Login}}" } ], "installationId": "{{InstallationGUID}}" } 

104 

## У результаті на екрані з’являються графіки лікарів. 

105 

## **Метод spzIBPatientUserList** 

Метод **spzIBPatientUserList** призначений для виводу списку лікарів. Вхідні параметри: 

- l **@UserSpecialityCode** nvarchar(200) = null — фільтрування за головною спеціальністю лікаря (необов’язковий параметр); 

- l **@UserLogin** nvarchar(200) = null — фільтрування за логіном лікаря (необов’язковий параметр); 

- l **@Search** nvarchar(200) = null — фільтрування за ПІБ лікаря 

   - (необов’язковий параметр). Пошук співпадінь за введеним текстом; 

- l **@Login** nvarchar(200) — логін користувача, під яким запускається метод. 

Приклад виклику: 

{ "name": "spzIBPatientUserList", "params": [ { "name": "Login", "value": "{{PatientLogin}}" } ], "installationId": "{{InstallationGUID}}" } 

106 

У результаті на екрані з’являється розширена інформація про лікарів та послуги, які вони надають: 

- l **userLogin** — логін користувача; 

- l **userShortName** — ПІБ скорочено; 

- l **userName** — ПІБ повністю; 

- l **userSpecialityCode** — код спеціальності; 

- l **userSpecialityName** — назва спеціальності; 

- l **userEmail** — електронна скринька користувача; 

- l **userPhone** — номер телефону користувача; 

- l **userPhoto** — фото користувача; 

- l **userSpecialityList** — список додаткових спеціальностей користувача. 

107 

## **Метод spzIBPatientDataFileList** 

Метод **spzIBPatientDataFileList** призначений для виведення списку списку файлів для авторизованих пацієнтів. Вхідні параметри: 

- l **PatientID** int — ідентифікатор пацієнта; 

- l **DataFileID** int— ідентифікатор потрібного файла; 

- l **Login** nvarchar (200) — логін пацієнта, під яким здійснено авторизацію (обов'язковий параметр). 

Вихідні параметри: 

- l **DataFileID** — ідентифікатор файла; 

- l **PatientID** — ідентифікатор пацієнта; 

- l **CourseID** — ідентифікатор курсу; 

- l **DataFileURL** — посилання на файл; 

- l **DataFileName** — назва файла; 

- l **DataFileGUID** — GUID файла; 

- l **DataFileDate** — дата створення файла; 

- l **DataFileExternalID1** — зовнішній ідентифікатор файлу №1; 

- l **DataFileExternalID2** — зовнішній ідентифікатор файлу №2; 

- l **DataFileXML** — xml файла. 

У результаті на екрані з’являється список файлів для авторизованих пацієнтів. 

108 

## **Метод spzIBPatientCompanyList** 

Метод **spzIBPatientCompanyList** призначений для виведення списку клінік. 

Вхідні параметри: 

- l **@CompanyExternalID1** nvarchar(200) — зовнішній ідентифікатор компанії №1; 

- l **@CompanyExternalID2** nvarchar(200) — зовнішній ідентифікатор компанії №2; 

- l **@Login** nvarchar(200) — логін користувача, під яким запущено метод. 

Приклад виклику: 

{ "name": "spzIBPatientCompanyList", "params": [ { "name": "Login", "value": "{{Login}}" } ], "installationId": "{{InstallationGUID}}" } 

109 

Вихідні параметри: 

- l **companyGUID** — GUID клініки; 

- l **сompanyName** — назва клініки; 

- l **companyShortName** — коротка назва клініки; 

- l **companyAddress** — адреса клініки; 

- l **companyEmail** — електронна пошта клініки; 

- l **companyPhone** — номер телефону клініки; 

- l **companyExternalID1** — зовнішній ідентифікатор компанії №1; 

- l **companyExternalID2** — зовнішній ідентифікатор компанії №2. 

- У результаті на екрані з’являється список клінік. 

110 

## **Метод spzIBPatientScheduleCreate** 

Метод **spzIBPatientScheduleCreate** призначений для створення візитів для авторизованих пацієнтів або споріднених пацієнтів користувача (родини). 

Тип запиту — **POST** . Вхідні параметри: 

- l **PatientID** int — ідентифікатор пацієнта; 

- l **StartTime** datetime — дата і час час початку слота для запису; 

- l **EndTime** datetime — дата і час час кінця слота для запису; 

- l **ServiceID** int — ідентифікатор послуги; 

- l **VenueID** int — ідентифікатор кабінету; 

- l **UserLogin** nvarchar(200) — логін лікаря; 

- l **Login** nvarchar(200) — логін, під яким виконується метод. 

Вихідний параметр: 

- l **ScheduleID** — ідентифікатор створеного візиту. 

Приклад body запиту: 

{ "name": "spzIBPatientScheduleCreate", "params": [ "name": "PatientID", "value": "1302" }, { "name": "StartTime", "value": "2023-09-07T08:30:00.000" }, { "name": "EndTime", "value": "2023-09-07T09:00:00.000" }, { "name": "ServiceID", "value": "97" } ], "name": "VenueID", "value": "79" } ], "name": "UserLogin", "value": "hanna2" } ], "name": "Login", 

111 

"value": "{{Login}}" } 

], 

"installationId": "{{InstallationGUID}}" 

} 

У результаті на екрані з’являється ідентифікатор створеного візиту **ScheduleID** . 

112 

## **Метод spzIBPatientScheduleCancel** 

Метод **spzIBPatientScheduleCancel** призначений для скасування візитів авторизованих пацієнтів. Можна скасовувати свої візити або візити споріднених пацієнтів, проте тільки ті, які не були змінені. Тип запиту — **POST** . Вхідні параметри: 

- l **Login** — логін авторизованого пацієнта; 

- l **PatientID** int — ідентифікатор пацієнта, візит якого потрібно скасувати (обов'язковий параметр). Потрібно вказати **PatientID** пацієнта, під яким запускається метод, або **PatientID** споріднених пацієнтів; 

- l **ScheduleID int** int — ідентифікатор візиту. 

Приклад body запиту: 

{ "name": "spzIBPatientScheduleCancel", "params": [ "name": "PatientID", "value": "1302" }, { "name": "ScheduleID", "value": "66547" }, { "name": "Login", "value": "{{Login}}" }, ], "installationId": "{{InstallationGUID}}" } 

У результаті вибрані візити буде скасовано. 

Метод видасть помилку, якщо введено неправильні дані. 

113 

## **Метод spzIBServiceRequestValidate** 

Метод **spzIBServiceRequestValidate** призначений для отримання інформації про направлення. Тип запиту — **POST** . Вхідні параметри: 

- l **Login** nvarchar(200) — логін, під яким виконується метод (обов'язковий параметр); 

- l **ServiceRequestNumber** nvarchar(200) — номер направлення (обов'язковий параметр); 

- l **@LegalEntityId** nvarchar(200) — гуїд закладу. 

Вихідні параметри: 

- l **id** — ідентифікатор послуги направлення; 

- l **patientName** — ПІБ пацієнта; 

- l **status** — статус послуги в направленні; 

- l **programProcessingStatus** — статус обробки; 

- l **category** — категорія послуги в направленні; 

- l **serviceRequestCode** — код послуги; 

- l **serviceRequestName** — назва послуги; 

- l **priority** — пріоритет; 

- l **requestLegalEntity** — юридична особа; 

- l **medicalProgramId** — ідентифікатор медичної програми; 

- l **isExternal** — параметр, який вказує на зовнішнє направлення. 

Приклад body запиту: 

{ "name": "spzIBServiceRequestValidate", "params": [ { "name": "ServiceRequestNumber", "value": "1111-1111-1111-1111" }, { "name": "Login", "value": "{{Login}}" } ], "installationId": "{{InstallationGUID}}" } 

114 

## У результаті на екрані з’являється інформація про направлення. 

115 

## **Метод spzIBPatientServiceRequestValidate** 

Метод **spzIBPatientServiceRequestValidate** призначений для отримання інформації про направлення для авторизованих пацієнтів. Тип запиту — **POST** . Вхідні параметри: 

- l **Login** nvarchar(200) — логін, під яким виконується метод (обов'язковий параметр); 

- l **ServiceRequestNumber** nvarchar(200) — номер направлення (обов'язковий параметр). 

Вихідні параметри: 

- l **id** — ідентифікатор послуги направлення; 

- l **patientName** — ПІБ пацієнта; 

- l **status** — статус послуги в направленні; 

- l **programProcessingStatus** — статус обробки; 

- l **category** — категорія послуги в направленні; 

- l **serviceRequestCode** — код послуги; 

- l **serviceRequestName** — назва послуги; 

- l **expirationDate** — термін завершення дії направлення; 

- l **priority** — пріоритет; 

- l **requestLegalEntity** — юридична особа; 

- l **medicalProgramId** — ідентифікатор медичної програми; 

- l **isExternal** — параметр, який вказує на зовнішнє направлення. 

Приклад body запиту: 

{ "name": "spzIBPatientServiceRequestValidate", "params": [ { "name": "ServiceRequestNumber", "value": "1111-1111-1111-1111" }, { "name": "Login", "value": "{{Login}}" } ], "installationId": "{{InstallationGUID}}" } 

116 

У результаті на екрані з’являється інформація про направлення. 

117 

## **Метод spzIBPatientRoleList** 

Метод **spzIBPatientRoleList** призначений для виведення списку споріднених пацієнтів. 

Тип запиту — **POST** . Вхідний параметр: 

- l **Login** nvarchar(200) — логін користувача, під яким виконується метод, для цього користувача буде виведено список споріднених пацієнтів (обов'язковий параметр). 

Вихідні параметри: 

- l **PatientID** — ідентифікатор пацієнта; 

- l **PatientName** — ПІБ пацієнта; 

- l **PatientRoleName** — назва ролі (який доступ). 

Приклад body запиту: 

{ "name": "spzIBPatientRoleList", "params": [ { "name": "Login", "value": "{{Login}}" } ], "installationId": "{{InstallationGUID}}" } 

## У результаті на екрані з’являється список споріднених пацієнтів. 

118 

## **Методи для роботи із складською системою** 

## **Метод spiOutputDeliveryDetails** 

Метод **spiOutputDeliveryDetails** призначений для виведення інформації по розхідних фактурах. 

Тип запиту — **POST** . 

Вхідні параметри: 

- l **@Login** nvarchar — логін користувача, під яким запущено метод. 

- l **@FilterXML** — містить теги із фільтром за датою. 

Приклад: 

<Filter> <StartDate>2023-08-01T00:00:00.000</StartDate> <EndDate>2023-08-31T23:59:59.998</EndDate> </Filter> 

Вихідні параметри: 

- l **DeliveryID** — ID фактури; 

- l **DeliveryNumber** — номер фактури; 

- l **DeliveryKindCode** — код виду списання; 

- l **DeliveryKindName** — вид списання; 

- l **DeliveryDate** — дата і час списання; 

- l **DeliveryItemID** — ID позиції тіла фактури списання; 

- l **ItemKindID** — ID позиції довідника номенклатури; 

- l **ItemKindCode** — код позиції довідника номенклатури; 

- l **ItemKindName** — назва позиції довідника номенклатури; 

- l **DeliveryItemUnitQuantity** — кількість; 

- l **DeliveryItemTotalPriceValue** — вартість без ПДВ; 

- l **DeliveryItemTotalPriceValueWithGST** — вартість із ПДВ; 

- l **DeliveryGST** — ПДВ; 

- l **DeliveryItemExpirationDate** — термін; 

- l **DeliveryItemNumber** — партія; 

- l **DeliveryItemDesc** — коментар до позиції; 

- l **VenueID** — ID складу, із якого списано; 

119 

- l **VenueName** — склад, із якого було списано; 

- l **BookingID** — ID послуги; 

- l **UserName** — ім'я користувача, який проводив списання. 

Приклад виклику: 

{ "name": "spiOutputDeliveryDetails", "params": [ {"name": "Login", "value": "Login"}, {"name":"FilterXML", "value": "<Filter> <StartDate>2023-08-01 00:00:00.000</StartDate> <EndDate>2023-08-31 23:59:59.998</EndDate> </Filter>"} ], "installationId": "InstallationGUID" } 

## У результаті на екрані з’являється інформація по розхідних фактурах. 

120 

## **Метод spiItemKindDetails** 

Метод **spiItemKindDetails** призначений для виведення наборів даних за номенклатурним довідником. 

Тип запиту — **POST** . Вхідний параметр: 

- l **@Login** nvarchar — логін користувача, під яким запущено метод. 

- Вихідні параметри: 

- l **ItemKindID** — унікальний ідентифікатор позиції у Doctor Eleks; 

- l **ItemKindName** — назва номенклатури; 

- l **ItemKindCode** — код (додатковий параметр); 

- l **ItemKindExternalID1** — зовнішній код 1 (додатковий параметр); 

- l **ItemKindExternalID2** — зовнішній код 2 (додатковий параметр); 

- l **ItemKindUnit** — одиниці вимірювання; 

- l **ItemKindManufacturer** — виробник; 

- l **ItemKindIsDisabled** — активність позиції у довіднику; 

- l **ItemKindEAN** — код ЕАН (додатковий параметр); 

- l **ItemKindIsDivisible** — можливість списання дробовим значенням; 

- l **ItemKindDLC** — дата і час останньої модифікації примірника; 

- l **MedicineCategoryID** — ID категорії (додатковий параметр); 

- l **MedicineCategoryName** — назва категорії (додатковий параметр); 

- l **MedicinePackageID** — ID групи (додатковий параметр); 

- l **MedicinePackageName** — назва групи (додатковий параметр). 

Приклад виклику: 

{ "name": "spiItemKindDetails", "params": [ {"name": "Login", "value": "Login"} ], "installationId": "InstallationGUID" } 

121 

У результаті на екрані з’являється набір даних за номенклатурним довідником. 

122 

## **Метод spiDeliveryChange** 

Метод **spiDeliveryChange** призначений для створення фактур приходу. Тип запиту — **POST** . 

Вхідні параметри: 

- l **@Login** nvarchar — логін користувача, під яким запущено метод; 

- l **FilterXML** xml — набір тегів шапки і тіла фактури. 

Вихідний параметр: 

- l **DeliveryID** — ідентифікатор створеної фактури. 

Приклад виклику: 

{ "name": "spiDeliveryChange", "params": [ {"name": "Login", "value": "Login"} {"name": "FilterXML","value": "<Filter> <DeliveryNumber>ІГ0000000015</DeliveryNumber> <DeliveryDate>2023-06-27</DeliveryDate> <DeliveryStatusRef>ACC</DeliveryStatusRef> <DeliveryTypeRef>INP</DeliveryTypeRef> <DeliveryToVenueRef>10</DeliveryToVenueRef> <DeliveryItem> <ItemKindRef>2</ItemKindRef> <DeliveryItemQuantity>10</DeliveryItemQuantity> </DeliveryItem> <DeliveryItem> <ItemKindRef>0</ItemKindRef> <DeliveryItemQuantity>2 </DeliveryItemQuantity> </DeliveryItem> <DeliveryItem> <ItemKindRef>1 </ItemKindRef> <DeliveryItemQuantity>2</DeliveryItemQuantity> </DeliveryItem> </Filter>"}, ], "installationId": "InstallationGUID" } 

У результаті на екрані з’являється **DeliveryID** — ідентифікатор створеної фактури. 

123 

## **Метод spiDeliveryKindList** 

Метод **spiDeliveryKindList** призначений для виведення довідника номенклатури. 

Тип запиту — **POST** . Вхідний параметр: 

- l **@Login** nvarchar — логін користувача, під яким запущено метод. 

- Вихідні параметри: 

- l **DeliveryKindCode** — унікальний код у Doctor Eleks; 

- l **DeliveryKindName** — назва виду; 

- l **DeliveryKindDLC** — дата і час останньої зміни; 

- l **DeliveryTypeRef** — тип фактури, до якої належить (додатковий параметр). 

Приклад виклику: 

{ "name": "spiDeliveryKindList", "params": [ {"name": "Login", "value": "Login"} ], "installationId": "InstallationGUID" } 

У результаті на екрані з’являється довідник номенклатури. 

124 

## **Статичні словники** 

## **Метод spzIBDocumentApproveStatusDictionary** 

Метод **spzIBDocumentApproveStatus Dictionary** — це отримання списку статусів затвердження документів. Вхідний параметр: 

- l **@Login** nvarchar(200) — логін користувача, під яким запущено метод. 

- Приклад виклику: 

{ "name": "spzIBDocumentApproveStatusDictionary", "params": [ { "name": "Login", "value": "{{Login}}" } ], "installationId": "{{InstallationGUID}}" } У результаті на екран виводиться список статусів затвердження документів, а саме такі поля: 

- l **documentApproveStatusCode** — код статусу затвердження документів; 

- l **documentApproveStatusName** — назва статусу затвердження документів. 

125 

## **Метод spzIBGuarantorStatusDictionary** 

Метод **spzIBGuarantorStatusDictionary** — це отримання списку статусів гарантування. 

Вхідний параметр: 

- l **@Login** nvarchar(200) — логін користувача, під яким запущено метод. 

- Приклад виклику: 

{ "name": "spzIBGuarantorStatusDictionary", "params": [ { "name": "Login", "value": "{{Login}}" } ], "installationId": "{{InstallationGUID}}" } 

У результаті на екран виводиться список статусів затвердження документів, а саме такі поля: 

- l **guarantorStatusCode** — код статусу гарантування; 

- l **guarantorStatusName** — назва статусу гарантування. 

126 

## **Метод spzIBBookingStatusDictionary** 

Метод **spzIBBookingStatusDictionary** — це отримання списку статусів гарантування. 

Вхідний параметр: 

- l **@Login** nvarchar(200) — логін користувача, під яким запущено метод. 

- Приклад виклику: 

{ "name": "spzIBBookingStatusDictionary", "params": [ { "name": "Login", "value": "{{Login}}" } ], "installationId": "{{InstallationGUID}}" } 

У результаті на екран виводиться список статусів затвердження документів, а саме такі поля: 

- l **bookingStatusCode** — код статусу бронювання послуги; 

- l **bookingStatusName** — назва статусу бронювання послуги. 

127 

## **Метод spzIBBookingPaymentStatusDictionary** 

## Метод **spzIBBookingPaymentStatus** 

**Dictionary** — це отримання списку статусів гарантування. Вхідний параметр: 

- l **@Login** nvarchar(200) — логін користувача, під яким запущено метод. 

- Приклад виклику: 

{ "name": "spzIBBookingPaymentStatusDictionary", "params": [ { "name": "Login", "value": "{{Login}}" } ], "installationId": "{{InstallationGUID}}" } 

У результаті на екран виводиться список статусів затвердження документів, а саме такі поля: 

- l **bookingPaymentStatusCode** — код статусу оплати бронювання; 

- l **bookingPaymentStatusName** — назва статусу оплати бронювання. 

128 

## **Метод spzIBScheduleStatusDictionary** 

Метод **spzIBScheduleStatusDictionary** — це отримання списку статусів гарантування. 

Вхідний параметр: 

- l **@Login** nvarchar(200) — логін користувача, під яким запущено метод. 

Приклад виклику: 

{ "name": "spzIBScheduleStatusDictionary", "params": [ { "name": "Login", "value": "{{Login}}" } ], "installationId": "{{InstallationGUID}}" } 

129 

У результаті на екран виводиться список статусів затвердження документів, а саме такі поля: 

- l **scheduleStatusCode** — код статусу візиту; 

- l **scheduleStatusName** — назва статусу візиту. 

130 

## **Методи АРІ для інтеграції з платіжним терміналом (ПТКС) Метод spzIBPatientDebt** 

Метод **spzIBPatientDebt** призначений для отримання списку заборгованих послуг пацієнта або групи пацієнтів. Вхідні параметри: 

- l **@PatientID** int — ID пацієнта; 

- l **@Login** nvarchar (max) — логін користувача, під яким запущено метод; 

- l **@PatientGroupMemberIsDisabled** bit = 0 — враховувати груповий рахунок. 

Приклад виклику: 

{ "name": "spzIBPatientDebt", "params": [ { {"name": "PatientID","value": "347"}, {"name": "Login","value": "{{Login}}"} ], "installationId": "{{InstallationGUID}}" } 

131 

Вихідні параметри: 

- l **CompanyEDRPOU** nvarchar(200) — ЕДРПОУ компанії; 

- l **CompanyName** — назва клініки / відділення; 

- l **BookingIsDebt** біт — послуга завершена, ідентифікується як заборгована. 

У результаті на екрані з’являються дані про заборговані послуги пацієнта або групи пацієнтів. 

132 

## **Метод spzBalanceBillInput** 

Метод **spzBalanceBillInput** призначений для оплати вибраних послуг. Вхідні параметри: 

- l **@XML** xml — XML із переліком послуг, які будуть оплачуватися; 

- l **BookingID** — ID послуги, яку потрібно оплатити; 

- l **PaymentValue** — вартість послуги, яку потрібно оплатити; 

- l **@Date** datetime = null — додатковий параметр (дата рахунку); 

- l **@PaymentChequeNumber** nvarchar (max) — унікальний ідентифікатор оплати; 

- l **@PaymentUserLogin** nvarchar(200) — додатковий параметр (користувач, який здійснює оплату); 

- l **@PaymentTypeCode** nvarchar(200) — код типу оплати; 

- l **@Login** nvarchar (200) — логін користувача, під яким запущено метод. 

Приклад структури XML: 

<root> <row BookingID="1592" PaymentValue="48.0" /> <row BookingID="1593" PaymentValue="50.0" /> … </root> Приклад виклику: { "name": "spzBalanceBillInput", "params": [ { {"name": "XML","value": "<root><row BookingID=\"3978\" PaymentValue=\"100.00\" /><row BookingID=\"4228\" PaymentValue=\"100.00\" /></root>"}, {"name": "Date","value": "2023-02-18T00:00:00.000"}, {"name": "PaymentChequeNumber","value": "5B9F389F-349F-4EF9-864D6DDEC7B0F595"}, {"name": "PaymentUserLogin","value": "UserPTKSAPI"}, {"name": "PaymentTypeCode","value": "INT"}, {"name": "Login","value": "{{Login}}"} ], "installationId": "{{InstallationGUID}}" } 

133 

У результаті на екрані з’являється повідомлення про успішне здійснення оплати. 

134 

## **Метод spzIBPatientInfo** 

Метод **spzIBPatientInfo** призначений для отримання інформації про рахунок і послуги в рахунку. Вхідні параметри: 

- l **@DocumentSequenceNumber** nvarchar(200) — номер рахунку, на який нараховуватиметься оплата; 

- l **@Login** nvarchar(200) — логін користувача, під яким запущено метод. 

Для налаштування методів, потрібно щоб у клієнта був **Примітка.** шаблон **Рахунок** , де вказано номер рахунку, на який має надходити оплата. 

Приклад виклику: 

{ "name": "spzIBPatientInfo", "params": [ { {"name": "DocumentSequenceNumber","value": "347"}, {"name": "Login","value": "{{Login}}"} ], "installationId": "{{InstallationGUID}}" } 

135 

У результаті на екрані з’являється інформація про пацієнта та послуги, які слід оплатити: 

- l **patientInfo** містить наступну інформацію про пацієнта: 

   - l **patientID** — ідентифікатор пацієнта; 

   - l **patientName** — ПІБ пацієнта; 

   - l **accountValue** — баланс рахунку; 

   - l **patientBirthDate** — дата народження; 

   - l **patientAddress** — адреса пацієнта; 

   - l **patientBookingAct** — список послуг до оплати; 

   - l **patientBookingSum** — сума вартості послуг; 

   - l **patientDebt** — список заборгованих послуг, які потрібно оплатити; 

   - l **patientDebtSum** — сума заборгованих послуг. 

- l **CompanyEDRPOU** nvarchar(200) — ЕДРПОУ компанії. 

136 

## **Метод spzIBPaymentStatus** 

Метод **spzIBPaymentStatus** призначений для перевірки статусу оплати. Вхідні параметри: 

- l **@Login** nvarchar (max) — логін користувача, під яким запущено метод; 

- l **PaymentChequeNumber** nvarchar (200) — унікальний ідентифікатор оплати. 

Вихідні параметри: 

- l **PaymentStatusCode** — код статусу оплати; 

- l **PaymentStatusName** — назва статусу оплати. 

Приклад виклику: 

{ "name": "spzIBPaymentStatus", "params": [ { "name": "PaymentChequeNumber", "value": "DA80B1A9-E923-4655-BA86-D076345EAD49" }, { "name": "Login", "value": "{{Login}}" } ], "installationId": "{{InstallationGUID}}" } 

## У результаті на екрані з’являється код і статус оплати. 

137 

## **помилок Опис стандартних** 

## **Помилки при авторизації** 

1. При введенні неправильного логіну або пароля отримуємо помилку **400 Bad Request** . 

2. При введенні неправильного **InstallationGUID** отримуємо помилку **500 Internal Server Error** . 

3. При успішній авторизації отримуємо токен та статус-код **200 ОК** . 

138 

## **Стандартні помилки при виконанні методів** 

1. Якщо не задано обов’язкові параметри, то отримуємо успішне виконання методу із кодом **200 ОК** , але з описом та статусом помилки в **data** . 

2. Якщо метод не проходить конкретну перевірку, наприклад, при створенні візиту, то отримуємо повідомлення, що пацієнт уже має створений візит на вказану дату. 

3. Якщо є конфліктні ситуації, наприклад, із зовнішніми ключами, то отримуємо про це повідомлення. Нижче на скріншоті введено тип комунікації, якого не існує в таблиці типів комунікації. 

139 

4. Якщо у користувача немає прав на виконання методів, то отримуємо відповідне повідомлення. 

5. Якщо при оплаті методом ПТКС вказано ідентифікатор, який вже збережений у системі, то отримуємо відповідне повідомлення. 

6. Якщо при оплаті методом ПТКС вказано суму, яка більша за вартість заборгованих послуг, то отримуємо відповідне повідомлення. 

140 

7. Якщо при оплаті методом ПТКС вказано тип оплати, якого не існує у системі, то отримуємо відповідне повідомлення. 

8. Якщо при оплаті методом ПТКС вказано суму меншу або рівну 0, то отримуємо відповідне повідомлення. 

141 

A,s, DOCTOR CLEKS 

