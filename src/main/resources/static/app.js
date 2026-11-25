'use strict';
const $ = id => document.getElementById(id);
const escapeHtml = value => String(value ?? '').replace(/[&<>"']/g, char => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[char]));
const passwords = {alice:'AliceDemo21!',bob:'BobDemo21!',operateur:'OperateurDemo21!'};
let authorization = '', csrf = null, actor = null, vehicles = [], stations = [], searchSnapshot = null;
const localDate = value => new Intl.DateTimeFormat('fr-FR',{dateStyle:'short',timeStyle:'short'}).format(new Date(value));
function message(text, error = false) { $('message').textContent = text; $('message').className = error ? 'error' : ''; $('message').hidden = false; }
async function api(path, method = 'GET', body) {
  const headers = {Accept:'application/json'};
  if (authorization) headers.Authorization = authorization;
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (method !== 'GET' && csrf) headers[csrf.headerName] = csrf.token;
  const response = await fetch('/api' + path,{method,headers,body:body === undefined ? undefined : JSON.stringify(body),credentials:'same-origin'});
  const data = response.status === 204 ? null : await response.json();
  if (!response.ok) {
    const reasons = data.conflicts?.map(c => c.message).filter((v,i,a) => a.indexOf(v) === i).join('\n');
    throw new Error([`${response.status} · ${data.message || 'Requête refusée'}`,reasons && reasons !== data.message ? reasons : '',data.proposal || ''].filter(Boolean).join('\n'));
  }
  return data;
}
function action(handler) { return async event => { event.preventDefault(); const button = event.submitter || event.currentTarget; if (button) button.disabled = true; try { await handler(event); } catch (error) { message(error.message,true); } finally { if (button) button.disabled = false; } }; }
function defaultDates() {
  const tomorrow = new Date(); tomorrow.setDate(tomorrow.getDate()+1); tomorrow.setHours(9,0,0,0);
  const format = date => new Date(date.getTime()-date.getTimezoneOffset()*60000).toISOString().slice(0,16);
  for (const id of ['start','maintenanceStart']) $(id).value = format(tomorrow);
  tomorrow.setHours(10); for (const id of ['end','maintenanceEnd']) $(id).value = format(tomorrow);
}
function stationOptions(target, allowAll = false) {
  const old = $(target).value;
  $(target).innerHTML = (allowAll ? '<option value="">Toutes les stations</option>' : '') + stations.map(s => `<option value="${s.id}">${escapeHtml(s.name)}${s.active ? '' : ' (inactive)'}</option>`).join('');
  if ([...$(target).options].some(o => o.value === old)) $(target).value = old;
}
function maintenanceConnectors() {
  const station = stations.find(s => s.id === Number($('maintenanceStation').value));
  $('maintenanceConnector').innerHTML = '<option value="">Toute la station</option>' + (station?.connectors || []).map(c => `<option value="${c.id}">${escapeHtml(c.name)}</option>`).join('');
}
async function refresh() {
  const [network,cars,bookings,notes] = await Promise.all([api('/stations'),api('/vehicles'),api('/reservations'),api('/notifications')]);
  stations = network; vehicles = cars;
  const oldVehicle = $('vehicle').value;
  $('vehicle').innerHTML = vehicles.map(v => `<option value="${v.id}">${escapeHtml(v.name)} · ${v.connectorType} · ${v.maximumAcceptedPowerKw} kW max.</option>`).join('');
  if (vehicles.some(v => String(v.id) === oldVehicle)) $('vehicle').value = oldVehicle;
  stationOptions('station',true); stationOptions('connectorStation'); stationOptions('maintenanceStation'); maintenanceConnectors();
  renderStations(); renderReservations(bookings); renderNotifications(notes);
  if (actor.operator) renderMaintenances(await api('/maintenances'));
}
function renderStations() {
  $('stations').innerHTML = stations.map(s => `<div class="station-card"><div class="station-header"><h3>${escapeHtml(s.name)} · ${s.maximumPowerKw} kW</h3><span class="status ${s.active ? '' : 'off'}">${s.active ? 'Active' : 'Inactive'}</span></div>${actor.operator ? `<button class="secondary small-btn" data-edit-station="${s.id}">Modifier la station</button>` : ''}${s.connectors.map(c => `<div class="connector-row"><span>${escapeHtml(c.name)} · ${c.type} · ${c.maximumPowerKw} kW ${c.active ? '' : '· inactif'}</span>${actor.operator ? `<button class="secondary small-btn" data-edit-connector="${c.id}">Modifier</button>` : ''}</div>`).join('')}</div>`).join('') || '<p class="empty">Aucune station.</p>';
}
function renderReservations(bookings) {
  $('reservations').innerHTML = bookings.length ? `<table><thead><tr><th>N° / conducteur</th><th>Station / connecteur</th><th>Début → fin</th><th>Puissance</th><th>Statut</th><th>Action</th></tr></thead><tbody>${bookings.map(r => `<tr><td>#${r.id} / ${r.driverId}</td><td>${escapeHtml(stations.find(s => s.id === r.stationId)?.name || r.stationId)} / #${r.connectorId}</td><td>${escapeHtml(localDate(r.startAt))}<br>${escapeHtml(localDate(r.endAt))}</td><td>${r.allocatedPowerKw} kW</td><td><span class="status ${r.status === 'CANCELLED' ? 'off' : ''}">${r.status === 'CONFIRMED' ? 'Confirmée' : 'Annulée'}</span></td><td>${r.status === 'CONFIRMED' && new Date(r.startAt)>new Date() ? `<button class="danger" data-cancel="${r.id}">Annuler</button>` : '—'}</td></tr>`).join('')}</tbody></table>` : '<p class="empty">Aucune réservation pour le moment.</p>';
}
function renderNotifications(notes) {
  $('notifications').innerHTML = notes.length ? notes.slice(0,20).map(n => `<article class="note"><span class="status ${n.status === 'FAILED' ? 'failed' : ''}">${n.status === 'FAILED' ? 'Échec de livraison' : 'Notification locale'}</span><p>Réservation #${n.reservationId} ${n.eventType === 'CONFIRMED' ? 'confirmée' : 'annulée'}.</p><time>${escapeHtml(localDate(n.createdAt))}</time></article>`).join('') : '<p class="empty">Les confirmations et annulations apparaîtront ici.</p>';
}
function renderMaintenances(windows) {
  $('maintenances').innerHTML = windows.length ? `<table><thead><tr><th>Station / connecteur</th><th>Période</th><th>Motif</th><th>Action</th></tr></thead><tbody>${windows.map(m => `<tr><td>#${m.stationId} / ${m.connectorId ? '#'+m.connectorId : 'toute la station'}</td><td>${escapeHtml(localDate(m.startAt))}<br>${escapeHtml(localDate(m.endAt))}</td><td>${escapeHtml(m.reason)}</td><td><button class="danger" data-delete-maintenance="${m.id}">Supprimer</button></td></tr>`).join('')}</tbody></table>` : '<p class="empty">Aucune maintenance planifiée.</p>';
}
$('username').addEventListener('change',() => { $('password').value = passwords[$('username').value]; });
$('loginForm').addEventListener('submit',action(async () => {
  authorization = 'Basic ' + btoa($('username').value + ':' + $('password').value);
  csrf = await api('/csrf'); actor = await api('/me'); await refresh();
  $('loginPanel').hidden = true; $('workspace').hidden = false; $('operatorPanel').hidden = !actor.operator;
  $('identity').textContent = `${actor.username} · ${actor.operator ? 'opérateur' : 'conducteur '+actor.driverId}`;
  $('reservationTitle').textContent = actor.operator ? 'Toutes les réservations' : 'Mes réservations';
  message(`Connecté : ${actor.username}. Choisis un créneau pour commencer.`);
}));
$('logout').addEventListener('click',() => { authorization = ''; actor = null; vehicles = []; stations = []; searchSnapshot = null; $('workspace').hidden = true; $('loginPanel').hidden = false; $('availability').innerHTML = '<p class="empty">Choisis un créneau pour voir les disponibilités.</p>'; message('Déconnexion effectuée.'); });
$('refresh').addEventListener('click',action(async () => { await refresh(); message('Données actualisées.'); }));
$('searchForm').addEventListener('submit',action(async () => {
  const snapshot = {vehicleId:Number($('vehicle').value),startAt:new Date($('start').value).toISOString(),endAt:new Date($('end').value).toISOString(),requestedPowerKw:Number($('power').value)};
  const parameters = new URLSearchParams(snapshot);
  if ($('station').value) parameters.set('stationId',$('station').value);
  if ($('type').value) parameters.set('connectorType',$('type').value);
  const available = await api('/availability?'+parameters.toString()); searchSnapshot = snapshot;
  $('availability').innerHTML = available.length ? available.map(c => `<article class="result-card"><h3>${escapeHtml(c.stationName)}</h3><p>${escapeHtml(c.connectorName)} · ${c.type}<br>Jusqu’à <strong>${c.availablePowerKw} kW</strong> sur tout le créneau</p><button data-book="${c.connectorId}">Réserver à ${snapshot.requestedPowerKw} kW</button></article>`).join('') : '<p class="empty">Aucun connecteur disponible pour cette demande. Essaie un autre créneau, une puissance plus faible ou une autre station.</p>';
  message(`${available.length} connecteur(s) disponible(s).`);
}));
document.addEventListener('click',async event => {
  const button = event.target.closest('button[data-book],button[data-cancel],button[data-edit-station],button[data-edit-connector],button[data-delete-maintenance]');
  if (!button) return; button.disabled = true;
  try {
    if (button.dataset.book) {
      const vehicle = vehicles.find(v => v.id === searchSnapshot.vehicleId);
      const reservation = await api('/reservations','POST',{...searchSnapshot,driverId:vehicle.driverId,connectorId:Number(button.dataset.book)});
      $('availability').innerHTML = '<p class="empty">Réservation enregistrée. Relance une recherche pour consulter les disponibilités restantes.</p>';
      await refresh(); message(`Réservation #${reservation.id} confirmée à ${reservation.allocatedPowerKw} kW.`);
    } else if (button.dataset.cancel) { await api(`/reservations/${button.dataset.cancel}/cancel`,'POST'); await refresh(); $('availability').innerHTML = '<p class="empty">Relance une recherche pour actualiser les disponibilités.</p>'; message('Réservation annulée, créneau libéré.');
    } else if (button.dataset.deleteMaintenance) { await api(`/maintenances/${button.dataset.deleteMaintenance}`,'DELETE'); await refresh(); message('Maintenance supprimée.');
    } else if (button.dataset.editStation) {
      const s = stations.find(s => s.id === Number(button.dataset.editStation));
      $('stationEditId').value = s.id; $('stationName').value = s.name; $('stationPower').value = s.maximumPowerKw; $('stationActive').checked = s.active; $('stationFormTitle').textContent = 'Modifier la station #'+s.id; $('stationForm').scrollIntoView({behavior:'smooth',block:'center'});
    } else if (button.dataset.editConnector) {
      const c = stations.flatMap(s => s.connectors).find(c => c.id === Number(button.dataset.editConnector));
      $('connectorEditId').value = c.id; $('connectorStation').value = c.stationId; $('connectorStation').disabled = true; $('connectorName').value = c.name; $('connectorType').value = c.type; $('connectorPower').value = c.maximumPowerKw; $('connectorActive').checked = c.active; $('connectorFormTitle').textContent = 'Modifier le connecteur #'+c.id; $('connectorForm').scrollIntoView({behavior:'smooth',block:'center'});
    }
  } catch(error) { message(error.message,true); } finally { button.disabled = false; }
});
function resetStation() { $('stationForm').reset(); $('stationEditId').value = ''; $('stationFormTitle').textContent = 'Ajouter une station'; }
function resetConnector() { $('connectorForm').reset(); $('connectorEditId').value = ''; $('connectorStation').disabled = false; $('connectorFormTitle').textContent = 'Ajouter un connecteur'; }
$('resetStation').addEventListener('click',resetStation); $('resetConnector').addEventListener('click',resetConnector);
$('stationForm').addEventListener('submit',action(async () => {
  const id = $('stationEditId').value; await api('/stations'+(id ? '/'+id : ''),id ? 'PATCH' : 'POST',{name:$('stationName').value,maximumPowerKw:Number($('stationPower').value),active:$('stationActive').checked});
  resetStation(); await refresh(); message('Station enregistrée.');
}));
$('connectorForm').addEventListener('submit',action(async () => {
  const id = $('connectorEditId').value; const path = id ? '/connectors/'+id : '/stations/'+$('connectorStation').value+'/connectors';
  await api(path,id ? 'PATCH' : 'POST',{name:$('connectorName').value,type:$('connectorType').value,maximumPowerKw:Number($('connectorPower').value),active:$('connectorActive').checked});
  resetConnector(); await refresh(); message('Connecteur enregistré.');
}));
$('maintenanceStation').addEventListener('change',maintenanceConnectors);
$('maintenanceForm').addEventListener('submit',action(async () => {
  await api('/maintenances','POST',{stationId:Number($('maintenanceStation').value),connectorId:$('maintenanceConnector').value ? Number($('maintenanceConnector').value) : null,startAt:new Date($('maintenanceStart').value).toISOString(),endAt:new Date($('maintenanceEnd').value).toISOString(),reason:$('maintenanceReason').value});
  await refresh(); message('Maintenance planifiée.');
}));
defaultDates();
