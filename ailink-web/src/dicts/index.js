import { getPublicDictItemsApi } from '@/api/dict';
import i18n from '@/locales';

const DICT_CACHE_KEY = 'ailink.dict.center.cache.v1';
const DICT_TYPES = ['CATEGORY', 'COUNTRY', 'USER_ROLE', 'DEMAND_STATUS', 'ORDER_STATUS', 'PAY_STATUS'];

const DEFAULT_CATEGORY_PRESETS = [
  '翻译本地化',
  '远程助理',
  '视频剪辑',
  '海外投放',
  '客服支持',
  '平面设计',
  'UI/UX设计',
  '文案策划',
  '社媒运营',
  'SEO优化',
  '网红/KOL合作',
  '网站开发',
  '电商代运营',
  '跨境物流',
  '财税服务',
  '海外公司注册',
  '法律咨询',
];
const DEFAULT_CATEGORY_OPTIONS = DEFAULT_CATEGORY_PRESETS.map((label, index) => ({
  code: label,
  label,
  sort: (index + 1) * 10,
  hot: index < 6,
}));

const DEFAULT_COUNTRY_PRESETS = [
  'Singapore',
  'Japan',
  'UAE',
  'Germany',
  'Australia',
];

const DEFAULT_USER_ROLE_DICT = {
  ADMIN: { label: '管理员', tag: 'danger' },
  USER: { label: '用户', tag: 'info' },
  EMPLOYER: { label: '雇主', tag: 'warning' },
  WORKER: { label: '执行者', tag: 'success' },
};

const DEFAULT_DEMAND_STATUS_DICT = {
  OPEN: { label: '开放中', tag: 'success', groups: ['OPEN'] },
  MATCHED: { label: '已匹配', tag: 'warning', groups: ['ACTIVE'] },
  IN_PROGRESS: { label: '进行中', tag: 'warning', groups: ['ACTIVE'] },
  DONE: { label: '已完成', tag: 'info', groups: ['DONE'] },
  CLOSED: { label: '已关闭', tag: 'danger', groups: ['CLOSED'] },
};

const DEFAULT_ORDER_STATUS_DICT = {
  CREATED: { label: '已创建', tag: 'info', groups: ['ACTIVE'] },
  SERVICE_FEE_REQUIRED: { label: '待支付服务费', tag: 'warning', groups: ['ACTIVE'] },
  SERVICE_FEE_PAID: { label: '服务费已支付', tag: 'info', groups: ['ACTIVE'] },
  WAIT_WORKER_ACCEPT: { label: '待执行者接单', tag: 'warning', groups: ['ACTIVE'] },
  MATCH_UNLOCKED: { label: '已解锁匹配', tag: 'success', groups: ['ACTIVE'] },
  IN_PROGRESS: { label: '进行中', tag: 'warning', groups: ['ACTIVE'] },
  COMPLETED: { label: '已完成', tag: 'success', groups: ['COMPLETED'] },
  DISPUTE: { label: '争议中', tag: 'danger', groups: ['ACTIVE', 'DISPUTE'] },
  ARBITRATION: { label: '仲裁中', tag: 'danger', groups: ['ACTIVE', 'DISPUTE'] },
  CLOSED: { label: '已关闭', tag: 'info', groups: ['CLOSED'] },
};

const DEFAULT_PAY_STATUS_DICT = {
  UNPAID: { label: '待支付', tag: 'info' },
  PAID: { label: '已支付', tag: 'warning' },
  RELEASED: { label: '已释放', tag: 'success' },
  REFUNDED: { label: '已退款', tag: 'danger' },
};

export const CATEGORY_PRESETS = [...DEFAULT_CATEGORY_PRESETS];
export const CATEGORY_OPTIONS = [...DEFAULT_CATEGORY_OPTIONS];
export const COUNTRY_PRESETS = [...DEFAULT_COUNTRY_PRESETS];
export const USER_ROLE_DICT = { ...DEFAULT_USER_ROLE_DICT };
export const DEMAND_STATUS_DICT = { ...DEFAULT_DEMAND_STATUS_DICT };
export const ORDER_STATUS_DICT = { ...DEFAULT_ORDER_STATUS_DICT };
export const PAY_STATUS_DICT = { ...DEFAULT_PAY_STATUS_DICT };

export const USER_ROLE = Object.freeze({
  ADMIN: 'ADMIN',
  USER: 'USER',
  EMPLOYER: 'EMPLOYER',
  WORKER: 'WORKER',
});

export const DEMAND_STATUS = Object.freeze({
  OPEN: 'OPEN',
  MATCHED: 'MATCHED',
  IN_PROGRESS: 'IN_PROGRESS',
  DONE: 'DONE',
  CLOSED: 'CLOSED',
});

export const ORDER_STATUS = Object.freeze({
  CREATED: 'CREATED',
  SERVICE_FEE_REQUIRED: 'SERVICE_FEE_REQUIRED',
  SERVICE_FEE_PAID: 'SERVICE_FEE_PAID',
  WAIT_WORKER_ACCEPT: 'WAIT_WORKER_ACCEPT',
  MATCH_UNLOCKED: 'MATCH_UNLOCKED',
  IN_PROGRESS: 'IN_PROGRESS',
  COMPLETED: 'COMPLETED',
  DISPUTE: 'DISPUTE',
  ARBITRATION: 'ARBITRATION',
  CLOSED: 'CLOSED',
});

export const PAY_STATUS = Object.freeze({
  UNPAID: 'UNPAID',
  PAID: 'PAID',
  RELEASED: 'RELEASED',
  REFUNDED: 'REFUNDED',
});

export const RISK_STATUS = Object.freeze({
  NORMAL: 'NORMAL',
  ABNORMAL: 'ABNORMAL',
});

export const DEMAND_OPEN_STATUSES = [];
export const DEMAND_ACTIVE_STATUSES = [];
export const ORDER_ACTIVE_STATUSES = [];
export const ORDER_COMPLETED_STATUSES = [];
export const ORDER_CLOSED_STATUSES = [];
export const ORDER_FINISHED_STATUSES = [];
export const ORDER_ESCROW_FROZEN_STATUSES = [];
export const ORDER_DISPUTE_STATUSES = [];

let currentVersion = 0;

function overwriteArray(target, source) {
  target.splice(0, target.length, ...source);
}

function overwriteDict(target, source) {
  Object.keys(target).forEach((key) => delete target[key]);
  Object.entries(source).forEach(([key, value]) => {
    target[key] = value;
  });
}

function parseEntryGroups(entry) {
  const groups = [];
  if (entry?.group) {
    groups.push(String(entry.group).trim());
  }
  if (entry?.extra) {
    try {
      const extra = JSON.parse(entry.extra);
      if (Array.isArray(extra?.groups)) {
        extra.groups.forEach((value) => {
          if (value && !groups.includes(value)) {
            groups.push(String(value).trim());
          }
        });
      }
    } catch {
      // ignore malformed extra
    }
  }
  return groups;
}

function parseEntryExtra(entry) {
  if (!entry?.extra) return {};
  try {
    const extra = JSON.parse(entry.extra);
    if (extra && typeof extra === 'object') return extra;
    return {};
  } catch {
    return {};
  }
}

function mapEntriesToCategoryOptions(entries = []) {
  return entries
    .slice()
    .sort((a, b) => Number(a?.sort || 0) - Number(b?.sort || 0))
    .map((entry) => {
      const extra = parseEntryExtra(entry);
      const label = String(entry?.label || entry?.code || '').trim();
      const code = String(entry?.code || label).trim();
      if (!label || !code) return null;
      return {
        code,
        label,
        sort: Number(entry?.sort || 0),
        hot: Boolean(extra?.hot),
      };
    })
    .filter(Boolean);
}

function mapEntriesToPresets(entries = []) {
  return entries
    .slice()
    .sort((a, b) => Number(a?.sort || 0) - Number(b?.sort || 0))
    .map((entry) => entry?.label || entry?.code)
    .filter(Boolean);
}

function mapEntriesToDict(entries = []) {
  const rows = entries
    .slice()
    .sort((a, b) => Number(a?.sort || 0) - Number(b?.sort || 0));
  return rows.reduce((acc, entry) => {
    const code = entry?.code;
    if (!code) return acc;
    acc[code] = {
      label: entry?.label || code,
      tag: entry?.tag || '',
      groups: parseEntryGroups(entry),
    };
    return acc;
  }, {});
}

function parseCache() {
  try {
    const text = localStorage.getItem(DICT_CACHE_KEY);
    if (!text) return null;
    const data = JSON.parse(text);
    if (!data || typeof data !== 'object') return null;
    return data;
  } catch {
    return null;
  }
}

function saveCache(data) {
  try {
    localStorage.setItem(DICT_CACHE_KEY, JSON.stringify(data));
  } catch {
    // ignore cache write errors
  }
}

function applyDefaultDicts() {
  overwriteArray(CATEGORY_PRESETS, DEFAULT_CATEGORY_PRESETS);
  overwriteArray(CATEGORY_OPTIONS, DEFAULT_CATEGORY_OPTIONS);
  overwriteArray(COUNTRY_PRESETS, DEFAULT_COUNTRY_PRESETS);
  overwriteDict(USER_ROLE_DICT, DEFAULT_USER_ROLE_DICT);
  overwriteDict(DEMAND_STATUS_DICT, DEFAULT_DEMAND_STATUS_DICT);
  overwriteDict(ORDER_STATUS_DICT, DEFAULT_ORDER_STATUS_DICT);
  overwriteDict(PAY_STATUS_DICT, DEFAULT_PAY_STATUS_DICT);
  rebuildStatusGroups();
}

function applyRemoteItems(items = {}) {
  const categories = mapEntriesToCategoryOptions(items.CATEGORY || []);
  const countries = mapEntriesToPresets(items.COUNTRY || []);
  const roleDict = mapEntriesToDict(items.USER_ROLE || []);
  const demandDict = mapEntriesToDict(items.DEMAND_STATUS || []);
  const orderDict = mapEntriesToDict(items.ORDER_STATUS || []);
  const payDict = mapEntriesToDict(items.PAY_STATUS || []);

  if (categories.length) {
    overwriteArray(CATEGORY_OPTIONS, categories);
    overwriteArray(CATEGORY_PRESETS, categories.map((item) => item.label));
  }
  if (countries.length) overwriteArray(COUNTRY_PRESETS, countries);
  if (Object.keys(roleDict).length) overwriteDict(USER_ROLE_DICT, roleDict);
  if (Object.keys(demandDict).length) overwriteDict(DEMAND_STATUS_DICT, demandDict);
  if (Object.keys(orderDict).length) overwriteDict(ORDER_STATUS_DICT, orderDict);
  if (Object.keys(payDict).length) overwriteDict(PAY_STATUS_DICT, payDict);
  rebuildStatusGroups();
}

function getCodesByGroup(dict, groupName) {
  return Object.entries(dict)
    .filter(([, meta]) => Array.isArray(meta?.groups) && meta.groups.includes(groupName))
    .map(([code]) => code);
}

function rebuildStatusGroups() {
  overwriteArray(DEMAND_OPEN_STATUSES, getCodesByGroup(DEMAND_STATUS_DICT, 'OPEN'));
  overwriteArray(DEMAND_ACTIVE_STATUSES, getCodesByGroup(DEMAND_STATUS_DICT, 'ACTIVE'));
  overwriteArray(ORDER_ACTIVE_STATUSES, getCodesByGroup(ORDER_STATUS_DICT, 'ACTIVE'));
  overwriteArray(ORDER_COMPLETED_STATUSES, getCodesByGroup(ORDER_STATUS_DICT, 'COMPLETED'));
  overwriteArray(ORDER_CLOSED_STATUSES, getCodesByGroup(ORDER_STATUS_DICT, 'CLOSED'));
  overwriteArray(ORDER_ESCROW_FROZEN_STATUSES, getCodesByGroup(ORDER_STATUS_DICT, 'ESCROW_FROZEN'));
  overwriteArray(ORDER_DISPUTE_STATUSES, getCodesByGroup(ORDER_STATUS_DICT, 'DISPUTE'));
  overwriteArray(ORDER_FINISHED_STATUSES, [...ORDER_COMPLETED_STATUSES, ...ORDER_CLOSED_STATUSES]);
}

function pickDictItem(dict, key) {
  if (!key) return null;
  return dict[key] || null;
}

function getI18nText(key) {
  try {
    if (i18n?.global?.te?.(key)) {
      return i18n.global.t(key);
    }
  } catch {
    // ignore i18n lookup errors
  }
  return '';
}

function getI18nDictLabel(namespace, key) {
  const code = String(key || '').trim();
  if (!code) return '';
  return getI18nText(`dict.${namespace}.${code}`);
}

export function getDictLabel(dict, key, fallback = '') {
  const item = pickDictItem(dict, key);
  const fallbackText = fallback || getI18nText('common.unknown') || 'Unknown';
  return item?.label || key || fallbackText;
}

export function getDictTag(dict, key, fallback = '') {
  const item = pickDictItem(dict, key);
  return item?.tag || fallback;
}

export function toSelectOptions(dict) {
  return Object.entries(dict).map(([value, meta]) => ({
    value,
    label: meta?.label || value,
  }));
}

export function getDemandStatusLabel(status) {
  return getI18nDictLabel('demandStatus', status) || getDictLabel(DEMAND_STATUS_DICT, status);
}

export function getDemandStatusTag(status) {
  return getDictTag(DEMAND_STATUS_DICT, status);
}

export function getOrderStatusLabel(status) {
  return getI18nDictLabel('orderStatus', status) || getDictLabel(ORDER_STATUS_DICT, status);
}

export function getOrderStatusTag(status) {
  return getDictTag(ORDER_STATUS_DICT, status, 'info');
}

export function getPayStatusLabel(status) {
  return getI18nDictLabel('payStatus', status) || getDictLabel(PAY_STATUS_DICT, status, '—');
}

export function getPayStatusTag(status) {
  return getDictTag(PAY_STATUS_DICT, status, 'info');
}

export function getRoleLabel(role) {
  return getI18nDictLabel('userRole', role)
    || getDictLabel(USER_ROLE_DICT, role, getI18nText('dict.userRole.USER') || 'User');
}

export function getRoleTag(role) {
  return getDictTag(USER_ROLE_DICT, role, 'info');
}

export async function initDictCenter() {
  applyDefaultDicts();

  const cache = parseCache();
  if (cache?.items) {
    applyRemoteItems(cache.items);
    currentVersion = Number(cache.version || 0);
  }

  try {
    const payload = await getPublicDictItemsApi({
      types: DICT_TYPES.join(','),
      version: currentVersion || undefined,
      silentBusinessError: true,
      silentHttpError: true,
      ignoreErrorStatuses: [401, 403, 404, 500],
    });
    const remoteVersion = Number(payload?.version || 0);
    if (payload?.unchanged) {
      currentVersion = remoteVersion || currentVersion;
      return;
    }
    if (payload?.items && Object.keys(payload.items).length) {
      applyRemoteItems(payload.items);
      currentVersion = remoteVersion;
      saveCache({
        version: currentVersion,
        items: payload.items,
        cachedAt: Date.now(),
      });
    }
  } catch {
    // keep defaults or cache fallback
  }
}

applyDefaultDicts();
