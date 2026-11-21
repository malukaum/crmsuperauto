package br.com.superauto.crm.compartilhado.entidade;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Em que fila o contrato está (RG-14). O "hoje" é sempre o de Brasília, vindo do relógio da aplicação (RG-01),
 * nunca da data do banco. Núcleo compartilhado: as filas (relacionamento) e a carteira (cadastro) falam a mesma
 * língua.
 */
public enum Fila {
    ATRASADO,
    HOJE,
    FUTURO,
    SEM_DATA,
    SUSPENSO,
    CANCELADO;

    /** A fila pela data do próximo contato de uma régua ativa, para contratos A conferir ou Ativo. */
    public static Fila daAgenda(Optional<LocalDate> proximoContato, LocalDate hoje) {
        return proximoContato
                .map(data -> data.isBefore(hoje) ? ATRASADO : data.isEqual(hoje) ? HOJE : FUTURO)
                .orElse(SEM_DATA);
    }

    /** Quantos dias o próximo contato já passou de hoje; zero se ainda não venceu ou se não há data. */
    public static int diasDeAtraso(Optional<LocalDate> proximoContato, LocalDate hoje) {
        return proximoContato
                .map(data -> (int) Math.max(0, ChronoUnit.DAYS.between(data, hoje)))
                .orElse(0);
    }
}
