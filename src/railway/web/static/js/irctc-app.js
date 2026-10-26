/**
 * IRCTC Next Generation e-Ticketing System - Official Frontend Application
 * High-Fidelity IRCTC Clone with Local AI Concurrent Swarm & Ask DISHA 2.0
 * Course Code: E1CSA25306 (Alliance University)
 */

let allDemoTrains = [];
let allBookingsCache = [];
let activeTrainNumber = 'DEMO101';
let activeTicketClass = 'GENERAL';
let aiFeedLastId = 0;
let aiPollingInterval = null;

document.addEventListener('DOMContentLoaded', () => {
    initClock();
    initTabs();
    initDatePicker();
    initSearchForm();
    initBookingForm();
    initPnrForm();
    initSwarmControls();
    initWebAudio();
    init3dRailwayDiorama();
    initTatkalWarpCanvas();

    // Initial load
    loadInitialTrains();
    startAiFeedPolling();
});

// -------------------------------------------------------------
// LIVE IST DIGITAL CLOCK (IRCTC OFFICIAL FORMAT)
// -------------------------------------------------------------
function initClock() {
    function updateClock() {
        const now = new Date();
        const options = { timeZone: 'Asia/Kolkata', day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false };
        const formatter = new Intl.DateTimeFormat('en-GB', options);
        const parts = formatter.formatToParts(now);
        
        let day = '', month = '', year = '', hour = '', min = '', sec = '';
        parts.forEach(p => {
            if (p.type === 'day') day = p.value;
            if (p.type === 'month') month = p.value;
            if (p.type === 'year') year = p.value;
            if (p.type === 'hour') hour = p.value;
            if (p.type === 'minute') min = p.value;
            if (p.type === 'second') sec = p.value;
        });

        const clockEl = document.getElementById('ist-clock');
        if (clockEl) {
            clockEl.textContent = `${day}-${month}-${year} [${hour}:${min}:${sec} IST]`;
        }
    }
    updateClock();
    setInterval(updateClock, 1000);
}

// -------------------------------------------------------------
// NAVIGATION TABS
// -------------------------------------------------------------
function initTabs() {
    const tabButtons = document.querySelectorAll('.nav-item-btn, .tab-btn');
    tabButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            const targetId = btn.getAttribute('data-tab');
            if (targetId) switchTab(targetId);
        });
    });
}

function switchTab(tabId) {
    document.querySelectorAll('.nav-item-btn').forEach(b => b.classList.remove('active'));
    document.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('active'));

    const activeNav = document.querySelector(`.nav-item-btn[data-tab="${tabId}"]`);
    const activePanel = document.getElementById(tabId);

    if (activeNav) activeNav.classList.add('active');
    if (activePanel) activePanel.classList.add('active');

    // Trigger tab-specific loaders
    if (tabId === 'availability-tab') {
        loadAvailabilityGrid();
    } else if (tabId === 'wl-tab') {
        loadWaitingListTab();
    } else if (tabId === 'history-tab') {
        loadBookingHistory();
    }
}

// -------------------------------------------------------------
// SEARCH & TRAIN CARDS
// -------------------------------------------------------------
function initDatePicker() {
    const dateInput = document.getElementById('travel-date');
    if (dateInput) {
        const today = new Date().toISOString().split('T')[0];
        dateInput.value = today;
        dateInput.min = today;
    }
}

function setStation(type, name) {
    if (type === 'source') {
        document.getElementById('source-input').value = name;
    } else {
        document.getElementById('dest-input').value = name;
    }
}

function swapStations() {
    const src = document.getElementById('source-input');
    const dst = document.getElementById('dest-input');
    const tmp = src.value;
    src.value = dst.value;
    dst.value = tmp;
}

function initSearchForm() {
    const form = document.getElementById('search-form');
    if (!form) return;
    form.addEventListener('submit', (e) => {
        e.preventDefault();
        const src = document.getElementById('source-input').value.trim();
        const dst = document.getElementById('dest-input').value.trim();
        const preferredClass = document.getElementById('class-filter').value;
        searchTrains(src, dst, preferredClass);
    });
}

async function loadInitialTrains() {
    try {
        const res = await fetch('/api/trains');
        const json = await res.json();
        if (json.success && json.data) {
            allDemoTrains = json.data;
            searchTrains('Bengaluru', 'Chennai', 'ALL');
        }
    } catch (err) {
        showToast('Error loading trains from server: ' + err.message, 'error');
    }
}

async function searchTrains(source, dest, preferredClass = 'ALL') {
    const resultsContainer = document.getElementById('train-results');
    const countBadge = document.getElementById('results-count');
    const heading = document.getElementById('results-heading');

    resultsContainer.innerHTML = '<div style="padding:20px; text-align:center; color:#666;">Searching Indian Railways timetable...</div>';

    try {
        const url = `/api/trains/search?source=${encodeURIComponent(source)}&destination=${encodeURIComponent(dest)}`;
        const res = await fetch(url);
        const json = await res.json();

        if (json.success && json.data) {
            let trains = json.data;
            if (preferredClass !== 'ALL') {
                trains = trains.filter(t => t.availableSeats > 0);
            }

            heading.textContent = `${source.toUpperCase()} ➔ ${dest.toUpperCase()}`;
            countBadge.textContent = `${trains.length} Train(s) Found`;

            if (trains.length === 0) {
                resultsContainer.innerHTML = `
                    <div style="background:#fff; border:1px solid #dcdfe6; border-radius:6px; padding:32px; text-align:center;">
                        <h4 style="color:#213d77; margin-bottom:8px;">No direct trains found between ${source} and ${dest}</h4>
                        <p style="color:#666; font-size:13px;">Try popular routes: <strong>Bengaluru ➔ Chennai</strong>, <strong>Bengaluru ➔ Mysuru</strong>, or <strong>Chennai ➔ Hyderabad</strong>.</p>
                        <button class="btn-search-irctc" style="margin:16px auto 0 auto; padding:8px 16px; font-size:13px;" onclick="setStation('source','Bengaluru'); setStation('dest','Chennai'); searchTrains('Bengaluru','Chennai');">Load Bengaluru ➔ Chennai Trains</button>
                    </div>`;
                return;
            }

            resultsContainer.innerHTML = '';
            trains.forEach(t => {
                resultsContainer.appendChild(createIrctcTrainCard(t));
            });
        } else {
            resultsContainer.innerHTML = `<div style="background:#fff; padding:20px; border-radius:6px; color:#d32f2f;">${json.message || 'Error occurred.'}</div>`;
        }
    } catch (err) {
        resultsContainer.innerHTML = `<div style="background:#fff; padding:20px; border-radius:6px; color:#d32f2f;">Failed to search trains: ${err.message}</div>`;
    }
}

function createIrctcTrainCard(train) {
    const card = document.createElement('div');
    card.className = 'irctc-train-card';
    card.id = `train-card-${train.number}`;

    const baseFare = train.baseFare;
    const generalFare = baseFare.toFixed(2);
    const sleeperFare = (baseFare * 1.5).toFixed(2);
    const acFare = (baseFare * 2.5).toFixed(2);

    const isAvailable = train.availableSeats > 0;
    const availLabel = isAvailable 
        ? `AVAILABLE-${String(train.availableSeats).padStart(4, '0')}` 
        : `WL (Waiting List)`;
    const availClass = isAvailable ? 'available' : 'waiting';

    card.innerHTML = `
        <div class="train-header-row">
            <div class="train-title-area">
                <span class="train-name-tag">${train.number} | ${train.name.toUpperCase()}</span>
                <span style="font-size:11px; background:#eef2f8; color:#213d77; font-weight:700; padding:2px 8px; border-radius:3px;">DAILY EXPRESS</span>
            </div>
            <div class="runs-on-days">
                <span>Runs On:</span>
                <span class="day active">M</span>
                <span class="day active">T</span>
                <span class="day active">W</span>
                <span class="day active">T</span>
                <span class="day active">F</span>
                <span class="day active">S</span>
                <span class="day active">S</span>
            </div>
        </div>

        <div class="train-schedule-row">
            <div class="sched-col">
                <span class="time-val">${train.departureTime}</span>
                <span class="station-val">${train.source.toUpperCase()}</span>
            </div>
            <div class="sched-col center">
                <span class="duration-line">⏱️ ~04:30 hrs</span>
                <div class="track-line"></div>
                <span style="font-size:11px; color:#888;">Direct Service</span>
            </div>
            <div class="sched-col right">
                <span class="time-val">${train.arrivalTime}</span>
                <span class="station-val">${train.destination.toUpperCase()}</span>
            </div>
        </div>

        <div class="class-boxes-row">
            <!-- 2S GENERAL -->
            <div class="class-card selected" onclick="selectClassCard('${train.number}', 'GENERAL', this)">
                <div class="class-top-meta">
                    <span class="class-code">2S (General)</span>
                    <span class="class-fare">₹${generalFare}</span>
                </div>
                <div class="avail-status-pill ${availClass}" id="avail-${train.number}-general">
                    ${availLabel}
                </div>
            </div>

            <!-- SL SLEEPER -->
            <div class="class-card" onclick="selectClassCard('${train.number}', 'SLEEPER', this)">
                <div class="class-top-meta">
                    <span class="class-code">SL (Sleeper)</span>
                    <span class="class-fare">₹${sleeperFare}</span>
                </div>
                <div class="avail-status-pill ${availClass}" id="avail-${train.number}-sleeper">
                    ${availLabel}
                </div>
            </div>

            <!-- 3A AC -->
            <div class="class-card" onclick="selectClassCard('${train.number}', 'AC', this)">
                <div class="class-top-meta">
                    <span class="class-code">3A (AC 3 Tier)</span>
                    <span class="class-fare">₹${acFare}</span>
                </div>
                <div class="avail-status-pill ${availClass}" id="avail-${train.number}-ac">
                    ${availLabel}
                </div>
            </div>

            <div class="card-actions-row">
                <button type="button" class="btn-book-class" onclick="openBookingModal('${train.number}', 'GENERAL')">Book Now</button>
                <button type="button" class="btn-card-rush" title="Simulate AI Passenger Tatkal Bookings" onclick="triggerTatkalRushOnTrain('${train.number}')">⚡ Tatkal Rush</button>
            </div>
        </div>
    `;

    return card;
}

function selectClassCard(trainNumber, ticketClass, element) {
    const parent = element.closest('.class-boxes-row');
    if (parent) {
        parent.querySelectorAll('.class-card').forEach(c => c.classList.remove('selected'));
        element.classList.add('selected');
        const bookBtn = parent.querySelector('.btn-book-class');
        if (bookBtn) {
            bookBtn.onclick = () => openBookingModal(trainNumber, ticketClass);
        }
    }
}

// -------------------------------------------------------------
// LOCAL AI PASSENGER SWARM & LIVE FEED
// -------------------------------------------------------------
function initSwarmControls() {
    const rushBtn = document.getElementById('trigger-rush-btn');
    if (rushBtn) {
        rushBtn.addEventListener('click', () => {
            const trainSelect = document.getElementById('rush-train-select');
            const targetTrain = trainSelect ? trainSelect.value : 'DEMO101';
            triggerTatkalRushOnTrain(targetTrain);
        });
    }

    const modeButtons = document.querySelectorAll('.swarm-mode-btn');
    modeButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            const mode = btn.getAttribute('data-mode');
            setSwarmMode(mode);
            modeButtons.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
        });
    });
}

async function triggerTatkalRushOnTrain(trainNumber) {
    const train = allDemoTrains.find(t => t.number.toUpperCase() === trainNumber.toUpperCase());
    const available = (train && train.availableSeats > 0) ? train.availableSeats : (train ? train.totalSeats : 40);
    // User specification: strictly >15 bots, exactly half of seats (e.g. 40 seats -> 20 bots)
    const halfSeats = Math.ceil(available / 2);
    const botCount = Math.max(16, halfSeats);

    // Audio SFX & 3D Visual Effects
    playAudioTone('tatkalRush');
    launch3dTatkalWarp(trainNumber, botCount);
    boost3dTrainSpeed();

    showToast(`⚡ 10:00 AM Tatkal Rush: Dispatched ${botCount} AI passengers on Tatkal Quota (50% capacity, min >15)!`, 'info');

    // High concurrency visual pulse
    const card = document.getElementById(`train-card-${trainNumber}`);
    if (card) {
        card.classList.add('tatkal-pulsing');
        setTimeout(() => card.classList.remove('tatkal-pulsing'), 3500);
    }

    try {
        const res = await fetch(`/api/ai/rush?trainNumber=${encodeURIComponent(trainNumber)}&count=${botCount}`, {
            method: 'POST'
        });
        const json = await res.json();
        if (json.success) {
            pollAiFeedOnce();
            // Rapid polling for 3 seconds to animate dynamic seat drops
            let rapidTicks = 0;
            const rapidPoll = setInterval(() => {
                pollAiFeedOnce();
                refreshActiveTrainAvailability();
                refresh3dCoachIfOpen(trainNumber);
                rapidTicks++;
                if (rapidTicks >= 6) clearInterval(rapidPoll);
            }, 500);
        }
    } catch (err) {
        showToast('Error triggering Tatkal rush: ' + err.message, 'error');
    }
}

async function setSwarmMode(mode) {
    try {
        const res = await fetch(`/api/ai/toggle?mode=${encodeURIComponent(mode)}`, { method: 'POST' });
        const json = await res.json();
        if (json.success) {
            showToast(`AI Passenger Swarm mode set to ${mode}`, 'info');
            const statusBadge = document.getElementById('swarm-header-status');
            if (statusBadge) {
                statusBadge.textContent = mode === 'OFF' ? 'AI Swarm Paused' : `AI Swarm Active (${mode})`;
            }
        }
    } catch (err) {
        console.error('Error toggling swarm mode:', err);
    }
}

function startAiFeedPolling() {
    if (aiPollingInterval) clearInterval(aiPollingInterval);
    pollAiFeedOnce();
    aiPollingInterval = setInterval(pollAiFeedOnce, 1800);
}

async function pollAiFeedOnce() {
    try {
        const res = await fetch('/api/ai/feed?limit=25');
        const json = await res.json();
        if (json.success && json.data) {
            renderAiFeed(json.data);
            updateLiveSeatCounters(json.data);
        }
    } catch (err) {
        // Silent catch for background polling
    }
}

function renderAiFeed(events) {
    const feedBox = document.getElementById('ai-live-feed');
    if (!feedBox) return;

    if (!events || events.length === 0) {
        feedBox.innerHTML = '<div style="color:#78909c; text-align:center; padding:20px;">Waiting for AI passenger activity...</div>';
        return;
    }

    feedBox.innerHTML = '';
    events.forEach(e => {
        const item = document.createElement('div');
        let typeClass = 'confirmed';
        if (e.eventType === 'WAITING_LIST') typeClass = 'waiting';
        else if (e.eventType === 'CANCELLED') typeClass = 'cancelled';
        else if (e.eventType === 'TATKAL_RUSH_START') typeClass = 'rush';

        const isTatkal = e.isTatkal || e.quota === 'TATKAL' || e.eventType === 'TATKAL_RUSH_START';
        const tatkalBadge = isTatkal 
            ? '<span style="background:#e65100; color:#fff; font-size:9.5px; padding:1px 6px; border-radius:3px; font-weight:800; margin-right:4px;">⚡ TATKAL</span>' 
            : '';

        item.className = `feed-item ${typeClass}`;
        item.innerHTML = `
            <span class="feed-time">[${e.timestamp}]</span>
            ${tatkalBadge}
            <span class="feed-bot">🤖 ${e.botName}</span>
            <span style="color:#90caf9;">(${e.trainNumber})</span>:
            <span>${e.detail}</span>
        `;
        feedBox.appendChild(item);
    });
}

function updateLiveSeatCounters(events) {
    // If recent bookings exist, refresh the active search view
    const latestEvent = events && events.length > 0 ? events[0] : null;
    if (latestEvent && latestEvent.id !== aiFeedLastId) {
        aiFeedLastId = latestEvent.id;
        refreshActiveTrainAvailability();
    }
}

async function refreshActiveTrainAvailability() {
    try {
        const res = await fetch('/api/trains');
        const json = await res.json();
        if (json.success && json.data) {
            allDemoTrains = json.data;
            const quotaSelect = document.getElementById('quota-select');
            const isTatkalSearch = quotaSelect && (quotaSelect.value === 'TATKAL' || quotaSelect.value === 'PREMIUM_TATKAL');

            allDemoTrains.forEach(t => {
                const card = document.getElementById(`train-card-${t.number}`);
                if (card) {
                    const isAvail = t.availableSeats > 0;
                    const text = isAvail 
                        ? `AVAILABLE-${String(t.availableSeats).padStart(4, '0')}` 
                        : (isTatkalSearch ? `TQWL (Tatkal Waiting List)` : `WL (Waiting List)`);
                    const pillClass = isAvail ? 'available' : 'waiting';

                    ['general', 'sleeper', 'ac'].forEach(cls => {
                        const pill = document.getElementById(`avail-${t.number}-${cls}`);
                        if (pill) {
                            pill.textContent = text;
                            pill.className = `avail-status-pill ${pillClass}`;
                        }
                    });
                }
            });
        }
    } catch (err) {
        console.error('Error refreshing train seats:', err);
    }
}

// -------------------------------------------------------------
// BOOKING MODAL & SUBMISSION
// -------------------------------------------------------------
function handleModalQuotaChange() {
    const quotaSelect = document.getElementById('book-quota');
    const quota = quotaSelect ? quotaSelect.value : 'GENERAL';
    const tatkalBanner = document.getElementById('tatkal-modal-banner');
    const seniorBanner = document.getElementById('senior-discount-banner');
    const ageInput = document.getElementById('book-passenger-age');
    const age = ageInput ? parseInt(ageInput.value, 10) : 0;

    if (quota === 'TATKAL') {
        if (tatkalBanner) tatkalBanner.style.display = 'block';
        if (seniorBanner) seniorBanner.style.display = 'none';
    } else {
        if (tatkalBanner) tatkalBanner.style.display = 'none';
        if (seniorBanner) seniorBanner.style.display = (age >= 60) ? 'block' : 'none';
    }
}

function initBookingForm() {
    const form = document.getElementById('booking-form');
    if (!form) return;

    const ageInput = document.getElementById('book-passenger-age');
    if (ageInput) {
        ageInput.addEventListener('input', () => {
            handleModalQuotaChange();
        });
    }

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        const trainNumber = document.getElementById('book-train-number').value;
        const passengerName = document.getElementById('book-passenger-name').value.trim();
        const passengerAge = parseInt(document.getElementById('book-passenger-age').value, 10);
        const seatCount = parseInt(document.getElementById('book-seat-count').value, 10);
        const ticketClass = document.getElementById('book-ticket-class').value;
        const quota = document.getElementById('book-quota') ? document.getElementById('book-quota').value : 'GENERAL';
        const simulateFailure = document.getElementById('book-fail-payment').checked;

        const payload = {
            trainNumber,
            passengerName,
            passengerAge,
            seatCount,
            ticketClass,
            quota,
            simulateFailure
        };

        const submitBtn = document.getElementById('btn-submit-booking');
        submitBtn.disabled = true;
        submitBtn.textContent = 'Processing Payment...';

        try {
            const res = await fetch('/api/bookings', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            const json = await res.json();

            if (json.success && json.data) {
                closeModal('booking-modal');
                showToast(json.message, 'success');
                displayErsTicket(json.data);
                refreshActiveTrainAvailability();
                pollAiFeedOnce();
            } else {
                showToast(json.message || 'Booking failed.', 'error');
            }
        } catch (err) {
            showToast('Network error during booking: ' + err.message, 'error');
        } finally {
            submitBtn.disabled = false;
            submitBtn.textContent = 'Pay & Confirm Reservation';
        }
    });
}

function openBookingModal(trainNumber, ticketClass = 'GENERAL') {
    const modal = document.getElementById('booking-modal');
    if (!modal) return;

    activeTrainNumber = trainNumber;
    activeTicketClass = ticketClass;

    const train = allDemoTrains.find(t => t.number.toUpperCase() === trainNumber.toUpperCase());
    const titleEl = document.getElementById('modal-train-info');
    if (titleEl && train) {
        titleEl.textContent = `${train.number} - ${train.name} (${train.source} ➔ ${train.destination})`;
    }

    document.getElementById('book-train-number').value = trainNumber;
    document.getElementById('book-ticket-class').value = ticketClass;
    document.getElementById('book-passenger-name').value = '';
    document.getElementById('book-passenger-age').value = '';
    document.getElementById('book-seat-count').value = '1';
    document.getElementById('book-fail-payment').checked = false;

    // Sync search quota preference
    const searchQuota = document.getElementById('quota-select');
    const modalQuota = document.getElementById('book-quota');
    if (searchQuota && modalQuota) {
        modalQuota.value = (searchQuota.value === 'TATKAL' || searchQuota.value === 'PREMIUM_TATKAL') ? 'TATKAL' : 'GENERAL';
    }
    handleModalQuotaChange();

    modal.classList.remove('hidden');
}

function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) modal.classList.add('hidden');
}

// -------------------------------------------------------------
// OFFICIAL IRCTC ELECTRONIC RESERVATION SLIP (ERS)
// -------------------------------------------------------------
function displayErsTicket(booking) {
    const modal = document.getElementById('ers-modal');
    if (!modal) return;

    document.getElementById('ers-pnr').textContent = booking.pnr;
    document.getElementById('ers-status').textContent = booking.statusLabel || booking.status;
    document.getElementById('ers-booking-time').textContent = booking.bookingTime;
    document.getElementById('ers-train').textContent = `${booking.train.number} / ${booking.train.name}`;
    document.getElementById('ers-route').textContent = `${booking.train.source} ➔ ${booking.train.destination}`;
    document.getElementById('ers-times').textContent = `Dep: ${booking.train.departureTime} | Arr: ${booking.train.arrivalTime}`;
    document.getElementById('ers-class').textContent = booking.classDisplayName || booking.ticketClass;
    document.getElementById('ers-seats').textContent = `${booking.seatCount} Seat(s)`;
    document.getElementById('ers-fare').textContent = `₹${booking.fare.toFixed(2)}`;

    const quotaEl = document.getElementById('ers-quota');
    if (quotaEl) quotaEl.textContent = booking.quota || 'GENERAL';

    // Passenger row
    const tbody = document.getElementById('ers-passenger-tbody');
    if (tbody) {
        const isSenior = booking.passenger.isSenior;
        const isTatkal = booking.quota === 'TATKAL' || booking.isTatkal;
        const berthStatus = booking.status === 'WAITING_LIST' 
            ? (isTatkal ? `TQWL-${booking.waitingListNumber}` : `WL-${booking.waitingListNumber}`)
            : (isTatkal ? 'TATKAL CONFIRMED / COACH S1' : 'CONFIRMED / COACH S1');

        tbody.innerHTML = `
            <tr>
                <td>1</td>
                <td><strong>${booking.passenger.name}</strong></td>
                <td>${booking.passenger.age}</td>
                <td>${booking.passenger.category}</td>
                <td><span style="color:${booking.status === 'WAITING_LIST' ? '#e65100' : '#0f8a3c'}; font-weight:800;">${berthStatus}</span></td>
                <td>${isTatkal ? '<span style="color:#e65100; font-weight:700;">Tatkal Quota (No Concession)</span>' : (isSenior ? '<span style="color:#4527a0; font-weight:700;">40% Senior Concession</span>' : 'None')}</td>
            </tr>
        `;
    }

    const barcodeLbl = document.getElementById('barcode-pnr-label');
    if (barcodeLbl) barcodeLbl.textContent = `${booking.pnr}-IRCTC-AUTHENTIC`;

    playAudioTone('bookingSuccess');
    modal.classList.remove('hidden');
}

function printErsTicket() {
    window.print();
}

// -------------------------------------------------------------
// PNR STATUS TAB & CANCELLATION
// -------------------------------------------------------------
function initPnrForm() {
    const form = document.getElementById('pnr-form');
    if (!form) return;

    form.addEventListener('submit', (e) => {
        e.preventDefault();
        const pnr = document.getElementById('pnr-search-input').value.trim();
        lookupPnr(pnr);
    });
}

async function lookupPnr(pnr) {
    const resultBox = document.getElementById('pnr-result-box');
    resultBox.innerHTML = '<div style="padding:20px; text-align:center;">Looking up PNR from IRCTC Database...</div>';

    try {
        const res = await fetch(`/api/bookings/${encodeURIComponent(pnr)}`);
        const json = await res.json();

        if (json.success && json.data) {
            const b = json.data;
            const isConfirmed = b.status === 'CONFIRMED';
            const isWl = b.status === 'WAITING_LIST';

            resultBox.innerHTML = `
                <div style="background:#fff; border:1px solid #dcdfe6; border-radius:6px; padding:20px;">
                    <div style="display:flex; justify-content:space-between; align-items:center; border-bottom:1px solid #eee; padding-bottom:12px; margin-bottom:12px;">
                        <div>
                            <span style="font-size:12px; color:#888;">PNR NUMBER</span>
                            <h3 style="color:#213d77; font-family:'JetBrains Mono',monospace;">${b.pnr}</h3>
                        </div>
                        <div>
                            <span class="avail-status-pill ${isConfirmed ? 'available' : (isWl ? 'waiting' : '')}" style="font-size:13px;">
                                ${b.statusLabel || b.status}
                            </span>
                        </div>
                    </div>

                    <div style="display:grid; grid-template-columns:1fr 1fr; gap:12px; font-size:13px; margin-bottom:16px;">
                        <div><strong>Passenger:</strong> ${b.passenger.name} (${b.passenger.age} yrs, ${b.passenger.category})</div>
                        <div><strong>Train:</strong> ${b.train.number} - ${b.train.name}</div>
                        <div><strong>Route:</strong> ${b.train.source} ➔ ${b.train.destination}</div>
                        <div><strong>Class / Seats:</strong> ${b.classDisplayName} (${b.seatCount} seat(s))</div>
                        <div><strong>Total Fare:</strong> ₹${b.fare.toFixed(2)}</div>
                        <div><strong>Booked At:</strong> ${b.bookingTime}</div>
                    </div>

                    <div style="display:flex; gap:10px;">
                        <button class="btn-search-irctc" style="padding:8px 16px; font-size:12px;" onclick="displayErsTicket(allBookingsCache.find(x => x.pnr === '${b.pnr}') || ${JSON.stringify(b).replace(/"/g, '&quot;')})">View E-Ticket Slip</button>
                        ${b.status !== 'CANCELLED' ? `<button class="btn-card-rush" style="padding:8px 16px;" onclick="cancelBookingAction('${b.pnr}')">Cancel This Ticket</button>` : '<span style="color:#d32f2f; font-weight:700; align-self:center;">This ticket is CANCELLED</span>'}
                    </div>
                </div>
            `;
        } else {
            resultBox.innerHTML = `<div style="background:#ffebee; color:#c62828; padding:16px; border-radius:4px;">${json.message || 'No record found for this PNR.'}</div>`;
        }
    } catch (err) {
        resultBox.innerHTML = `<div style="background:#ffebee; color:#c62828; padding:16px; border-radius:4px;">Error querying PNR: ${err.message}</div>`;
    }
}

async function cancelBookingAction(pnr) {
    if (!confirm(`Are you sure you want to cancel ticket PNR: ${pnr}? If confirmed seats are released, Waiting List passengers will be automatically promoted.`)) {
        return;
    }

    try {
        const res = await fetch(`/api/bookings/${encodeURIComponent(pnr)}/cancel`, { method: 'POST' });
        const json = await res.json();
        if (json.success) {
            showToast('✅ Ticket cancelled successfully! Waiting List auto-promotion evaluated.', 'success');
            lookupPnr(pnr);
            refreshActiveTrainAvailability();
            pollAiFeedOnce();
        } else {
            showToast('Cancellation failed: ' + json.message, 'error');
        }
    } catch (err) {
        showToast('Error cancelling ticket: ' + err.message, 'error');
    }
}

// -------------------------------------------------------------
// WAITING LIST (WL) TAB
// -------------------------------------------------------------
async function loadWaitingListTab() {
    const container = document.getElementById('wl-content-area');
    if (!container) return;

    container.innerHTML = '<div style="padding:20px; text-align:center;">Loading active Waiting List queues...</div>';

    try {
        const res = await fetch('/api/trains');
        const json = await res.json();
        if (json.success && json.data) {
            const trains = json.data;
            let html = '<div style="display:flex; flex-direction:column; gap:16px;">';

            for (const t of trains) {
                const wlRes = await fetch(`/api/trains/${t.number}/waiting-list`);
                const wlJson = await wlRes.json();
                const wlList = wlJson.data || [];

                html += `
                    <div class="card">
                        <div class="card-header-irctc">
                            <h2>${t.number} - ${t.name} (${t.source} ➔ ${t.destination})</h2>
                            <span style="font-size:12px; font-weight:700;">Waiting List: ${wlList.length} passenger(s)</span>
                        </div>
                        <div class="card-body">
                            ${wlList.length === 0 ? '<p style="color:#78909c; font-size:13px;">No passengers currently in Waiting List queue for this train.</p>' : `
                                <table class="ers-table">
                                    <thead>
                                        <tr>
                                            <th>Queue Rank</th>
                                            <th>PNR</th>
                                            <th>Passenger Name</th>
                                            <th>Age</th>
                                            <th>Class</th>
                                            <th>Seats</th>
                                            <th>Waitlisted At</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        ${wlList.map(b => `
                                            <tr>
                                                <td><span style="color:#e65100; font-weight:800;">WL-${b.waitingListNumber}</span></td>
                                                <td><code>${b.pnr}</code></td>
                                                <td><strong>${b.passenger.name}</strong></td>
                                                <td>${b.passenger.age}</td>
                                                <td>${b.classDisplayName}</td>
                                                <td>${b.seatCount}</td>
                                                <td>${b.bookingTime}</td>
                                            </tr>
                                        `).join('')}
                                    </tbody>
                                </table>
                            `}
                        </div>
                    </div>
                `;
            }

            html += '</div>';
            container.innerHTML = html;
        }
    } catch (err) {
        container.innerHTML = `<div style="color:#d32f2f;">Failed to load Waiting List: ${err.message}</div>`;
    }
}

// -------------------------------------------------------------
// AVAILABILITY GRID TAB
// -------------------------------------------------------------
async function loadAvailabilityGrid() {
    const container = document.getElementById('availability-grid-container');
    if (!container) return;

    container.innerHTML = '<div style="padding:20px; text-align:center;">Fetching live availability across Indian Railways grid...</div>';

    try {
        const res = await fetch('/api/trains');
        const json = await res.json();
        if (json.success && json.data) {
            allDemoTrains = json.data;
            let html = `
                <table class="ers-table" style="background:#fff;">
                    <thead>
                        <tr>
                            <th>Train No & Name</th>
                            <th>Origin ➔ Destination</th>
                            <th>Timings</th>
                            <th>Total Berths</th>
                            <th>Available Berths</th>
                            <th>Status</th>
                            <th>Action</th>
                        </tr>
                    </thead>
                    <tbody>
            `;

            allDemoTrains.forEach(t => {
                const isAvail = t.availableSeats > 0;
                html += `
                    <tr>
                        <td><strong>${t.number}</strong><br><span style="color:#666; font-size:12px;">${t.name}</span></td>
                        <td>${t.source} ➔ ${t.destination}</td>
                        <td>${t.departureTime} - ${t.arrivalTime}</td>
                        <td>${t.totalSeats}</td>
                        <td><strong style="color:${isAvail ? '#0f8a3c' : '#d32f2f'};">${t.availableSeats}</strong></td>
                        <td>
                            <span class="avail-status-pill ${isAvail ? 'available' : 'waiting'}">
                                ${isAvail ? `AVAILABLE (${t.availableSeats})` : 'FULL (WL ACTIVE)'}
                            </span>
                        </td>
                        <td>
                            <button class="btn-book-class" style="padding:4px 10px; font-size:11px;" onclick="switchTab('search-tab'); openBookingModal('${t.number}', 'GENERAL');">Book</button>
                            <button class="btn-card-rush" style="padding:4px 8px; font-size:11px;" onclick="triggerTatkalRushOnTrain('${t.number}')">⚡ Rush</button>
                        </td>
                    </tr>
                `;
            });

            html += '</tbody></table>';
            container.innerHTML = html;
        }
    } catch (err) {
        container.innerHTML = `<div style="color:#d32f2f;">Failed to load availability: ${err.message}</div>`;
    }
}

// -------------------------------------------------------------
// BOOKING HISTORY TAB
// -------------------------------------------------------------
async function loadBookingHistory() {
    const container = document.getElementById('history-content-area');
    if (!container) return;

    container.innerHTML = '<div style="padding:20px; text-align:center;">Retrieving booking audit history...</div>';

    try {
        const res = await fetch('/api/bookings');
        const json = await res.json();
        if (json.success && json.data) {
            allBookingsCache = json.data;
            if (allBookingsCache.length === 0) {
                container.innerHTML = '<div style="background:#fff; padding:24px; text-align:center; color:#666; border-radius:6px;">No reservations in history yet. Search trains and make your first booking!</div>';
                return;
            }

            let html = `
                <table class="ers-table" style="background:#fff;">
                    <thead>
                        <tr>
                            <th>PNR</th>
                            <th>Status</th>
                            <th>Passenger</th>
                            <th>Train</th>
                            <th>Route</th>
                            <th>Class / Seats</th>
                            <th>Fare</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
            `;

            allBookingsCache.forEach(b => {
                const isConfirmed = b.status === 'CONFIRMED';
                const isWl = b.status === 'WAITING_LIST';
                const statusClass = isConfirmed ? 'available' : (isWl ? 'waiting' : '');

                html += `
                    <tr>
                        <td><code>${b.pnr}</code></td>
                        <td><span class="avail-status-pill ${statusClass}">${b.statusLabel || b.status}</span></td>
                        <td>${b.passenger.name} (${b.passenger.age}y${b.passenger.isSenior ? ' 👵' : ''})</td>
                        <td>${b.train.number} - ${b.train.name}</td>
                        <td>${b.train.source} ➔ ${b.train.destination}</td>
                        <td>${b.classDisplayName} (${b.seatCount})</td>
                        <td>₹${b.fare.toFixed(2)}</td>
                        <td>
                            <button class="btn-book-class" style="padding:4px 8px; font-size:11px; background:#213d77;" onclick='displayErsTicket(${JSON.stringify(b)})'>View E-Ticket</button>
                            ${b.status !== 'CANCELLED' ? `<button class="btn-card-rush" style="padding:4px 8px; font-size:11px;" onclick="cancelBookingAction('${b.pnr}')">Cancel</button>` : ''}
                        </td>
                    </tr>
                `;
            });

            html += '</tbody></table>';
            container.innerHTML = html;
        }
    } catch (err) {
        container.innerHTML = `<div style="color:#d32f2f;">Failed to load history: ${err.message}</div>`;
    }
}

// -------------------------------------------------------------
// TOAST NOTIFICATIONS
// -------------------------------------------------------------
function showToast(message, type = 'info') {
    let toast = document.getElementById('irctc-toast');
    if (!toast) {
        toast = document.createElement('div');
        toast.id = 'irctc-toast';
        toast.className = 'irctc-toast';
        document.body.appendChild(toast);
    }

    toast.textContent = message;
    if (type === 'error') {
        toast.style.background = '#d32f2f';
    } else if (type === 'success') {
        toast.style.background = '#1b5e20';
    } else {
        toast.style.background = '#213d77';
    }

    toast.style.display = 'block';
    setTimeout(() => {
        toast.style.display = 'none';
    }, 4000);
}

// =========================================================================
// 1. NATIVE WEB AUDIO API SYNTHESIZER
// =========================================================================
let audioCtx = null;
let isAudioMuted = false;

function initWebAudio() {
    isAudioMuted = localStorage.getItem('irctc_audio_muted') === 'true';
    updateAudioBtn();
}

function getAudioContext() {
    if (!audioCtx) {
        const AudioContextClass = window.AudioContext || window.webkitAudioContext;
        if (AudioContextClass) {
            audioCtx = new AudioContextClass();
        }
    }
    if (audioCtx && audioCtx.state === 'suspended') {
        audioCtx.resume();
    }
    return audioCtx;
}

function toggleAudio() {
    isAudioMuted = !isAudioMuted;
    localStorage.setItem('irctc_audio_muted', isAudioMuted);
    updateAudioBtn();
    if (!isAudioMuted) {
        playAudioTone('click');
        showToast('🔊 Web Audio Sound Effects Enabled', 'info');
    } else {
        showToast('🔇 Web Audio Muted', 'info');
    }
}

function updateAudioBtn() {
    const btn = document.getElementById('audio-toggle-btn');
    if (btn) {
        if (isAudioMuted) {
            btn.textContent = '🔇 Audio OFF';
            btn.classList.add('muted');
        } else {
            btn.textContent = '🔊 Audio ON';
            btn.classList.remove('muted');
        }
    }
}

function playAudioTone(type) {
    if (isAudioMuted) return;
    try {
        const ctx = getAudioContext();
        if (!ctx) return;

        const now = ctx.currentTime;
        if (type === 'whistle') {
            // Authentic dual-tone train horn chords (311Hz D#4 & 370Hz F#4)
            [311.13, 369.99].forEach(freq => {
                const osc = ctx.createOscillator();
                const gain = ctx.createGain();
                osc.type = 'triangle';
                osc.frequency.setValueAtTime(freq, now);
                gain.gain.setValueAtTime(0, now);
                gain.gain.linearRampToValueAtTime(0.12, now + 0.1);
                gain.gain.exponentialRampToValueAtTime(0.001, now + 1.2);
                osc.connect(gain);
                gain.connect(ctx.destination);
                osc.start(now);
                osc.stop(now + 1.2);
            });
        } else if (type === 'tatkalRush') {
            // Charging sci-fi sweep
            const osc = ctx.createOscillator();
            const gain = ctx.createGain();
            osc.type = 'sawtooth';
            osc.frequency.setValueAtTime(140, now);
            osc.frequency.exponentialRampToValueAtTime(880, now + 0.6);
            gain.gain.setValueAtTime(0.1, now);
            gain.gain.exponentialRampToValueAtTime(0.001, now + 0.8);
            osc.connect(gain);
            gain.connect(ctx.destination);
            osc.start(now);
            osc.stop(now + 0.8);
        } else if (type === 'bookingSuccess') {
            // Triumphant 3-note arpeggio C5 (523Hz), E5 (659Hz), G5 (784Hz)
            [523.25, 659.25, 783.99].forEach((freq, i) => {
                const osc = ctx.createOscillator();
                const gain = ctx.createGain();
                osc.type = 'sine';
                osc.frequency.setValueAtTime(freq, now + i * 0.12);
                gain.gain.setValueAtTime(0, now + i * 0.12);
                gain.gain.linearRampToValueAtTime(0.15, now + i * 0.12 + 0.02);
                gain.gain.exponentialRampToValueAtTime(0.001, now + i * 0.12 + 0.5);
                osc.connect(gain);
                gain.connect(ctx.destination);
                osc.start(now + i * 0.12);
                osc.stop(now + i * 0.12 + 0.5);
            });
        } else if (type === 'error') {
            const osc = ctx.createOscillator();
            const gain = ctx.createGain();
            osc.type = 'square';
            osc.frequency.setValueAtTime(160, now);
            gain.gain.setValueAtTime(0.1, now);
            gain.gain.exponentialRampToValueAtTime(0.001, now + 0.35);
            osc.connect(gain);
            gain.connect(ctx.destination);
            osc.start(now);
            osc.stop(now + 0.35);
        } else if (type === 'click') {
            const osc = ctx.createOscillator();
            const gain = ctx.createGain();
            osc.type = 'sine';
            osc.frequency.setValueAtTime(900, now);
            gain.gain.setValueAtTime(0.06, now);
            gain.gain.exponentialRampToValueAtTime(0.001, now + 0.04);
            osc.connect(gain);
            gain.connect(ctx.destination);
            osc.start(now);
            osc.stop(now + 0.04);
        } else if (type === 'flip') {
            const osc = ctx.createOscillator();
            const gain = ctx.createGain();
            osc.type = 'sine';
            osc.frequency.setValueAtTime(320, now);
            osc.frequency.linearRampToValueAtTime(480, now + 0.15);
            gain.gain.setValueAtTime(0.08, now);
            gain.gain.exponentialRampToValueAtTime(0.001, now + 0.2);
            osc.connect(gain);
            gain.connect(ctx.destination);
            osc.start(now);
            osc.stop(now + 0.2);
        }
    } catch (e) {
        console.warn('Web Audio warning:', e);
    }
}

// =========================================================================
// 2. 3D RAILWAY DIORAMA ENGINE (THREE.JS WITH PROCEDURAL CANVAS FALLBACK)
// =========================================================================
let threeScene, threeCamera, threeRenderer, threeTrainGroup;
let threeSleepers = [];
let threeMasts = [];
let dioramaSpeed = 0.8;
let targetDioramaSpeed = 0.8;
let dioramaLightingMode = 'day';
let dioramaSunLight, dioramaAmbLight, dioramaHeadlight;
let camRadius = 34;
let camTheta = 0.85; // azimuth
let camPhi = 0.45;   // elevation
let isDioramaDragging = false;
let dioramaPointerX = 0, dioramaPointerY = 0;

function init3dRailwayDiorama() {
    const container = document.getElementById('rail-3d-viewport');
    const canvas = document.getElementById('railway-3d-canvas');
    if (!container || !canvas) return;

    if (window.THREE) {
        initThreeJsRailway(container, canvas);
    } else {
        initCanvasRailwayFallback(canvas);
    }
}

function initThreeJsRailway(container, canvas) {
    const width = container.clientWidth || 800;
    const height = container.clientHeight || 250;

    threeScene = new THREE.Scene();
    threeScene.background = new THREE.Color(0x132240);

    threeCamera = new THREE.PerspectiveCamera(42, width / height, 0.1, 1000);
    updateCameraOrbit();

    threeRenderer = new THREE.WebGLRenderer({ canvas, antialias: true, alpha: false });
    threeRenderer.setSize(width, height);
    threeRenderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));

    // Lighting
    dioramaAmbLight = new THREE.AmbientLight(0xdce7f9, 0.8);
    threeScene.add(dioramaAmbLight);

    dioramaSunLight = new THREE.DirectionalLight(0xffffff, 1.2);
    dioramaSunLight.position.set(30, 45, 20);
    threeScene.add(dioramaSunLight);

    dioramaHeadlight = new THREE.SpotLight(0xfff2a8, 2.5, 60, Math.PI / 5, 0.4);
    dioramaHeadlight.position.set(0, 4, 12);
    dioramaHeadlight.target.position.set(0, 0, 45);
    threeScene.add(dioramaHeadlight);
    threeScene.add(dioramaHeadlight.target);

    // Ground Ballast (Gravel)
    const groundGeo = new THREE.PlaneGeometry(160, 200);
    const groundMat = new THREE.MeshStandardMaterial({ color: 0x1a2639, roughness: 0.9, metalness: 0.1 });
    const ground = new THREE.Mesh(groundGeo, groundMat);
    ground.rotation.x = -Math.PI / 2;
    ground.position.y = -0.6;
    threeScene.add(ground);

    // Parallel Metallic Rails
    const railMat = new THREE.MeshStandardMaterial({ color: 0xd0d8e2, metalness: 0.9, roughness: 0.2 });
    [-2.2, 2.2].forEach(x => {
        const railGeo = new THREE.BoxGeometry(0.35, 0.45, 180);
        const rail = new THREE.Mesh(railGeo, railMat);
        rail.position.set(x, 0, 0);
        threeScene.add(rail);
    });

    // Railway Sleepers (ties) spaced along Z
    threeSleepers = [];
    const sleeperGeo = new THREE.BoxGeometry(6.2, 0.35, 1.2);
    const sleeperMat = new THREE.MeshStandardMaterial({ color: 0x3d4b60, roughness: 0.8 });
    for (let z = -80; z <= 80; z += 3.8) {
        const sleeper = new THREE.Mesh(sleeperGeo, sleeperMat);
        sleeper.position.set(0, -0.2, z);
        threeScene.add(sleeper);
        threeSleepers.push(sleeper);
    }

    // OHE Overhead Catenary Posts
    threeMasts = [];
    const mastMat = new THREE.MeshStandardMaterial({ color: 0x78909c, metalness: 0.5 });
    for (let z = -75; z <= 75; z += 25) {
        const mastGroup = new THREE.Group();
        // vertical mast
        const pole = new THREE.Mesh(new THREE.CylinderGeometry(0.2, 0.25, 12), mastMat);
        pole.position.set(-5.5, 6, 0);
        mastGroup.add(pole);
        // cantilever arm
        const arm = new THREE.Mesh(new THREE.BoxGeometry(6, 0.2, 0.2), mastMat);
        arm.position.set(-2.5, 11, 0);
        mastGroup.add(arm);

        mastGroup.position.z = z;
        threeScene.add(mastGroup);
        threeMasts.push(mastGroup);
    }

    // Overhead Wire
    const wireMat = new THREE.MeshBasicMaterial({ color: 0xb0bec5 });
    const wire = new THREE.Mesh(new THREE.CylinderGeometry(0.04, 0.04, 180), wireMat);
    wire.rotation.x = Math.PI / 2;
    wire.position.set(0, 10.8, 0);
    threeScene.add(wire);

    // Build Vande Bharat Locomotive Train
    threeTrainGroup = new THREE.Group();

    // =========================================================================
    // BUILD AUTHENTIC INDIAN RAILWAYS WAP-7 LOCOMOTIVE + RAJDHANI LHB COACHES
    // =========================================================================
    threeTrainGroup = new THREE.Group();

    // Palette & Materials
    const wap7BodyMat = new THREE.MeshStandardMaterial({ color: 0xedeef0, roughness: 0.35, metalness: 0.2 }); // Off-white WAP-7 body
    const irRedStripeMat = new THREE.MeshStandardMaterial({ color: 0xd32f2f, roughness: 0.35, metalness: 0.2 }); // Indian Railways Signal Red
    const irDarkChassisMat = new THREE.MeshStandardMaterial({ color: 0x212121, roughness: 0.8, metalness: 0.5 }); // Dark chassis & bogies
    const bufferMat = new THREE.MeshStandardMaterial({ color: 0x1f2937, metalness: 0.85, roughness: 0.25 }); // Steel buffers
    const bufferBeamMat = new THREE.MeshStandardMaterial({ color: 0xb71c1c, roughness: 0.4, metalness: 0.3 }); // Signal Red buffer beam
    const cowcatcherMat = new THREE.MeshStandardMaterial({ color: 0x1a1a1a, roughness: 0.7 });
    const hazardYellowMat = new THREE.MeshStandardMaterial({ color: 0xfbc02d, roughness: 0.4 }); // High-visibility hazard yellow
    const glassMat = new THREE.MeshStandardMaterial({ color: 0x0f172a, metalness: 0.95, roughness: 0.05 }); // Dark tinted cab glass
    const louverMat = new THREE.MeshStandardMaterial({ color: 0x455a64, metalness: 0.6, roughness: 0.5 }); // Engine vent grilles
    const pantoRedMat = new THREE.MeshStandardMaterial({ color: 0xb71c1c, metalness: 0.6, roughness: 0.3 });
    const insulatorMat = new THREE.MeshStandardMaterial({ color: 0x8d6e63, roughness: 0.4 }); // Ceramic brown/terracotta
    const headlightMat = new THREE.MeshStandardMaterial({ color: 0xfff9c4, emissive: 0xffeb3b, emissiveIntensity: 1.4 });
    const ditchLightMat = new THREE.MeshStandardMaterial({ color: 0xffffff, emissive: 0xfff59d, emissiveIntensity: 1.1 });

    // -------------------------------------------------------------
    // 1. WAP-7 LOCOMOTIVE BODY & CABIN
    // -------------------------------------------------------------
    const locoGroup = new THREE.Group();

    // Main Boxy Locomotive Body
    const locoMain = new THREE.Mesh(new THREE.BoxGeometry(4.3, 3.8, 18), wap7BodyMat);
    locoMain.position.set(0, 2.9, 0);
    locoGroup.add(locoMain);

    // Aerodynamic Sloped Cab Brow (Front)
    const cabFront = new THREE.Mesh(new THREE.BoxGeometry(4.3, 3.6, 3.5), wap7BodyMat);
    cabFront.position.set(0, 2.8, 9.6);
    cabFront.rotation.x = -0.06;
    locoGroup.add(cabFront);

    // Rounded Aerodynamic Cab Nose Wedge
    const cabNose = new THREE.Mesh(new THREE.CylinderGeometry(2.15, 2.15, 4.3, 16, 1, false, 0, Math.PI), wap7BodyMat);
    cabNose.rotation.z = Math.PI / 2;
    cabNose.rotation.y = Math.PI / 2;
    cabNose.position.set(0, 2.3, 11.2);
    locoGroup.add(cabNose);

    // Rounded Roof Arch
    const roofCurve = new THREE.Mesh(new THREE.CylinderGeometry(2.15, 2.15, 20.8, 20, 1, false, 0, Math.PI), irDarkChassisMat);
    roofCurve.rotation.x = Math.PI / 2;
    roofCurve.position.set(0, 4.8, 0.4);
    locoGroup.add(roofCurve);

    // -------------------------------------------------------------
    // 2. ICONIC INDIAN RAILWAYS LIVERY & CHEVRON
    // -------------------------------------------------------------
    // Waist Red Livery Bands along engine flanks
    [-2.16, 2.16].forEach(x => {
        const stripe = new THREE.Mesh(new THREE.BoxGeometry(0.04, 0.7, 21), irRedStripeMat);
        stripe.position.set(x, 2.2, 0.5);
        locoGroup.add(stripe);
    });

    // Front Red Chevron / V-Stripe on Nose (Iconic Indian Railways WAP-7 livery)
    const chevronLeft = new THREE.Mesh(new THREE.BoxGeometry(1.6, 0.4, 0.1), irRedStripeMat);
    chevronLeft.position.set(-0.8, 2.2, 11.55);
    chevronLeft.rotation.z = 0.35;
    locoGroup.add(chevronLeft);

    const chevronRight = new THREE.Mesh(new THREE.BoxGeometry(1.6, 0.4, 0.1), irRedStripeMat);
    chevronRight.position.set(0.8, 2.2, 11.55);
    chevronRight.rotation.z = -0.35;
    locoGroup.add(chevronRight);

    // Center Locomotive Number Plate: "WAP-7 #30201"
    const numPlate = new THREE.Mesh(new THREE.BoxGeometry(1.4, 0.5, 0.12), irRedStripeMat);
    numPlate.position.set(0, 1.8, 11.6);
    locoGroup.add(numPlate);

    // Side Ventilation Louver Grilles
    [-2.16, 2.16].forEach(x => {
        [-4, 0, 4].forEach(z => {
            const louver = new THREE.Mesh(new THREE.BoxGeometry(0.05, 1.4, 2.6), louverMat);
            louver.position.set(x, 3.4, z);
            locoGroup.add(louver);
        });
    });

    // -------------------------------------------------------------
    // 3. CABIN WINDSHIELD & LIGHTING
    // -------------------------------------------------------------
    // Twin Split Dark Windshields
    [-1.05, 1.05].forEach(x => {
        const win = new THREE.Mesh(new THREE.BoxGeometry(1.5, 1.15, 0.2), glassMat);
        win.position.set(x, 3.75, 10.95);
        win.rotation.x = -0.22;
        locoGroup.add(win);
    });

    // Top Cyclops Twin-Beam Headlight (Roof Peak Center)
    const cyclopsHousing = new THREE.Mesh(new THREE.CylinderGeometry(0.5, 0.55, 0.6, 16), irDarkChassisMat);
    cyclopsHousing.rotation.x = Math.PI / 2;
    cyclopsHousing.position.set(0, 4.85, 10.6);
    locoGroup.add(cyclopsHousing);

    const cyclopsLens1 = new THREE.Mesh(new THREE.CylinderGeometry(0.18, 0.18, 0.1), headlightMat);
    cyclopsLens1.rotation.x = Math.PI / 2;
    cyclopsLens1.position.set(-0.2, 4.85, 10.92);
    locoGroup.add(cyclopsLens1);

    const cyclopsLens2 = new THREE.Mesh(new THREE.CylinderGeometry(0.18, 0.18, 0.1), headlightMat);
    cyclopsLens2.rotation.x = Math.PI / 2;
    cyclopsLens2.position.set(0.2, 4.85, 10.92);
    locoGroup.add(cyclopsLens2);

    // Twin Lower Ditch / Marker Lights (above buffer beam)
    [-1.6, 1.6].forEach(x => {
        const ditch = new THREE.Mesh(new THREE.CylinderGeometry(0.22, 0.25, 0.3), irDarkChassisMat);
        ditch.rotation.x = Math.PI / 2;
        ditch.position.set(x, 1.65, 11.45);
        locoGroup.add(ditch);

        const ditchLens = new THREE.Mesh(new THREE.CylinderGeometry(0.16, 0.16, 0.1), ditchLightMat);
        ditchLens.rotation.x = Math.PI / 2;
        ditchLens.position.set(x, 1.65, 11.6);
        locoGroup.add(ditchLens);
    });

    // -------------------------------------------------------------
    // 4. FRONT RED BUFFER BEAM, ROUND BUFFERS & COW CATCHER
    // -------------------------------------------------------------
    // Front Signal Red Buffer Beam
    const bufferBeam = new THREE.Mesh(new THREE.BoxGeometry(4.4, 0.8, 0.6), bufferBeamMat);
    bufferBeam.position.set(0, 1.0, 11.4);
    locoGroup.add(bufferBeam);

    // Left and Right Circular Buffers
    [-1.5, 1.5].forEach(x => {
        const shaft = new THREE.Mesh(new THREE.CylinderGeometry(0.15, 0.15, 0.7), bufferMat);
        shaft.rotation.x = Math.PI / 2;
        shaft.position.set(x, 1.0, 11.8);
        locoGroup.add(shaft);

        const plate = new THREE.Mesh(new THREE.CylinderGeometry(0.42, 0.42, 0.12, 16), bufferMat);
        plate.rotation.x = Math.PI / 2;
        plate.position.set(x, 1.0, 12.18);
        locoGroup.add(plate);
    });

    // Center Knuckle / Screw Coupler Hook
    const coupler = new THREE.Mesh(new THREE.BoxGeometry(0.5, 0.4, 0.9), bufferMat);
    coupler.position.set(0, 0.95, 11.9);
    locoGroup.add(coupler);

    // Indian Cattle Guard (Cowcatcher / Pilot) with Hazard Stripes
    const cowcatcherBase = new THREE.Mesh(new THREE.BoxGeometry(4.2, 0.6, 0.8), cowcatcherMat);
    cowcatcherBase.position.set(0, 0.35, 11.1);
    cowcatcherBase.rotation.x = -0.35;
    locoGroup.add(cowcatcherBase);

    // Alternating Yellow and Black warning slats on the cowcatcher
    for (let bx = -1.8; bx <= 1.8; bx += 0.45) {
        const isYellow = Math.round((bx + 1.8) / 0.45) % 2 === 0;
        const slat = new THREE.Mesh(new THREE.BoxGeometry(0.2, 0.65, 0.1), isYellow ? hazardYellowMat : cowcatcherMat);
        slat.position.set(bx, 0.35, 11.45);
        slat.rotation.x = -0.35;
        locoGroup.add(slat);
    }

    // Heavy-Duty Bogie Undercarriage & Wheels
    [-5.5, 5.5].forEach(bz => {
        const bogieFrame = new THREE.Mesh(new THREE.BoxGeometry(4.0, 0.6, 6.5), irDarkChassisMat);
        bogieFrame.position.set(0, 0.5, bz);
        locoGroup.add(bogieFrame);

        [-2.2, 0, 2.2].forEach(wz => {
            [-2.1, 2.1].forEach(wx => {
                const wheel = new THREE.Mesh(new THREE.CylinderGeometry(0.65, 0.65, 0.28, 16), bufferMat);
                wheel.rotation.z = Math.PI / 2;
                wheel.position.set(wx, 0.45, bz + wz);
                locoGroup.add(wheel);
            });
        });
    });

    // -------------------------------------------------------------
    // 5. HIGH-REACH ROOF PANTOGRAPHS (ELECTRIC TRACTION)
    // -------------------------------------------------------------
    [-5, 5].forEach(pz => {
        // Ceramic Insulators
        [-1, 1].forEach(ix => {
            [-1, 1].forEach(iz => {
                const ins = new THREE.Mesh(new THREE.CylinderGeometry(0.12, 0.15, 0.45), insulatorMat);
                ins.position.set(ix, 5.15, pz + iz * 0.9);
                locoGroup.add(ins);
            });
        });

        const pantoBase = new THREE.Mesh(new THREE.BoxGeometry(2.4, 0.2, 2.2), pantoRedMat);
        pantoBase.position.set(0, 5.35, pz);
        locoGroup.add(pantoBase);

        // Pantograph Arms
        const pantoArm1 = new THREE.Mesh(new THREE.CylinderGeometry(0.07, 0.07, 2.8), pantoRedMat);
        pantoArm1.rotation.x = (pz > 0 ? -0.55 : 0.55);
        pantoArm1.position.set(0, 6.6, pz + (pz > 0 ? -0.7 : 0.7));
        locoGroup.add(pantoArm1);

        const pantoArm2 = new THREE.Mesh(new THREE.CylinderGeometry(0.07, 0.07, 2.8), pantoRedMat);
        pantoArm2.rotation.x = (pz > 0 ? 0.55 : -0.55);
        pantoArm2.position.set(0, 8.0, pz + (pz > 0 ? 0.1 : -0.1));
        locoGroup.add(pantoArm2);

        // Contact Shoe touching OHE catenary wire
        const pantoShoe = new THREE.Mesh(new THREE.BoxGeometry(3.2, 0.12, 0.45), new THREE.MeshStandardMaterial({ color: 0xd7ccc8, metalness: 0.8 }));
        pantoShoe.position.set(0, 9.4, pz + (pz > 0 ? 0.8 : -0.8));
        locoGroup.add(pantoShoe);
    });

    threeTrainGroup.add(locoGroup);

    // -------------------------------------------------------------
    // 6. TRAILING RAJDHANI LHB COACHES (RED & SILVER LIVERY)
    // -------------------------------------------------------------
    const lhbRedMat = new THREE.MeshStandardMaterial({ color: 0x9b111e, roughness: 0.35, metalness: 0.2 }); // Deep Rajdhani Red
    const lhbCreamMat = new THREE.MeshStandardMaterial({ color: 0xe0e0e0, roughness: 0.3, metalness: 0.3 }); // Silver-grey cream
    const lhbRoofMat = new THREE.MeshStandardMaterial({ color: 0xb0bec5, roughness: 0.4, metalness: 0.6 }); // Ribbed stainless roof
    const lhbStripeMat = new THREE.MeshStandardMaterial({ color: 0xfbc02d, roughness: 0.3 }); // Gold/yellow pin-stripe
    const coachWinMat = new THREE.MeshStandardMaterial({ color: 0xffd54f, emissive: 0xffa000, emissiveIntensity: 0.65 });
    const gangwayMat = new THREE.MeshStandardMaterial({ color: 0x1a1a1a, roughness: 0.9 });

    [-24, -46].forEach(coachZ => {
        const coachGroup = new THREE.Group();

        // Coach Lower Body (Rajdhani Red)
        const coachLower = new THREE.Mesh(new THREE.BoxGeometry(4.2, 2.2, 19.5), lhbRedMat);
        coachLower.position.set(0, 1.8, coachZ);
        coachGroup.add(coachLower);

        // Coach Upper Body & Window Band
        const coachUpper = new THREE.Mesh(new THREE.BoxGeometry(4.2, 1.8, 19.5), lhbCreamMat);
        coachUpper.position.set(0, 3.6, coachZ);
        coachGroup.add(coachUpper);

        // Yellow Accent Pin-Stripe
        [-2.12, 2.12].forEach(x => {
            const pin = new THREE.Mesh(new THREE.BoxGeometry(0.04, 0.15, 19.4), lhbStripeMat);
            pin.position.set(x, 2.8, coachZ);
            coachGroup.add(pin);
        });

        // Stainless Steel Ribbed Roof
        const coachRoof = new THREE.Mesh(new THREE.CylinderGeometry(2.1, 2.1, 19.5, 20, 1, false, 0, Math.PI), lhbRoofMat);
        coachRoof.rotation.x = Math.PI / 2;
        coachRoof.position.set(0, 4.4, coachZ);
        coachGroup.add(coachRoof);

        // Panoramic LHB Passenger Windows (Glowing warm cabin lights)
        [-2.12, 2.12].forEach(x => {
            for (let wz = -7; wz <= 7; wz += 2.8) {
                const win = new THREE.Mesh(new THREE.BoxGeometry(0.08, 1.0, 1.9), coachWinMat);
                win.position.set(x, 3.4, coachZ + wz);
                coachGroup.add(win);
            }
        });

        // FIAT Bogies & Wheels under coaches
        [-6.5, 6.5].forEach(bz => {
            const bogie = new THREE.Mesh(new THREE.BoxGeometry(3.8, 0.5, 4.5), irDarkChassisMat);
            bogie.position.set(0, 0.5, coachZ + bz);
            coachGroup.add(bogie);

            [-1.5, 1.5].forEach(wz => {
                [-2.1, 2.1].forEach(wx => {
                    const wheel = new THREE.Mesh(new THREE.CylinderGeometry(0.6, 0.6, 0.25, 16), bufferMat);
                    wheel.rotation.z = Math.PI / 2;
                    wheel.position.set(wx, 0.45, coachZ + bz + wz);
                    coachGroup.add(wheel);
                });
            });
        });

        // Accordion Vestibule Gangway connecting cars
        const gangway = new THREE.Mesh(new THREE.BoxGeometry(2.8, 3.4, 2.6), gangwayMat);
        gangway.position.set(0, 2.4, coachZ + 10.9);
        coachGroup.add(gangway);

        threeTrainGroup.add(coachGroup);
    });

    threeScene.add(threeTrainGroup);

    // Event Listeners for Orbit & Resize
    setupDioramaInteraction(container);

    window.addEventListener('resize', () => {
        const nw = container.clientWidth || 800;
        const nh = container.clientHeight || 250;
        threeCamera.aspect = nw / nh;
        threeCamera.updateProjectionMatrix();
        threeRenderer.setSize(nw, nh);
    });

    animateThreeDiorama();
}

function updateCameraOrbit() {
    if (!threeCamera) return;
    const x = camRadius * Math.sin(camPhi) * Math.sin(camTheta);
    const y = camRadius * Math.cos(camPhi);
    const z = camRadius * Math.sin(camPhi) * Math.cos(camTheta);
    threeCamera.position.set(x, Math.max(y, 2), z);
    threeCamera.lookAt(0, 3, 2);
}

function setupDioramaInteraction(container) {
    container.addEventListener('mousedown', (e) => {
        isDioramaDragging = true;
        dioramaPointerX = e.clientX;
        dioramaPointerY = e.clientY;
    });

    window.addEventListener('mousemove', (e) => {
        if (!isDioramaDragging) return;
        const dx = e.clientX - dioramaPointerX;
        const dy = e.clientY - dioramaPointerY;
        dioramaPointerX = e.clientX;
        dioramaPointerY = e.clientY;

        camTheta -= dx * 0.008;
        camPhi = Math.max(0.15, Math.min(Math.PI * 0.48, camPhi - dy * 0.008));
        updateCameraOrbit();
    });

    window.addEventListener('mouseup', () => {
        isDioramaDragging = false;
    });

    // Touch support
    container.addEventListener('touchstart', (e) => {
        if (e.touches.length === 1) {
            isDioramaDragging = true;
            dioramaPointerX = e.touches[0].clientX;
            dioramaPointerY = e.touches[0].clientY;
        }
    }, { passive: true });

    window.addEventListener('touchmove', (e) => {
        if (!isDioramaDragging || e.touches.length !== 1) return;
        const dx = e.touches[0].clientX - dioramaPointerX;
        const dy = e.touches[0].clientY - dioramaPointerY;
        dioramaPointerX = e.touches[0].clientX;
        dioramaPointerY = e.touches[0].clientY;

        camTheta -= dx * 0.008;
        camPhi = Math.max(0.15, Math.min(Math.PI * 0.48, camPhi - dy * 0.008));
        updateCameraOrbit();
    }, { passive: true });

    window.addEventListener('touchend', () => {
        isDioramaDragging = false;
    });

    // Wheel zoom
    container.addEventListener('wheel', (e) => {
        e.preventDefault();
        camRadius = Math.max(18, Math.min(60, camRadius + e.deltaY * 0.04));
        updateCameraOrbit();
    }, { passive: false });
}

function animateThreeDiorama() {
    requestAnimationFrame(animateThreeDiorama);

    // Smooth speed interpolation
    dioramaSpeed += (targetDioramaSpeed - dioramaSpeed) * 0.05;

    // Scroll sleepers backward to simulate high forward train velocity
    threeSleepers.forEach(s => {
        s.position.z -= dioramaSpeed * 1.4;
        if (s.position.z < -80) {
            s.position.z += 160;
        }
    });

    // Scroll catenary masts backward
    threeMasts.forEach(m => {
        m.position.z -= dioramaSpeed * 1.4;
        if (m.position.z < -75) {
            m.position.z += 150;
        }
    });

    // Gentle realistic engine suspension sway
    if (threeTrainGroup) {
        threeTrainGroup.position.y = Math.sin(Date.now() * 0.012) * 0.04;
        threeTrainGroup.rotation.z = Math.sin(Date.now() * 0.006) * 0.005;
    }

    threeRenderer.render(threeScene, threeCamera);
}

function set3dLighting(mode) {
    dioramaLightingMode = mode;
    document.querySelectorAll('.ctrl-btn-3d').forEach(btn => {
        if (['day', 'sunset', 'night'].includes(btn.textContent.trim().toLowerCase())) {
            btn.classList.toggle('active', btn.textContent.toLowerCase().includes(mode));
        }
    });

    if (!threeScene) return;

    if (mode === 'day') {
        threeScene.background = new THREE.Color(0x1a335a);
        dioramaAmbLight.color.setHex(0xdce7f9);
        dioramaAmbLight.intensity = 0.85;
        dioramaSunLight.color.setHex(0xffffff);
        dioramaSunLight.intensity = 1.2;
        dioramaHeadlight.intensity = 1.0;
    } else if (mode === 'sunset') {
        threeScene.background = new THREE.Color(0x3e2030);
        dioramaAmbLight.color.setHex(0xffb74d);
        dioramaAmbLight.intensity = 0.65;
        dioramaSunLight.color.setHex(0xff7043);
        dioramaSunLight.intensity = 1.5;
        dioramaHeadlight.intensity = 2.0;
    } else if (mode === 'night') {
        threeScene.background = new THREE.Color(0x060b14);
        dioramaAmbLight.color.setHex(0x152238);
        dioramaAmbLight.intensity = 0.35;
        dioramaSunLight.color.setHex(0x5c6bc0);
        dioramaSunLight.intensity = 0.4;
        dioramaHeadlight.intensity = 3.5;
    }
    playAudioTone('click');
}

function reset3dCamera() {
    camRadius = 34;
    camTheta = 0.85;
    camPhi = 0.45;
    updateCameraOrbit();
    playAudioTone('click');
}

function boost3dTrainSpeed() {
    targetDioramaSpeed = 2.4;
    playAudioTone('whistle');
    const speedEl = document.getElementById('rail-3d-speed');
    if (speedEl) speedEl.textContent = '⚡ 180 km/h (HIGH SPEED SURGE)';

    setTimeout(() => {
        targetDioramaSpeed = 0.8;
        if (speedEl) speedEl.textContent = 'Cruising at 130 km/h';
    }, 4000);
}

// Fallback procedural canvas if Three.js CDN is unavailable
function initCanvasRailwayFallback(canvas) {
    const ctx = canvas.getContext('2d');
    function draw() {
        ctx.fillStyle = '#101d36';
        ctx.fillRect(0, 0, canvas.width, canvas.height);
        // Draw perspective track
        ctx.strokeStyle = '#d0d8e2';
        ctx.lineWidth = 3;
        ctx.beginPath();
        ctx.moveTo(canvas.width * 0.2, canvas.height);
        ctx.lineTo(canvas.width * 0.45, canvas.height * 0.3);
        ctx.moveTo(canvas.width * 0.8, canvas.height);
        ctx.lineTo(canvas.width * 0.55, canvas.height * 0.3);
        ctx.stroke();
        requestAnimationFrame(draw);
    }
    draw();
}

// =========================================================================
// 3. 3D COACH BERTH VISUALIZER MODAL
// =========================================================================
let activeCoachTrainNumber = 'DEMO101';
let activeCoachClass = 'AC';

function open3dCoachVisualizer(trainNumber) {
    activeCoachTrainNumber = trainNumber || activeTrainNumber || 'DEMO101';
    const modal = document.getElementById('coach-3d-modal');
    if (!modal) return;

    const train = allDemoTrains.find(t => t.number.toUpperCase() === activeCoachTrainNumber.toUpperCase());
    const trainTitle = train ? `${train.number} - ${train.name.toUpperCase()}` : activeCoachTrainNumber;
    
    document.getElementById('coach-3d-train-title').textContent = `${trainTitle} - 3D Coach Berth Visualizer`;
    renderCoachBerths();
    modal.classList.remove('hidden');
    playAudioTone('click');
}

function switchCoachClass(cClass) {
    activeCoachClass = cClass;
    document.querySelectorAll('.coach-tab-btn').forEach(btn => {
        btn.classList.toggle('active', btn.getAttribute('data-class') === cClass);
    });
    renderCoachBerths();
    playAudioTone('click');
}

function renderCoachBerths() {
    const grid = document.getElementById('compartment-3d-grid');
    if (!grid) return;

    const train = allDemoTrains.find(t => t.number.toUpperCase() === activeCoachTrainNumber.toUpperCase());
    const available = train ? train.availableSeats : 40;
    const total = train ? train.totalSeats : 50;

    const coachBerthCount = (activeCoachClass === 'AC') ? 36 : (activeCoachClass === 'SLEEPER' ? 48 : 54);
    const vacantBerths = Math.max(0, Math.round((available / total) * coachBerthCount));
    const aiBookedBerths = coachBerthCount - vacantBerths;

    document.getElementById('coach-total-berths').textContent = `${coachBerthCount} Berths`;
    document.getElementById('coach-vacant-berths').textContent = vacantBerths;
    document.getElementById('coach-ai-berths').textContent = aiBookedBerths;

    const baysCount = Math.ceil(coachBerthCount / 8);
    let html = '';
    let berthNum = 1;

    for (let b = 1; b <= baysCount; b++) {
        html += `
            <div class="coach-bay">
                <div class="bay-title">BAY ${b}</div>
                <div class="bay-berths-row">
                    <div class="berth-column">
                        ${renderBerthBox(berthNum++, 'LB', vacantBerths, coachBerthCount)}
                        ${renderBerthBox(berthNum++, 'MB', vacantBerths, coachBerthCount)}
                        ${renderBerthBox(berthNum++, 'UB', vacantBerths, coachBerthCount)}
                    </div>
                    <div class="berth-column">
                        ${renderBerthBox(berthNum++, 'LB', vacantBerths, coachBerthCount)}
                        ${renderBerthBox(berthNum++, 'MB', vacantBerths, coachBerthCount)}
                        ${renderBerthBox(berthNum++, 'UB', vacantBerths, coachBerthCount)}
                    </div>
                    <div class="berth-column" style="margin-left:6px; border-left:1px dashed #c0c4cc; padding-left:6px;">
                        ${renderBerthBox(berthNum++, 'SL', vacantBerths, coachBerthCount)}
                        ${renderBerthBox(berthNum++, 'SU', vacantBerths, coachBerthCount)}
                    </div>
                </div>
            </div>
        `;
    }

    grid.innerHTML = html;
}

function renderBerthBox(num, code, vacantCount, totalCount) {
    const isVacant = num <= vacantCount;
    let statusClass = 'avail';
    let statusLabel = 'Available';
    let pnr = 'VACANT';

    if (!isVacant) {
        if (num % 3 === 0) {
            statusClass = 'tatkal-res';
            statusLabel = '⚡ TATKAL BOOKED';
            pnr = `PNR-TK${1000 + num}`;
        } else if (num % 5 === 0) {
            statusClass = 'senior-res';
            statusLabel = '👵 SR CITIZEN (40% OFF)';
            pnr = `PNR-SR${1000 + num}`;
        } else {
            statusClass = 'ai-res';
            statusLabel = '🔴 AI BOT RESERVED';
            pnr = `PNR-AI${1000 + num}`;
        }
    }

    return `
        <div class="berth-box-3d ${statusClass}" title="Berth #${num} (${code}) &bull; ${statusLabel} &bull; ${pnr}" onclick="showToast('Berth #${num} (${code}): ${statusLabel} [${pnr}]')">
            <span>${num}</span>
            <span class="berth-lbl">${code}</span>
        </div>
    `;
}

function refresh3dCoachIfOpen(trainNumber) {
    const modal = document.getElementById('coach-3d-modal');
    if (modal && !modal.classList.contains('hidden') && activeCoachTrainNumber.toUpperCase() === trainNumber.toUpperCase()) {
        renderCoachBerths();
    }
}

function triggerTatkalRushFromCoach() {
    triggerTatkalRushOnTrain(activeCoachTrainNumber);
}



// =========================================================================
// 5. 3D TATKAL WARP SPEED FX & HUD OVERLAY
// =========================================================================
let warpCanvas, warpCtx, warpAnimId;
let warpStars = [];
const WARP_STAR_COUNT = 300;

function initTatkalWarpCanvas() {
    warpCanvas = document.getElementById('tatkal-warp-canvas');
    if (!warpCanvas) return;
    warpCtx = warpCanvas.getContext('2d');
    resizeWarpCanvas();
    window.addEventListener('resize', resizeWarpCanvas);

    warpStars = [];
    for (let i = 0; i < WARP_STAR_COUNT; i++) {
        warpStars.push({
            x: (Math.random() - 0.5) * 2000,
            y: (Math.random() - 0.5) * 2000,
            z: Math.random() * 1000,
            pz: 1000
        });
    }
}

function resizeWarpCanvas() {
    if (warpCanvas) {
        warpCanvas.width = window.innerWidth;
        warpCanvas.height = window.innerHeight;
    }
}

function launch3dTatkalWarp(trainNumber, botCount) {
    const overlay = document.getElementById('tatkal-warp-overlay');
    if (!overlay) return;

    document.getElementById('hud-bots-count').textContent = botCount;
    document.getElementById('tatkal-hud-subtitle').textContent = 
        `${botCount} Autonomous AI Passengers Competing Simultaneously on Train ${trainNumber} (Tatkal Quota)`;

    overlay.classList.remove('hidden');
    animateWarp();

    setTimeout(() => {
        dismissTatkalWarp();
    }, 3200);
}

function dismissTatkalWarp() {
    const overlay = document.getElementById('tatkal-warp-overlay');
    if (overlay) overlay.classList.add('hidden');
    if (warpAnimId) cancelAnimationFrame(warpAnimId);
}

function animateWarp() {
    if (!warpCtx || !warpCanvas) return;
    const w = warpCanvas.width;
    const h = warpCanvas.height;
    const cx = w / 2;
    const cy = h / 2;

    warpCtx.fillStyle = 'rgba(10, 16, 30, 0.25)';
    warpCtx.fillRect(0, 0, w, h);

    const speed = 28;
    for (let i = 0; i < warpStars.length; i++) {
        const s = warpStars[i];
        s.pz = s.z;
        s.z -= speed;

        if (s.z <= 0) {
            s.x = (Math.random() - 0.5) * 2000;
            s.y = (Math.random() - 0.5) * 2000;
            s.z = 1000;
            s.pz = 1000;
        }

        const k = 250 / s.z;
        const px = s.x * k + cx;
        const py = s.y * k + cy;

        const pk = 250 / s.pz;
        const ppx = s.x * pk + cx;
        const ppy = s.y * pk + cy;

        const size = Math.max(1, (1 - s.z / 1000) * 3);
        const alpha = Math.min(1, (1 - s.z / 1000) * 1.5);

        warpCtx.beginPath();
        warpCtx.moveTo(ppx, ppy);
        warpCtx.lineTo(px, py);
        warpCtx.strokeStyle = (i % 2 === 0) ? `rgba(251, 121, 43, ${alpha})` : `rgba(255, 235, 59, ${alpha})`;
        warpCtx.lineWidth = size;
        warpCtx.stroke();
    }

    warpAnimId = requestAnimationFrame(animateWarp);
}



