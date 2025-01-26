/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.mycompany.client_for_sftp;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.xfer.FileSystemFile;
import javax.json.*;
import java.io.*;
import java.io.ByteArrayOutputStream;
import net.schmizz.sshj.sftp.RemoteFile;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;
import javax.json.Json;
import javax.json.JsonObject;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.AbstractMap;


/**
 *
 * @author Alexa
 */
public class Client_for_SFTP {

    public static void main(String[] args) {
        String remoteFilePath = "PATH"; // относительный путь к JSON-файлу на сервере
        Scanner scan = new Scanner(System.in);
        SSHClient ssh = null;
        while (true) {
            try {
                ssh = new SSHClient();
                String sftpHost;
                int sftpPort;
                String sftpUser;
                String sftpPassword;

                // ввод данных для сервера
                while (true) {
                    System.out.println("Enter the server address: ");
                    sftpHost = scan.nextLine();
                    if (isValidIPv4(sftpHost)) {
                        break;
                    } else {
                        System.out.println("Invalid IP address. Please enter a valid IPv4 address.");
                    }
                }

                System.out.println("Enter the server port: ");
                sftpPort = scan.nextInt();
                scan.nextLine(); 

                System.out.println("Enter login: ");
                sftpUser = scan.nextLine();

                System.out.println("Enter password: ");
                sftpPassword = scan.nextLine();

                // отключаем проверку ключей для тестов
                ssh.addHostKeyVerifier((h, p, k) -> true);
                
                // тайм-ауты
                int connectionTimeout = 5000; // Тайм-аут подключения (в миллисекундах)
                int authenticationTimeout = 5000; // Тайм-аут аутентификации (в миллисекундах)

                // Подключение к серверу 
                ssh.setConnectTimeout(connectionTimeout); 
                ssh.connect(sftpHost, sftpPort);
                // аутентификация
                ssh.setTimeout(authenticationTimeout); 
                ssh.authPassword(sftpUser, sftpPassword);
                System.out.println("Connection successful!");
                break; // если подключение удалось
            } catch (Exception e) {
                System.out.println("Connection failed. Please try again.");
            }
        }

        System.out.println("\nMenu:");
        System.out.println("1. Getting a list of domain - address pairs from a file");
        System.out.println("2. Getting an IP address by domain name");
        System.out.println("3. Getting a domain name by IP address");
        System.out.println("4. Adding a new domain - address pair to a file");
        System.out.println("5. Deleting a domain - address pair by domain name or IP address");
        System.out.println("6. Exit");

        boolean exit = false;

        while (!exit) {
            System.out.print("Choose an action: ");
            int choice = Integer.parseInt(scan.nextLine());
            try {
                File tempFile = File.createTempFile("tempData", ".json");

                switch (choice) {
                    case 1:
                        //  Получение списка пар "домен – адрес" из файла
                        try (SFTPClient sftp = ssh.newSFTPClient()) {
                            List<Map.Entry<String, String>> domainIpList = getDomainIpList(sftp, remoteFilePath);

                            // вывод списка доменов и IP
                            for (Map.Entry<String, String> entry : domainIpList) {
                                System.out.println(entry.getKey() + " - " + entry.getValue());
                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        break;

                    case 2:
                        //  получение IP-адреса по доменному имени
                        try (SFTPClient sftp = ssh.newSFTPClient()) {
                            GetIpByDomain(sftp, remoteFilePath, scan);
                        } catch (IOException | JsonException e) {
                            e.printStackTrace();
                        }

                        break;

                    case 3:
                        // Получение доменного имени по IP-адресу
                        try (SFTPClient sftp = ssh.newSFTPClient()) {
                            GetDomainByIp(sftp, remoteFilePath, scan);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        break;

                    case 4:
                        // Добавление новой пары "домен – адрес" в файл
                        tempFile = File.createTempFile("tempData", ".json");
                        try (SFTPClient sftp = ssh.newSFTPClient()) {
                            AddNewDomainIp(sftp, remoteFilePath, tempFile, scan);
                        } catch (IOException e) {
                            System.err.println("Error with SFTP: " + e.getMessage());
                            e.printStackTrace();
                        }

                        if (!tempFile.delete()) {
                            System.err.println("Failed to delete temporary file.");
                        }

                        break;

                    case 5:
                        // Удаление пары "домен – адрес"
                        tempFile = File.createTempFile("tempData", ".json");
                        try (SFTPClient sftp = ssh.newSFTPClient()) {
                            removeDomainIp(sftp, remoteFilePath, tempFile, scan);
                        } catch (IOException e) {
                            System.err.println("Error with SFTP: " + e.getMessage());
                            e.printStackTrace();
                        }

                        if (!tempFile.delete()) {
                            System.err.println("Failed to delete temporary file.");
                        }

                        break;

                    case 6:
                        // Выход
                        System.out.println("Exit have done");
                        exit = true;
                        break;

                    default:
                        System.out.println("Invalid choice. Please try again.");

                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }

        }
        scan.close();

    }

    // получение домена и IP в алфавитном порядке (сортировка по домену)
    public static List<Map.Entry<String, String>> getDomainIpList(SFTPClient sftp, String remoteFilePath) throws IOException {
        List<Map.Entry<String, String>> domainIpList = new ArrayList<>();

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            
            // Чтение содержимого файла 
            try (RemoteFile remoteFile = sftp.open(remoteFilePath)) {
                byte[] buffer = new byte[8192]; 
                int bytesRead;
                long offset = 0;

                // чтение файла блоками
                while ((bytesRead = remoteFile.read(offset, buffer, 0, buffer.length)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    offset += bytesRead;
                }
            } catch (IOException e) {
                System.err.println("Ошибка при открытии или чтении файла: " + e.getMessage());
                throw e; // Пробрасываем исключение дальше
            }
            
            String jsonContent = outputStream.toString("UTF-8");

            // Парсинг JSON
            try (InputStream inputStream = new ByteArrayInputStream(jsonContent.getBytes("UTF-8")); JsonReader jsonReader = Json.createReader(inputStream)) {
                JsonObject rootNode = jsonReader.readObject();

                JsonArray addresses = rootNode.getJsonArray("addresses");
                if (addresses != null) {
                    System.out.println("List domains and IP:");

                    // создаем список пар
                    for (JsonObject address : addresses.getValuesAs(JsonObject.class)) {
                        String domain = address.getString("domain", "Unknown");
                        String ip = address.getString("ip", "Unknown");
                        domainIpList.add(new AbstractMap.SimpleEntry<>(domain, ip));
                    }

                    // сортировка
                    domainIpList.sort(Map.Entry.comparingByKey());

                } else {
                    System.out.println("No massive");
                }
            } catch (JsonException e) {
                System.err.println("Error in parsing JSON: " + e.getMessage());
                throw new IOException("Error in parsing JSON: ", e); 
            }
        }

        return domainIpList;
    }

    // получение Ip по домену
    public static void GetIpByDomain(SFTPClient sftp, String remoteFilePath, Scanner scan) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
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

            try (InputStream inputStream = new ByteArrayInputStream(jsonContent.getBytes("UTF-8")); JsonReader jsonReader = Json.createReader(inputStream)) {

                JsonObject rootNode = jsonReader.readObject();
                JsonArray addresses = rootNode.getJsonArray("addresses");

                System.out.println("Enter domain name: ");
                String domainName = scan.nextLine().trim();
                boolean found = false;

                if (addresses != null) {
                    for (JsonObject address : addresses.getValuesAs(JsonObject.class)) {
                        if (address.getString("domain", "").equals(domainName)) {
                            System.out.println("Ip: " + address.getString("ip", "Unknown"));
                            found = true;
                            break;
                        }
                    }
                } else {
                    System.out.println("No addresses array found in the JSON.");
                }

                if (!found) {
                    System.out.println("Domain not found.");
                }
            }
        } catch (IOException e) {
            System.err.println("An I/O error occurred: " + e.getMessage());
            e.printStackTrace();
        } catch (JsonException e) {
            System.err.println("A JSON processing error occurred: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            // Обработка любых других неожиданных исключений
            System.err.println("An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // получение домена по Ip
    public static void GetDomainByIp(SFTPClient sftp, String remoteFilePath, Scanner scan) {
        try {
            // чтение содержимого
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (RemoteFile remoteFile = sftp.open(remoteFilePath)) {
                byte[] buffer = new byte[8192]; // Буфер для чтения
                int bytesRead;
                long offset = 0;

                // чтение блоками
                while ((bytesRead = remoteFile.read(offset, buffer, 0, buffer.length)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    offset += bytesRead;
                }
            }

            String jsonContent = outputStream.toString("UTF-8");

            // парсинг json
            try (InputStream inputStream = new ByteArrayInputStream(jsonContent.getBytes("UTF-8")); JsonReader jsonReader = Json.createReader(inputStream)) {

                JsonObject rootNode = jsonReader.readObject();

                JsonArray addresses = rootNode.getJsonArray("addresses");

                System.out.println("Enter IP name: ");
                String ipName = scan.nextLine().trim();
                boolean track = false;

                if (addresses != null) {
                    for (JsonObject address : addresses.getValuesAs(JsonObject.class)) {
                        if (address.getString("ip", "").equals(ipName)) {
                            System.out.println("Domain: " + address.getString("domain", "Unknown"));
                            track = true;
                            break;
                        }
                    }
                } else {
                    System.out.println("No addresses array found in the JSON.");
                }

                if (!track) {
                    System.out.println("IP not found.");
                }
            }
        } catch (IOException e) {
            System.err.println("An I/O error occurred: " + e.getMessage());
            e.printStackTrace();
        } catch (JsonException e) {
            System.err.println("A JSON processing error occurred: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // добавление новой пары домен - ip
    public static void AddNewDomainIp(SFTPClient sftp, String remoteFilePath, File tempFile, Scanner scan) {
        try {
            // загрузка файла с удаленного сервера в локальный файл
            FileSystemFile localFile = new FileSystemFile(tempFile);
            sftp.get(remoteFilePath, localFile);

            // чтение и обновление JSON-файла
            JsonObject updatedJsonObject;
            try (JsonReader jsonReader = Json.createReader(new FileInputStream(tempFile))) {
                JsonObject jsonObject = jsonReader.readObject();
                JsonArray addressesArray = jsonObject.getJsonArray("addresses");

                String newDomainName;
                String newIpAddress;

                while (true) {
 
                    System.out.println("Enter domain name: ");
                    newDomainName = scan.nextLine().trim();

                    do {
                        System.out.println("Enter IP address: ");
                        newIpAddress = scan.nextLine().trim();
                        if (!isValidIPv4(newIpAddress)) {
                            System.out.println("Invalid IP address. Please enter a valid IPv4 address.");
                        }
                    } while (!isValidIPv4(newIpAddress)); // Пока IP-адрес не валиден, повторяем ввод

                    // проверка на уникальность
                    if (!isDomainOrIpExists(addressesArray, newDomainName, newIpAddress)) {
                        break; 
                    }

                    System.out.println("Domain or IP address already exists in the file. Please try again.");
                }

                // новый объект
                JsonObject newEntry = Json.createObjectBuilder()
                        .add("domain", newDomainName)
                        .add("ip", newIpAddress)
                        .build();

                JsonArrayBuilder arrayBuilder = Json.createArrayBuilder(addressesArray);
                arrayBuilder.add(newEntry);
                JsonArray updatedArray = arrayBuilder.build();

                // обновление основного объекта
                updatedJsonObject = Json.createObjectBuilder()
                        .add("addresses", updatedArray)
                        .build();
            }

            // запись обновленных данных во временный файл
            try (OutputStream tempOutputStream = new FileOutputStream(tempFile)) {
                JsonWriter jsonWriter = Json.createWriter(tempOutputStream);
                jsonWriter.writeObject(updatedJsonObject);
            }

            // запись файла на сервере
            FileSystemFile localFileUpdated = new FileSystemFile(tempFile);
            sftp.put(localFileUpdated, remoteFilePath);

            System.out.println("File uploaded and updated successfully.");
        } catch (IOException e) {
            System.err.println("I/O error during JSON update: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // удаление пары домен - ip
    public static void removeDomainIp(SFTPClient sftp, String remoteFilePath, File tempFile, Scanner scan) {
        try {
            // загрузка файла с удаленного сервера в локальный файл
            FileSystemFile localFile = new FileSystemFile(tempFile);
            sftp.get(remoteFilePath, localFile);

            JsonObject updatedJsonObject;
            try (JsonReader jsonReader = Json.createReader(new FileInputStream(tempFile))) {
                JsonObject jsonObject = jsonReader.readObject();
                JsonArray addressesArray = jsonObject.getJsonArray("addresses");

                System.out.println("Enter domain or IP to remove:");
                String inputToRemove = scan.nextLine().trim();

                boolean isRemoved = false;

                // Создаем новый массив без удаленного элемента
                JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
                for (JsonValue value : addressesArray) {
                    if (value.getValueType() == JsonValue.ValueType.OBJECT) {
                        JsonObject address = value.asJsonObject();
                        String domain = address.getString("domain", "");
                        String ip = address.getString("ip", "");

                        // если введенное значение совпадает с доменом или IP, пропускаем его и не добавляем в массив
                        if (domain.equals(inputToRemove) || ip.equals(inputToRemove)) {
                            isRemoved = true;
                            continue; 
                        }

                        // добавляем все остальные элементы
                        arrayBuilder.add(address);
                    }
                }

                if (isRemoved) {
                    // обновление основного объекта
                    JsonArray updatedArray = arrayBuilder.build();
                    updatedJsonObject = Json.createObjectBuilder()
                            .add("addresses", updatedArray)
                            .build();

                    // запись обновленных данных обратно во временный файл
                    try (OutputStream tempOutputStream = new FileOutputStream(tempFile)) {
                        JsonWriter jsonWriter = Json.createWriter(tempOutputStream);
                        jsonWriter.writeObject(updatedJsonObject);
                    }

                    // перезапись файла на сервер
                    FileSystemFile localFileUpdated = new FileSystemFile(tempFile);
                    sftp.put(localFileUpdated, remoteFilePath);
                    System.out.println("Record removed and file updated successfully.");
                } else {
                    System.out.println("No matching record found");
                }
            }
        } catch (IOException e) {
            System.err.println("I/O error during JSON update: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Метод для проверки, существует ли домен или IP в массиве
    private static boolean isDomainOrIpExists(JsonArray addressesArray, String domainName, String ipAddress) {
        for (JsonValue value : addressesArray) { 
            if (value.getValueType() == JsonValue.ValueType.OBJECT) { 
                JsonObject address = value.asJsonObject();
                if (address.getString("domain").equals(domainName) || address.getString("ip").equals(ipAddress)) {
                    return true;
                }
            }
        }
        return false;
    }

    // Метод для проверки валидности IPv4-адреса
    private static boolean isValidIPv4(String ipAddress) {
        try {
            InetAddress inetAddress = InetAddress.getByName(ipAddress);
            return inetAddress.getHostAddress().equals(ipAddress) && ipAddress.contains(".");
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
