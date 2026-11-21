package br.com.superauto.crm.relacionamento.repositorio;

import java.util.UUID;

/** Uma opção do filtro de usuário do histórico (ATD-05), inclusive "Importação da planilha". */
public record AutorDeAtendimento(UUID id, String nome) {}
