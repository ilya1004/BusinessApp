// =========================================================
// CONSOLE BRIDGE → FILE LOGGER (deferred initialization)
// =========================================================
(function() {
    const queue = [];
    const MAX_QUEUE = 2000;
    let isBridgeReady = false;
    let isLogging = false;

    function flush() {
        if (!isBridgeReady || !window.javaBridge || typeof window.javaBridge.log !== 'function') return;
        while (queue.length > 0) {
            const entry = queue.shift();
            try { window.javaBridge.log(entry.level, entry.msg); } catch(e) {}
        }
    }

    function intercept(level) {
        const orig = console[level];
        console[level] = function(...args) {
            if (isLogging) return orig.apply(console, args);
            isLogging = true;
            try {
                orig.apply(console, args);

                if (!isBridgeReady || !window.javaBridge || typeof window.javaBridge.log !== 'function') {
                    const msg = args.map(a => {
                        try { return typeof a === 'object' && a !== null ? JSON.stringify(a) : String(a); }
                        catch(e) { return '[Unserializable]'; }
                    }).join(' ');
                    queue.push({ level: level.toUpperCase(), msg });
                    if (queue.length > MAX_QUEUE) queue.shift();
                } else {
                    const msg = args.map(a => {
                        try { return typeof a === 'object' && a !== null ? JSON.stringify(a) : String(a); }
                        catch(e) { return '[Unserializable]'; }
                    }).join(' ');
                    window.javaBridge.log(level.toUpperCase(), msg);
                }
            } finally {
                isLogging = false;
            }
        };
    }

    ['log','error','warn','info','debug'].forEach(intercept);

    // Called from Java after javaBridge is registered
    window.__enableConsoleBridge = function() {
        isBridgeReady = true;
        flush();
    };

    // Fallback: if Java didn't call __enableConsoleBridge, try ourselves
    const checker = setInterval(() => {
        if (window.javaBridge && typeof window.javaBridge.log === 'function' && !isBridgeReady) {
            isBridgeReady = true;
            flush();
            clearInterval(checker);
        }
    }, 500);
    setTimeout(() => clearInterval(checker), 10000);
})();

// =========================================================
// BPMN MODELER
// =========================================================
let bpmnModeler;

async function createModeler() {
    const props = window.BpmnJSPropertiesPanel || {};
    const additionalModules = [];

    if (props.BpmnPropertiesPanelModule) {
        additionalModules.push(
            props.BpmnPropertiesPanelModule,
            props.BpmnPropertiesProviderModule
        );
    }

    try {
        bpmnModeler = new BpmnJS({
            container: '#canvas',
            keyboard: true,
            additionalModules: additionalModules
        });
        console.log('✅ BpmnJS instance created');
        setupElementListeners();
        await loadEmptyDiagram();
    } catch (err) {
        console.error('❌ BpmnJS init failed:', err);
    }
}

async function loadEmptyDiagram() {
    const emptyBpmn = `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                  id="Definitions_1" targetNamespace="http://bpmn.io/schema/bpmn">
  <bpmn:process id="Process_1" isExecutable="true">
    <bpmn:startEvent id="StartEvent_1"/>
  </bpmn:process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="Process_1"/>
  </bpmndi:BPMNDiagram>
</bpmn:definitions>`;

    try {
        await bpmnModeler.importXML(emptyBpmn);
        console.log('✅ Empty diagram loaded successfully');
    } catch (err) {
        console.error('❌ Failed to load empty diagram:', err.message);
    }
}

function setupElementListeners() {
    var eventBus = bpmnModeler.get('eventBus');

    eventBus.on('commandStack.shape.create.executed', function(event) {
        var context = event.context;
        var shape = context.shape;
        if (shape && shape.businessObject) {
            sendElementInfo(shape, 'created');
        }
    });

    eventBus.on('commandStack.connection.create.executed', function(event) {
        var context = event.context;
        var connection = context.connection;
        if (connection && connection.businessObject) {
            sendElementInfo(connection, 'connected');
        }
    });

    eventBus.on('element.changed', function(event) {
        var element = event.element;
        if (element && element.type !== 'label' && element.businessObject) {
            sendElementInfo(element, 'changed');
        }
    });

    eventBus.on('selection.changed', function(event) {
        var selected = event.newSelection;
        if (selected && selected.length > 0) {
            var element = selected[0];
            if (element.type !== 'label' && element.businessObject) {
                sendElementSelected(element);
            }
        }
    });
}

function sendElementInfo(element, action) {
    var info = {
        action: action,
        id: element.id,
        type: element.businessObject.$type,
        name: element.businessObject.name || '',
        x: element.x || 0,
        y: element.y || 0,
        width: element.width || 0,
        height: element.height || 0
    };
    try {
        window.javaBridge.onElementCreated(JSON.stringify(info));
    } catch(e) {
        console.error('Bridge error:', e);
    }
}

function sendElementSelected(element) {
    var info = {
        id: element.id,
        type: element.businessObject.$type,
        name: element.businessObject.name || '',
        x: element.x || 0,
        y: element.y || 0,
        width: element.width || 0,
        height: element.height || 0
    };
    try {
        window.javaBridge.onElementSelected(JSON.stringify(info));
    } catch(e) {
        console.error('Bridge error:', e);
    }
}

// Global functions (called from Java)
window.saveXML = function() {
    if (!bpmnModeler) return console.error('saveXML: bpmnModeler not ready');
    bpmnModeler.saveXML({ format: true }).then(function(result) {
        if (window.javaBridge && window.javaBridge.onXMLSaved) {
            window.javaBridge.onXMLSaved(result.xml);
        }
    }).catch(function(err) {
        console.error('saveXML failed:', err);
    });
};

window.exportSVG = function() {
    if (!bpmnModeler) return console.error('exportSVG: bpmnModeler not ready');
    bpmnModeler.saveSVG().then(function(result) {
        if (window.javaBridge && window.javaBridge.onSVGExported) {
            window.javaBridge.onSVGExported(result.svg);
        }
    }).catch(function(err) {
        console.error('exportSVG failed:', err);
    });
};

window.importXML = function(xmlContent) {
    if (!bpmnModeler) return console.error('importXML: bpmnModeler not ready');
    bpmnModeler.importXML(xmlContent).then(function() {
        console.log('XML imported successfully');
    }).catch(function(err) {
        console.error('importXML failed:', err.message);
    });
};

// Initialization
document.addEventListener('DOMContentLoaded', createModeler);