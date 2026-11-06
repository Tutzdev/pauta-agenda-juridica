'use strict';

/* ================================================================
   Domínio: rótulos e sugestões
   ================================================================ */

const TYPES = {
  HEARING: {
    label: 'Audiência', icon: 'gavel', placeholder: 'Ex.: Audiência de conciliação',
    suggestions: ['Audiência de conciliação', 'Audiência de instrução e julgamento', 'Audiência una', 'Audiência de custódia', 'Sessão de julgamento']
  },
  DEADLINE: {
    label: 'Prazo', icon: 'file-clock', placeholder: 'Ex.: Contestação',
    suggestions: ['Contestação', 'Réplica', 'Recurso de apelação', 'Contrarrazões', 'Embargos de declaração', 'Agravo de instrumento', 'Recurso inominado', 'Manifestação', 'Alegações finais', 'Cumprimento de sentença']
  },
  MEETING: {
    label: 'Reunião', icon: 'users', placeholder: 'Ex.: Reunião sobre acordo',
    suggestions: ['Reunião sobre acordo', 'Ligação com o perito', 'Reunião com a parte contrária', 'Videochamada']
  },
  CLIENT_SERVICE: {
    label: 'Atendimento', icon: 'handshake', placeholder: 'Ex.: Primeira consulta',
    suggestions: ['Primeira consulta', 'Retorno da consulta', 'Assinatura de procuração', 'Entrega de documentos']
  },
  DILIGENCE: {
    label: 'Diligência', icon: 'map-pin', placeholder: 'Ex.: Cópia dos autos no cartório',
    suggestions: ['Cópia dos autos no cartório', 'Protocolo presencial', 'Despacho com o juiz', 'Retirada de alvará']
  },
  OTHER: {
    label: 'Outro', icon: 'dots', placeholder: 'Ex.: Pagar anuidade da OAB',
    suggestions: ['Pagar anuidade da OAB', 'Emitir guia de custas', 'Conferir publicações']
  }
};

const VIEWS = { painel: 'Painel', agenda: 'Agenda', prazos: 'Prazos', clientes: 'Clientes' };
const MS_PER_DAY = 86_400_000;
const CELLS_PER_MONTH = 42;
const MAX_ITEMS_PER_CELL = 3;

/* ================================================================
   Utilitários
   ================================================================ */

const $ = (selector, root = document) => root.querySelector(selector);
const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];

const escapeHtml = (value = '') => String(value).replace(/[&<>"']/g, (char) => ({
  '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;'
}[char]));

const icon = (name) => `<svg class="icon" aria-hidden="true"><use href="#i-${name}"/></svg>`;
const pad2 = (value) => String(value).padStart(2, '0');
const plural = (count, singular, pluralForm) => `${count} ${count === 1 ? singular : pluralForm}`;

/* Datas trafegam como "AAAA-MM-DD" e viram Date local ao meio-dia, para nunca escorregar de dia por fuso. */
const toIso = (date) => `${date.getFullYear()}-${pad2(date.getMonth() + 1)}-${pad2(date.getDate())}`;
const parseIso = (iso) => { const [y, m, d] = iso.split('-').map(Number); return new Date(y, m - 1, d, 12); };
const addDays = (iso, days) => { const date = parseIso(iso); date.setDate(date.getDate() + days); return toIso(date); };
const daysBetween = (fromIso, toIsoDate) => Math.round((parseIso(toIsoDate) - parseIso(fromIso)) / MS_PER_DAY);
const format = (iso, options) => new Intl.DateTimeFormat('pt-BR', options).format(parseIso(iso));
const capitalize = (text) => text.charAt(0).toUpperCase() + text.slice(1);
const longDate = (iso) => capitalize(format(iso, { weekday: 'long', day: 'numeric', month: 'long' }));
const fullDate = (iso) => capitalize(format(iso, { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }));
const shortDate = (iso) => format(iso, { day: '2-digit', month: '2-digit', year: 'numeric' });
const isWeekend = (iso) => [0, 6].includes(parseIso(iso).getDay());

const initials = (name = '') => name.split(/\s+/).filter((part) => part.length > 2 || /^[A-Z]/.test(part))
  .slice(0, 2).map((part) => part[0]).join('').toUpperCase() || '?';

function formatPhone(digits) {
  if (!digits) return '';
  const area = digits.slice(0, 2);
  const rest = digits.slice(2);
  return rest.length === 9 ? `(${area}) ${rest.slice(0, 5)}-${rest.slice(5)}` : `(${area}) ${rest.slice(0, 4)}-${rest.slice(4)}`;
}

const whatsappUrl = (digits) => `https://wa.me/55${digits}`;

/** Máscara progressiva do número CNJ: NNNNNNN-DD.AAAA.J.TR.OOOO */
function maskProcessNumber(value) {
  const digits = value.replace(/\D/g, '').slice(0, 20);
  const parts = [[0, 7, ''], [7, 9, '-'], [9, 13, '.'], [13, 14, '.'], [14, 16, '.'], [16, 20, '.']];
  return parts.reduce((text, [start, end, sep]) => digits.length > start ? text + sep + digits.slice(start, end) : text, '');
}

function maskPhone(value) {
  const digits = value.replace(/\D/g, '').slice(0, 11);
  if (digits.length <= 2) return digits.length ? `(${digits}` : '';
  const area = digits.slice(0, 2);
  const rest = digits.slice(2);
  const split = rest.length > 8 ? 5 : 4;
  return rest.length > split ? `(${area}) ${rest.slice(0, split)}-${rest.slice(split)}` : `(${area}) ${rest}`;
}

function greeting() {
  const hour = new Date().getHours();
  if (hour < 12) return 'Bom dia';
  if (hour < 18) return 'Boa tarde';
  return 'Boa noite';
}

/** "Dra. Helena Duarte" vira "Dra. Helena"; "Helena Duarte" vira "Helena". */
function shortName(name) {
  const parts = name.split(/\s+/).filter(Boolean);
  if (!parts.length) return '';
  return /^dr(a)?\.?$/i.test(parts[0]) ? parts.slice(0, 2).join(' ') : parts[0];
}

/* ================================================================
   API
   ================================================================ */

class ApiError extends Error {
  constructor(message, { status, code, fieldErrors } = {}) {
    super(message);
    this.status = status;
    this.code = code;
    this.fieldErrors = fieldErrors || {};
  }
}

const api = {
  async request(url, { method = 'GET', body } = {}) {
    let response;
    try {
      response = await fetch(url, {
        method,
        headers: body === undefined ? {} : { 'Content-Type': 'application/json' },
        body: body === undefined ? undefined : JSON.stringify(body)
      });
    } catch {
      throw new ApiError('Sem conexão com a agenda. Confira se ela está aberta neste computador.');
    }
    if (response.ok) return response.status === 204 ? null : response.json();

    let problem = {};
    try { problem = await response.json(); } catch { /* resposta sem corpo */ }
    throw new ApiError(problem.detail || 'Não foi possível concluir a operação.', {
      status: response.status, code: problem.code, fieldErrors: problem.fieldErrors
    });
  },

  office: () => api.request('/api/office'),
  dashboard: () => api.request('/api/dashboard'),
  between: (from, to) => api.request(`/api/appointments?${new URLSearchParams({ from, to })}`),
  byClient: (clientId) => api.request(`/api/appointments?clientId=${clientId}`),
  deadlines: () => api.request('/api/appointments/deadlines'),
  conflicts: (params) => api.request(`/api/appointments/conflicts?${new URLSearchParams(params)}`),
  createAppointment: (payload) => api.request('/api/appointments', { method: 'POST', body: payload }),
  updateAppointment: (id, payload) => api.request(`/api/appointments/${id}`, { method: 'PUT', body: payload }),
  complete: (id) => api.request(`/api/appointments/${id}/complete`, { method: 'POST' }),
  cancel: (id) => api.request(`/api/appointments/${id}/cancel`, { method: 'POST' }),
  reopen: (id) => api.request(`/api/appointments/${id}/reopen`, { method: 'POST' }),
  deleteAppointment: (id) => api.request(`/api/appointments/${id}`, { method: 'DELETE' }),
  clients: (search = '') => api.request(`/api/clients${search ? `?${new URLSearchParams({ search })}` : ''}`),
  createClient: (payload) => api.request('/api/clients', { method: 'POST', body: payload }),
  updateClient: (id, payload) => api.request(`/api/clients/${id}`, { method: 'PUT', body: payload }),
  deleteClient: (id) => api.request(`/api/clients/${id}`, { method: 'DELETE' }),
  calculate: (startDate, businessDays) => api.request(`/api/deadlines/calculate?${new URLSearchParams({ startDate, businessDays })}`),
  closedDays: (from, to) => api.request(`/api/deadlines/closed-days?${new URLSearchParams({ from, to })}`)
};

/* ================================================================
   Estado
   ================================================================ */

const state = {
  office: null,
  today: toIso(new Date()),
  view: 'painel',
  month: null,
  selectedDate: null,
  monthItems: [],
  closedDays: new Map(),
  byId: new Map(),
  clients: [],
  editing: null,
  editingClient: null,
  lastCalculation: null
};

const remember = (items) => { items.forEach((item) => state.byId.set(item.id, item)); return items; };

/* ================================================================
   Componentes de interface
   ================================================================ */

const ui = {
  skeleton: (rows = 3) => `<div class="skeleton" aria-busy="true" aria-label="Carregando">${'<span></span>'.repeat(rows)}</div>`,

  empty: (title, text, { success = false, action = '' } = {}) => `
    <div class="state ${success ? 'state--success' : ''}">
      <span class="state-icon">${icon(success ? 'check-circle' : 'inbox')}</span>
      <div><h3>${title}</h3><p>${text}</p></div>${action}
    </div>`,

  error(target, message, retry) {
    target.innerHTML = `<div class="alert" role="alert">${icon('alert')}<span>${escapeHtml(message)}</span>
      ${retry ? '<button class="button button--secondary button--sm" type="button">Tentar de novo</button>' : ''}</div>`;
    if (retry) $('button', target).addEventListener('click', retry, { once: true });
  },

  pill: (label, tone = '') => `<span class="pill ${tone ? `pill--${tone}` : ''}">${escapeHtml(label)}</span>`,

  toast(message, action) {
    const toast = document.createElement('div');
    toast.className = 'toast';
    toast.innerHTML = `${icon('check')}<span>${escapeHtml(message)}</span>${action ? `<button type="button">${escapeHtml(action.label)}</button>` : ''}`;
    $('#toasts').append(toast);
    const dismiss = () => {
      toast.classList.add('is-leaving');
      toast.addEventListener('animationend', () => toast.remove(), { once: true });
    };
    if (action) {
      $('button', toast).addEventListener('click', () => { dismiss(); action.run(); }, { once: true });
    }
    setTimeout(dismiss, action ? 6000 : 3000);
  },

  confirm(title, text, confirmLabel = 'Excluir') {
    const dialog = $('#confirm-dialog');
    $('#confirm-title').textContent = title;
    $('#confirm-text').textContent = text;
    $('#confirm-button').textContent = confirmLabel;
    dialog.returnValue = '';
    dialog.showModal();
    return new Promise((resolve) => {
      dialog.addEventListener('close', () => resolve(dialog.returnValue === 'confirm'), { once: true });
    });
  },

  /** Larguras e alturas das barras vêm de data-value: a CSP não deixa usar style inline no HTML. */
  applyBarValues(root) {
    $$('[data-value]', root).forEach((element) => element.style.setProperty('--value', element.dataset.value));
  }
};

/* ---------- Compromisso em linha ---------- */

function alertPill(item) {
  const deadline = item.type === 'DEADLINE';
  switch (item.alert) {
    case 'OVERDUE': {
      const days = daysBetween(item.date, state.today);
      const since = days === 1 ? 'ontem' : `há ${days} dias`;
      return ui.pill(deadline ? `Vencido ${since}` : `Ficou para trás (${since})`, 'danger');
    }
    case 'TODAY': return deadline ? ui.pill('Vence hoje', 'warning') : '';
    case 'UPCOMING': {
      const days = daysBetween(state.today, item.date);
      return ui.pill(days === 1 ? 'Vence amanhã' : `Vence em ${days} dias`, 'accent');
    }
    default:
      if (item.status === 'DONE') return ui.pill('Feito', 'success');
      if (item.status === 'CANCELED') return ui.pill('Cancelado');
      return '';
  }
}

function whenBlock(item, showDate) {
  const tone = item.alert === 'OVERDUE' ? 'row-when--danger' : item.alert === 'TODAY' && item.type === 'DEADLINE' ? 'row-when--warning' : '';
  if (showDate) {
    const month = format(item.date, { month: 'short' }).replace('.', '');
    return `<span class="row-when ${tone}"><strong>${format(item.date, { day: '2-digit' })}</strong><small>${month}</small></span>`;
  }
  if (item.startTime) {
    return `<span class="row-when ${tone}"><strong>${item.startTime}</strong><small>${item.endTime ? `até ${item.endTime}` : ''}</small></span>`;
  }
  return `<span class="row-when ${tone}"><small>${item.type === 'DEADLINE' ? 'prazo' : 'dia'}</small><strong>${item.type === 'DEADLINE' ? icon('file-clock') : 'todo'}</strong></span>`;
}

function rowActions(item) {
  if (item.status === 'SCHEDULED') {
    return `<button class="action" type="button" data-action="complete" data-id="${item.id}" aria-label="Marcar ${escapeHtml(item.title)} como feito">${icon('check')}Feito</button>`;
  }
  return `<button class="action action--accent" type="button" data-action="reopen" data-id="${item.id}" aria-label="Reabrir ${escapeHtml(item.title)}">${icon('reopen')}Reabrir</button>`;
}

function row(item, { showDate = false, actions = true } = {}) {
  const type = TYPES[item.type];
  const meta = [
    `<span class="type-tag type-${item.type}">${type.label}</span>`,
    showDate && item.startTime ? `<span>${icon('clock')}${item.startTime}</span>` : '',
    item.client ? `<span>${icon('user')}${escapeHtml(item.client.name)}</span>` : '',
    item.location ? `<span>${icon('map-pin')}${escapeHtml(item.location)}</span>` : '',
    item.processNumber ? `<span>${icon('scale')}${escapeHtml(item.processNumber)}</span>` : ''
  ].join('');
  const stateClass = item.status === 'DONE' ? 'is-done' : item.status === 'CANCELED' ? 'is-canceled' : '';

  return `<div class="row ${stateClass}" role="button" tabindex="0" data-open="${item.id}" aria-label="Abrir ${escapeHtml(item.title)}">
    ${whenBlock(item, showDate)}
    <div class="row-main">
      <div class="row-title"><span>${escapeHtml(item.title)}</span>${alertPill(item)}</div>
      <div class="row-meta">${meta}</div>
    </div>
    ${actions ? `<div class="row-actions">${rowActions(item)}</div>` : ''}
  </div>`;
}

function rows(items, options) {
  return items.map((item) => row(item, options)).join('');
}

/* ================================================================
   Navegação
   ================================================================ */

function route() {
  const view = location.hash.replace('#', '');
  showView(VIEWS[view] ? view : 'painel');
}

function showView(view) {
  state.view = view;
  $$('[data-panel]').forEach((panel) => { panel.hidden = panel.dataset.panel !== view; });
  $$('.nav-link').forEach((link) => {
    const active = link.dataset.view === view;
    link.classList.toggle('is-active', active);
    if (active) link.setAttribute('aria-current', 'page'); else link.removeAttribute('aria-current');
  });
  $('#page-name').textContent = VIEWS[view];
  document.title = `${VIEWS[view]} · Pauta`;
  closeMenu();
  loadCurrentView();
}

function loadCurrentView() {
  const loaders = { painel: loadPainel, agenda: loadAgenda, prazos: loadPrazos, clientes: loadClientes };
  return loaders[state.view]();
}

function goToDay(iso) {
  state.selectedDate = iso;
  state.month = monthOf(iso);
  if (location.hash === '#agenda') loadAgenda(); else location.hash = '#agenda';
}

function openMenu() {
  $('#app').classList.add('is-menu-open');
  $('#sidebar-backdrop').hidden = false;
  $('#menu-button').setAttribute('aria-expanded', 'true');
}

function closeMenu() {
  $('#app').classList.remove('is-menu-open');
  $('#sidebar-backdrop').hidden = true;
  $('#menu-button').setAttribute('aria-expanded', 'false');
}

/* ================================================================
   Painel
   ================================================================ */

async function loadPainel() {
  ['#today-list', '#attention-list', '#upcoming-list'].forEach((selector) => { $(selector).innerHTML = ui.skeleton(2); });
  $('#painel-error').innerHTML = '';
  try {
    const [dashboard, closed] = await Promise.all([api.dashboard(), api.closedDays(state.today, addDays(state.today, 6))]);
    state.today = dashboard.today;
    remember([...dashboard.agendaToday, ...dashboard.attention, ...dashboard.upcomingDeadlines]);
    renderPainel(dashboard, new Map(closed.map((day) => [day.date, day.reason])));
  } catch (error) {
    ['#today-list', '#attention-list', '#upcoming-list'].forEach((selector) => { $(selector).innerHTML = ''; });
    ui.error($('#painel-error'), error.message, loadPainel);
  }
}

function renderPainel(dashboard, closed) {
  const { totals } = dashboard;
  const name = shortName(state.office?.lawyerName || '');
  $('#painel-date').textContent = fullDate(dashboard.today);
  const title = `${greeting()}${name ? `, ${name}` : ''}`;
  $('#painel-title').textContent = title.endsWith('.') ? title : `${title}.`;
  $('#painel-summary').textContent = summaryText(dashboard);

  $('#stat-today').textContent = totals.today;
  $('#stat-week').textContent = totals.nextSevenDays;
  $('#stat-overdue').textContent = totals.overdue;
  $('#stat-overdue-card').classList.toggle('has-value', totals.overdue > 0);
  $('#stat-upcoming').textContent = totals.upcomingDeadlines;
  $('#stat-upcoming-label').textContent = `Prazos em ${state.office?.upcomingDays ?? 5} dias`;
  updateOverdueBadge(totals.overdue);

  $('#today-list').innerHTML = dashboard.agendaToday.length
    ? rows(dashboard.agendaToday)
    : ui.empty('Nada marcado para hoje', closed.get(dashboard.today) ? `Hoje é ${closed.get(dashboard.today).toLowerCase()}.` : 'Use "Adicionar hoje" para marcar algo.');

  const attention = $('#attention-list');
  attention.closest('.surface').classList.toggle('has-items', dashboard.attention.length > 0);
  $('#attention-count').hidden = dashboard.attention.length === 0;
  $('#attention-count').textContent = dashboard.attention.length;
  attention.innerHTML = dashboard.attention.length
    ? rows(dashboard.attention, { showDate: true })
    : ui.empty('Tudo em dia', 'Nenhum prazo vencido e nada esquecido para trás.', { success: true });

  $('#upcoming-description').textContent = `Vencimentos nos próximos ${state.office?.upcomingDays ?? 5} dias, sem contar hoje.`;
  $('#upcoming-list').innerHTML = dashboard.upcomingDeadlines.length
    ? rows(dashboard.upcomingDeadlines, { showDate: true })
    : ui.empty('Nenhum prazo próximo', 'Os prazos que vencerem nos próximos dias aparecem aqui.');

  renderDayBars(dashboard.nextDays, closed);
}

function summaryText(dashboard) {
  const open = dashboard.agendaToday.filter((item) => item.status === 'SCHEDULED');
  if (!open.length && !dashboard.totals.overdue) return 'Agenda de hoje livre e nada atrasado.';
  const parts = [];
  if (open.length) {
    const next = open.find((item) => item.startTime);
    parts.push(`${plural(open.length, 'compromisso', 'compromissos')} hoje${next ? `, o primeiro às ${next.startTime}` : ''}`);
  }
  if (dashboard.totals.overdue) parts.push(`${plural(dashboard.totals.overdue, 'item atrasado', 'itens atrasados')}`);
  return `${parts.join(' e ')}.`.replace(/^./, (char) => char.toUpperCase());
}

function renderDayBars(days, closed) {
  const max = Math.max(1, ...days.map((day) => day.open));
  $('#day-bars').innerHTML = days.map((day) => {
    const weekday = day.date === state.today ? 'hoje' : format(day.date, { weekday: 'short' }).replace('.', '');
    const reason = closed.get(day.date);
    const classes = ['day-bar', day.open ? 'has-items' : '', day.date === state.today ? 'is-today' : '', isWeekend(day.date) || reason ? 'is-closed' : ''].join(' ');
    return `<button class="${classes}" type="button" data-go-day="${day.date}" title="${escapeHtml(longDate(day.date))}${reason ? ` · ${escapeHtml(reason)}` : ''}">
      <span class="day-bar-count">${day.open}</span>
      <span class="day-bar-track"><span class="day-bar-fill" data-value="${Math.round((day.open / max) * 100)}"></span></span>
      <span class="day-bar-day">${weekday} ${format(day.date, { day: '2-digit' })}</span>
    </button>`;
  }).join('');
  ui.applyBarValues($('#day-bars'));
}

function updateOverdueBadge(count) {
  const badge = $('#nav-overdue');
  badge.hidden = count === 0;
  badge.textContent = count;
  badge.setAttribute('aria-label', `${plural(count, 'atrasado', 'atrasados')}`);
}

/* ================================================================
   Agenda (mês + dia)
   ================================================================ */

const monthOf = (iso) => `${iso.slice(0, 7)}-01`;

function gridStart(monthIso) {
  return addDays(monthIso, -parseIso(monthIso).getDay());
}

async function loadAgenda() {
  state.month ||= monthOf(state.today);
  state.selectedDate ||= state.today;
  const from = gridStart(state.month);
  const to = addDays(from, CELLS_PER_MONTH - 1);
  const title = format(state.month, { month: 'long', year: 'numeric' });
  $('#agenda-title').textContent = capitalize(title);
  $('#agenda-error').innerHTML = '';
  if (!state.monthItems.length) $('#day-list').innerHTML = ui.skeleton(2);

  try {
    const [items, closed] = await Promise.all([api.between(from, to), api.closedDays(from, to)]);
    state.monthItems = remember(items);
    state.closedDays = new Map(closed.map((day) => [day.date, day.reason]));
    renderCalendar(from);
    renderDayPanel();
  } catch (error) {
    ui.error($('#agenda-error'), error.message, loadAgenda);
  }
}

function itemsOn(iso) {
  return state.monthItems.filter((item) => item.date === iso);
}

function renderCalendar(from) {
  const monthPrefix = state.month.slice(0, 7);
  const cells = Array.from({ length: CELLS_PER_MONTH }, (_, index) => {
    const iso = addDays(from, index);
    const items = itemsOn(iso);
    const closed = state.closedDays.get(iso);
    const classes = ['cell',
      iso.slice(0, 7) !== monthPrefix ? 'is-outside' : '',
      iso === state.today ? 'is-today' : '',
      iso === state.selectedDate ? 'is-selected' : '',
      isWeekend(iso) ? 'is-weekend' : ''].join(' ');
    const visible = items.slice(0, MAX_ITEMS_PER_CELL).map((item) => {
      const itemClasses = ['cell-item', `type-${item.type}`, item.status !== 'SCHEDULED' ? 'is-done' : '', item.alert === 'OVERDUE' ? 'is-overdue' : ''].join(' ');
      return `<span class="${itemClasses}">${item.startTime ? `<time>${item.startTime}</time> ` : ''}${escapeHtml(item.title)}</span>`;
    }).join('');
    const more = items.length > MAX_ITEMS_PER_CELL ? `<span class="cell-more">+${items.length - MAX_ITEMS_PER_CELL} mais</span>` : '';
    const dots = items.slice(0, 4).map((item) => `<i class="type-${item.type}"></i>`).join('');
    const label = `${longDate(iso)}: ${items.length ? plural(items.length, 'compromisso', 'compromissos') : 'livre'}${closed ? `, ${closed}` : ''}`;

    return `<button class="${classes}" type="button" data-day="${iso}" aria-label="${escapeHtml(label)}" aria-pressed="${iso === state.selectedDate}"${iso === state.today ? ' aria-current="date"' : ''}>
      <span class="cell-head"><span class="cell-day">${parseIso(iso).getDate()}</span>${closed ? `<span class="cell-closed" title="${escapeHtml(closed)}">${escapeHtml(closed)}</span>` : ''}</span>
      <span class="cell-items">${visible}${more}</span>
      <span class="cell-dots" aria-hidden="true">${dots}</span>
    </button>`;
  });
  $('#calendar-grid').innerHTML = cells.join('');
}

function renderDayPanel() {
  const iso = state.selectedDate;
  const items = itemsOn(iso);
  const closed = state.closedDays.get(iso);
  $('#day-title').textContent = iso === state.today ? `Hoje, ${format(iso, { day: 'numeric', month: 'long' })}` : longDate(iso);
  $('#day-subtitle').textContent = items.length ? plural(items.length, 'compromisso', 'compromissos') : 'Dia livre';
  $('#day-list').innerHTML = (closed ? `<p class="day-closed">${icon('calendar')} ${escapeHtml(closed)}: sem expediente forense.</p>` : '')
    + (items.length ? rows(items) : ui.empty('Nada marcado', 'Clique em "Adicionar" ou dê dois cliques no dia do calendário.'));
}

function selectDay(iso) {
  state.selectedDate = iso;
  if (iso.slice(0, 7) !== state.month.slice(0, 7)) {
    state.month = monthOf(iso);
    loadAgenda();
    return;
  }
  $$('.cell', $('#calendar-grid')).forEach((cell) => {
    const selected = cell.dataset.day === iso;
    cell.classList.toggle('is-selected', selected);
    cell.setAttribute('aria-pressed', String(selected));
  });
  renderDayPanel();
}

function stepMonth(step) {
  if (step === 0) {
    state.month = monthOf(state.today);
    state.selectedDate = state.today;
  } else {
    const date = parseIso(state.month);
    date.setMonth(date.getMonth() + step);
    state.month = toIso(date);
    state.selectedDate = state.month.slice(0, 7) === state.today.slice(0, 7) ? state.today : state.month;
  }
  state.monthItems = [];
  loadAgenda();
}

function renderLegend() {
  $('#legend').innerHTML = Object.entries(TYPES)
    .map(([key, type]) => `<li><span class="type-tag type-${key}">${type.label}</span></li>`).join('');
}

/* ================================================================
   Prazos
   ================================================================ */

async function loadPrazos() {
  const target = $('#prazos-groups');
  target.innerHTML = `<section class="surface">${ui.skeleton(3)}</section>`;
  $('#prazos-error').innerHTML = '';
  try {
    const deadlines = remember(await api.deadlines());
    renderPrazos(deadlines);
  } catch (error) {
    target.innerHTML = '';
    ui.error($('#prazos-error'), error.message, loadPrazos);
  }
}

function renderPrazos(deadlines) {
  const days = state.office?.upcomingDays ?? 5;
  const groups = [
    { key: 'OVERDUE', title: 'Vencidos', hint: 'Cumpra e marque como feito, ou corrija a data.', css: 'deadline-group--overdue' },
    { key: 'TODAY', title: 'Vencem hoje', hint: 'Último dia para protocolar.' },
    { key: 'UPCOMING', title: `Próximos ${days} dias`, hint: 'Hora de preparar.' },
    { key: 'NONE', title: 'Mais adiante', hint: 'Já anotados, sem pressa por enquanto.' }
  ];
  updateOverdueBadge(deadlines.filter((item) => item.alert === 'OVERDUE').length);

  if (!deadlines.length) {
    $('#prazos-groups').innerHTML = `<section class="surface">${ui.empty('Nenhum prazo em aberto',
      'Use a calculadora acima para achar o vencimento e agendar o prazo.', { success: true })}</section>`;
    return;
  }
  $('#prazos-groups').innerHTML = groups.map((group) => {
    const items = deadlines.filter((item) => item.alert === group.key);
    if (!items.length) return '';
    return `<section class="surface deadline-group ${group.css || ''}">
      <header class="surface-head"><div>
        <h2>${group.title} <span class="count">(${items.length})</span></h2><p>${group.hint}</p>
      </div></header>
      <div class="list">${rows(items, { showDate: true })}</div>
    </section>`;
  }).join('');
}

async function calculateDeadline(event) {
  event.preventDefault();
  const form = $('#calc-form');
  const result = $('#calc-result');
  try {
    const start = readDateField(form, 'calcStart', 'Intimação ou publicação', { required: true });
    const businessDays = Number(form.elements.businessDays.value);
    const calculation = await api.calculate(start, businessDays);
    state.lastCalculation = calculation;
    result.hidden = false;
    result.innerHTML = `<div>
        <p>Vence em</p>
        <strong>${escapeHtml(fullDate(calculation.dueDate))}</strong>
        <p>${plural(calculation.businessDays, 'dia útil', 'dias úteis')} contados da intimação em ${shortDate(calculation.startDate)}${calculation.weekendDays ? `, sem ${plural(calculation.weekendDays, 'dia', 'dias')} de fim de semana` : ''}.</p>
        ${skippedList(calculation)}
      </div>
      <button class="button button--primary" type="button" id="schedule-deadline">${icon('plus')}Agendar este prazo</button>`;
    $('#schedule-deadline').addEventListener('click', () => openAppointmentDialog(null, {
      type: 'DEADLINE', date: calculation.dueDate,
      notes: `Prazo de ${plural(calculation.businessDays, 'dia útil', 'dias úteis')} contado da intimação em ${shortDate(calculation.startDate)}.`
    }));
  } catch (error) {
    result.hidden = false;
    result.innerHTML = `<p class="form-error">${escapeHtml(error.message)}</p>`;
  }
}

function skippedList(calculation) {
  if (!calculation.skipped.length) return '';
  const items = calculation.skipped.map((period) => {
    const when = period.from === period.to ? shortDate(period.from) : `${shortDate(period.from)} a ${shortDate(period.to)}`;
    return `<li>${when}: ${escapeHtml(period.reason)}</li>`;
  }).join('');
  return `<ul aria-label="Dias que não contaram">${items}</ul>`;
}

/* ================================================================
   Clientes
   ================================================================ */

let clientSearchTimer;

async function loadClientes() {
  const grid = $('#client-grid');
  if (!grid.children.length) grid.innerHTML = ui.skeleton(3);
  $('#clientes-error').innerHTML = '';
  try {
    const search = $('#client-search').value.trim();
    const clients = await api.clients(search);
    if (!search) state.clients = clients;
    renderClients(clients, search);
  } catch (error) {
    grid.innerHTML = '';
    ui.error($('#clientes-error'), error.message, loadClientes);
  }
}

function renderClients(clients, search) {
  const grid = $('#client-grid');
  if (!clients.length) {
    grid.innerHTML = search
      ? ui.empty('Ninguém encontrado', `Nenhum cliente com "${escapeHtml(search)}" no nome ou telefone.`)
      : ui.empty('Nenhum cliente ainda', 'Cadastre aqui ou direto ao marcar um compromisso: basta digitar o nome.',
        { action: `<button class="button button--primary" type="button" data-new-client>${icon('plus')}Cadastrar cliente</button>` });
    return;
  }
  grid.innerHTML = clients.map((client) => {
    const contacts = [
      client.phone && client.mobile ? `<a class="contact contact--whatsapp" href="${whatsappUrl(client.phone)}" target="_blank" rel="noopener noreferrer">${icon('message')}WhatsApp</a>` : '',
      client.phone ? `<a class="contact" href="tel:+55${client.phone}">${icon('phone')}${formatPhone(client.phone)}</a>` : '',
      client.email ? `<a class="contact" href="mailto:${escapeHtml(client.email)}">${icon('mail')}E-mail</a>` : ''
    ].join('');
    const upcoming = client.upcomingAppointments
      ? plural(client.upcomingAppointments, 'compromisso marcado', 'compromissos marcados')
      : 'Nada marcado';
    return `<article class="client-card" role="button" tabindex="0" data-client="${client.id}" aria-label="Abrir ${escapeHtml(client.name)}">
      <div class="client-head">
        <span class="avatar" aria-hidden="true">${escapeHtml(initials(client.name))}</span>
        <span class="client-name"><strong>${escapeHtml(client.name)}</strong><small>${upcoming}</small></span>
      </div>
      ${contacts ? `<div class="client-contacts">${contacts}</div>` : ''}
    </article>`;
  }).join('');
}

async function refreshClientOptions() {
  try {
    state.clients = await api.clients();
  } catch {
    /* sem a lista, o campo continua aceitando um nome novo */
  }
  $('#client-options').innerHTML = state.clients.map((client) => `<option value="${escapeHtml(client.name)}"></option>`).join('');
}

const findClientByName = (name) => {
  const wanted = name.trim().toLocaleLowerCase('pt-BR');
  return state.clients.find((client) => client.name.toLocaleLowerCase('pt-BR') === wanted);
};

/* ---------- Diálogo de cliente ---------- */

const clientDialog = $('#client-dialog');
const clientForm = $('#client-form');

async function openClientDialog(client = null) {
  state.editingClient = client;
  clientForm.reset();
  clearErrors(clientForm, '#client-error');
  $('#client-dialog-title').textContent = client ? client.name : 'Novo cliente';
  $('#delete-client').hidden = !client;
  $('#client-history').hidden = !client;
  if (client) {
    clientForm.elements.name.value = client.name;
    clientForm.elements.phone.value = formatPhone(client.phone);
    clientForm.elements.email.value = client.email || '';
    clientForm.elements.notes.value = client.notes || '';
    loadClientHistory(client);
  }
  clientDialog.showModal();
  if (!client) clientForm.elements.name.focus();
}

async function loadClientHistory(client) {
  const target = $('#client-history-list');
  target.innerHTML = ui.skeleton(1);
  try {
    const items = remember(await api.byClient(client.id));
    target.innerHTML = items.length
      ? rows(items, { showDate: true, actions: false })
      : '<p class="hint">Nenhum compromisso com este cliente ainda.</p>';
  } catch (error) {
    target.innerHTML = `<p class="form-error">${escapeHtml(error.message)}</p>`;
  }
}

async function saveClient(event) {
  event.preventDefault();
  clearErrors(clientForm, '#client-error');
  const payload = {
    name: clientForm.elements.name.value.trim(),
    phone: clientForm.elements.phone.value.trim() || null,
    email: clientForm.elements.email.value.trim() || null,
    notes: clientForm.elements.notes.value.trim() || null
  };
  if (!payload.name) return showErrors(clientForm, '#client-error', new ApiError('Informe o nome do cliente.', { fieldErrors: { name: 'Informe o nome.' } }));

  const button = $('#save-client');
  button.disabled = true;
  try {
    const editing = state.editingClient;
    await (editing ? api.updateClient(editing.id, payload) : api.createClient(payload));
    clientDialog.close();
    ui.toast(editing ? 'Cliente atualizado.' : 'Cliente cadastrado.');
    state.clients = [];
    await refreshClientOptions();
    if (state.view === 'clientes') loadClientes();
  } catch (error) {
    showErrors(clientForm, '#client-error', error, { phone: 'phone', email: 'email', name: 'name' });
  } finally {
    button.disabled = false;
  }
}

async function deleteClient() {
  const client = state.editingClient;
  if (!client) return;
  const confirmed = await ui.confirm('Excluir cliente?', `${client.name} sairá do cadastro.`);
  if (!confirmed) return;
  try {
    await api.deleteClient(client.id);
    clientDialog.close();
    ui.toast('Cliente excluído.');
    await refreshClientOptions();
    loadClientes();
  } catch (error) {
    showErrors(clientForm, '#client-error', error);
  }
}

/* ================================================================
   Campos de data (dia / mês / ano) e hora (24h)
   ================================================================ */

const MONTH_NAMES = Array.from({ length: 12 }, (_, index) =>
  new Intl.DateTimeFormat('pt-BR', { month: 'long' }).format(new Date(2000, index, 1)));
const option = (value, label, selected = false) => `<option value="${value}"${selected ? ' selected' : ''}>${label}</option>`;

function buildDateField(container) {
  const { id, label } = container.dataset;
  const name = container.dataset.dateField;
  const year = new Date().getFullYear();
  const days = Array.from({ length: 31 }, (_, index) => option(pad2(index + 1), pad2(index + 1))).join('');
  const months = MONTH_NAMES.map((month, index) => option(pad2(index + 1), month)).join('');
  const years = Array.from({ length: 8 }, (_, index) => year - 2 + index).map((value) => option(value, value)).join('');

  container.setAttribute('role', 'group');
  container.setAttribute('aria-label', label);
  container.innerHTML = `
    <select id="${id}-day" class="control" name="${name}Day" aria-label="${label}: dia">${option('', 'Dia')}${days}</select>
    <select id="${id}-month" class="control" name="${name}Month" aria-label="${label}: mês">${months}</select>
    <select id="${id}-year" class="control" name="${name}Year" aria-label="${label}: ano">${years}</select>`;
}

function buildTimeField(container) {
  const { id } = container.dataset;
  const name = container.dataset.timeField;
  const hours = Array.from({ length: 17 }, (_, index) => option(pad2(index + 6), pad2(index + 6))).join('');
  const minutes = Array.from({ length: 12 }, (_, index) => option(pad2(index * 5), pad2(index * 5))).join('');

  container.setAttribute('role', 'group');
  container.setAttribute('aria-label', 'Horário');
  container.innerHTML = `
    <select id="${id}-hour" class="control" name="${name}Hour" aria-label="Hora">${option('', 'Hora')}${hours}</select>
    <span class="time-sep" aria-hidden="true">:</span>
    <select id="${id}-minute" class="control" name="${name}Minute" aria-label="Minutos">${minutes}</select>`;
}

function ensureOption(select, value) {
  if (![...select.options].some((item) => item.value === value)) select.insertAdjacentHTML('beforeend', option(value, value));
}

function setDateField(form, name, iso) {
  const [year, month, day] = (iso || state.today).split('-');
  ensureOption(form.elements[`${name}Year`], year);
  form.elements[`${name}Day`].value = iso ? day : '';
  form.elements[`${name}Month`].value = month;
  form.elements[`${name}Year`].value = year;
}

function readDateField(form, name, label, { required = false } = {}) {
  const day = form.elements[`${name}Day`].value;
  if (!day) {
    if (required) throw new ApiError(`Escolha o dia em "${label}".`, { fieldErrors: { [name]: 'Escolha o dia.' } });
    return null;
  }
  const month = form.elements[`${name}Month`].value;
  const year = form.elements[`${name}Year`].value;
  const date = new Date(Number(year), Number(month) - 1, Number(day));
  if (date.getMonth() !== Number(month) - 1) {
    throw new ApiError(`${Number(day)} de ${MONTH_NAMES[Number(month) - 1]} não existe.`, { fieldErrors: { [name]: 'Data inexistente.' } });
  }
  return `${year}-${month}-${day}`;
}

function setTimeField(form, name, time) {
  const [hour, minute] = time ? time.split(':') : ['', '00'];
  ensureOption(form.elements[`${name}Hour`], hour);
  ensureOption(form.elements[`${name}Minute`], minute);
  form.elements[`${name}Hour`].value = hour;
  form.elements[`${name}Minute`].value = minute;
}

function readTimeField(form, name) {
  const hour = form.elements[`${name}Hour`].value;
  return hour ? `${hour}:${form.elements[`${name}Minute`].value}` : null;
}

/* ================================================================
   Diálogo de compromisso (criar e editar)
   ================================================================ */

const appointmentDialog = $('#appointment-dialog');
const appointmentForm = $('#appointment-form');
let conflictTimer;
let conflictRequest = 0;

function buildTypePicker() {
  $('#type-picker').insertAdjacentHTML('beforeend', Object.entries(TYPES).map(([key, type]) => `
    <div class="type-option type-${key}">
      <input type="radio" name="type" id="type-${key}" value="${key}">
      <label for="type-${key}">${icon(type.icon)}${type.label}</label>
    </div>`).join(''));
}

const selectedType = () => appointmentForm.elements.type.value || 'HEARING';

function onTypeChange() {
  const type = TYPES[selectedType()];
  $('#appointment-title').placeholder = type.placeholder;
  $('#title-suggestions').innerHTML = type.suggestions.map((value) => `<option value="${escapeHtml(value)}"></option>`).join('');
  const deadline = selectedType() === 'DEADLINE';
  $('#open-inline-calc').hidden = !deadline;
  if (!deadline) $('#inline-calc').hidden = true;
}

function openAppointmentDialog(item = null, defaults = {}) {
  state.editing = item;
  appointmentForm.reset();
  clearErrors(appointmentForm, '#appointment-error');
  $('#conflict-warning').hidden = true;
  $('#inline-calc').hidden = true;
  $('#client-hint').textContent = '';

  const source = item || {
    type: defaults.type || 'HEARING',
    date: defaults.date || state.selectedDate || state.today,
    startTime: null,
    durationMinutes: 60,
    client: defaults.client || null,
    notes: defaults.notes || ''
  };
  appointmentForm.elements.type.value = source.type;
  onTypeChange();
  appointmentForm.elements.title.value = item?.title || '';
  setDateField(appointmentForm, 'date', source.date);
  setTimeField(appointmentForm, 'startTime', source.startTime);
  appointmentForm.elements.durationMinutes.value = String(source.durationMinutes || 60);
  syncDurationVisibility();
  appointmentForm.elements.clientName.value = source.client?.name || '';
  appointmentForm.elements.processNumber.value = item?.processNumber || '';
  appointmentForm.elements.location.value = item?.location || '';
  appointmentForm.elements.notes.value = source.notes || '';
  setDateField(appointmentForm, 'inlineStart', state.today);

  $('#appointment-dialog-title').textContent = item ? 'Editar compromisso' : 'Novo compromisso';
  $('#appointment-dialog-eyebrow').textContent = item ? TYPES[item.type].label : 'Agenda';
  renderExtraActions(item);
  refreshClientOptions();
  appointmentDialog.showModal();
  if (!item) appointmentForm.elements.title.focus();
  scheduleConflictCheck();
}

function renderExtraActions(item) {
  const target = $('#appointment-extra-actions');
  if (!item) { target.innerHTML = ''; return; }
  const status = item.status === 'SCHEDULED'
    ? `<button class="button button--ghost" type="button" data-dialog-action="cancel">${icon('x')}Cancelar compromisso</button>`
    : `<button class="button button--ghost" type="button" data-dialog-action="reopen">${icon('reopen')}Reabrir</button>`;
  target.innerHTML = `<button class="button button--ghost button--danger-text" type="button" data-dialog-action="delete">${icon('trash')}Excluir</button>${status}`;
}

function syncDurationVisibility() {
  $('#appointment-duration').hidden = !appointmentForm.elements.startTimeHour.value;
}

function scheduleConflictCheck() {
  clearTimeout(conflictTimer);
  conflictTimer = setTimeout(checkConflicts, 250);
}

async function checkConflicts() {
  const warning = $('#conflict-warning');
  const request = ++conflictRequest;
  let date;
  try { date = readDateField(appointmentForm, 'date', 'Data'); } catch { date = null; }
  const startTime = readTimeField(appointmentForm, 'startTime');
  if (!date || !startTime) { warning.hidden = true; return; }

  try {
    const params = { date, startTime, durationMinutes: appointmentForm.elements.durationMinutes.value };
    if (state.editing) params.excludeId = state.editing.id;
    const conflicts = await api.conflicts(params);
    if (request !== conflictRequest) return;
    warning.hidden = conflicts.length === 0;
    warning.innerHTML = `${icon('alert')}<div>Choque de horário com:
      <ul>${conflicts.map((other) => `<li>${other.startTime}–${other.endTime} · ${escapeHtml(other.title)} (${TYPES[other.type].label})</li>`).join('')}</ul>
      Você pode salvar mesmo assim.</div>`;
  } catch {
    warning.hidden = true;
  }
}

function updateClientHint() {
  const name = appointmentForm.elements.clientName.value.trim();
  $('#client-hint').textContent = name && !findClientByName(name) ? 'Cliente novo: será cadastrado ao salvar.' : '';
}

async function resolveClientId() {
  const name = appointmentForm.elements.clientName.value.trim();
  if (!name) return null;
  const existing = findClientByName(name);
  if (existing) return existing.id;
  const created = await api.createClient({ name });
  state.clients.push(created);
  return created.id;
}

async function saveAppointment(event) {
  event.preventDefault();
  clearErrors(appointmentForm, '#appointment-error');
  const button = $('#save-appointment');
  button.disabled = true;
  try {
    const type = selectedType();
    const date = readDateField(appointmentForm, 'date', 'Data', { required: true });
    const startTime = readTimeField(appointmentForm, 'startTime');
    const clientName = appointmentForm.elements.clientName.value.trim();
    const title = appointmentForm.elements.title.value.trim() || (clientName ? `${TYPES[type].label} · ${clientName}` : TYPES[type].label);
    const payload = {
      title,
      type,
      date,
      startTime,
      durationMinutes: startTime ? Number(appointmentForm.elements.durationMinutes.value) : null,
      location: appointmentForm.elements.location.value.trim() || null,
      processNumber: appointmentForm.elements.processNumber.value.trim() || null,
      clientId: await resolveClientId(),
      notes: appointmentForm.elements.notes.value.trim() || null
    };
    const editing = state.editing;
    const saved = await (editing ? api.updateAppointment(editing.id, payload) : api.createAppointment(payload));
    appointmentDialog.close();
    ui.toast(editing ? 'Compromisso atualizado.' : `${TYPES[type].label} marcado para ${format(saved.date, { day: 'numeric', month: 'long' })}.`);
    state.selectedDate = saved.date;
    if (state.view === 'agenda') state.month = monthOf(saved.date);
    await loadCurrentView();
  } catch (error) {
    showErrors(appointmentForm, '#appointment-error', error, {
      title: 'title', processNumber: 'processNumber', location: 'location', notes: 'notes', date: 'dateDay'
    }, { INVALID_PROCESS_NUMBER: 'processNumber' });
  } finally {
    button.disabled = false;
  }
}

async function dialogAction(action) {
  const item = state.editing;
  if (!item) return;
  if (action === 'delete') {
    appointmentDialog.close();
    const confirmed = await ui.confirm('Excluir compromisso?', `"${item.title}" sairá da agenda. Esta ação não pode ser desfeita.`);
    if (!confirmed) return;
    await runAndReload(() => api.deleteAppointment(item.id), 'Compromisso excluído.');
    return;
  }
  appointmentDialog.close();
  await runAndReload(() => (action === 'cancel' ? api.cancel(item.id) : api.reopen(item.id)),
    action === 'cancel' ? 'Compromisso cancelado.' : 'Compromisso reaberto.',
    action === 'cancel' ? { label: 'Desfazer', run: () => runAndReload(() => api.reopen(item.id), 'Cancelamento desfeito.') } : null);
}

async function runAndReload(operation, message, undo = null) {
  try {
    await operation();
    ui.toast(message, undo);
    await loadCurrentView();
  } catch (error) {
    ui.toast(error.message);
  }
}

async function rowAction(button) {
  const id = Number(button.dataset.id);
  const action = button.dataset.action;
  button.disabled = true;
  if (action === 'complete') {
    await runAndReload(() => api.complete(id), 'Marcado como feito.',
      { label: 'Desfazer', run: () => runAndReload(() => api.reopen(id), 'Voltou para a agenda.') });
  } else {
    await runAndReload(() => api.reopen(id), 'Voltou para a agenda.');
  }
}

async function applyInlineCalculation() {
  const result = $('#inline-calc-result');
  try {
    const start = readDateField(appointmentForm, 'inlineStart', 'Intimação', { required: true });
    const days = Number(appointmentForm.elements.inlineDays.value);
    const calculation = await api.calculate(start, days);
    setDateField(appointmentForm, 'date', calculation.dueDate);
    const skipped = calculation.skipped.map((period) => period.reason).join(', ');
    result.textContent = `Vence ${fullDate(calculation.dueDate)}${skipped ? ` (pulou: ${skipped})` : ''}.`;
    if (!appointmentForm.elements.notes.value.trim()) {
      appointmentForm.elements.notes.value = `Prazo de ${plural(days, 'dia útil', 'dias úteis')} contado da intimação em ${shortDate(start)}.`;
    }
    scheduleConflictCheck();
  } catch (error) {
    result.textContent = error.message;
  }
}

/* ---------- Erros de formulário ---------- */

function clearErrors(form, errorSelector) {
  $(errorSelector).textContent = '';
  $$('[aria-invalid="true"]', form).forEach((field) => field.removeAttribute('aria-invalid'));
}

function showErrors(form, errorSelector, error, fieldMap = {}, codeMap = {}) {
  $(errorSelector).textContent = error.message;
  const fields = Object.keys(error.fieldErrors || {}).map((name) => fieldMap[name] || `${name}Day`);
  if (codeMap[error.code]) fields.push(codeMap[error.code]);
  if (error.code === 'INVALID_PHONE') fields.push('phone');
  fields.forEach((name) => form.elements[name]?.setAttribute('aria-invalid', 'true'));
  form.elements[fields[0]]?.focus?.();
}

/* ================================================================
   Eventos e início
   ================================================================ */

function bindEvents() {
  window.addEventListener('hashchange', route);
  $('#menu-button').addEventListener('click', openMenu);
  $('#close-menu').addEventListener('click', closeMenu);
  $('#sidebar-backdrop').addEventListener('click', closeMenu);

  document.addEventListener('click', (event) => {
    const target = event.target;
    const newButton = target.closest('[data-new-appointment]');
    if (newButton) {
      openAppointmentDialog(null, newButton.hasAttribute('data-date-today') ? { date: state.today } : {});
      return;
    }
    const action = target.closest('[data-action]');
    if (action) { event.stopPropagation(); rowAction(action); return; }
    const open = target.closest('[data-open]');
    if (open && !target.closest('a')) { openStored(Number(open.dataset.open)); return; }
    const day = target.closest('[data-go-day]');
    if (day) { goToDay(day.dataset.goDay); return; }
    if (target.closest('[data-go-today]')) { state.selectedDate = state.today; state.month = monthOf(state.today); return; }
    if (target.closest('[data-close-dialog]')) { target.closest('dialog').close(); return; }
    const card = target.closest('[data-client]');
    if (card && !target.closest('a')) { openClientById(Number(card.dataset.client)); return; }
    if (target.closest('[data-new-client]')) openClientDialog();
  });

  document.addEventListener('keydown', (event) => {
    const focusable = event.target.closest?.('[data-open], [data-client]');
    if (focusable && (event.key === 'Enter' || event.key === ' ') && event.target === focusable) {
      event.preventDefault();
      focusable.click();
    }
    const typing = ['INPUT', 'TEXTAREA', 'SELECT'].includes(event.target.tagName) || document.querySelector('dialog[open]');
    if (!typing && !event.ctrlKey && !event.metaKey && !event.altKey && event.key.toLowerCase() === 'n') {
      event.preventDefault();
      openAppointmentDialog();
    }
  });

  // Agenda
  $('#calendar-grid').addEventListener('click', (event) => {
    const cell = event.target.closest('[data-day]');
    if (cell) selectDay(cell.dataset.day);
  });
  $('#calendar-grid').addEventListener('dblclick', (event) => {
    const cell = event.target.closest('[data-day]');
    if (cell) openAppointmentDialog(null, { date: cell.dataset.day });
  });
  $$('[data-month-step]').forEach((button) => button.addEventListener('click', () => stepMonth(Number(button.dataset.monthStep))));
  $('#add-on-day').addEventListener('click', () => openAppointmentDialog(null, { date: state.selectedDate }));

  // Prazos
  $('#calc-form').addEventListener('submit', calculateDeadline);
  $$('[data-calc-days]').forEach((chip) => chip.addEventListener('click', () => {
    $('#calc-days').value = chip.dataset.calcDays;
    $('#calc-form').requestSubmit();
  }));

  // Clientes
  $('#client-search').addEventListener('input', () => {
    clearTimeout(clientSearchTimer);
    clientSearchTimer = setTimeout(loadClientes, 200);
  });
  $('#new-client').addEventListener('click', () => openClientDialog());
  clientForm.addEventListener('submit', saveClient);
  $('#delete-client').addEventListener('click', deleteClient);
  $('#client-phone').addEventListener('input', (event) => { event.target.value = maskPhone(event.target.value); });
  $('#schedule-for-client').addEventListener('click', () => {
    const client = state.editingClient;
    clientDialog.close();
    openAppointmentDialog(null, { type: 'CLIENT_SERVICE', client });
  });

  // Compromisso
  appointmentForm.addEventListener('submit', saveAppointment);
  appointmentForm.addEventListener('change', (event) => {
    const name = event.target.name || '';
    if (name === 'type') onTypeChange();
    if (name === 'startTimeHour') syncDurationVisibility();
    if (name.startsWith('date') || name.startsWith('startTime') || name === 'durationMinutes') scheduleConflictCheck();
  });
  $('#appointment-client').addEventListener('input', updateClientHint);
  $('#appointment-process').addEventListener('input', (event) => { event.target.value = maskProcessNumber(event.target.value); });
  $$('[data-date-shortcut]').forEach((chip) => chip.addEventListener('click', () => {
    setDateField(appointmentForm, 'date', addDays(state.today, Number(chip.dataset.dateShortcut)));
    scheduleConflictCheck();
  }));
  $('#open-inline-calc').addEventListener('click', () => {
    $('#inline-calc').hidden = !$('#inline-calc').hidden;
  });
  $('#apply-inline-calc').addEventListener('click', applyInlineCalculation);
  $('#appointment-extra-actions').addEventListener('click', (event) => {
    const button = event.target.closest('[data-dialog-action]');
    if (button) dialogAction(button.dataset.dialogAction);
  });
}

function openStored(id) {
  const item = state.byId.get(id);
  if (!item) return;
  if (clientDialog.open) clientDialog.close();
  openAppointmentDialog(item);
}

async function openClientById(id) {
  const client = (await api.clients()).find((candidate) => candidate.id === id);
  if (client) openClientDialog(client);
}

async function init() {
  $$('[data-date-field]').forEach(buildDateField);
  $$('[data-time-field]').forEach(buildTimeField);
  buildTypePicker();
  renderLegend();
  bindEvents();

  try {
    state.office = await api.office();
    state.today = state.office.today;
  } catch {
    state.office = null;
  }
  setDateField($('#calc-form'), 'calcStart', state.today);
  $('#today-chip').textContent = longDate(state.today);
  route();
  refreshClientOptions();
}

init();
