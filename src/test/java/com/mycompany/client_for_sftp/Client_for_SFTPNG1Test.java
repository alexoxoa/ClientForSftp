/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/EmptyTestNGTest.java to edit this template
 */
package com.mycompany.client_for_sftp;

import static com.mycompany.client_for_sftp.Client_for_SFTP.getDomainIpList;
import java.io.IOException;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 *
 * @author Александра
 */
public class Client_for_SFTPNG1Test {
    
    @Test
    public void testGetDomainIpListWithInvalidFilePath() {
        
        String invalidFilePath = "invalid/path/to/file.json"; //указать неверный путь

        // Подключаем SFTP
        try (SSHClient ssh = new SSHClient()) {
            ssh.addHostKeyVerifier((h, p, k) -> true); 
            ssh.connect("HOST", 22); // указать хост и порт
            ssh.authPassword("user", "password"); // указать имя пользователя и пароль 

            try (SFTPClient sftp = ssh.newSFTPClient()) {
                // ловим исключение
                try {
                    getDomainIpList(sftp, invalidFilePath);
                    fail("Expected IOException was not thrown"); 
                } catch (IOException e) {
                    // Проверяем сообщение исключения
                    assertTrue(e.getMessage().contains("No such file"),
                            "Message must have information about file error.");
                }
            }
        } catch (IOException e) {
            fail("Error test set up: " + e.getMessage());
        }
    }
    
}
