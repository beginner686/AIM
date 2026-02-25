export function formatMoney(value) {
  const num = Number(value ?? 0);
  if (Number.isNaN(num)) {
    return '0.00';
  }
  return num.toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}
