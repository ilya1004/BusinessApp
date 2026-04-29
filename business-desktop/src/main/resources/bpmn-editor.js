// =========================================================
// CONSOLE BRIDGE → FILE LOGGER (отложенная инициализация)
// =========================================================
(function() {
    const queue = [];
    const MAX_QUEUE = 2000;
    let isBridgeReady = false;

    function flush() {
        if (!isBridgeReady || !window.javaBridge?.log) return;
        while (queue.length > 0) {
            const entry = queue.shift();
            try { window.javaBridge.log(entry.level, entry.msg); } catch(e) {}
        }
    }

    function intercept(level) {
        const orig = console[level];
        console[level] = function(...args) {
            orig.apply(console, args); // Оставляем родную консоль для WebView

            const msg = args.map(a => {
                try { return typeof a === 'object' && a !== null ? JSON.stringify(a) : String(a); }
                catch(e) { return '[Unserializable]'; }
            }).join(' ');

            if (isBridgeReady && window.javaBridge?.log) {
                try { window.javaBridge.log(level.toUpperCase(), msg); } catch(e) {}
            } else {
                queue.push({ level: level.toUpperCase(), msg });
                if (queue.length > MAX_QUEUE) queue.shift();
            }
        };
    }

    ['log','error','warn','info','debug'].forEach(intercept);

    // Функция вызывается из Java после регистрации javaBridge
    window.__enableConsoleBridge = function() {
        isBridgeReady = true;
        console.log('🔌 JS Console Bridge activated. Logs will be written to java console.log file.');
        flush();
    };

    // Fallback: если Java не вызвала __enableConsoleBridge, пробуем сами
    const checker = setInterval(() => {
        if (window.javaBridge?.log && !isBridgeReady) {
            isBridgeReady = true;
            flush();
            clearInterval(checker);
            console.log('🔍 Auto-detected javaBridge. Bridge activated.');
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
        console.log('✅ Properties Panel modules loaded');
    } else {
        console.warn('⚠️ Properties Panel not found. Skipping.');
    }

    try {
        bpmnModeler = new BpmnJS({
            container: '#canvas',
            // ❌ Убрали keyboard: { bindTo: document } → вызывает warning в UMD
            keyboard: true,
            propertiesPanel: { parent: '#properties-panel' },
            additionalModules: additionalModules
        });
        console.log('✅ BpmnJS instance created');
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

// ====================== Глобальные функции (вызов из Java) ======================
window.saveXML = async function() {
    console.log("window.saveXML called");
    if (!bpmnModeler) {
        return console.error('❌ saveXML: bpmnModeler not ready');
    }
    try {
        // ✅ bpmn-js v18 возвращает Promise, а не вызывает callback
        const result = await bpmnModeler.saveXML({ format: true });
        console.log('📄 XML generated, sending to Java...');
        if (window.javaBridge?.onXMLSaved) {
            window.javaBridge.onXMLSaved(result.xml);
        }
    } catch (err) {
        console.error('❌ saveXML failed:', err.message || err);
    }
};

window.exportSVG = async function() {
    console.log("window.exportSVG called");
    if (!bpmnModeler) {
        return console.error('❌ exportSVG: bpmnModeler not ready');
    }
    try {
        const result = await bpmnModeler.saveSVG();
        console.log('🖼️ SVG generated, sending to Java...');
        if (window.javaBridge?.onSVGExported) {
            window.javaBridge.onSVGExported(result.svg);
        }
    } catch (err) {
        console.error('❌ exportSVG failed:', err.message || err);
    }
};

window.importXML = async function(xmlContent) {
    if (!bpmnModeler) return console.error('❌ importXML: bpmnModeler not ready');
    try {
        await bpmnModeler.importXML(xmlContent);
        console.log('✅ XML imported successfully');
    } catch (err) {
        console.error('❌ importXML failed:', err.message);
    }
};

// Инициализация
document.addEventListener('DOMContentLoaded', createModeler);