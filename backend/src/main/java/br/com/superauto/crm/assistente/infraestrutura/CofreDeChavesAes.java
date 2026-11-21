package br.com.superauto.crm.assistente.infraestrutura;

import br.com.superauto.crm.assistente.repositorio.CofreDeChaves;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * IA-05 · Cifra as chaves dos provedores com AES-GCM. A chave-mestra vem da configuração do servidor
 * ({@code crm.ia.chave-mestra}, por variável de ambiente em produção) e nunca vai para o banco.
 */
@Component
public class CofreDeChavesAes implements CofreDeChaves {
    private static final String ALGORITMO = "AES/GCM/NoPadding";
    private static final int TAMANHO_DO_IV = 12;
    private static final int TAMANHO_DA_ETIQUETA = 128;

    private final SecretKeySpec chaveMestra;
    private final SecureRandom aleatorio = new SecureRandom();

    public CofreDeChavesAes(@Value("${crm.ia.chave-mestra}") String chaveMestra) {
        this.chaveMestra = new SecretKeySpec(sha256(chaveMestra), "AES");
    }

    @Override
    public String cifrar(String chave) {
        try {
            byte[] iv = new byte[TAMANHO_DO_IV];
            aleatorio.nextBytes(iv);
            Cipher cifra = Cipher.getInstance(ALGORITMO);
            cifra.init(Cipher.ENCRYPT_MODE, chaveMestra, new GCMParameterSpec(TAMANHO_DA_ETIQUETA, iv));
            byte[] cifrado = cifra.doFinal(chave.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + cifrado.length).put(iv).put(cifrado).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Não foi possível cifrar a chave do provedor.", e);
        }
    }

    @Override
    public String decifrar(String chaveCifrada) {
        try {
            ByteBuffer dados = ByteBuffer.wrap(Base64.getDecoder().decode(chaveCifrada));
            byte[] iv = new byte[TAMANHO_DO_IV];
            dados.get(iv);
            byte[] cifrado = new byte[dados.remaining()];
            dados.get(cifrado);
            Cipher cifra = Cipher.getInstance(ALGORITMO);
            cifra.init(Cipher.DECRYPT_MODE, chaveMestra, new GCMParameterSpec(TAMANHO_DA_ETIQUETA, iv));
            return new String(cifra.doFinal(cifrado), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Não foi possível ler a chave do provedor. A chave-mestra mudou?", e);
        }
    }

    private static byte[] sha256(String texto) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
