package com.atguigu.gmall.auth;

import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.common.utils.RsaUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

public class JwtTest {

    // 别忘了创建D:\\project\rsa目录
	private static final String pubKeyPath = "D:\\project-0420\\rsa\\rsa.pub";
    private static final String priKeyPath = "D:\\project-0420\\rsa\\rsa.pri";

    private PublicKey publicKey;

    private PrivateKey privateKey;

    @Test
    public void testRsa() throws Exception {
        RsaUtils.generateKey(pubKeyPath, priKeyPath, "#$#332#$sfsdSDSD%");
    }

    @BeforeEach
    public void testGetRsa() throws Exception {
        this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        this.privateKey = RsaUtils.getPrivateKey(priKeyPath);
    }

    @Test
    public void testGenerateToken() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("id", "11");
        map.put("username", "liuyan");
        // 生成token
        String token = JwtUtils.generateToken(map, privateKey, 1);
        System.out.println("token = " + token);
    }

    @Test
    public void testParseToken() throws Exception {
        String token = "eyJhbGciOiJSUzI1NiJ9.eyJpZCI6IjExIiwidXNlcm5hbWUiOiJsaXV5YW4iLCJleHAiOjE2MDI2Mzg1MDB9.hSX8nMQPrXp8xFkCxPZfiy6NAzepdFe9aDXfzN2IsTiyKFmxRgO9eNgaZo5EBkeGLsjemyX08_Z1emi50OENHDjII4yu_cmqlvfqPnTnQQCex889hTcSrrggMqNE_OcsJgpM9LuAUdTnAJjU1syBJFIOWStIdVxHrpj48mN-54RjAoS_vp-hWtMljyIp7HnDW_N7AE6uj6fkigQzDv8CYA9CAgw5TLTdH_e7OPcG9ivI7ZxFI4zlZ07FSkXxuCDvuiKmGHEmZUSvhjARH1QWL8jWlM0nOxpyIG1PSfzlkG5yti8GkF7dfpupRd3_5r7FD-qSkRjIfVqu5PPrlelyCg";

        // 解析token
        Map<String, Object> map = JwtUtils.getInfoFromToken(token, publicKey);
        System.out.println("id: " + map.get("id"));
        System.out.println("userName: " + map.get("username"));
    }
}
