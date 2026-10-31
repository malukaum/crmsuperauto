export type Papel = 'gerencial' | 'vendedor' | 'atendente' | 'cliente';

export const PAPEIS: readonly Papel[] = ['gerencial', 'vendedor', 'atendente', 'cliente'];

/** Papéis que usam o layout interno (menu lateral). */
export const EQUIPE_INTERNA: readonly Papel[] = ['gerencial', 'vendedor', 'atendente'];

/** Papéis que alguém da equipe pode ter (contas de cliente nascem pelo primeiro acesso). */
export type PapelEquipe = Exclude<Papel, 'cliente'>;
export const PAPEIS_EQUIPE: readonly PapelEquipe[] = ['gerencial', 'vendedor', 'atendente'];

export const GERENCIAL_OU_VENDEDOR: readonly Papel[] = ['gerencial', 'vendedor'];

export const NOME_PAPEL: Record<Papel, string> = {
  gerencial: 'Gerencial',
  vendedor: 'Vendedor',
  atendente: 'Atendente',
  cliente: 'Cliente',
};

export function ehPapel(valor: unknown): valor is Papel {
  return typeof valor === 'string' && (PAPEIS as readonly string[]).includes(valor);
}
