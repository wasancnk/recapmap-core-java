// app.js
// You can add JS code here if needed
console.log('App JS loaded');

// Global chat state for multiple conversations
let username = null;
let userRole = null;
let DEMO_MODE = false;
const copilotName = "IP Copilot";
const copilotRole = "assistant";

// Fetch user info from backend
function setUserInfoFromBackend() {
    return fetch('/api/userinfo', { credentials: 'same-origin' })
        .then(r => r.json())
        .then(info => {
            username = info.username || 'User';
            // Determine role: if roles contains ROLE_ADMIN, set as admin, else user
            if (Array.isArray(info.roles) && info.roles.some(r => r === 'ROLE_ADMIN')) {
                userRole = 'admin';
                DEMO_MODE = false;
            } else {
                userRole = 'user';
                DEMO_MODE = true;
            }
        })
        .catch(() => {
            username = 'User';
            userRole = 'user';
            DEMO_MODE = true;
        });
}

function uuidv4() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        var r = Math.random() * 16 | 0, v = c === 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
    });
}

// Helper to truncate message
function truncate(str, n, useWordBoundary) {
    if (str.length <= n) { return str; }
    const subString = str.slice(0, n - 1); // the original check
    return (useWordBoundary
        ? subString.slice(0, subString.lastIndexOf(" "))
        : subString) + "...";
}

// Helper for localStorage persistence
const LS_KEY = 'core_conversations';
function saveConversations() {
    localStorage.setItem(LS_KEY, JSON.stringify(conversations));
}
function loadConversations() {
    const data = localStorage.getItem(LS_KEY);
    if (data) {
        try {
            const arr = JSON.parse(data);
            if (Array.isArray(arr)) return arr;
        } catch {}
    }
    return null;
}

// Mock conversations
let conversations = loadConversations() || [
    {
        id: uuidv4(),
        name: "New Conversation",
        messages: [
            {
                uuid: uuidv4(),
                username: copilotName,
                userRole: copilotRole,
                message: "Hello! I am IP Copilot. How can I assist you today?",
                timestamp: new Date(Date.now() - 59000).toLocaleTimeString()
            }
        ]
    },
];
let currentConversationIdx = 0;

// Ensure uploadProgress is defined globally for all functions
let uploadProgress = [];
// Ensure uploadFiles and uploadPolling are also defined globally
let uploadFiles = [];
let uploadPolling = null;

// --- Prompt Config Persistence (global) ---
let promptList = [
    {
        detail: "Extract all raw paragraphs from the document, preserving their order and structure. Output as a well-structured Markdown document. Use headings for sections, bullet points for lists, and preserve assignments and key topics as subheadings or lists.",
        note: "Raw paragraph extraction",
        extractionTypes: ["raw"],
        includeImage: true
    },
    {
        detail: "Analyze all diagrams, charts, and images in the window. For each, provide a concise semantic summary of its meaning and relevance to the document. Output as a JSON array of objects with fields: imageIndex, summary.",
        note: "Semantic/diagram extraction",
        extractionTypes: ["semantic"],
        includeImage: true
    },
    {
        detail: "Generate 3-5 high-quality Q&A pairs that cover the key concepts, facts, or reasoning in this window. Output as a JSON array of objects with fields: question, answer.",
        note: "Q&A generation",
        extractionTypes: ["qa"],
        includeImage: true
    },
    {
        detail: "Extract all important legal, technical, and IP-related keywords and phrases from this window. Output as a JSON array of strings.",
        note: "Structured keyword extraction",
        extractionTypes: ["keywords"],
        includeImage: true
    }
];
let mergePrompts = false;
const PROMPT_CONFIG_KEY = 'core_prompt_config';
function loadPromptConfig() {
    try {
        const data = localStorage.getItem(PROMPT_CONFIG_KEY);
        if (data) {
            const obj = JSON.parse(data);
            if (Array.isArray(obj.promptList)) promptList = obj.promptList;
            if (typeof obj.mergePrompts === 'boolean') mergePrompts = obj.mergePrompts;
        } else {
            // If no config in localStorage, set default prompts
            promptList = [
                {
                    detail: "Extract all raw paragraphs from the document, preserving their order and structure. Output as a well-structured Markdown document. Use headings for sections, bullet points for lists, and preserve assignments and key topics as subheadings or lists.",
                    note: "Raw paragraph extraction",
                    extractionTypes: ["raw"],
                    includeImage: true
                },
                {
                    detail: "Analyze all diagrams, charts, and images in the window. For each, provide a concise semantic summary of its meaning and relevance to the document. Output as a JSON array of objects with fields: imageIndex, summary.",
                    note: "Semantic/diagram extraction",
                    extractionTypes: ["semantic"],
                    includeImage: true
                },
                {
                    detail: "Generate 3-5 high-quality Q&A pairs that cover the key concepts, facts, or reasoning in this window. Output as a JSON array of objects with fields: question, answer.",
                    note: "Q&A generation",
                    extractionTypes: ["qa"],
                    includeImage: true
                },
                {
                    detail: "Extract all important legal, technical, and IP-related keywords and phrases from this window. Output as a JSON array of strings.",
                    note: "Structured keyword extraction",
                    extractionTypes: ["keywords"],
                    includeImage: true
                }
            ];
        }
    } catch {}
}
function savePromptConfig() {
    try {
        localStorage.setItem(PROMPT_CONFIG_KEY, JSON.stringify({ promptList, mergePrompts }));
    } catch {}
}
// Load on startup
loadPromptConfig();
window.addEventListener('storage', function(e) {
    if (e.key === PROMPT_CONFIG_KEY) loadPromptConfig();
});


window.addEventListener('DOMContentLoaded', function() {
    setUserInfoFromBackend().then(() => {
        // Build layout
        const body = document.body;
        body.innerHTML = `
            <div id="left-panel">
                <div id="app-header">
                    <h2>IP Copilot</h2>
                </div>
                <div id="menu-list">
                    <div class="menu-item" data-menu="upload" ${DEMO_MODE ? 'style="pointer-events:none;opacity:0.6;cursor:not-allowed;" title="Disabled in demo"' : ''}>Upload documents</div>
                    <div class="menu-item" data-menu="addsource" ${DEMO_MODE ? 'style="pointer-events:none;opacity:0.6;cursor:not-allowed;" title="Disabled in demo"' : ''}>Add document sources</div>
                    <div class="menu-item" data-menu="admin" ${DEMO_MODE ? 'style="pointer-events:none;opacity:0.6;cursor:not-allowed;" title="Disabled in demo"' : ''}>Admin Console</div>
                </div>
                <div id="conversation-history">
                    <div class="history-title">Conversations</div>
                    <div id="history-list"></div>
                </div>
                <form id="logout-form" method="post" action="/logout" style="margin-top:auto;">
                    <button id="logout-btn" type="submit">Logout</button>
                </form>
                <div id="left-footer">
                    <div class="version-line">Version Alpha 0.1.0</div>
                    <div class="platform-line">Platform powered by RecapMap</div>
                </div>
            </div>
            <div id="container">
                <div id="navbar">
                    <input id="conversation-name" value="" />
                    <button id="new-conv-btn">New Conversation</button>
                </div>
                <div id="chat-area">
                    <div id="chat-messages"></div>
                </div>
                <form id="chat-input-section" autocomplete="off">
                    <textarea id="chat-input" rows="2" placeholder="Type your message..."></textarea>
                    <button id="send-btn" type="submit" disabled>Send</button>
                </form>
                <div id="chat-footer" class="chat-footer-muted">
                    IP Copilot may make mistakes. Please verify important information.
                </div>
            </div>
            <div id="modal-overlay" style="display:none;">
                <div id="modal">
                    <div id="modal-title"></div>
                    <div id="modal-content"></div>
                    <button id="modal-close">Close</button>
                </div>
            </div>
        `;
        // Dynamically load jQuery after DOM is built
        var jqScript = document.createElement('script');
        jqScript.src = '/jquery-3.7.1.min.js';
        jqScript.type = 'text/javascript';
        document.head.appendChild(jqScript);

        jqScript.onload = function() {
            // --- CSRF Token Helper ---
            function getCsrfToken() {
                const match = document.cookie.match(/XSRF-TOKEN=([^;]+)/);
                return match ? decodeURIComponent(match[1]) : null;
            }
            // Set up jQuery to always send CSRF token
            $.ajaxSetup({
                beforeSend: function(xhr, settings) {
                    var csrfToken = getCsrfToken();
                    if (csrfToken) {
                        xhr.setRequestHeader('X-CSRF-TOKEN', csrfToken);
                    }
                }
            });

            // --- Upload Modal Logic ---
            function showUploadModal() {
                // Clear previous upload state
                uploadFiles = [];
                uploadProgress = [];
                uploadPolling = null;
                modalTitle.textContent = 'Upload PDF Documents';
                renderUploadModalContent();
                modalOverlay.style.display = 'flex';
            }

            function renderUploadModalContent() {
                let html = `
                    <div style="margin-bottom:10px;">
                        <input type="file" id="upload-input" accept="application/pdf" multiple style="display:none;" />
                        <button id="add-upload-btn">Add PDF(s)</button>
                    </div>
                    <table id="upload-table" style="width:100%;margin-bottom:10px;font-size:0.97em;">
                        <thead><tr><th></th><th>File</th><th>Size</th><th>Upload Status</th><th>Conversion Status</th><th>Extraction Status</th><th></th></tr></thead>
                        <tbody>
                            ${uploadFiles.map((f,i)=>`<tr>
                                <td><input type="checkbox" class="extract-checkbox" data-idx="${i}" ${f.extracted?'disabled checked':''}></td>
                                <td>${f.name}</td>
                                <td>${(f.size/1024/1024).toFixed(2)} MB</td>
                                <td id="upload-status-${i}">${f.uploadStatus || ''}</td>
                                <td id="convert-status-${i}">${f.convertStatus || ''}</td>
                                <td id="extract-status-${i}">${f.extractStatus || (f.extracted ? 'Extracted' : '')}</td>
                                <td><button class="remove-upload-btn" data-idx="${i}">Remove</button></td>
                            </tr>`).join('')}
                        </tbody>
                    </table>
                    <button id="start-upload-btn" ${uploadFiles.length===0?'disabled':''} class="upload-btn">Upload</button>
                    <button id="convert-all-btn" class="upload-btn" ${uploadFiles.length===0?'disabled':''} style="margin-left:10px;">Convert All</button>
                    <button id="extract-data-btn" class="upload-btn" ${uploadFiles.length===0?'disabled':''} style="margin-left:10px;">Extract Data</button>
                    <button id="extract-simple-btn" class="upload-btn" ${uploadFiles.length===0?'disabled':''} style="margin-left:10px;">Extract Simple (Per-Page)</button>
                    <button id="save-extracted-data" class="upload-btn" ${uploadFiles.length===0?'disabled':''} style="margin-left:10px;">Save Extracted Data</button>
                    <div id="upload-progress-log" style="margin-top:12px;max-height:120px;overflow:auto;font-size:0.95em;background:#f9f9f9;padding:8px 10px;border-radius:6px;"></div>
                `;
                document.getElementById('modal-content').innerHTML = html;
                // Bind file input
                document.getElementById('add-upload-btn').onclick = function() {
                    document.getElementById('upload-input').click();
                };
                document.getElementById('upload-input').onchange = function(e) {
                    for (let file of e.target.files) {
                        if (!uploadFiles.some(f=>f.name===file.name && f.size===file.size)) uploadFiles.push(file);
                    }
                    renderUploadModalContent();
                };
                // Remove file
                document.querySelectorAll('.remove-upload-btn').forEach(btn=>{
                    btn.onclick = function() {
                        uploadFiles.splice(+btn.dataset.idx,1);
                        renderUploadModalContent();
                    };
                });
                // Upload
                document.getElementById('start-upload-btn').onclick = startUpload;
                // Convert All
                document.getElementById('convert-all-btn').onclick = startConvertAll;
                // Extract Data
                document.getElementById('extract-data-btn').onclick = startExtractData;
                // Extract Simple (Per-Page)
                document.getElementById('extract-simple-btn').onclick = startExtractSimplePerPage;
                // Save Extracted Data
                document.getElementById('save-extracted-data').onclick = saveExtractedData;
                // Show progress log
                updateUploadProgressLog();
            }

            window.startUpload = function() {
                if (uploadFiles.length===0) return;
                let formData = new FormData();
                uploadFiles.forEach(f=>formData.append('file',f));
                // Set all upload statuses to 'Uploading...'
                uploadFiles.forEach((f,i)=>{ f.uploadStatus = 'Uploading...'; document.getElementById(`upload-status-${i}`).textContent = f.uploadStatus; });
                $.ajax({
                    url: '/file/upload',
                    method: 'POST',
                    data: formData,
                    processData: false,
                    contentType: false,
                    success: function(res) {
                        if (Array.isArray(res)) {
                            // Mark all as uploaded
                            uploadFiles.forEach((f,i)=>{ f.uploadStatus = 'Uploaded'; document.getElementById(`upload-status-${i}`).textContent = f.uploadStatus; });
                            uploadProgress = ['Upload complete. Select a file below to convert.'];
                            // List files and show convert buttons
                            $.get('/file/list', function(list) {
                                // Attach uuid to uploadFiles by matching name
                                Object.entries(list).forEach(([uuid, name])=>{
                                    const fileObj = uploadFiles.find(f => f.name === name);
                                    if (fileObj) fileObj.uuid = uuid;
                                });
                                let html = '<div style="margin:8px 0 4px 0;font-weight:bold;">Uploaded Files</div>';
                                html += '<ul style="padding-left:18px;">';
                                Object.entries(list).forEach(([uuid, name])=>{
                                    html += `<li>${name} <input type="hidden" data-uuid=\"${uuid}\">Convert</input></li>`;
                                });
                                html += '</ul>';
                                document.getElementById('upload-progress-log').innerHTML = html;
                                $('.convert-btn').each(function() {
                                    $(this).on('click', function() {
                                        window.startConvert($(this).data('uuid'));
                                    });
                                });
                            });
                        } else {
                            uploadFiles.forEach((f,i)=>{ f.uploadStatus = 'Error'; document.getElementById(`upload-status-${i}`).textContent = f.uploadStatus; });
                            uploadProgress = [res];
                            window.updateUploadProgressLog();
                        }
                    },
                    error: function(xhr) {
                        uploadFiles.forEach((f,i)=>{ f.uploadStatus = 'Error'; document.getElementById(`upload-status-${i}`).textContent = f.uploadStatus; });
                        uploadProgress = [xhr.responseText || 'Upload failed.'];
                        window.updateUploadProgressLog();
                    }
                });
            }

            window.startConvertAll = function() {
                uploadFiles.forEach((f,i)=>{ f.convertStatus = 'Queued'; document.getElementById(`convert-status-${i}`).textContent = f.convertStatus; });
                uploadProgress = ['Starting conversion for all files...'];
                window.updateUploadProgressLog();
                $.get('/file/list', function(list) {
                    const uuids = Object.keys(list);
                    if (uuids.length === 0) {
                        uploadProgress = ['No files to convert.'];
                        window.updateUploadProgressLog();
                        return;
                    }
                    let completed = 0;
                    uuids.forEach((uuid, idx) => {
                        uploadFiles[idx].convertStatus = 'Converting...';
                        document.getElementById(`convert-status-${idx}`).textContent = uploadFiles[idx].convertStatus;
                        $.ajax({
                            url: '/file/convert',
                            method: 'POST',
                            data: { uuid: uuid },
                            success: function(msg) {
                                completed++;
                                uploadFiles[idx].convertStatus = 'Done';
                                document.getElementById(`convert-status-${idx}`).textContent = uploadFiles[idx].convertStatus;
                                uploadProgress.push(msg);
                                window.updateUploadProgressLog();
                            },
                            error: function(xhr) {
                                completed++;
                                uploadFiles[idx].convertStatus = 'Error';
                                document.getElementById(`convert-status-${idx}`).textContent = uploadFiles[idx].convertStatus;
                                uploadProgress.push(xhr.responseText || 'Conversion failed.');
                                window.updateUploadProgressLog();
                            }
                        });
                    });
                });
            }

            window.startExtractData = function() {
                // Gather all selected files
                const selectedFiles = uploadFiles.filter((f, i) => {
                    const checkbox = document.querySelector(`.extract-checkbox[data-idx="${i}"]`);
                    return checkbox && checkbox.checked;
                });
                if (selectedFiles.length === 0) {
                    alert('No files selected for extraction.');
                    return;
                }
                // Mark all as extracting
                selectedFiles.forEach((f, i) => {
                    f.extractStatus = 'Extracting...';
                    document.getElementById(`extract-status-${i}`).textContent = f.extractStatus;
                });
                uploadProgress = ['Starting data extraction...'];
                updateUploadProgressLog();
                // Send extraction request to backend for each selected file
                selectedFiles.forEach((f, i) => {
                    if (!f.uuid) {
                        f.extractStatus = 'Error: No UUID';
                        document.getElementById(`extract-status-${i}`).textContent = f.extractStatus;
                        uploadProgress.push('Extraction failed for ' + (f.name || f.uuid) + ': No UUID found. Please re-upload.');
                        updateUploadProgressLog();
                        return;
                    }
                    $.ajax({
                        url: '/file/extract',
                        method: 'POST',
                        contentType: 'application/json',
                        data: JSON.stringify({
                            uuid: f.uuid,
                            promptList: promptList
                        }),
                        success: function(res) {
                            f.extracted = true;
                            f.extractStatus = 'Extracted';
                            document.getElementById(`extract-status-${i}`).textContent = f.extractStatus;
                            uploadProgress.push('Extraction completed for ' + (f.name || f.uuid));
                            updateUploadProgressLog();
                        },
                        error: function(xhr) {
                            f.extractStatus = 'Error';
                            document.getElementById(`extract-status-${i}`).textContent = f.extractStatus;
                            uploadProgress.push('Extraction failed for ' + (f.name || f.uuid) + ': ' + (xhr.responseText || 'Error'));
                            updateUploadProgressLog();
                        }
                    });
                });
            }

            window.startExtractSimplePerPage = function() {
                // Gather all selected files
                const selectedFiles = uploadFiles.filter((f, i) => {
                    const checkbox = document.querySelector(`.extract-checkbox[data-idx="${i}"]`);
                    return checkbox && checkbox.checked;
                });
                if (selectedFiles.length === 0) {
                    alert('No files selected for extraction.');
                    return;
                }
                // Mark all as extracting
                selectedFiles.forEach((f, i) => {
                    f.extractStatus = 'Extracting (Simple)...';
                    document.getElementById(`extract-status-${i}`).textContent = f.extractStatus;
                });
                uploadProgress = ['Starting simple per-page extraction...'];
                updateUploadProgressLog();
                // Send extraction request to backend for each selected file
                selectedFiles.forEach((f, i) => {
                    if (!f.uuid) {
                        f.extractStatus = 'Error: No UUID';
                        document.getElementById(`extract-status-${i}`).textContent = f.extractStatus;
                        uploadProgress.push('Extraction failed for ' + (f.name || f.uuid) + ': No UUID found. Please re-upload.');
                        updateUploadProgressLog();
                        return;
                    }
                    $.ajax({
                        url: '/file/extract-simple',
                        method: 'POST',
                        contentType: 'application/json',
                        data: JSON.stringify({ uuid: f.uuid }),
                        success: function(res) {
                            f.extracted = true;
                            f.extractStatus = 'Extracted (Simple)';
                            document.getElementById(`extract-status-${i}`).textContent = f.extractStatus;
                            uploadProgress.push('Simple extraction completed for ' + (f.name || f.uuid));
                            updateUploadProgressLog();
                        },
                        error: function(xhr) {
                            f.extractStatus = 'Error';
                            document.getElementById(`extract-status-${i}`).textContent = f.extractStatus;
                            uploadProgress.push('Simple extraction failed for ' + (f.name || f.uuid) + ': ' + (xhr.responseText || 'Error'));
                            updateUploadProgressLog();
                        }
                    });
                });
            }

            window.saveExtractedData = function() {
                // Gather all selected files
                const selectedFiles = uploadFiles.filter((f, i) => {
                    const checkbox = document.querySelector(`.extract-checkbox[data-idx="${i}"]`);
                    return checkbox && checkbox.checked;
                });
                if (selectedFiles.length === 0) {
                    alert('No files selected for saving extracted data.');
                    return;
                }
                // Send save request to backend for each selected file
                selectedFiles.forEach((f, i) => {
                    if (!f.uuid) {
                        alert('Error: No UUID found for ' + (f.name || f.uuid));
                        return;
                    }
                    $.ajax({
                        url: '/file/save-extracted-data',
                        method: 'POST',
                        contentType: 'application/json',
                        data: JSON.stringify({ uuid: f.uuid }),
                        success: function(res) {
                            alert('Extracted data saved successfully for ' + (f.name || f.uuid));
                        },
                        error: function(xhr) {
                            alert('Error saving extracted data for ' + (f.name || f.uuid) + ': ' + (xhr.responseText || 'Error'));
                        }
                    });
                });
            }

            // DOM refs
            const chatMessages = document.getElementById('chat-messages');
            const chatInput = document.getElementById('chat-input');
            const sendBtn = document.getElementById('send-btn');
            const chatForm = document.getElementById('chat-input-section');
            const historyList = document.getElementById('history-list');
            const convNameInput = document.getElementById('conversation-name');
            const newConvBtn = document.getElementById('new-conv-btn');
            const menuItems = document.querySelectorAll('.menu-item');
            const modalOverlay = document.getElementById('modal-overlay');
            const modalTitle = document.getElementById('modal-title');
            const modalClose = document.getElementById('modal-close');

            function renderMessages() {
                const conv = conversations[currentConversationIdx];
                chatMessages.innerHTML = '';
                conv.messages.forEach(msg => {
                    const msgDiv = document.createElement('div');
                    msgDiv.className = 'message' + (msg.userRole === userRole ? ' user' : '');
                    msgDiv.innerHTML = `
                        <div class="meta">${msg.username} • ${msg.timestamp}</div>
                        <div class="bubble">${msg.message}</div>
                    `;
                    chatMessages.appendChild(msgDiv);
                });
                chatMessages.scrollTop = chatMessages.scrollHeight;
            }

            function renderHistory() {
                historyList.innerHTML = '';
                conversations.forEach((conv, idx) => {
                    const lastMsg = conv.messages[conv.messages.length-1];
                    const preview = lastMsg ? truncate(lastMsg.message, 32) : '';
                    const time = lastMsg ? lastMsg.timestamp : '';
                    const div = document.createElement('div');
                    div.className = 'history-item' + (idx === currentConversationIdx ? ' active' : '');
                    div.innerHTML = `
                        <div class="conv-name">${conv.name}</div>
                        <div class="conv-preview">${preview}</div>
                        <div class="conv-time">${time}</div>
                        <div class="conv-menu-btn" title="More">&#x22EE;</div>
                    `;
                    div.addEventListener('click', (e) => {
                        // Only select if not clicking menu
                        if (!e.target.classList.contains('conv-menu-btn')) {
                            currentConversationIdx = idx;
                            renderAll();
                        }
                    });
                    // Submenu logic
                    const menuBtn = div.querySelector('.conv-menu-btn');
                    menuBtn.addEventListener('click', (e) => {
                        e.stopPropagation();
                        showConvMenu(menuBtn, idx);
                    });
                    historyList.appendChild(div);
                });
            }

            // Submenu for conversation actions
            let convMenuDiv = null;
            function showConvMenu(btn, idx) {
                if (convMenuDiv) convMenuDiv.remove();
                convMenuDiv = document.createElement('div');
                convMenuDiv.className = 'conv-submenu';
                convMenuDiv.innerHTML = `<div class="submenu-item">Delete this conversation</div>`;
                document.body.appendChild(convMenuDiv);
                const rect = btn.getBoundingClientRect();
                convMenuDiv.style.position = 'fixed';
                convMenuDiv.style.left = (rect.right - 160) + 'px';
                convMenuDiv.style.top = rect.bottom + 'px';
                convMenuDiv.style.zIndex = 2000;
                convMenuDiv.onclick = (e) => e.stopPropagation();
                convMenuDiv.querySelector('.submenu-item').onclick = function() {
                    if (conversations.length === 1) {
                        conversations[0].messages = [];
                        conversations[0].name = 'New Conversation';
                    } else {
                        conversations.splice(idx, 1);
                        if (currentConversationIdx >= conversations.length) currentConversationIdx = conversations.length - 1;
                    }
                    saveConversations();
                    renderAll();
                    convMenuDiv.remove();
                };
                document.addEventListener('click', hideConvMenu, { once: true });
            }
            function hideConvMenu() {
                if (convMenuDiv) convMenuDiv.remove();
                convMenuDiv = null;
            }

            function renderAll() {
                renderMessages();
                renderHistory();
                convNameInput.value = conversations[currentConversationIdx].name;
                saveConversations();
            }

            renderAll();

            chatInput.addEventListener('input', function() {
                sendBtn.disabled = chatInput.value.trim().length === 0;
            });

            chatForm.addEventListener('submit', function(e) {
                e.preventDefault();
                const text = chatInput.value.trim();
                if (!text) return;

                const userMsg = {
                    uuid: uuidv4(),
                    username: username,
                    userRole: userRole,
                    message: text,
                    timestamp: new Date().toLocaleTimeString()
                };
                conversations[currentConversationIdx].messages.push(userMsg);
                renderAll(); // Optimistically render user message

                chatInput.value = '';
                sendBtn.disabled = true;
                chatInput.focus();

                // Add a thinking indicator for the assistant
                const thinkingMsg = {
                    uuid: uuidv4(),
                    username: copilotName,
                    userRole: copilotRole,
                    message: "Thinking...", // Or use a loading spinner/animation
                    timestamp: new Date().toLocaleTimeString(),
                    isThinking: true // Custom flag
                };
                conversations[currentConversationIdx].messages.push(thinkingMsg);
                renderMessages(); // Re-render messages to show "Thinking..."

                // Call backend to get n8n response
                $.ajax({
                    url: '/api/chat/send', // New backend endpoint
                    method: 'POST',
                    contentType: 'application/json',
                    data: JSON.stringify({
                        message: text,
                        conversationId: conversations[currentConversationIdx].id
                        // You might want to send the whole conversation history
                        // messages: conversations[currentConversationIdx].messages.slice(0, -1) // Send all but the "Thinking..." message
                    }),
                    success: function(response) {
                        // Remove "Thinking..." message
                        const lastMessageIdx = conversations[currentConversationIdx].messages.length - 1;
                        if (conversations[currentConversationIdx].messages[lastMessageIdx].isThinking) {
                            conversations[currentConversationIdx].messages.pop();
                        }

                        const assistantMsg = {
                            uuid: uuidv4(),
                            username: copilotName,
                            userRole: copilotRole,
                            message: response.reply || "Sorry, I couldn\'t get a response.",
                            timestamp: new Date().toLocaleTimeString()
                        };
                        conversations[currentConversationIdx].messages.push(assistantMsg);
                        renderAll();
                    },
                    error: function(xhr) {
                        // Remove "Thinking..." message
                        const lastMessageIdx = conversations[currentConversationIdx].messages.length - 1;
                        if (conversations[currentConversationIdx].messages[lastMessageIdx].isThinking) {
                            conversations[currentConversationIdx].messages.pop();
                        }

                        const errorMsg = {
                            uuid: uuidv4(),
                            username: copilotName,
                            userRole: copilotRole,
                            message: "Error: Could not connect to the assistant. " + (xhr.responseText || ""),
                            timestamp: new Date().toLocaleTimeString(),
                            isError: true
                        };
                        conversations[currentConversationIdx].messages.push(errorMsg);
                        renderAll();
                        console.error("Error sending message to backend:", xhr);
                    },
                    complete: function() {
                        // Ensure input is re-enabled if needed, though it's handled by renderAll usually
                        chatInput.focus();
                    }
                });
            });

            convNameInput.addEventListener('change', function() {
                conversations[currentConversationIdx].name = convNameInput.value.trim() || 'Untitled Conversation';
                renderAll();
            });

            newConvBtn.addEventListener('click', function() {
                conversations.unshift({
                    id: uuidv4(),
                    name: 'New Conversation',
                    messages: []
                });
                currentConversationIdx = 0;
                renderAll();
            });

            menuItems.forEach(item => {
                item.addEventListener('click', function() {
                    if (DEMO_MODE) {
                        // Show a tooltip or message if desired
                        item.title = 'This feature is disabled in demo mode.';
                        return;
                    }
                    if (item.dataset.menu === 'upload') {
                        showUploadModal();
                    } else if (item.dataset.menu === 'admin') {
                        showAdminModal();
                    } else {
                        modalTitle.textContent = item.textContent;
                        document.getElementById('modal-content').innerHTML = '';
                        modalOverlay.style.display = 'flex';
                    }
                });
            });

            // Fetch current config on load
            fetch('/file/cmd/get-config', { credentials: 'same-origin' })
                .then(r => r.json())
                .then(cfg => {
                    window.MAX_UPLOAD_SIZE_MB = cfg.maxUploadSizeMB || 200;
                    window.PDF_TO_IMAGE_DPI = cfg.pdfToImageDpi || 150;
                    window.MAX_CONVERT_THREADS = cfg.maxConvertThreads || 20;
                })
                .catch(()=>{
                    window.MAX_UPLOAD_SIZE_MB = 200;
                    window.PDF_TO_IMAGE_DPI = 150;
                    window.MAX_CONVERT_THREADS = 20;
                });

            // --- Admin Console Section Switcher (simpler, jQuery-based) ---
            function showAdminModal() {
                modalTitle.textContent = 'Admin Console';
                document.getElementById('modal-content').innerHTML = `
                    <div style="margin-bottom:12px;">
                        <label for="admin-section-select" style="font-weight:bold;">Configuration Section:</label>
                        <select id="admin-section-select">
                            <option value="pdf2png">PDF-to-PNG Conversion</option>
                            <option value="datastorage">Data Storage Management</option>
                            <option value="extraction">Data Extraction Management</option>
                        </select>
                    </div>
                    <div id="section-pdf2png">
                        <form id="admin-config-form" style="margin-bottom:18px;">
                          <div style="margin-bottom:10px;">
                            <label for="max-upload-size-mb" style="font-weight:bold;">Max Upload Size (MB)</label><br>
                            <input id="max-upload-size-mb" name="maxUploadSizeMB" type="number" min="1" max="500" step="1" value="${window.MAX_UPLOAD_SIZE_MB||200}" style="width:80px;"> 
                            <span style="color:#888;font-size:0.95em;">(Example: 1–500 MB, default 200)</span>
                          </div>
                          <div style="margin-bottom:10px;">
                            <label for="pdf-to-image-dpi" style="font-weight:bold;">PDF to Image DPI</label><br>
                            <input id="pdf-to-image-dpi" name="pdfToImageDpi" type="number" min="72" max="600" step="1" value="${window.PDF_TO_IMAGE_DPI||150}" style="width:80px;">
                            <span style="color:#888;font-size:0.95em;">(Example: 72, 150, 200, 300, 600; default 150)</span>
                          </div>
                          <div style="margin-bottom:10px;">
                            <label for="max-convert-threads" style="font-weight:bold;">Max Conversion Threads</label><br>
                            <input id="max-convert-threads" name="maxConvertThreads" type="number" min="1" max="64" step="1" value="${window.MAX_CONVERT_THREADS||20}" style="width:80px;">
                            <span style="color:#888;font-size:0.95em;">(Example: 1–64, default 20; higher = faster, but uses more CPU/RAM)</span>
                          </div>
                          <button type="submit" class="upload-btn" style="margin-bottom:8px;">Apply Config</button>
                          <div id="admin-config-result" style="margin-top:6px;color:#2e7d32;"></div>
                        </form>
                    </div>
                    <div id="section-datastorage" style="display:none;">
                        <button id="cleanup-btn">Cleanup All Uploaded Data</button>
                        <div id="admin-result" style="margin-top:12px;color:#d32f2f;"></div>
                    </div>
                    <div id="section-extraction" style="display:none;">
                        <div style="margin-bottom:10px;font-weight:bold;">Extraction Settings</div>
                        <form id="extraction-config-form" style="margin-bottom:12px;">
                            <label>Window Size (tokens): <input id="window-size-tokens" type="number" min="1000" max="32000" step="1000" style="width:80px;" /></label>
                            <label style="margin-left:16px;">Step Size (tokens): <input id="step-size-tokens" type="number" min="500" max="32000" step="500" style="width:80px;" /></label>
                            <label style="margin-left:16px;">Extraction Types:
                                <select id="extraction-types" multiple style="min-width:120px;">
                                    <option value="raw">Raw Paragraphs</option>
                                    <option value="semantic">Semantic/Diagram</option>
                                    <option value="qa">Q&A Generation</option>
                                    <option value="keywords">Structured Keywords</option>
                                </select>
                            </label>
                            <button type="submit" style="margin-left:16px;">Apply</button>
                            <span id="extraction-config-result" style="margin-left:10px;color:#2e7d32;"></span>
                        </form>
                        <div style="margin-bottom:10px;font-weight:bold;">Prompt List</div>
                        <div style="margin-bottom:8px;"><input type="checkbox" id="merge-prompts-checkbox"> Merge all prompts and send as single prompt</div>
                        <table id="prompt-table" style="width:100%;margin-bottom:10px;font-size:0.97em;">
                            <thead><tr><th>No.</th><th>Prompt Detail</th><th>Prompt Note</th><th>Include Image</th></tr></thead>
                            <tbody id="prompt-table-body"></tbody>
                        </table>
                        <button id="add-prompt-btn">Add Prompt</button>
                    </div>
                `;
                // jQuery-based section toggle
                $('#admin-section-select').on('change', function() {
                    const val = $(this).val();
                    $('#section-pdf2png').hide();
                    $('#section-datastorage').hide();
                    $('#section-extraction').hide();
                    if (val === 'pdf2png') $('#section-pdf2png').show();
                    if (val === 'datastorage') $('#section-datastorage').show();
                    if (val === 'extraction') {
                        $('#section-extraction').show();
                        setTimeout(renderPromptTable, 0); // Ensure DOM is ready
                    }
                });
                // Always render prompt table after modal is shown (in case extraction is default)
                setTimeout(renderPromptTable, 0);
                // Bind add prompt button
                $('#add-prompt-btn').on('click', function() {
                    promptList.push({ detail: '', note: '', includeImage: true });
                    savePromptConfig();
                    renderPromptTable();
                });
                // Bind merge checkbox
                $('#merge-prompts-checkbox').prop('checked', mergePrompts).on('change', function() {
                    mergePrompts = this.checked;
                    savePromptConfig();
                });
                // Bind cleanup button
                $('#cleanup-btn').on('click', function() {
                    fetch('/file/cmd/cleanup-upload', { method: 'POST', credentials: 'same-origin', headers: { 'Content-Type': 'application/x-www-form-urlencoded' } })
                        .then(r => r.text())
                        .then(msg => {
                            $('#admin-result').text(msg);
                        })
                        .catch(e => {
                            $('#admin-result').text('Error: ' + e);
                        });
                });
                // Bind config form submit to send config to backend
                $('#admin-config-form').on('submit', function(e) {
                    e.preventDefault();
                    const maxUploadSizeMB = parseInt($('#max-upload-size-mb').val(), 10);
                    const pdfToImageDpi = parseInt($('#pdf-to-image-dpi').val(), 10);
                    const maxConvertThreads = parseInt($('#max-convert-threads').val(), 10);
                    $.ajax({
                        url: '/file/cmd/update-config',
                        method: 'POST',
                        contentType: 'application/json',
                        data: JSON.stringify({
                            maxUploadSizeMB,
                            pdfToImageDpi,
                            maxConvertThreads
                        }),
                        success: function(res) {
                            $('#admin-config-result').text('Config updated!');
                            window.MAX_UPLOAD_SIZE_MB = maxUploadSizeMB;
                            window.PDF_TO_IMAGE_DPI = pdfToImageDpi;
                            window.MAX_CONVERT_THREADS = maxConvertThreads;
                        },
                        error: function(xhr) {
                            let msg = 'Error: ' + (xhr.responseJSON && xhr.responseJSON.error ? xhr.responseJSON.error : (xhr.responseText || 'Failed to update config'));
                            $('#admin-config-result').text(msg);
                        }
                    });
                });
                // Extraction config form logic
                fetch('/file/extraction-config', { credentials: 'same-origin' })
                    .then(r => r.json())
                    .then(cfg => {
                        $('#window-size-tokens').val(cfg.windowSizeTokens);
                        $('#step-size-tokens').val(cfg.stepSizeTokens);
                        $('#extraction-types').val(cfg.extractionTypes);
                    });
                $('#extraction-config-form').on('submit', function(e) {
                    e.preventDefault();
                    const windowSizeTokens = parseInt($('#window-size-tokens').val(), 10);
                    const stepSizeTokens = parseInt($('#step-size-tokens').val(), 10);
                    const extractionTypes = $('#extraction-types').val();
                    $.ajax({
                        url: '/file/extraction-config',
                        method: 'POST',
                        contentType: 'application/json',
                        data: JSON.stringify({ windowSizeTokens, stepSizeTokens, extractionTypes }),
                        success: function() {
                            $('#extraction-config-result').text('Extraction config updated!');
                        },
                        error: function(xhr) {
                            let msg = 'Error: ' + (xhr.responseJSON && xhr.responseJSON.error ? xhr.responseJSON.error : (xhr.responseText || 'Failed to update extraction config'));
                            $('#extraction-config-result').text(msg);
                        }
                    });
                });
                // Add Reset Prompts button to admin console
                const resetPromptBtn = document.createElement('button');
                resetPromptBtn.id = 'reset-prompt-btn';
                resetPromptBtn.textContent = 'Reset Prompts to Default';
                resetPromptBtn.style.marginLeft = '10px';
                const addPromptBtn = document.getElementById('add-prompt-btn');
                if (addPromptBtn && addPromptBtn.parentNode) {
                    addPromptBtn.parentNode.insertBefore(resetPromptBtn, addPromptBtn.nextSibling);
                }

                resetPromptBtn && resetPromptBtn.addEventListener('click', function() {
                    if (confirm('Are you sure you want to reset all prompts to the default values? This will overwrite your current prompt list.')) {
                        promptList = [
                            {
                                detail: "Extract all raw paragraphs from the document, preserving their order and structure. Output as a well-structured Markdown document. Use headings for sections, bullet points for lists, and preserve assignments and key topics as subheadings or lists.",
                                note: "Raw paragraph extraction",
                                extractionTypes: ["raw"],
                                includeImage: true
                            },
                            {
                                detail: "Analyze all diagrams, charts, and images in the window. For each, provide a concise semantic summary of its meaning and relevance to the document. Output as a JSON array of objects with fields: imageIndex, summary.",
                                note: "Semantic/diagram extraction",
                                extractionTypes: ["semantic"],
                                includeImage: true
                            },
                            {
                                detail: "Generate 3-5 high-quality Q&A pairs that cover the key concepts, facts, or reasoning in this window. Output as a JSON array of objects with fields: question, answer.",
                                note: "Q&A generation",
                                extractionTypes: ["qa"],
                                includeImage: true
                            },
                            {
                                detail: "Extract all important legal, technical, and IP-related keywords and phrases from this window. Output as a JSON array of strings.",
                                note: "Structured keyword extraction",
                                extractionTypes: ["keywords"],
                                includeImage: true
                            }
                        ];
                        savePromptConfig();
                        if (typeof renderPromptTable === 'function') renderPromptTable();
                    }
                });

                // Show the modal overlay (fix: ensure modal is visible)
                modalOverlay.style.display = 'flex';
            }

            // --- Render Prompt Table for Admin Extraction Section ---
            function renderPromptTable() {
                const tbody = document.getElementById('prompt-table-body');
                if (!tbody) return;
                tbody.innerHTML = '';
                if (!Array.isArray(promptList) || promptList.length === 0) {
                    tbody.innerHTML = '<tr><td colspan="5" style="color:#888;text-align:center;">No prompts defined. Click "Add Prompt" to create one.</td></tr>';
                    return;
                }
                const extractionTypeOptions = [
                    { value: 'raw', label: 'Raw Paragraphs' },
                    { value: 'semantic', label: 'Semantic/Diagram' },
                    { value: 'qa', label: 'Q&A Generation' },
                    { value: 'keywords', label: 'Structured Keywords' }
                ];
                promptList.forEach((p, idx) => {
                    // Ensure extractionTypes is always an array
                    if (!Array.isArray(p.extractionTypes)) p.extractionTypes = [];
                    const tr = document.createElement('tr');
                    tr.innerHTML = `
                        <td>${idx + 1}</td>
                        <td><textarea class="prompt-detail" data-idx="${idx}" style="width:98%;min-height:48px;">${p.detail || ''}</textarea></td>
                        <td><textarea class="prompt-note" data-idx="${idx}" style="width:98%;min-height:32px;">${p.note || ''}</textarea></td>
                        <td><select class="prompt-extraction-types" data-idx="${idx}" multiple style="min-width:120px;">
                            ${extractionTypeOptions.map(opt => `<option value="${opt.value}"${p.extractionTypes.includes(opt.value) ? ' selected' : ''}>${opt.label}</option>`).join('')}
                        </select></td>
                        <td><input type="checkbox" class="prompt-include-image" data-idx="${idx}" ${p.includeImage ? 'checked' : ''}></td>
                        <td><button class="delete-prompt-btn" data-idx="${idx}" title="Delete">🗑️</button></td>
                    `;
                    tbody.appendChild(tr);
                });
                // Bind input events for detail/note
                tbody.querySelectorAll('.prompt-detail').forEach(input => {
                    input.addEventListener('input', function() {
                        promptList[this.dataset.idx].detail = this.value;
                        savePromptConfig();
                    });
                });
                tbody.querySelectorAll('.prompt-note').forEach(input => {
                    input.addEventListener('input', function() {
                        promptList[this.dataset.idx].note = this.value;
                        savePromptConfig();
                    });
                });
                // Bind extraction types multi-select
                tbody.querySelectorAll('.prompt-extraction-types').forEach(sel => {
                    sel.addEventListener('change', function() {
                        const selected = Array.from(this.selectedOptions).map(opt => opt.value);
                        promptList[this.dataset.idx].extractionTypes = selected;
                        savePromptConfig();
                    });
                });
                // Bind checkbox for includeImage
                tbody.querySelectorAll('.prompt-include-image').forEach(cb => {
                    cb.addEventListener('change', function() {
                        promptList[this.dataset.idx].includeImage = this.checked;
                        savePromptConfig();
                    });
                });
                // Bind delete button
                tbody.querySelectorAll('.delete-prompt-btn').forEach(btn => {
                    btn.addEventListener('click', function() {
                        promptList.splice(this.dataset.idx, 1);
                        savePromptConfig();
                        renderPromptTable();
                    });
                });
            }

            modalClose.addEventListener('click', function() {
                modalOverlay.style.display = 'none';
            });
        };
    });
});

function updateUploadProgressLog() {
    document.getElementById('upload-progress-log').innerHTML = uploadProgress.map(l=>`<div>${l}</div>`).join('');
}
window.updateUploadProgressLog = updateUploadProgressLog;
