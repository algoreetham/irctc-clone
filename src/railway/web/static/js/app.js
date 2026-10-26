/**
 * IRCTC-Inspired Railway Reservation System - Frontend Client Logic
 * Aligned with Alliance University Course Code: E1CSA25306
 */

let allDemoTrains = [];
let allBookingsCache = [];

document.addEventListener('DOMContentLoaded', () => {
    initTabs();
    initDatePicker();
    initSearchForm();
    initBookingForm();
    initPnrForm();

    // Initial data load
    loadInitialTrains();
});

// -------------------------------------------------------------
// TABS HANDLING
// -------------------------------------------------------------
function initTabs() {
    const tabButtons = document.querySelectorAll('.tab-btn');
    tabButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            const targetId = btn.getAttribute('data-tab');
            switchTab(targetId);
        });
    });
}

function switchTab(tabId) {
    document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
    document.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('active'));

    const activeBtn = document.querySelector(`.tab-btn[data-tab="${tabId}"]`);
    const activePanel = document.getElementById(tabId);

    if (activeBtn) activeBtn.classList.add('active');
    if (activePanel) activePanel.classList.add('active');

    // Trigger loads if needed
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
        console.error('Error fetching trains:', err);
    }
}

async function searchTrains(source, destination, classFilter) {
    const resultsContainer = document.getElementById('train-results');
    const heading = document.getElementById('results-heading');
    const countBadge = document.getElementById('results-count');

    heading.textContent = `Trains: ${source} ➔ ${destination}`;
    resultsContainer.innerHTML = '<div style="text-align:center; padding: 30px; color:#6b7280;">Searching available demo trains...</div>';

    try {
        const res = await fetch(`/api/trains/search?source=${encodeURIComponent(source)}&destination=${encodeURIComponent(destination)}`);
        const json = await res.json();

        if (!json.success) {
            showToast(json.message, 'error');
            resultsContainer.innerHTML = `
                <div class="card" style="text-align:center; padding: 40px;">
                    <div style="font-size: 32px; margin-bottom: 10px;">⚠️</div>
                    <h3 style="color: #b91c1c; margin-bottom: 6px;">Invalid Search</h3>
                    <p style="color: #6b7280;">${json.message}</p>
                </div>
            `;
            countBadge.textContent = '0 trains found';
            return;
        }

        const trains = json.data || [];
        countBadge.textContent = `${trains.length} train(s) found`;

        if (trains.length === 0) {
            resultsContainer.innerHTML = `
                <div class="card" style="text-align: center; padding: 40px;">
                    <div style="font-size: 36px; margin-bottom: 10px;">🛤️</div>
                    <h3 style="color: #0b2545; margin-bottom: 8px;">No Demo Trains Found</h3>
                    <p style="color: #6b7280; max-width: 480px; margin: 0 auto 16px auto;">
                        No demo services run directly between <strong>${source}</strong> and <strong>${destination}</strong>.
                    </p>
                    <p style="font-size: 13px; color: #f58220; font-weight: 600;">
                        Try popular routes: Bengaluru ⇄ Chennai, Bengaluru ⇄ Mysuru, Chennai ⇄ Hyderabad.
                    </p>
                </div>
            `;
            return;
        }

        resultsContainer.innerHTML = '';
        trains.forEach(t => {
            resultsContainer.appendChild(createTrainCard(t, classFilter));
        });

    } catch (err) {
        showToast('Failed to connect to railway backend', 'error');
        resultsContainer.innerHTML = `<div style="color:red; text-align:center;">Network error: ${err.message}</div>`;
    }
}

function createTrainCard(t, classFilter) {
    const card = document.createElement('div');
    card.className = 'train-card';

    const isAvailable = t.availableSeats > 0;
    const seatClass = isAvailable ? 'available' : 'full';
    const seatText = isAvailable ? `✓ ${t.availableSeats} Seats Available` : `⏳ Sold Out (Waiting List Available)`;
    const btnLabel = isAvailable ? '🎫 Book Now' : '⏳ Book on Waiting List';

    card.innerHTML = `
        <div class="train-header">
            <div class="train-title-area">
                <span class="train-number">${t.number}</span>
                <span class="train-name">${t.name}</span>
            </div>
            <div class="fare-pill">Base: ₹${t.baseFare.toFixed(2)}</div>
        </div>

        <div class="train-schedule">
            <div class="time-box">
                <div class="time-val">${t.departureTime}</div>
                <div class="station-name">${t.source}</div>
            </div>

            <div class="route-arrow">
                <span>Demo Daily Service</span>
                <div class="arrow-line"></div>
                <span>Direct Route</span>
            </div>

            <div class="time-box">
                <div class="time-val">${t.arrivalTime}</div>
                <div class="station-name">${t.destination}</div>
            </div>
        </div>

        <div class="train-footer">
            <div class="seat-chips">
                <div class="seat-chip ${seatClass}">
                    ${seatText}
                </div>
                <div class="seat-chip" style="background:#f1f5f9; color:#475569;">
                    Total Capacity: ${t.totalSeats}
                </div>
            </div>

            <button class="btn btn-primary btn-sm"
                onclick="openBookingModal('${t.number}', ${t.baseFare}, '${t.name}', '${t.source}', '${t.destination}', '${t.departureTime}', '${t.arrivalTime}', ${t.availableSeats})">
                ${btnLabel}
            </button>
        </div>
    `;
    return card;
}

// -------------------------------------------------------------
// BOOKING MODAL & FARE CALCULATOR
// -------------------------------------------------------------
function openBookingModal(number, baseFare, name, source, dest, dept, arrv, availableSeats) {
    document.getElementById('modal-train-number').value = number;
    document.getElementById('modal-base-fare').value = baseFare;
    
    let info = `${number} - ${name} (${source} ➔ ${dest}) | Dept: ${dept}`;
    if (availableSeats <= 0) {
        info += ' [WAITING LIST BOOKING]';
    }
    document.getElementById('modal-train-info').textContent = info;
    
    // Reset defaults
    document.getElementById('passenger-name').value = '';
    document.getElementById('passenger-age').value = '28';
    document.getElementById('seat-count').value = '1';
    document.getElementById('modal-ticket-class').value = 'SLEEPER';

    recalculateFare();

    document.getElementById('booking-modal').classList.remove('hidden');
}

function closeBookingModal() {
    document.getElementById('booking-modal').classList.add('hidden');
}

function recalculateFare() {
    const baseFare = parseFloat(document.getElementById('modal-base-fare').value) || 0;
    const classSelect = document.getElementById('modal-ticket-class');
    const selectedOption = classSelect.options[classSelect.selectedIndex];
    const multiplier = parseFloat(selectedOption.getAttribute('data-multiplier')) || 1.0;
    const age = parseInt(document.getElementById('passenger-age').value) || 0;
    const seats = parseInt(document.getElementById('seat-count').value) || 1;

    const seniorNotice = document.getElementById('senior-notice');
    const concessionRow = document.getElementById('concession-row');

    let discountFactor = 1.0;
    if (age >= 60) {
        discountFactor = 0.6; // 40% senior discount
        seniorNotice.classList.remove('hidden');
        concessionRow.style.display = 'flex';
    } else {
        seniorNotice.classList.add('hidden');
        concessionRow.style.display = 'none';
    }

    const fareBaseDisplay = document.getElementById('fare-base');
    const fareMultiplierDisplay = document.getElementById('fare-multiplier');
    const fareTotalDisplay = document.getElementById('fare-total');

    fareBaseDisplay.textContent = `₹${baseFare.toFixed(2)}`;
    fareMultiplierDisplay.textContent = `${multiplier}x (${selectedOption.text.split('-')[0].trim()})`;

    const totalFare = baseFare * multiplier * discountFactor * Math.max(1, seats);
    fareTotalDisplay.textContent = `₹${totalFare.toFixed(2)}`;
}

function initBookingForm() {
    const form = document.getElementById('booking-form');
    form.addEventListener('submit', async (e) => {
        e.preventDefault();

        const trainNumber = document.getElementById('modal-train-number').value;
        const passengerName = document.getElementById('passenger-name').value.trim();
        const passengerAge = parseInt(document.getElementById('passenger-age').value);
        const ticketClass = document.getElementById('modal-ticket-class').value;
        const seatCount = parseInt(document.getElementById('seat-count').value);
        const paymentSim = document.querySelector('input[name="paymentSim"]:checked').value;
        const simulateFailure = (paymentSim === 'fail');

        const submitBtn = document.getElementById('confirm-booking-btn');
        submitBtn.disabled = true;
        submitBtn.textContent = 'Processing Payment...';

        try {
            const response = await fetch('/api/bookings', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    trainNumber,
                    passengerName,
                    passengerAge,
                    seatCount,
                    ticketClass,
                    simulateFailure
                })
            });

            const result = await response.json();
            submitBtn.disabled = false;
            submitBtn.textContent = 'Proceed to Simulated Payment & Book';

            if (!result.success) {
                showToast(`Booking Failed: ${result.message}`, 'error');
                return;
            }

            closeBookingModal();
            const msg = result.data.status === 'WAITING_LIST'
                ? `Booking Registered on WAITING LIST (Rank: WL-${result.data.waitingListNumber})`
                : `Success! PNR ${result.data.pnr} Confirmed.`;
            showToast(msg, 'success');
            displayDigitalTicket(result.data);

            // Refresh search results & availability
            const src = document.getElementById('source-input').value;
            const dst = document.getElementById('dest-input').value;
            searchTrains(src, dst, 'ALL');

        } catch (err) {
            submitBtn.disabled = false;
            submitBtn.textContent = 'Proceed to Simulated Payment & Book';
            showToast(`Network error: ${err.message}`, 'error');
        }
    });
}

// -------------------------------------------------------------
// DIGITAL TICKET MODAL
// -------------------------------------------------------------
function displayDigitalTicket(booking) {
    const printArea = document.getElementById('ticket-print-area');
    const isWl = (booking.status === 'WAITING_LIST');
    const badgeClass = isWl ? 'status-waiting' : (booking.status === 'CANCELLED' ? 'status-cancelled' : 'status-confirmed');
    const statusText = isWl ? `STATUS: WAITING LIST (WL-${booking.waitingListNumber})` : `STATUS: ${booking.status}`;

    printArea.innerHTML = `
        <div class="ticket-container">
            <div class="ticket-top">
                <div>
                    <div style="font-size: 18px; font-weight: 800; color: #0b2545;">INDIAN RAILWAYS RESERVATION</div>
                    <div style="font-size: 11px; color: #f58220; font-weight: 600;">ALLIANCE UNIVERSITY - E1CSA25306 SIMULATION</div>
                    <div class="ticket-barcode">||| | |||| | ||||| |||</div>
                </div>
                <div class="ticket-pnr-box">
                    <div class="ticket-pnr-label">Booking PNR</div>
                    <div class="ticket-pnr-value">${booking.pnr}</div>
                    <div class="status-badge ${badgeClass}">${statusText}</div>
                </div>
            </div>

            <div class="ticket-grid">
                <div class="ticket-item">
                    <span>Train Service</span>
                    <span>${booking.train.name} (${booking.train.number})</span>
                </div>
                <div class="ticket-item">
                    <span>Route</span>
                    <span>${booking.train.source} ➔ ${booking.train.destination}</span>
                </div>
                <div class="ticket-item">
                    <span>Departure / Arrival</span>
                    <span>${booking.train.departureTime} ➔ ${booking.train.arrivalTime}</span>
                </div>
                <div class="ticket-item">
                    <span>Travel Class & Seats</span>
                    <span>${booking.classDisplayName} • ${booking.seatCount} Seat(s)</span>
                </div>
                <div class="ticket-item">
                    <span>Passenger Name</span>
                    <span>${booking.passenger.name} (${booking.passenger.age} yrs)</span>
                </div>
                <div class="ticket-item">
                    <span>Passenger Category</span>
                    <span>${booking.passenger.category}</span>
                </div>
                <div class="ticket-item">
                    <span>Total Demo Fare Paid</span>
                    <span style="font-size: 16px; color: #0b2545; font-weight: 800;">₹${booking.fare.toFixed(2)}</span>
                </div>
                <div class="ticket-item">
                    <span>Date of Booking</span>
                    <span>${booking.bookingTime}</span>
                </div>
            </div>

            <div class="ticket-footer-msg">
                * Happy Journey! Simulation only — built for Alliance University Java OOP Microproject (E1CSA25306). *
            </div>
        </div>
    `;

    document.getElementById('ticket-modal').classList.remove('hidden');
}

function closeTicketModal() {
    document.getElementById('ticket-modal').classList.add('hidden');
}

// -------------------------------------------------------------
// PNR STATUS & CANCELLATION
// -------------------------------------------------------------
function initPnrForm() {
    const form = document.getElementById('pnr-form');
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        const pnr = document.getElementById('pnr-input').value.trim().toUpperCase();
        if (!pnr) return;

        const area = document.getElementById('pnr-result-area');
        area.innerHTML = '<div style="text-align:center; padding: 20px;">Fetching booking details...</div>';

        try {
            const res = await fetch(`/api/bookings/${encodeURIComponent(pnr)}`);
            const json = await res.json();

            if (!json.success) {
                area.innerHTML = `
                    <div class="card" style="text-align:center; padding: 30px;">
                        <div style="font-size: 32px; margin-bottom: 8px;">❌</div>
                        <h3 style="color: #b91c1c;">PNR Not Found</h3>
                        <p style="color: #6b7280;">${json.message}</p>
                    </div>
                `;
                return;
            }

            renderPnrCard(json.data);

        } catch (err) {
            area.innerHTML = `<div style="color:red; text-align:center;">Network error: ${err.message}</div>`;
        }
    });
}

function renderPnrCard(booking) {
    const area = document.getElementById('pnr-result-area');
    const isCancelled = booking.status === 'CANCELLED';
    const isWl = booking.status === 'WAITING_LIST';
    const statusClass = isCancelled ? 'status-cancelled' : (isWl ? 'status-waiting' : 'status-confirmed');
    const statusText = isWl ? `STATUS: WAITING LIST (WL-${booking.waitingListNumber})` : `STATUS: ${booking.status}`;

    area.innerHTML = `
        <div class="card">
            <div class="card-header-flex">
                <div>
                    <span style="font-size: 12px; color: #6b7280; text-transform: uppercase;">PNR NUMBER</span>
                    <h2 style="font-family: 'JetBrains Mono', monospace; color: #0b2545;">${booking.pnr}</h2>
                </div>
                <div>
                    <span class="status-badge ${statusClass}">${statusText}</span>
                </div>
            </div>

            <div class="ticket-grid" style="margin-top: 15px;">
                <div class="ticket-item">
                    <span>Train</span>
                    <span>${booking.train.name} (${booking.train.number})</span>
                </div>
                <div class="ticket-item">
                    <span>Route</span>
                    <span>${booking.train.source} ➔ ${booking.train.destination}</span>
                </div>
                <div class="ticket-item">
                    <span>Passenger</span>
                    <span>${booking.passenger.name} (${booking.passenger.age} yrs, ${booking.passenger.category})</span>
                </div>
                <div class="ticket-item">
                    <span>Class & Seats</span>
                    <span>${booking.classDisplayName} • ${booking.seatCount} Seat(s)</span>
                </div>
                <div class="ticket-item">
                    <span>Fare Paid</span>
                    <span>₹${booking.fare.toFixed(2)}</span>
                </div>
                <div class="ticket-item">
                    <span>Booking Timestamp</span>
                    <span>${booking.bookingTime}</span>
                </div>
            </div>

            <div style="display: flex; justify-content: flex-end; gap: 10px; margin-top: 15px; border-top: 1px solid #e5e7eb; padding-top: 15px;">
                <button class="btn btn-secondary btn-sm" onclick='displayDigitalTicket(${JSON.stringify(booking)})'>
                    📄 View Ticket
                </button>
                ${!isCancelled ? `
                    <button class="btn btn-danger btn-sm" onclick="cancelTicket('${booking.pnr}')">
                        🚫 Cancel Ticket
                    </button>
                ` : `
                    <span style="font-size: 12px; color: #b91c1c; font-weight: 600; align-self: center;">
                        Ticket was cancelled and seats restored to train.
                    </span>
                `}
            </div>
        </div>
    `;
}

async function cancelTicket(pnr) {
    if (!confirm(`Are you sure you want to cancel booking ${pnr}? Seats will be released immediately.`)) {
        return;
    }

    try {
        const res = await fetch(`/api/bookings/${encodeURIComponent(pnr)}/cancel`, {
            method: 'POST'
        });
        const json = await res.json();

        if (!json.success) {
            showToast(`Cancellation failed: ${json.message}`, 'error');
            return;
        }

        showToast(`Booking ${pnr} has been CANCELLED. Any waiting passengers auto-promoted!`, 'success');
        renderPnrCard(json.data);

        // Reload history table and search list
        loadBookingHistory();

    } catch (err) {
        showToast(`Error cancelling ticket: ${err.message}`, 'error');
    }
}

// -------------------------------------------------------------
// AVAILABILITY GRID
// -------------------------------------------------------------
async function loadAvailabilityGrid() {
    const grid = document.getElementById('availability-grid');
    grid.innerHTML = '<div style="grid-column: 1/-1; text-align:center; padding: 20px;">Updating live availability...</div>';

    try {
        const res = await fetch('/api/trains');
        const json = await res.json();
        if (!json.success || !json.data) return;

        grid.innerHTML = '';
        json.data.forEach(t => {
            const pct = Math.round((t.availableSeats / t.totalSeats) * 100);
            const card = document.createElement('div');
            card.className = 'avail-card';
            card.innerHTML = `
                <div class="avail-card-header">
                    <span class="train-number">${t.number}</span>
                    <span style="font-size: 12px; font-weight: 700; color: ${t.availableSeats > 0 ? '#15803d' : '#b45309'};">
                        ${t.availableSeats > 0 ? (t.availableSeats + ' / ' + t.totalSeats + ' Left') : 'WL Available'}
                    </span>
                </div>
                <h4 style="color: #0b2545; font-size: 15px; margin-bottom: 2px;">${t.name}</h4>
                <p style="font-size: 12px; color: #6b7280;">${t.source} ➔ ${t.destination}</p>
                <div class="avail-bar-bg">
                    <div class="avail-bar-fill" style="width: ${pct}%; background: ${pct < 20 ? '#ef4444' : '#10b981'};"></div>
                </div>
                <div style="display: flex; justify-content: space-between; font-size: 11.5px; color: #64748b;">
                    <span>Dept: ${t.departureTime}</span>
                    <span>Base: ₹${t.baseFare.toFixed(2)}</span>
                </div>
            `;
            grid.appendChild(card);
        });

    } catch (err) {
        grid.innerHTML = `<div style="color:red;">Failed to load seat grid: ${err.message}</div>`;
    }
}

// -------------------------------------------------------------
// WAITING LIST TAB (Alliance University CDAP Specification)
// -------------------------------------------------------------
async function loadWaitingListTab() {
    const container = document.getElementById('wl-container');
    container.innerHTML = '<div style="text-align:center; padding: 20px;">Loading Waiting List queues...</div>';

    try {
        const trainsRes = await fetch('/api/trains');
        const trainsJson = await trainsRes.json();
        if (!trainsJson.success) return;

        container.innerHTML = '';
        for (const train of trainsJson.data) {
            const wlRes = await fetch(`/api/trains/${train.number}/waiting-list`);
            const wlJson = await wlRes.json();
            const waitingList = wlJson.data || [];

            const card = document.createElement('div');
            card.className = 'card';
            card.style.marginBottom = '16px';
            card.innerHTML = `
                <div class="card-header-flex">
                    <div>
                        <span class="train-number">${train.number}</span>
                        <strong style="margin-left: 8px; font-size: 16px; color:#0b2545;">${train.name}</strong>
                        <span style="font-size: 12px; color: #64748b; margin-left: 8px;">(${train.source} ➔ ${train.destination})</span>
                    </div>
                    <span class="status-badge ${waitingList.length > 0 ? 'status-waiting' : 'status-confirmed'}">
                        ${waitingList.length} in Waiting Queue (Max 100)
                    </span>
                </div>
                ${waitingList.length === 0 ? `
                    <p style="font-size: 13px; color: #6b7280; font-style: italic; margin-top: 8px;">
                        No passengers currently waiting for this train. Seats available: ${train.availableSeats}.
                    </p>
                ` : `
                    <div class="table-responsive" style="margin-top: 10px;">
                        <table class="data-table">
                            <thead>
                                <tr>
                                    <th>Rank</th>
                                    <th>PNR</th>
                                    <th>Passenger</th>
                                    <th>Class</th>
                                    <th>Seats</th>
                                    <th>Booking Time</th>
                                </tr>
                            </thead>
                            <tbody>
                                ${waitingList.map((b, idx) => `
                                    <tr>
                                        <td><span class="status-badge status-waiting">WL-${b.waitingListNumber || (idx + 1)}</span></td>
                                        <td><strong style="font-family:'JetBrains Mono',monospace;">${b.pnr}</strong></td>
                                        <td>${b.passenger.name} (${b.passenger.age} yrs)</td>
                                        <td>${b.classDisplayName}</td>
                                        <td>${b.seatCount}</td>
                                        <td>${b.bookingTime}</td>
                                    </tr>
                                `).join('')}
                            </tbody>
                        </table>
                    </div>
                `}
            `;
            container.appendChild(card);
        }

    } catch (err) {
        container.innerHTML = `<div style="color:red;">Error loading waiting list: ${err.message}</div>`;
    }
}

// -------------------------------------------------------------
// BOOKING HISTORY & TABLE
// -------------------------------------------------------------
async function loadBookingHistory() {
    const tbody = document.getElementById('history-tbody');
    tbody.innerHTML = '<tr><td colspan="10" style="text-align:center; padding: 20px;">Loading history records...</td></tr>';

    try {
        const res = await fetch('/api/bookings');
        const json = await res.json();

        if (!json.success || !json.data || json.data.length === 0) {
            tbody.innerHTML = '<tr><td colspan="10" style="text-align:center; padding: 30px; color: #6b7280;">No bookings found in history or persistent storage.</td></tr>';
            allBookingsCache = [];
            return;
        }

        allBookingsCache = json.data;
        renderHistoryRows(allBookingsCache);

    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="10" style="color:red; text-align:center;">Failed to load bookings: ${err.message}</td></tr>`;
    }
}

function renderHistoryRows(bookings) {
    const tbody = document.getElementById('history-tbody');
    tbody.innerHTML = '';

    bookings.forEach(b => {
        const tr = document.createElement('tr');
        const isCancelled = b.status === 'CANCELLED';
        const isWl = b.status === 'WAITING_LIST';
        const badgeClass = isCancelled ? 'status-cancelled' : (isWl ? 'status-waiting' : 'status-confirmed');
        const statusLabel = isWl ? `WL-${b.waitingListNumber}` : b.status;

        tr.innerHTML = `
            <td><span class="status-badge ${badgeClass}">${statusLabel}</span></td>
            <td><strong style="font-family:'JetBrains Mono',monospace;">${b.pnr}</strong></td>
            <td>${b.passenger.name} (${b.passenger.age})</td>
            <td>${b.train.source} ➔ ${b.train.destination}</td>
            <td>${b.classDisplayName}</td>
            <td>${b.seatCount}</td>
            <td>₹${b.fare.toFixed(2)}</td>
            <td>${b.train.number}</td>
            <td style="font-size: 11.5px; color:#64748b;">${b.bookingTime}</td>
            <td>
                <div style="display: flex; gap: 6px;">
                    <button class="btn btn-secondary btn-sm" onclick='displayDigitalTicket(${JSON.stringify(b)})'>View</button>
                    ${!isCancelled ? `<button class="btn btn-danger btn-sm" onclick="cancelTicket('${b.pnr}')">Cancel</button>` : ''}
                </div>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

function filterHistoryTable() {
    const query = document.getElementById('history-filter').value.toLowerCase().trim();
    if (!query) {
        renderHistoryRows(allBookingsCache);
        return;
    }

    const filtered = allBookingsCache.filter(b => {
        return b.pnr.toLowerCase().includes(query) ||
               b.passenger.name.toLowerCase().includes(query) ||
               b.train.name.toLowerCase().includes(query) ||
               b.train.number.toLowerCase().includes(query) ||
               b.train.source.toLowerCase().includes(query) ||
               b.train.destination.toLowerCase().includes(query);
    });
    renderHistoryRows(filtered);
}

// -------------------------------------------------------------
// TOAST NOTIFICATIONS
// -------------------------------------------------------------
function showToast(msg, type = 'info') {
    const toast = document.getElementById('toast');
    toast.textContent = msg;
    toast.className = `toast toast-${type}`;
    toast.classList.remove('hidden');

    setTimeout(() => {
        toast.classList.add('hidden');
    }, 4000);
}
