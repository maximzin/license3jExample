Инструкция по работе с механизмом лицензий:

1. Пихаем зависимость в нужные проекты:
  <dependency>
			<groupId>com.javax0.license3j</groupId>
			<artifactId>license3j</artifactId>
			<version>3.3.0</version>
  </dependency>

  + скачиваем файлик с https://github.com/verhas/license3jrepl (спец приложуха, облегчающая жизнь)

2. Создаём файл лицензии через отдельный проект для лицензий:
     License newLicense = new License();
   
3. Добавляем features (информация типа (названия предприятия), какое-нибудь значение, которое хотим проверять)
     Feature feature1 = Feature.Create.stringFeature("Company name", "Zavod №3");
     Feature feature2 = Feature.Create.intFeature("MachinesCount", 10);
     newLicense.add(feature1);
     newLicense.add(feature2);
   
4. Добавляем дату окончания лицензии (опционально)
     LocalDateTime expireDate = LocalDateTime.now().plusMonths(3).withHour(23).withMinute(59).withSecond(0);
     long expireDateMillis = expireDate.atZone(ZoneId.systemDefault())
                .toInstant().toEpochMilli();
		 newLicense.setExpiry(new Date(expireDateMillis));
   
5. Записываем на диск
    try (FileOutputStream fos = new FileOutputStream("D:\\license\\license.lic")) {
    			byte[] licenseByteArray = newLicense.serialized();
    			fos.write(licenseByteArray, 0, licenseByteArray.length);
    			System.out.println("SUCCESS");
    } catch (Exception e) {
    			System.out.println("Ошибка");
    }
   
6. Включаем нашу скачанную прогу из первого шага
     cmd -> java -jar License3jrepl-3.1.5-jar-with-dependencies.jar
   
7. Создаём ключи и подписываем ими нашу лицензию
     generateKeys RSA size=512 format=BINARY public=myPublic.key private=myPrivate.key
     (в той же папке создаются ключики)
     ключи уже находятся в памяти запущенной приложухи, но если что, то подцепляем командами "loadPublicKey format=BINARY myPublic.key" и "loadPrivateKey format=BINARY myPrivate.key"
     подключаем нашу созданную лицезнию "licenseLoad format=BINARY license.lic"
     когда лицензия и ключи подключены к приложухе, то подписываем лицензию: "sign digest=SHA-256"
     Лицензия подписана, сохраняем её "saveLicense format=BINARY licenseSigned.lic"
     Не закрываем окно и не выходим!
     Пишем dumpPublicKey и получаем два страшных блока из массива байт, второй нужно копировать и намертво вставлять в лицензируемое приложение,
     чтобы пользователи не могли его поменять, это важно.
   
8. Теперь работаем с лицензией в нашем приложении, которое непосредственно лицензируется
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
       
     
