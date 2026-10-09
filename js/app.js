const APPLICATIONS_API = 'http://localhost:8080/api/applications';
const PAGE_SIZE = 10;

async function apiRequest(path = '', options = {}) {
  const response = await fetch(`${APPLICATIONS_API}${path}`, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...options.headers }
  });
  if (response.status === 204) return null;
  const body = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(body.message || `Request failed (${response.status})`);
  return body;
}

function byId(id) { return document.getElementById(id); }
function setText(id, value) { const element = byId(id); if (element) element.textContent = value ?? ''; }
function displayStatus(status) { return (status || '').split('-').map(part => part[0]?.toUpperCase() + part.slice(1)).join(' '); }
function restrictRejectedAfterOffer(statusSelect, offerRecorded, helpText) {
  const rejectedOption = statusSelect.querySelector('option[value="rejected"]');
  if (rejectedOption) rejectedOption.disabled = offerRecorded;
  helpText.hidden = !offerRecorded;
}

function decodeDescriptionEntities(value) {
  const namedEntities = { amp: '&', lt: '<', gt: '>', quot: '"', apos: "'", nbsp: ' ' };
  return String(value).replace(/&(#(?:x[\da-f]+|\d+)|amp|lt|gt|quot|apos|nbsp);/gi, (entity, code) => {
    if (code[0] !== '#') return namedEntities[code.toLowerCase()] ?? entity;
    const hexadecimal = code[1]?.toLowerCase() === 'x';
    const number = Number.parseInt(code.slice(hexadecimal ? 2 : 1), hexadecimal ? 16 : 10);
    if (!Number.isInteger(number) || number < 0 || number > 0x10ffff
        || (number >= 0xd800 && number <= 0xdfff)) return entity;
    return String.fromCodePoint(number);
  });
}

function formatDescriptionBlocks(value) {
  if (!value || !String(value).trim()) return [];

  const sectionHeadings = [
    'ABOUT THE COMPANY', 'ABOUT THE ROLE', 'RESPONSIBILITIES', 'ELIGIBILITY',
    'REQUIRED SKILLS', 'PERKS & BENEFITS'
  ];
  const headingPattern = new RegExp('(' + sectionHeadings.join('|') + ')(?:[ \\t]*:)?', 'gi');
  const fieldLabelPattern = /(?=(?:company(?:\s+name)?|employer|location|duration|employment\s*type|stipend|job\s+title|role|position|title|required\s+skills|skills\s+required)\s*:)/gi;
  let normalized = decodeDescriptionEntities(value)
    .replace(/\r\n?/g, '\n')
    .replace(/\u00a0/g, ' ')
    .replace(/\*\*([^*\n]+)\*\*/g, '$1')
    .replace(/__([^_\n]+)__/g, '$1')
    .replace(/(^|\n)[ \t]{0,3}#{1,6}[ \t]+/g, '$1');
  normalized = normalized.replace(headingPattern, '\n$1\n');
  normalized = normalized.replace(fieldLabelPattern, '\n');
  normalized = normalized.replace(/[\u2022\u25aa\u25e6\u2023\u25cf\u25cb]/g, '\n\u2022 ');
  normalized = normalized.replace(/(^|\n)[ \t]*[-*][ \t]+/g, '\n\u2022 ');
  normalized = normalized.replace(/[ \t]+[-*][ \t]+(?=[A-Za-z0-9])/g, '\n\u2022 ');
  normalized = normalized.replace(/[ \t]+/g, ' ')
    .replace(/[ \t]*\n[ \t]*/g, '\n')
    .replace(/\n{3,}/g, '\n\n')
    .trim();

  const blocks = [];
  const firstMeaningfulLine = normalized.split('\n').find(line => line.trim())?.trim();
  let paragraphLines = [];
  let bulletItems = [];
  let currentSection = '';
  const flushParagraph = () => {
    if (paragraphLines.length) blocks.push({ type: 'paragraph', text: paragraphLines.join(' ') });
    paragraphLines = [];
  };
  const flushBullets = () => {
    if (bulletItems.length) blocks.push({ type: 'list', items: bulletItems });
    bulletItems = [];
  };

  for (const rawLine of normalized.split('\n')) {
    const line = rawLine.trim();
    if (!line) {
      flushParagraph();
      flushBullets();
      continue;
    }

    const heading = sectionHeadings.find(item => item.toLowerCase() === line.replace(/:$/, '').toLowerCase());
    if (heading) {
      flushParagraph();
      flushBullets();
      blocks.push({ type: 'heading', text: heading });
      currentSection = heading;
      continue;
    }

    const field = line.match(/^((?:company(?:\s+name)?|employer|location|duration|employment\s*type|stipend|job\s+title|role|position|title|required\s+skills|skills\s+required))\s*:\s*(.*)$/i);
    if (field) {
      flushParagraph();
      flushBullets();
      blocks.push({ type: 'field', label: field[1], text: field[2] });
      continue;
    }

    if (line === firstMeaningfulLine && /[A-Z]/.test(line) && line === line.toUpperCase()) {
      flushParagraph();
      flushBullets();
      blocks.push({ type: 'title', text: line });
      continue;
    }

    const bullet = line.match(/^(?:[\u2022\u25aa\u25e6\u2023\u25cf\u25cb]|[-*]|\d+[.)])\s*(.*)$/);
    if (bullet) {
      flushParagraph();
      const item = bullet[1].trim();
      if (item) bulletItems.push(item);
      continue;
    }

    if (currentSection === 'REQUIRED SKILLS') {
      flushParagraph();
      bulletItems.push(...line.split(/[,;|]/).map(item => item.trim()).filter(Boolean));
    } else {
      flushBullets();
      paragraphLines.push(line);
    }
  }
  flushParagraph();
  flushBullets();
  return blocks;
}

function renderFormattedDescription(element, value, emptyMessage) {
  element.replaceChildren();
  const blocks = formatDescriptionBlocks(value);
  if (!blocks.length) {
    element.textContent = emptyMessage;
    return;
  }

  for (const block of blocks) {
    if (block.type === 'title') {
      const heading = document.createElement('h3');
      heading.className = 'job-description-title';
      heading.textContent = block.text;
      element.append(heading);
    } else if (block.type === 'heading') {
      const heading = document.createElement('h3');
      heading.textContent = block.text;
      element.append(heading);
    } else if (block.type === 'field') {
      const paragraph = document.createElement('p');
      paragraph.className = 'description-field';
      const label = document.createElement('strong');
      label.className = 'description-field-label';
      label.textContent = block.label + ': ';
      paragraph.append(label, document.createTextNode(block.text));
      element.append(paragraph);
    } else if (block.type === 'list') {
      const list = document.createElement('ul');
      for (const item of block.items) {
        const listItem = document.createElement('li');
        listItem.textContent = item;
        list.append(listItem);
      }
      element.append(list);
    } else {
      const paragraph = document.createElement('p');
      paragraph.textContent = block.text;
      element.append(paragraph);
    }
  }
}

function renderRequiredSkills(element, value) {
  element.replaceChildren();
  const items = String(value || '').split(/[\n,;|\u2022\u25aa\u25e6\u2023\u25cf\u25cb]+/).map(item => item.trim()).filter(Boolean);
  if (!items.length) {
    element.textContent = 'No skills provided.';
    return;
  }
  const list = document.createElement('ul');
  for (const item of items) {
    const listItem = document.createElement('li');
    listItem.textContent = item.replace(/^[-*]\s*/, '');
    list.append(listItem);
  }
  element.append(list);
}
function dateOnly(value) { return value ? String(value).slice(0, 10) : ''; }
function displayDate(value) {
  if (!value) return 'Not provided';
  const date = new Date(`${dateOnly(value)}T00:00:00`);
  return Number.isNaN(date.getTime()) ? value : new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' }).format(date);
}
function displayDateTime(value) {
  if (!value) return 'Not provided';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(date);
}
function makeCell(row, value) {
  const cell = document.createElement('td');
  cell.textContent = value || '—';
  row.append(cell);
  return cell;
}
function makeStatusCell(row, status) {
  const cell = document.createElement('td');
  const badge = document.createElement('span');
  badge.className = 'status-badge';
  badge.dataset.status = status || '';
  badge.textContent = displayStatus(status) || '—';
  cell.append(badge);
  row.append(cell);
  return cell;
}

async function renderStatusHistory(applicationId) {
  const events = await apiRequest(`/${encodeURIComponent(applicationId)}/status-history`);
  const timeline = byId('application-status-timeline');
  const emptyState = byId('status-history-empty');
  timeline.replaceChildren();
  emptyState.hidden = events.length > 0;

  events.forEach(event => {
    const item = document.createElement('li');
    item.className = 'status-timeline-event';
    item.dataset.status = event.status || '';

    const heading = document.createElement('div');
    heading.className = 'status-timeline-event-heading';
    const status = document.createElement('strong');
    status.textContent = displayStatus(event.status);
    const timestamp = document.createElement('time');
    timestamp.dateTime = event.changedAt;
    timestamp.textContent = displayDateTime(event.changedAt);
    heading.append(status, timestamp);
    item.append(heading);

    if (event.note) {
      const note = document.createElement('p');
      note.className = 'status-timeline-note';
      note.textContent = event.note;
      item.append(note);
    }
    timeline.append(item);
  });
  return events;
}
function makeLink(label, href) {
  const link = document.createElement('a');
  link.textContent = label;
  link.href = href;
  return link;
}
function showError(error) { window.alert(error.message || 'Unable to complete the request.'); }

function rowActions(id, includeDelete = true) {
  const actions = document.createElement('td');
  actions.append(makeLink('View', `application-details.html?id=${encodeURIComponent(id)}`), document.createTextNode(' · '));
  actions.append(makeLink('Edit', `edit-application.html?id=${encodeURIComponent(id)}`));
  if (includeDelete) {
    actions.append(document.createTextNode(' · '));
    const button = document.createElement('button');
    button.type = 'button';
    button.textContent = 'Delete';
    button.addEventListener('click', async () => {
      if (!window.confirm('Delete this application? This cannot be undone.')) return;
      try {
        await apiRequest(`/${id}`, { method: 'DELETE' });
        window.location.reload();
      } catch (error) { showError(error); }
    });
    actions.append(button);
  }
  return actions;
}

async function renderDashboard() {
  const applications = await apiRequest();
  const count = status => applications.filter(application => application.status === status).length;
  const interviewStatuses = ['online-assessment', 'coding-test', 'technical-interview', 'hr-interview'];
  const interviews = applications.filter(application => interviewStatuses.includes(application.status)).length;
  setText('total-applications', applications.length);
  setText('applied-count', count('applied'));
  setText('shortlisted-count', count('shortlisted'));
  setText('online-assessment-count', count('online-assessment'));
  setText('interviews-count', interviews);
  setText('offers-count', count('offer'));
  setText('rejected-count', count('rejected'));
  setText('withdrawn-count', count('withdrawn'));

  const sortRecent = (a, b) => (b.updatedAt || b.createdAt || b.applicationDate || '').localeCompare(a.updatedAt || a.createdAt || a.applicationDate || '');
  const recent = [...applications].sort(sortRecent).slice(0, 5);
  const recentBody = byId('recent-applications-list');
  recentBody.replaceChildren();
  byId('recent-applications-empty').hidden = applications.length > 0;
  byId('recent-applications-table').hidden = applications.length === 0;
  recent.forEach(application => {
    const row = document.createElement('tr');
    makeCell(row, application.company);
    makeCell(row, application.role);
    makeCell(row, application.source);
    makeStatusCell(row, application.status);
    makeCell(row, displayDate(application.applicationDate));
    row.append(rowActions(application.id, false));
    recentBody.append(row);
  });

  const today = new Date().toISOString().slice(0, 10);
  const upcoming = applications.filter(application => application.interviewDate && dateOnly(application.interviewDate) >= today)
    .sort((a, b) => a.interviewDate.localeCompare(b.interviewDate));
  const upcomingBody = byId('upcoming-interviews-list');
  upcomingBody.replaceChildren();
  byId('upcoming-interviews-empty').hidden = upcoming.length > 0;
  byId('upcoming-interviews-table').hidden = upcoming.length === 0;
  upcoming.slice(0, 5).forEach(application => {
    const row = document.createElement('tr');
    makeCell(row, application.company);
    makeCell(row, application.role);
    makeCell(row, displayDateTime(application.interviewDate));
    makeCell(row, displayStatus(application.interviewType));
    makeStatusCell(row, application.status);
    upcomingBody.append(row);
  });

  const statuses = ['applied', 'shortlisted', 'online-assessment', 'coding-test', 'technical-interview', 'hr-interview', 'offer', 'rejected', 'withdrawn'];
  const statusChart = byId('applications-by-status-chart');
  statusChart.replaceChildren();
  statuses.forEach(status => {
    const line = document.createElement('p');
    line.textContent = `${displayStatus(status)}: ${count(status)}`;
    statusChart.append(line);
  });
  const portals = new Map();
  applications.forEach(application => {
    if (application.source) portals.set(application.source, (portals.get(application.source) || 0) + 1);
  });
  const portalChart = byId('applications-by-portal-chart');
  portalChart.replaceChildren();
  if (portals.size === 0) portalChart.textContent = 'No source data yet.';
  [...portals.entries()].sort((a, b) => b[1] - a[1]).forEach(([source, total]) => {
    const line = document.createElement('p');
    line.textContent = `${displayStatus(source)}: ${total}`;
    portalChart.append(line);
  });
  setText('interview-conversion-rate', applications.length ? `${Math.round(interviews * 100 / applications.length)}%` : '0%');
}

async function renderApplications() {
  const applications = await apiRequest();
  let params = new URLSearchParams(window.location.search);
  const searchForm = byId('application-search-form');
  const filterForm = byId('application-filter-form');
  const searchInput = searchForm.elements.search;

  searchInput.value = params.get('search') || '';
  filterForm.elements.status.value = params.get('status') || '';
  filterForm.elements.source.value = params.get('source') || '';
  filterForm.elements.location.value = params.get('location') || '';
  filterForm.elements.applicationDate.value = params.get('applicationDate') || '';

  function renderFilteredApplications() {
    const search = searchInput.value.trim().toLowerCase();
    const status = filterForm.elements.status.value;
    const source = filterForm.elements.source.value;
    const location = filterForm.elements.location.value.trim().toLowerCase();
    const applicationDate = filterForm.elements.applicationDate.value;

    const filtered = applications.filter(application => {
      const company = (application.company || '').toLowerCase();
      const applicationLocation = (application.location || '').toLowerCase();
      return (!search || company.includes(search))
        && (!status || application.status === status)
        && (!source || application.source === source)
        && (!location || applicationLocation.includes(location))
        && (!applicationDate || application.applicationDate === applicationDate);
    }).sort((a, b) => (b.applicationDate || '').localeCompare(a.applicationDate || ''));

    let page = Math.max(1, Number.parseInt(params.get('page') || '1', 10) || 1);
    const pageCount = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
    page = Math.min(page, pageCount);

    const list = byId('applications-list');
    list.replaceChildren();
    filtered.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE).forEach(application => {
      const row = document.createElement('tr');
      makeCell(row, application.company);
      makeCell(row, application.role);
      makeCell(row, application.location);
      makeCell(row, application.source);
      makeStatusCell(row, application.status);
      makeCell(row, displayDate(application.applicationDate));
      makeCell(row, displayDateTime(application.interviewDate));
      row.append(rowActions(application.id));
      list.append(row);
    });

    byId('applications-empty-state').hidden = filtered.length > 0;
    byId('applications-table').hidden = filtered.length === 0;
    setText('pagination-current-page', `Page ${page} of ${pageCount}`);

    const previous = byId('previous-page');
    const next = byId('next-page');
    const queryForPage = value => {
      const nextParams = new URLSearchParams(params);
      nextParams.set('page', value);
      return `?${nextParams.toString()}`;
    };
    previous.href = queryForPage(Math.max(1, page - 1));
    next.href = queryForPage(Math.min(pageCount, page + 1));
    previous.setAttribute('aria-disabled', String(page === 1));
    next.setAttribute('aria-disabled', String(page === pageCount));
  }

  function applyFilters() {
    params = new URLSearchParams(window.location.search);
    const values = {
      search: searchInput.value.trim(),
      status: filterForm.elements.status.value,
      source: filterForm.elements.source.value,
      location: filterForm.elements.location.value.trim(),
      applicationDate: filterForm.elements.applicationDate.value
    };
    Object.entries(values).forEach(([name, value]) => {
      if (value) params.set(name, value); else params.delete(name);
    });
    params.set('page', '1');

    const query = params.toString();
    window.history.replaceState(null, '', `${window.location.pathname}${query ? `?${query}` : ''}`);
    renderFilteredApplications();
  }

  searchInput.addEventListener('input', applyFilters);
  ['status', 'source', 'location', 'applicationDate'].forEach(name => {
    filterForm.elements[name].addEventListener('input', applyFilters);
    filterForm.elements[name].addEventListener('change', applyFilters);
  });
  searchForm.addEventListener('submit', event => {
    event.preventDefault();
    applyFilters();
  });
  filterForm.addEventListener('submit', event => {
    event.preventDefault();
    applyFilters();
  });
  document.querySelectorAll('#applications-pagination a').forEach(link => link.addEventListener('click', event => {
    if (link.getAttribute('aria-disabled') === 'true') event.preventDefault();
  }));

  renderFilteredApplications();
}

function navigateWithParams(params) {
  const query = params.toString();
  window.location.href = `${window.location.pathname}${query ? `?${query}` : ''}`;
}

function applicationFromForm(form) {
  const value = name => form.elements[name]?.value.trim() || null;
  const interviewDate = value('interviewDate');
  return {
    company: value('companyName'),
    role: value('jobRole'),
    applicationDate: value('applicationDate'),
    status: value('status'),
    location: value('location'),
    source: value('source'),
    interviewDate: interviewDate ? `${interviewDate}T00:00:00` : null,
    interviewType: value('interviewType'),
    jobUrl: value('jobUrl'),
    jobDescription: value('jobDescription'),
    requiredSkills: value('requiredSkills'),
    notes: value('notes')
  };
}

function connectAddForm() {
  const form = byId('add-application-form');
  const dateInput = form.elements.applicationDate;
  if (!dateInput.value) {
    const today = new Date();
    const year = today.getFullYear();
    const month = String(today.getMonth() + 1).padStart(2, '0');
    const day = String(today.getDate()).padStart(2, '0');
    dateInput.value = `${year}-${month}-${day}`;
  }
  if (!form.elements.status.value) form.elements.status.value = 'applied';

  connectJobDescriptionExtraction(form);
  form.addEventListener('submit', async event => {
    event.preventDefault();
    try {
      await apiRequest('', { method: 'POST', body: JSON.stringify(applicationFromForm(form)) });
      window.location.href = 'applications.html';
    } catch (error) { showError(error); }
  });
}

function connectJobDescriptionExtraction(form) {
  const button = byId('extract-job-information');
  if (!button) return;

  const pastedText = byId('pasted-job-description');
  const message = byId('extraction-message');
  button.addEventListener('click', async () => {
    const text = pastedText.value.trim();
    if (!text) {
      message.textContent = 'Paste a job description first.';
      message.dataset.state = 'error';
      message.hidden = false;
      return;
    }

    button.disabled = true;
    button.textContent = 'Extracting…';
    message.textContent = 'Reading the pasted text…';
    message.dataset.state = 'loading';
    message.hidden = false;

    try {
      const extracted = await apiRequest('/extract', {
        method: 'POST',
        body: JSON.stringify({ text })
      });
      const fields = [
        ['companyName', extracted.company],
        ['jobRole', extracted.role],
        ['location', extracted.location],
        ['requiredSkills', extracted.requiredSkills],
        ['jobUrl', extracted.jobUrl],
        ['jobDescription', extracted.jobDescription]
      ];
      const identified = [];
      fields.forEach(([name, value]) => {
        if (typeof value !== 'string' || !value.trim()) return;
        form.elements[name].value = value;
        if (name !== 'jobDescription') identified.push(name);
      });

      if (identified.length === 0) {
        message.textContent = 'The pasted text was added as the job description, but no other details were clear enough to fill in. Please enter them manually.';
      } else {
        message.textContent = `Found ${identified.length} detail${identified.length === 1 ? '' : 's'}. Review and edit the fields before saving; fill in any missing details manually.`;
      }
      message.dataset.state = 'success';
    } catch (error) {
      message.textContent = error.message || 'Could not extract information. You can still enter the details manually.';
      message.dataset.state = 'error';
    } finally {
      button.disabled = false;
      button.textContent = 'Extract Information';
    }
  });
}

async function connectEditForm() {
  const form = byId('edit-application-form');
  const id = new URLSearchParams(window.location.search).get('id');
  if (!id) { window.alert('Application ID is missing.'); window.location.href = 'applications.html'; return; }
  form.dataset.applicationId = id;
  byId('edit-application-id').value = id;
  try {
    const application = await apiRequest(`/${encodeURIComponent(id)}`);
    const statusHistory = await apiRequest(`/${encodeURIComponent(id)}/status-history`);
    restrictRejectedAfterOffer(
      form.elements.status,
      application.status === 'offer' || statusHistory.some(event => event.status === 'offer'),
      byId('edit-status-restriction'));
    const values = {
      companyName: application.company, jobRole: application.role, applicationDate: dateOnly(application.applicationDate),
      status: application.status, location: application.location, source: application.source,
      interviewDate: dateOnly(application.interviewDate), interviewType: application.interviewType,
      jobUrl: application.jobUrl, jobDescription: application.jobDescription,
      requiredSkills: application.requiredSkills, notes: application.notes
    };
    Object.entries(values).forEach(([name, value]) => { form.elements[name].value = value || ''; });
  } catch (error) { showError(error); window.location.href = 'applications.html'; return; }
  form.addEventListener('submit', async event => {
    event.preventDefault();
    try {
      await apiRequest(`/${encodeURIComponent(id)}`, { method: 'PUT', body: JSON.stringify(applicationFromForm(form)) });
      window.location.href = `application-details.html?id=${encodeURIComponent(id)}`;
    } catch (error) { showError(error); }
  });
}

async function renderDetails() {
  const id = new URLSearchParams(window.location.search).get('id');
  if (!id) { window.alert('Application ID is missing.'); window.location.href = 'applications.html'; return; }
  try {
    const application = await apiRequest(`/${encodeURIComponent(id)}`);
    setText('detail-job-role', application.role);
    setText('detail-company-name', application.company);
    setText('detail-location', application.location || 'Not provided');
    setText('detail-source', application.source || 'Not provided');
    const urlElement = byId('detail-job-url');
    urlElement.replaceChildren();
    if (application.jobUrl) {
      try {
        const url = new URL(application.jobUrl);
        if (['http:', 'https:'].includes(url.protocol)) {
          const link = makeLink(application.jobUrl, url.href);
          link.rel = 'noopener noreferrer';
          link.target = '_blank';
          urlElement.append(link);
        } else urlElement.textContent = 'Not provided';
      } catch { urlElement.textContent = 'Not provided'; }
    } else urlElement.textContent = 'Not provided';
    setText('detail-application-date', displayDate(application.applicationDate));
    setText('detail-status', displayStatus(application.status));
    setText('detail-interview-date', application.interviewDate ? displayDateTime(application.interviewDate) : 'Not scheduled');
    setText('detail-interview-type', application.interviewType ? displayStatus(application.interviewType) : 'Not provided');
    setText('detail-created-date', displayDateTime(application.createdAt));
    setText('detail-updated-date', displayDateTime(application.updatedAt));
    renderFormattedDescription(byId('detail-job-description'), application.jobDescription, 'No description provided.');
    renderRequiredSkills(byId('detail-required-skills'), application.requiredSkills);
    setText('detail-notes', application.notes || 'No notes.');
    byId('edit-application-link').href = `edit-application.html?id=${encodeURIComponent(id)}`;
    const statusHistory = await renderStatusHistory(id);

    const statusForm = byId('status-update-form');
    const statusInput = statusForm.elements.status;
    const noteInput = statusForm.elements.note;
    const statusMessage = byId('status-update-message');
    restrictRejectedAfterOffer(
      statusInput,
      application.status === 'offer' || statusHistory.some(event => event.status === 'offer'),
      byId('status-update-restriction'));
    let currentStatus = application.status;
    statusInput.value = currentStatus;
    statusForm.addEventListener('submit', async event => {
      event.preventDefault();
      const nextStatus = statusInput.value;
      const note = noteInput.value.trim();
      const unchanged = nextStatus === currentStatus;
      const submitButton = statusForm.querySelector('button[type="submit"]');
      submitButton.disabled = true;
      statusMessage.hidden = true;
      try {
        const updated = await apiRequest(`/${encodeURIComponent(id)}/status`, {
          method: 'PATCH',
          body: JSON.stringify({ status: nextStatus, note: note || null })
        });
        currentStatus = updated.status;
        statusInput.value = currentStatus;
        setText('detail-status', displayStatus(currentStatus));
        setText('detail-updated-date', displayDateTime(updated.updatedAt));
        await renderStatusHistory(id);
        noteInput.value = '';
        statusMessage.textContent = unchanged
          ? 'This is already the current status. No timeline event was added.'
          : 'Status updated and added to the timeline.';
        statusMessage.dataset.state = 'success';
      } catch (error) {
        statusMessage.textContent = error.message || 'Could not update the application status.';
        statusMessage.dataset.state = 'error';
      } finally {
        statusMessage.hidden = false;
        submitButton.disabled = false;
      }
    });
    byId('delete-application-button').addEventListener('click', async () => {
      if (!window.confirm('Delete this application? This cannot be undone.')) return;
      try {
        await apiRequest(`/${encodeURIComponent(id)}`, { method: 'DELETE' });
        window.location.href = 'applications.html';
      } catch (error) { showError(error); }
    });
  } catch (error) { showError(error); window.location.href = 'applications.html'; }
}

document.addEventListener('DOMContentLoaded', async () => {
  try {
    if (byId('total-applications')) await renderDashboard();
    if (byId('applications-list')) await renderApplications();
    if (byId('add-application-form')) connectAddForm();
    if (byId('edit-application-form')) await connectEditForm();
    if (byId('application-details')) await renderDetails();
  } catch (error) { showError(error); }
});
