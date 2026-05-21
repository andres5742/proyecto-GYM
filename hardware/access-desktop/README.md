# Sport Gym Acceso — aplicación de escritorio (.exe)

Ventana dedicada con [https://sportgymr10.com/acceso](https://sportgymr10.com/acceso), lector de tarjetas y **F2 / F3** para abrir el torniquete (igual que en Facturación).

## Requisitos

- Windows 10/11
- Node.js LTS (solo para **compilar** el .exe)
- Python + lector en `../turnstile-gateway/`
- `TURNSTILE_WEBHOOK` configurado en el servidor para la puerta física

## Configuración

1. Copie `config.example.json` → `config.json`
2. Ajuste `accessDeviceKey` (igual que `deploy/.env`)
3. `turnstileGatewayDir`: ruta a `hardware/turnstile-gateway`

## Compilar el .exe

```bat
compilar-acceso-electron.bat
```

Salida: `dist/SportGym-Acceso-1.0.0.exe`

## Uso diario

- Ejecute el `.exe` (o `npm start` en desarrollo)
- Arranca el lector (`iniciar-lector-tarjeta.bat`) y la pantalla de acceso
- **F2** — entreno del día → abre torniquete
- **F3** — bailes deportivos → abre torniquete

También funciona en el navegador en `/acceso` tras desplegar frontend + backend (sin .exe).

## Nota

F2/F3 en **Facturación** (panel con login) registran el pago y abren torniquete vía el mismo `TURNSTILE_WEBHOOK`.  
F2/F3 en **esta app** solo envían el pulso de puerta en la entrada (invitado / pase rápido).
