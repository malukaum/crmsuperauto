import { FiltroHistorico } from '../../../core/api/relacionamento-api.service';

/** ATD-05 · Filtro inicial: sem datas, o backend usa o mês corrente (RG-01). */
export const FILTRO_INICIAL: FiltroHistorico = {
  inicio: null,
  fim: null,
  tipo: null,
  resultado: null,
  usuario: null,
  cidade: null,
  busca: '',
};

/** Marca de correção da linha (ATD-04): o original corrigido ou o registro que corrige outro. */
export function marcaDeCorrecao(linha: {
  corrigido: boolean;
  corrigeAtendimentoId: number | null;
}): 'corrigido' | 'correção' | null {
  if (linha.corrigido) return 'corrigido';
  return linha.corrigeAtendimentoId === null ? null : 'correção';
}
