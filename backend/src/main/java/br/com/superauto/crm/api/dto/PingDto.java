package br.com.superauto.crm.api.dto;

import java.time.OffsetDateTime;

public record PingDto(String area, String mensagem, String usuario, OffsetDateTime dataHora) {
}
