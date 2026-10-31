/** Estilos comuns dos diálogos da ficha. */
export const ESTILOS_DIALOGO = `
  .campos {
    display: flex;
    flex-direction: column;
    gap: 4px;
    min-width: min(440px, 80vw);
  }
  .linha {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 12px;
  }
  @media (max-width: 599.98px) {
    .linha {
      grid-template-columns: 1fr;
      gap: 4px;
    }
  }
  .ajuda {
    margin: 0 0 12px;
    color: var(--crm-texto-secundario);
  }
  .erro {
    margin: 4px 0 0;
    color: var(--mat-sys-error);
  }
`;
