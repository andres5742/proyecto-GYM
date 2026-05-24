import {
  DailyBusinessReport,
  MonthlyBusinessReport,
  ReportView,
} from '../models/report.model';

const MONTH_NAMES = [
  'Enero',
  'Febrero',
  'Marzo',
  'Abril',
  'Mayo',
  'Junio',
  'Julio',
  'Agosto',
  'Septiembre',
  'Octubre',
  'Noviembre',
  'Diciembre',
] as const;

function cashRegisterLabel(status: string): string {
  switch (status) {
    case 'OPEN':
      return 'Caja abierta';
    case 'CLOSED':
      return 'Caja cerrada';
    default:
      return 'Sin caja registrada';
  }
}

export function toDailyReportView(report: DailyBusinessReport): ReportView {
  const {
    date,
    cashRegisterStatus,
    openingCashAmount,
    ...core
  } = report;
  return {
    ...core,
    period: 'daily',
    title: `Reporte del día — ${date}`,
    printTitle: date,
    incomeLabel: 'Ingresos del día',
    resultLabel: 'Resultado del día',
    inventoryTitle: 'Inventario y ventas del día',
    inventoryHint: 'Stock actual y unidades vendidas en la fecha seleccionada.',
    soldColumnLabel: 'Vendidos hoy',
    salesColumnLabel: 'Ventas hoy',
    cajaLabel: cashRegisterLabel(cashRegisterStatus),
    cajaMeta: 'Base inicial de caja',
    openingCashAmount,
    emptyIncomeHint: 'Sin ingresos registrados este día.',
    exportBaseName: `reporte-dia-${date}`,
  };
}

export function toMonthlyReportView(report: MonthlyBusinessReport): ReportView {
  const { year, month, startDate, endDate, cashRegisterDays, ...core } = report;
  const monthName = MONTH_NAMES[month - 1] ?? String(month);
  return {
    ...core,
    period: 'monthly',
    title: `Reporte mensual — ${monthName} ${year}`,
    printTitle: `${monthName} ${year}`,
    incomeLabel: 'Ingresos del mes',
    resultLabel: 'Resultado del mes',
    inventoryTitle: 'Inventario y ventas del mes',
    inventoryHint: `Stock actual y unidades vendidas del ${startDate} al ${endDate}.`,
    soldColumnLabel: 'Vendidos en el mes',
    salesColumnLabel: 'Ventas en el mes',
    cajaLabel: `${cashRegisterDays} día(s) con caja`,
    cajaMeta: `Período: ${startDate} — ${endDate}`,
    emptyIncomeHint: 'Sin ingresos registrados en este mes.',
    exportBaseName: `reporte-mes-${year}-${String(month).padStart(2, '0')}`,
  };
}
