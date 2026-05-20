/** Etiquetas de UI: «socio» → «afiliado» (el código de módulo SOCIOS no cambia). */

export function affiliateLabel(text: string): string {
  return text
    .replace(/\bSocios\b/g, 'Afiliados')
    .replace(/\bSocio\b/g, 'Afiliado')
    .replace(/\bsocios\b/g, 'afiliados')
    .replace(/\bsocio\b/g, 'afiliado');
}

export function moduleDisplayName(code: string, name: string): string {
  if (code === 'SOCIOS') {
    return 'Afiliados';
  }
  return affiliateLabel(name);
}

export function moduleDisplayDescription(code: string, description: string | undefined): string | undefined {
  if (!description) {
    return code === 'SOCIOS' ? 'Gestión de afiliados del gimnasio' : undefined;
  }
  if (code === 'SOCIOS') {
    return affiliateLabel(description);
  }
  return affiliateLabel(description);
}
