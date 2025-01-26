/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/EmptyTestNGTest.java to edit this template
 */
package com.mycompany.client_for_sftp;

import static org.testng.Assert.*;
import org.testng.annotations.Test;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import javax.json.*;
import java.io.*;
import java.io.ByteArrayOutputStream;
import net.schmizz.sshj.sftp.RemoteFile;
import java.io.IOException;
import javax.json.Json;
import javax.json.JsonObject;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.AbstractMap;

/**
 *
 * @author Александра
 */
public class Client_for_SFTPNG3Test {
    
    @Test
    public void testGetDomainIpList() {
        String sftpHost = "HOST"; // адрес SFTP-сервера
        int sftpPort = 22; // указать порт SFTP
        String sftpUser = "user"; // указать логин
        String sftpPassword = "password"; // указать пароль
        String remoteFilePath = "PATH"; // путь к тестовому файлу

        List<Map.Entry<String, String>> domainIpList = new ArrayList<>();

        SSHClient ssh = new SSHClient();
        try {
            ssh.addHostKeyVerifier((h, p, k) -> true);

            // подключаемся к серверу
            ssh.connect(sftpHost, sftpPort);
            ssh.authPassword(sftpUser, sftpPassword);

            try (SFTPClient sftp = ssh.newSFTPClient()) {
                try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                    try (RemoteFile remoteFile = sftp.open(remoteFilePath)) {
                        byte[] buffer = new byte[8192]; 
                        int bytesRead;
                        long offset = 0;
                        while ((bytesRead = remoteFile.read(offset, buffer, 0, buffer.length)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                            offset += bytesRead;
                        }
                    }

                    String jsonContent = outputStream.toString("UTF-8");

                    // парсим содержимое файла
                    try (InputStream inputStream = new ByteArrayInputStream(jsonContent.getBytes("UTF-8")); JsonReader jsonReader = Json.createReader(inputStream)) {

                        JsonObject rootNode = jsonReader.readObject();
                        JsonArray addresses = rootNode.getJsonArray("addresses");
                        if (addresses != null) {
                            
                            // Создаем список пар домен - адрес
                            for (JsonObject address : addresses.getValuesAs(JsonObject.class)) {
                                String domain = address.getString("domain", "Unknown");
                                String ip = address.getString("ip", "Unknown");
                                domainIpList.add(new AbstractMap.SimpleEntry<>(domain, ip));
                            }

                            // сортируем список по домену
                            domainIpList.sort(Map.Entry.comparingByKey());

                        } else {
                            System.out.println("No massive");
                        }
                    }
                }
            } catch (IOException | JsonException e) {
                e.printStackTrace();
                fail("Cannot read file " + e.getMessage());
            }

        } catch (Exception e) {
            e.printStackTrace();
            fail("No connection SFTP " + e.getMessage());
        } finally {
            try {
                ssh.disconnect();
            } catch (Exception ignored) {
            }
        }

        // проверка результата
        assertNotNull(domainIpList, "Can not be empty");
        assertFalse(domainIpList.isEmpty(), "must have elements");

        // проверка количества записей
        assertEquals(4, domainIpList.size(), "must have 4 records");

        // Проверка содержимого списка
        assertEquals("example.com", domainIpList.get(0).getKey());
        assertEquals("192.168.1.1", domainIpList.get(0).getValue());
    }
    
}
