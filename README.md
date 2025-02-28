## Инструкция по работе с механизмом лицензий:


## Вариант №1: через специальное приложение
1. Скачиваем приложение: https://github.com/verhas/license3jrepl (нужно скачать .jar) и запускаем
```
cmd -> java -jar License3jrepl-3.1.5-jar-with-dependencies.jar
```

2. Далее такой набор команд
```
newLicense // создаем новый экземпляр, он пока что в памяти приложения

generateKeys RSA size=512 format=BINARY public=myPublic.key private=myPrivate.key // набор ключей для подписи/сверки, они сразу окажутся и на диске и в памяти

feature company:STRING=Zavod #1 // атрибут лицензии (название предприятия)

feature expireDate:STRING=28-03-2025 // срок лицензии

feature machinesMaximum:INT=5 // ещё один атрибут (например это ограничение количества станков)

sign digest=SHA-256 // подписываем лицензию нашим ключем

dumpPublicKey // получаем два страшных блока из массива байт, второй нужно копировать и намертво вставить в лицензируемое приложение, чтобы пользователи не могли его поменять, это важно. Приложение будет сверять лицензию с помощью этого публичного ключа

dumpLicense // посмотреть что получилось

saveLicense format=BINARY licenseSigned.lic // сохраняем лицензию на диск (всё сохраняется в директории, откуда запускали прогу)
```

3. Вставляем в наш проект, где нужен механизм лицензий зависимость:
```
<dependency>
	<groupId>com.javax0.license3j</groupId>
	<artifactId>license3j</artifactId>
	<version>3.3.0</version>
</dependency>
```

4. Итоговый компонент в нашем приложении выглядит примерно так:
```
@Component
@NoArgsConstructor
public class LicenseWorker implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(CncServerApplication.class);

    @Getter
    private static int machinesMaximum = 0;

    private static final byte [] public_key = new byte[] {
            (byte)0x52,
            (byte)0x53, (byte)0x41, (byte)0x00, (byte)0x30, (byte)0x5C, (byte)0x30, (byte)0x0D, (byte)0x06,
            (byte)0x09, (byte)0x2A, (byte)0x86, (byte)0x48, (byte)0x86, (byte)0xF7, (byte)0x0D, (byte)0x01,
            (byte)0x01, (byte)0x01, (byte)0x05, (byte)0x00, (byte)0x03, (byte)0x4B, (byte)0x00, (byte)0x30,
            (byte)0x48, (byte)0x02, (byte)0x41, (byte)0x00, (byte)0x8E, (byte)0x71, (byte)0x3B, (byte)0xE9,
            (byte)0xF8, (byte)0x77, (byte)0x67, (byte)0xA9, (byte)0x00, (byte)0x97, (byte)0x73, (byte)0x18,
            (byte)0xF1, (byte)0x4E, (byte)0xA8, (byte)0x05, (byte)0xDB, (byte)0xF7, (byte)0x06, (byte)0xC3,
            (byte)0xB6, (byte)0xEA, (byte)0xD5, (byte)0x14, (byte)0x40, (byte)0x4A, (byte)0x77, (byte)0x6C,
            (byte)0x70, (byte)0x12, (byte)0x2E, (byte)0x9B, (byte)0x79, (byte)0x11, (byte)0x70, (byte)0x63,
            (byte)0x98, (byte)0x0F, (byte)0x05, (byte)0x5C, (byte)0xF9, (byte)0x93, (byte)0x4B, (byte)0x23,
            (byte)0xE0, (byte)0x8E, (byte)0x49, (byte)0x57, (byte)0xD9, (byte)0xD0, (byte)0xD0, (byte)0x56,
            (byte)0xBA, (byte)0x9B, (byte)0xCE, (byte)0x6F, (byte)0x26, (byte)0xD5, (byte)0xF0, (byte)0xA2,
            (byte)0x8F, (byte)0x68, (byte)0xF3, (byte)0x4D, (byte)0x02, (byte)0x03, (byte)0x01, (byte)0x00,
            (byte)0x01,
    };


    @Override
    public void run(ApplicationArguments args) {
        checkLicense();
    }

    private void checkLicense() {

        try {
            //Для работы создаем экземпляр
            License license;

            //В директорию программы (корень) кладем наш файл license.lic и делаем путь к нему
            String currentDir = System.getProperty("user.dir");
            String licensePath = Paths.get(currentDir, "license.lic").toString();

            try (LicenseReader reader = new LicenseReader(licensePath)) {
                //Читаем и работаем с файлом лицензии
                license = reader.read();

                System.out.println("Лицензия подписана корректно: " + license.isOK(publickey));
                System.out.println(license.getFeatures());

                LocalDate expireDateForShow = LocalDate.parse(license.get("expireDate").getString(), DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                System.out.println("Лицензия актуальна (день не настал?) " + expireDateForShow.isAfter(LocalDate.now()));
                System.out.println("Лицензия актуальна (день окончания сегодня?) " + expireDateForShow.isEqual(LocalDate.now()));

                System.out.println("Название компании " + license.get("company").getString());
                System.out.println("Наш дополнительный ограничивающий атрибут " + license.get("machinesMaximum").getInt());

                if (!license.isOK(publickey)) {
                    logger.info("Лицензия некорректно подписана. Завершаем работу приложения");
                    System.exit(1);
                }

                Object isExpireDate = license.get("expireDate");

                //Если лицензия содержит атрибут expireDate, то проверяем сроки
                if (isExpireDate != null) {

                    //Парсим атрибут
                    LocalDate expireDate = LocalDate.parse(license.get("expireDate").getString(), DateTimeFormatter.ofPattern("dd-MM-yyyy"));

                    // Если день окончания уже за  настал или настал
                    if (expireDate.isBefore(LocalDate.now()) && !expireDate.isEqual(LocalDate.now())) {
                        logger.info("Лицензия истекла. Завершаем работу приложения");
                        System.exit(1);
                    }
                }
                //Если лицензия не содержит сроков (атрибут expireDate), пропускаем (так обговорено с начальством)

                //Парсим наш атрибут на количество станков, если не найдено, то приложение не будет разрешать добавлять станки
                machinesMaximum = license.get("machinesMaximum").getInt();

            } catch (Exception e) {
                logger.error("Ошибка при работе с лицензией (чтение и внутренности): {}", e.getMessage());
		System.exit(1);
            }
        }
        catch (Exception e) {
            logger.error("Ошибка при работе с лицензией (общая): {}", e.getMessage());
	    System.exit(1);
        }
    }

}
```

5. Окей, работает, теперь нужно запомнить, что приватный и публичный ключ нужно сохранить где-то и пользоваться по мере надобности, допустим нужно создать ещё одну лицензию
Идём снова в нашу программу, предварительно закончив предыдущий сеанс с ней:
```
cmd -> java -jar License3jrepl-3.1.5-jar-with-dependencies.jar

newLicense // создаем новый экземпляр, он пока что в памяти приложения

feature company:STRING=Zavod #2 // атрибут лицензии (название предприятия)

feature expireDate:STRING=01-03-2025 // срок лицензии

feature machinesMaximum:INT=10 // ещё один атрибут (например это ограничение количества станков)

loadPublicKey format=BINARY myPublic.key // загружаем публичный ключ

loadPrivateKey format=BINARY myPrivate.key // загружаем приватный ключ

sign digest=SHA-256 // подписываем лицензию нашим ключем

dumpLicense // посмотреть что получилось

saveLicense format=BINARY license.lic // сохраняем лицензию на диск (всё сохраняется в директории, откуда запускали прогу)
```

---------------------------------------------------------------------------------
## Вариант №2: через своё отдельное приложение + спец. программу (в дальнейшем можно сделать цельное приложение под себя, просто нужно разобраться как самому создавать публичные и приватные ключи)
1. Пихаем зависимость в нужные проекты:
```   
<dependency>
	<groupId>com.javax0.license3j</groupId>
	<artifactId>license3j</artifactId>
	<version>3.3.0</version>
</dependency>
```
```
  + скачиваем jar-ку с https://github.com/verhas/license3jrepl (спец приложуха, облегчающая жизнь)
```

2. Создаём файл лицензии через отдельный проект для лицензий:
```
License newLicense = new License();
```
   
4. Добавляем features (информация типа (названия предприятия), какое-нибудь значение, которое хотим проверять)
```
Feature feature1 = Feature.Create.stringFeature("Company name", "Zavod №3");
Feature feature2 = Feature.Create.intFeature("MachinesCount", 10);
newLicense.add(feature1);
newLicense.add(feature2);
```
   
4. Добавляем дату окончания лицензии (опционально)
```
LocalDateTime expireDate = LocalDateTime.now().plusMonths(3).withHour(23).withMinute(59).withSecond(0);
long expireDateMillis = expireDate.atZone(ZoneId.systemDefault())
	.toInstant().toEpochMilli();
	 newLicense.setExpiry(new Date(expireDateMillis));
```
   
5. Записываем на диск
```
try (FileOutputStream fos = new FileOutputStream("D:\\license\\license.lic")) {
	byte[] licenseByteArray = newLicense.serialized();
	fos.write(licenseByteArray, 0, licenseByteArray.length);
	System.out.println("SUCCESS");
} catch (Exception e) {
	System.out.println("Ошибка");
}
```
   
8. Включаем нашу скачанную прогу из первого шага
```
cmd -> java -jar License3jrepl-3.1.5-jar-with-dependencies.jar
```
   
10. Создаём ключи и подписываем ими нашу лицензию
```
generateKeys RSA size=512 format=BINARY public=myPublic.key private=myPrivate.key
(в той же папке создаются ключики)
ключи уже находятся в памяти запущенной приложухи, но если что, то подцепляем командами "loadPublicKey format=BINARY myPublic.key" и "loadPrivateKey format=BINARY myPrivate.key"
подключаем нашу созданную лицезнию "licenseLoad format=BINARY license.lic"
когда лицензия и ключи подключены к приложухе, то подписываем лицензию: "sign digest=SHA-256"
Лицензия подписана, сохраняем её "saveLicense format=BINARY licenseSigned.lic"
Не закрываем окно и не выходим!
Пишем dumpPublicKey и получаем два страшных блока из массива байт, второй нужно копировать и намертво вставлять в лицензируемое приложение,
чтобы пользователи не могли его поменять, это важно.
```   
11. Теперь работаем с лицензией в нашем приложении, которое непосредственно лицензируется
```
Первым делом нужно намертво зашить публичный ключ (смотри предыдущий шаг, концовку)
Вот так выглядит наш ключик в приложении:
byte [] public_key = new byte[] {
(byte)0x52,
(byte)0x53, (byte)0x41, (byte)0x00, (byte)0x30, (byte)0x5C, (byte)0x30, (byte)0x0D, (byte)0x06,
(byte)0x09, (byte)0x2A, (byte)0x86, (byte)0x48, (byte)0x86, (byte)0xF7, (byte)0x0D, (byte)0x01,
(byte)0x01, (byte)0x01, (byte)0x05, (byte)0x00, (byte)0x03, (byte)0x4B, (byte)0x00, (byte)0x30,
(byte)0x48, (byte)0x02, (byte)0x41, (byte)0x00, (byte)0xA9, (byte)0x9D, (byte)0x65, (byte)0x75,
(byte)0xEC, (byte)0xE6, (byte)0xD5, (byte)0x50, (byte)0x91, (byte)0xE9, (byte)0x1F, (byte)0x39,
(byte)0x9C, (byte)0x3E, (byte)0x4B, (byte)0xFF, (byte)0xAF, (byte)0xB4, (byte)0x06, (byte)0xE6,
(byte)0xC5, (byte)0x79, (byte)0x37, (byte)0x22, (byte)0x3A, (byte)0x10, (byte)0x84, (byte)0x72,
(byte)0xF5, (byte)0xEF, (byte)0x09, (byte)0xD6, (byte)0xB8, (byte)0x3A, (byte)0x15, (byte)0xEB,
(byte)0x39, (byte)0x60, (byte)0xE8, (byte)0x5E, (byte)0xE6, (byte)0xAE, (byte)0x2A, (byte)0x4C,
(byte)0x06, (byte)0x89, (byte)0x25, (byte)0x60, (byte)0x49, (byte)0xD9, (byte)0x3D, (byte)0xA8,
(byte)0xF4, (byte)0x8C, (byte)0x27, (byte)0xCE, (byte)0x9C, (byte)0x47, (byte)0xC3, (byte)0x86,
(byte)0x0F, (byte)0x6A, (byte)0xA1, (byte)0x65, (byte)0x02, (byte)0x03, (byte)0x01, (byte)0x00,
(byte)0x01,
};

Грузим нашу обновленную лицензию:
License license;
	try (LicenseReader reader = new LicenseReader("D:\\license\\licenseSigned.lic")) {
  		license = reader.read();
		System.out.println(license.isExpired()); - чек на истекла ли
		System.out.println(license.getFeatures()); - смотрим атрибуты
		System.out.println(license.isOK(public_key)); - сверяем её легитимность с помощью публичного ключа, который вшили в приложение
		System.out.println(license.get("maximumPieces").getInt()); - достаём какой-нибудь атрибут, которым можно что-нибудь ограничивать
	} catch (Exception e) {
	      System.out.println(e.getMessage());
	}
```       
     
