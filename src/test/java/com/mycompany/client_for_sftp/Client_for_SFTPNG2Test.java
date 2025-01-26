/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/EmptyTestNGTest.java to edit this template
 */
package com.mycompany.client_for_sftp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonReader;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.RemoteFile;
import net.schmizz.sshj.sftp.SFTPClient;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 *
 * @author Александра
 */
public class Client_for_SFTPNG2Test {

    @Test
    public void testGetDomainIpListSortedByDomain() {
        String sftpHost = "HOST"; // адрес SFTP-сервера
        int sftpPort = 22; // указать порт SFTP
        String sftpUser = "user"; // указать логин
        String sftpPassword = "password"; // указать пароль
        String remoteFilePath = "PATH"; // путь к тестовому файлу

        List<Map.Entry<String, String>> domainIpList = new ArrayList<>();

        SSHClient ssh = new SSHClient();
        try {

            ssh.addHostKeyVerifier((h, p, k) -> true);
            ssh.connect(sftpHost, sftpPort);
            ssh.authPassword(sftpUser, sftpPassword);

            try (SFTPClient sftp = ssh.newSFTPClient()) {
                try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                    // чтение содержимого файла
                    try (RemoteFile remoteFile = sftp.open(remoteFilePath)) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        long offset = 0;

                        // блоки файла
                        while ((bytesRead = remoteFile.read(offset, buffer, 0, buffer.length)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                            offset += bytesRead;
                        }
                    }

                    String jsonContent = outputStream.toString("UTF-8");

                    // Парсим содержимое файла
                    try (InputStream inputStream = new ByteArrayInputStream(jsonContent.getBytes("UTF-8")); JsonReader jsonReader = Json.createReader(inputStream)) {

                        JsonObject rootNode = jsonReader.readObject();

                        // Получаем массив "addresses"
                        JsonArray addresses = rootNode.getJsonArray("addresses");
                        if (addresses != null) {
                            for (JsonObject address : addresses.getValuesAs(JsonObject.class)) {
                                String domain = address.getString("domain", "Unknown");
                                String ip = address.getString("ip", "Unknown");
                                domainIpList.add(new AbstractMap.SimpleEntry<>(domain, ip));
                            }

                            // Сортируем список по домену
                            domainIpList.sort(Map.Entry.comparingByKey());
                        }
                    }
                }
            } catch (IOException | JsonException e) {
                e.printStackTrace();
                fail("Error while reading file: " + e.getMessage());
            }

        } catch (Exception e) {
            e.printStackTrace();
            fail("Connection error: " + e.getMessage());
        } finally {
            try {
                ssh.disconnect();
            } catch (Exception ignored) {
            }
        }

        // проверка результата
        assertNotNull(domainIpList, "List cannot be empty");
        assertFalse(domainIpList.isEmpty(), "List must have elements");

        // проверка сортировки
        List<String> domains = domainIpList.stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // создаем копию списка доменов и сортируем
        List<String> sortedDomains = new ArrayList<>(domains);
        sortedDomains.sort(String::compareTo);

        // сравниваем оригинальный список с отсортированным
        assertEquals(sortedDomains, domains, "Not sorted");
    }

}
