/** Entrega ao navegador um arquivo gerado pela API (exportações CAR-07 e ATD-05). */
export function baixar(arquivo: Blob, nome: string): void {
  const url = URL.createObjectURL(arquivo);
  const link = document.createElement('a');
  link.href = url;
  link.download = nome;
  link.click();
  URL.revokeObjectURL(url);
}
