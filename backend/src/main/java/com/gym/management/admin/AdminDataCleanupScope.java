package com.gym.management.admin;

public enum AdminDataCleanupScope {
    BILLING("Facturación", "Pagos, gastos y cajas del día"),
    SALES_AND_SHIFTS("Ventas y turnos", "Ventas, turnos, entregas de caja y faltantes"),
    FIADO("Fiado", "Fiados y abonos de productos"),
    ACCESS_LOGS("Registro de acceso", "Historial de ingresos al torniquete"),
    MEMBER_BIOMETRICS("Biometría afiliados", "Huellas y rostro de afiliados (no borra afiliados)"),
    MEMBERS("Afiliados", "Todos los afiliados y datos vinculados a ellos"),
    FEEDBACK("Buzón", "Sugerencias, quejas y felicitaciones"),
    TRAINER_RATINGS("Calificaciones", "Calificaciones mensuales de entrenadores"),
    WORK_ATTENDANCE("Jornada laboral", "Entradas y salidas del personal"),
    WALL_POSTS("Publicaciones", "Publicaciones del muro de inicio"),
    ALL_OPERATIONAL(
            "Todo el operativo",
            "Limpia todos los datos operativos anteriores sin borrar empleados, planes, inventario ni configuración");

    private final String label;
    private final String description;

    AdminDataCleanupScope(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }
}
