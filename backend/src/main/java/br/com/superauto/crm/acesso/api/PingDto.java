package br.com.superauto.crm.acesso.api;

import java.time.OffsetDateTime;

public record PingDto(String area, String mensagem, String usuario, OffsetDateTime dataHora) {
}
