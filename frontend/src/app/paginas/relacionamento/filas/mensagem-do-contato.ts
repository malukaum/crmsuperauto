import { LinhaTrabalho } from '../../../core/api/relacionamento-api.service';
import { linkWhatsapp } from '../../../shared/formatos/formatos';
/** FIL-05 · Texto sugerido que a equipe pode editar antes de abrir a conversa. */
export function mensagemInicial(linha: LinhaTrabalho, aniversario: boolean): string {
  return aniversario
    ? `Olá, ${linha.nome}! A Super Auto deseja um feliz aniversário!`
    : `Olá, ${linha.nome}! Aqui é da Super Auto. Como está sua experiência com a proteção do seu veículo?`;
}
export function conversaComMensagem(telefone: string, mensagem: string): string {
  return linkWhatsapp(telefone) + (mensagem ? '?text=' + encodeURIComponent(mensagem) : '');
}
