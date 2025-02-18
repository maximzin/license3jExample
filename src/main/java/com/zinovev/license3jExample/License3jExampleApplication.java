package com.zinovev.license3jExample;

import javax0.license3j.Feature;
import javax0.license3j.License;
import javax0.license3j.io.LicenseReader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@SpringBootApplication
public class License3jExampleApplication {

	public static void main(String[] args) {

		        License newLicense = new License();
        Feature feature1 = Feature.Create.stringFeature("Company name", "Zavod №3");
        Feature feature2 = Feature.Create.intFeature("MachinesCount", 10);


        LocalDateTime expireDate = LocalDateTime.now().plusMonths(3).withHour(23).withMinute(59).withSecond(0);

        long expireDateMillis = expireDate.atZone(ZoneId.systemDefault())
                .toInstant().toEpochMilli();

		newLicense.setExpiry(new Date(expireDateMillis));

		newLicense.add(feature1);
		newLicense.add(feature2);


		try (FileOutputStream fos = new FileOutputStream("D:\\TrueLicense\\license3.lic")) {
			byte[] licenseByteArray = newLicense.serialized();
			fos.write(licenseByteArray, 0, licenseByteArray.length);
			System.out.println("SUCCESS");
		} catch (Exception e) {
			System.out.println("Ошибка");
        }


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

		License license;
		try (LicenseReader reader = new LicenseReader("D:\\TrueLicense\\licenseSigned3.lic")) {
			license = reader.read();
			System.out.println(license.isExpired());
			System.out.println(license.getFeatures());

			System.out.println(license.isOK(public_key));

			System.out.println(license.get("MachinesCount").getInt());

		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

	}

}
