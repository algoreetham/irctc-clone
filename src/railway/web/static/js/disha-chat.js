/**
 * Ask DISHA 2.0 - Local Conversational AI Assistant
 * Powered by pure local NLP with no external cloud API dependencies.
 */

let dishaInitialized = false;

document.addEventListener('DOMContentLoaded', () => {
    initDishaChat();
});

function initDishaChat() {
    const trigger = document.getElementById('disha-trigger');
    const dialog = document.getElementById('disha-dialog');
    const closeBtn = document.getElementById('disha-close');
    const sendBtn = document.getElementById('disha-send');
    const input = document.getElementById('disha-input');

    if (!trigger || !dialog) return;

    trigger.addEventListener('click', () => {
        dialog.classList.toggle('hidden');
        if (!dialog.classList.contains('hidden') && !dishaInitialized) {
            dishaInitialized = true;
            sendToDisha(""); // Fetch welcome greeting
            setTimeout(() => input.focus(), 200);
        }
    });

    if (closeBtn) {
        closeBtn.addEventListener('click', () => {
            dialog.classList.add('hidden');
        });
    }

    if (sendBtn && input) {
        sendBtn.addEventListener('click', () => submitDishaUserMessage());
        input.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                submitDishaUserMessage();
            }
        });
    }
}

function openDisha() {
    const dialog = document.getElementById('disha-dialog');
    if (dialog) {
        dialog.classList.remove('hidden');
        if (!dishaInitialized) {
            dishaInitialized = true;
            sendToDisha("");
        }
        const input = document.getElementById('disha-input');
        if (input) setTimeout(() => input.focus(), 200);
    }
}

function submitDishaUserMessage() {
    const input = document.getElementById('disha-input');
    if (!input) return;
    const msg = input.value.trim();
    if (!msg) return;

    if (window.playAudioTone) window.playAudioTone('click');
    appendDishaMessage(msg, 'user');
    input.value = '';
    sendToDisha(msg);
}

async function sendToDisha(message) {
    const messagesArea = document.getElementById('disha-messages');
    
    // Add typing indicator
    const typingElem = document.createElement('div');
    typingElem.className = 'disha-msg bot typing-indicator';
    typingElem.innerHTML = '<em>Ask DISHA is thinking...</em>';
    messagesArea.appendChild(typingElem);
    messagesArea.scrollTop = messagesArea.scrollHeight;

    try {
        const res = await fetch('/api/ai/disha', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ message: message })
        });
        const json = await res.json();
        
        // Remove typing
        typingElem.remove();

        if (json.success && json.data) {
            const data = json.data;
            if (window.playAudioTone) window.playAudioTone('flip');
            appendDishaMessage(data.reply, 'bot');
            renderDishaSuggestions(data.suggestions);

            // Handle Action
            if (data.actionType && data.actionPayload) {
                handleDishaAction(data.actionType, data.actionPayload);
            }
        } else {
            appendDishaMessage("I apologize, my NLP engine encountered a brief hiccup. Please ask again.", 'bot');
        }
    } catch (err) {
        typingElem.remove();
        appendDishaMessage("Unable to connect to local DISHA service. Please ensure WebServer is running.", 'bot');
    }
}

function appendDishaMessage(text, sender) {
    const messagesArea = document.getElementById('disha-messages');
    if (!messagesArea) return;

    const msgElem = document.createElement('div');
    msgElem.className = `disha-msg ${sender}`;

    // Convert simple markdown (**bold**, - bullets, `code`) to HTML
    let formatted = text
        .replace(/### (.*?)\n/g, '<h4 style="margin:4px 0; color:#213d77;">$1</h4>')
        .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
        .replace(/`(.*?)`/g, '<code style="background:#eef2f8; padding:1px 4px; border-radius:3px; color:#c7254e;">$1</code>')
        .replace(/\n\n/g, '<br><br>')
        .replace(/- (.*?)\n/g, '• $1<br>');

    msgElem.innerHTML = formatted;
    messagesArea.appendChild(msgElem);
    messagesArea.scrollTop = messagesArea.scrollHeight;
}

function renderDishaSuggestions(suggestions) {
    const suggRow = document.getElementById('disha-suggestions');
    if (!suggRow) return;

    suggRow.innerHTML = '';
    if (!suggestions || suggestions.length === 0) return;

    suggestions.forEach(s => {
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'disha-pill-btn';
        btn.textContent = s;
        btn.onclick = () => {
            appendDishaMessage(s, 'user');
            sendToDisha(s);
        };
        suggRow.appendChild(btn);
    });
}

function handleDishaAction(actionType, payload) {
    console.log("[DISHA Action]", actionType, payload);
    if (actionType === 'APPLY_SEARCH') {
        if (payload.source && payload.destination) {
            setStation('source', payload.source);
            setStation('dest', payload.destination);
            switchTab('search-tab');
            searchTrains(payload.source, payload.destination, 'ALL');
        }
    } else if (actionType === 'SHOW_PNR' && payload.pnr) {
        switchTab('pnr-tab');
        const pnrInput = document.getElementById('pnr-search-input');
        if (pnrInput) {
            pnrInput.value = payload.pnr;
            lookupPnr(payload.pnr);
        }
    } else if (actionType === 'SELECT_TRAIN' && payload.trainNumber) {
        switchTab('search-tab');
        openBookingModal(payload.trainNumber, 'GENERAL');
    } else if (actionType === 'REFRESH_FEED') {
        pollAiFeedOnce();
    }
}
