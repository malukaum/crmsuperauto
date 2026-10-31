import { Fila, Perfil, PotencialIndicacao, SituacaoContrato } from '../../core/api/carteira-api.service';
import { TipoContato } from '../../core/api/associados-api.service';
import { TipoStatus } from '../status-chip/status-chip';

/** RG-02 · Telefone gravado (55 + DDD + número) exibido como (37) 9 9869-2950 ou (37) 3321-1234. */
export function formatarTelefone(valor: string | null | undefined): string {
  const digitos = (valor ?? '').replace(/\D/g, '');
  const nacional = digitos.startsWith('55') && digitos.length >= 12 ? digitos.slice(2) : digitos;
  if (nacional.length === 11) {
    return `(${nacional.slice(0, 2)}) ${nacional[2]} ${nacional.slice(3, 7)}-${nacional.slice(7)}`;
  }
  if (nacional.length === 10) {
    return `(${nacional.slice(0, 2)}) ${nacional.slice(2, 6)}-${nacional.slice(6)}`;
  }
  return valor ?? '';
}

/** RG-02 · Placa em maiúsculas e sem hífen. */
export function normalizarPlaca(valor: string): string {
  return valor.replace(/[\s-]/g, '').toUpperCase();
}

/** RG-02 · aaaa-mm-dd → dd/mm/aaaa, sem passar pelo fuso do navegador (a data já é a de São Paulo, RG-01). */
export function formatarData(iso: string | null | undefined): string {
  if (!iso) return '';
  const [ano, mes, dia] = iso.slice(0, 10).split('-');
  return `${dia}/${mes}/${ano}`;
}

/** RG-10 · Abre a conversa no WhatsApp (wa.me); nunca envia nada sozinho. */
export function linkWhatsapp(telefone: string): string {
  return `https://wa.me/${telefone.replace(/\D/g, '')}`;
}

export const NOME_SITUACAO: Record<SituacaoContrato, string> = {
  A_CONFERIR: 'A conferir',
  ATIVO: 'Ativo',
  SUSPENSO: 'Suspenso',
  CANCELADO: 'Cancelado',
};

export const NOME_FILA: Record<Fila, string> = {
  HOJE: 'Hoje',
  ATRASADO: 'Atrasado',
  FUTURO: 'Futuro',
  SEM_DATA: 'Sem data',
  SUSPENSO: 'Suspenso',
  CANCELADO: 'Cancelado',
};

export const NOME_PERFIL: Record<Perfil, string> = {
  REGULAR: 'Regular',
  PROMOTOR: 'Promotor',
  ATENCAO: 'Atenção',
};

export const NOME_POTENCIAL: Record<PotencialIndicacao, string> = {
  NAO_AVALIADO: 'Não avaliado',
  ALTO: 'Alto',
  MEDIO: 'Médio',
  BAIXO: 'Baixo',
};

export const NOME_TIPO_CONTATO: Record<TipoContato, string> = {
  WHATSAPP: 'WhatsApp',
  TELEFONE: 'Telefone',
  EMAIL: 'E-mail',
};

export const NOME_RESULTADO: Record<string, string> = {
  CONCLUIDO: 'Concluído',
  PENDENTE: 'Pendente',
  SEM_RESPOSTA: 'Sem resposta',
  REAGENDADO: 'Reagendado',
};

export const NOME_STATUS_INDICACAO: Record<string, string> = {
  RECEBIDA: 'Recebida',
  EM_NEGOCIACAO: 'Em negociação',
  CONVERTIDA: 'Convertida',
  PERDIDA: 'Perdida',
};

const TIPO_CHIP_FILA: Record<Fila, TipoStatus> = {
  HOJE: 'hoje',
  ATRASADO: 'atrasado',
  FUTURO: 'ativo',
  SEM_DATA: 'neutro',
  SUSPENSO: 'neutro',
  CANCELADO: 'neutro',
};

export function chipDaFila(fila: Fila): TipoStatus {
  return TIPO_CHIP_FILA[fila];
}

/** "Atrasado · 12 dias" ou só o nome da fila. */
export function descreverFila(fila: Fila, diasAtraso: number): string {
  if (fila === 'ATRASADO' && diasAtraso > 0) {
    return `${NOME_FILA.ATRASADO} · ${diasAtraso} ${diasAtraso === 1 ? 'dia' : 'dias'}`;
  }
  return NOME_FILA[fila];
}

/** Ordem de urgência: a ficha mostra no cabeçalho a fila mais urgente entre os contratos. */
const URGENCIA: Fila[] = ['ATRASADO', 'HOJE', 'FUTURO', 'SEM_DATA', 'SUSPENSO', 'CANCELADO'];

export function filaMaisUrgente(filas: Fila[]): Fila | null {
  return URGENCIA.find((f) => filas.includes(f)) ?? null;
}

/** Só dígitos e letras, em maiúsculas: CPF/CNPJ (inclusive alfanumérico) como a API espera. */
export function normalizarDocumento(valor: string): string {
  return valor.replace(/[^0-9A-Za-z]/g, '').toUpperCase();
}
