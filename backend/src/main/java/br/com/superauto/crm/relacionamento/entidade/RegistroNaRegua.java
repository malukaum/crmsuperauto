package br.com.superauto.crm.relacionamento.entidade;

import java.time.LocalDate;
import java.util.Optional;

/** Resultado e retorno opcional informados ao trabalhar uma etapa. */
public record RegistroNaRegua(ResultadoContato resultado, Optional<LocalDate> retorno) {}
