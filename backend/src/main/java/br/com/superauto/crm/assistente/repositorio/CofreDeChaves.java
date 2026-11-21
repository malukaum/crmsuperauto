package br.com.superauto.crm.assistente.repositorio;

/** IA-05 · Porta para guardar as chaves dos provedores cifradas; a chave em claro existe só durante a chamada. */
public interface CofreDeChaves {

    String cifrar(String chave);

    String decifrar(String chaveCifrada);
}
