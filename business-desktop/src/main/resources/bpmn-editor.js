let bpmnModeler;

async function createModeler() {
    bpmnModeler = new BpmnJS({
        container: '#canvas',
        keyboard: {
            bindTo: document
        },
        // Можно добавить дополнительные модули позже (properties panel, minimap и т.д.)
    });

    // Загружаем пустую диаграмму при запуске
    await loadEmptyDiagram();
}

async function loadEmptyDiagram() {
    const emptyBpmn = `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                  id="Definitions_1"
                  targetNamespace="http://bpmn.io/schema/bpmn">
  <bpmn:process id="Process_1" isExecutable="true">
    <bpmn:startEvent id="StartEvent_1"/>
  </bpmn:process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="Process_1"/>
  </bpmndi:BPMNDiagram>
</bpmn:definitions>`;

    try {
        await bpmnModeler.importXML(emptyBpmn);
        console.log('Пустая BPMN-диаграмма загружена');
    } catch (err) {
        console.error('Ошибка импорта пустой диаграммы', err);
    }
}

// ====================== Java Bridge ======================

window.javaBridge = {

    /** Загрузить BPMN XML из Java */
    importXML: async function(xmlContent) {
        try {
            await bpmnModeler.importXML(xmlContent);
            console.log('Диаграмма успешно импортирована');
        } catch (err) {
            console.error('Ошибка импорта XML:', err);
            alert('Ошибка при загрузке диаграммы: ' + err.message);
        }
    },

    /** Сохранить текущую диаграмму и отправить XML обратно в Java */
    saveXML: function() {
        bpmnModeler.saveXML({ format: true }, (err, xml) => {
            if (err) {
                console.error('Ошибка сохранения XML', err);
                return;
            }
            if (window.javaBridge && window.javaBridge.onXMLSaved) {
                window.javaBridge.onXMLSaved(xml);
            }
        });
    },

    /** Экспорт как SVG */
    exportSVG: function() {
        bpmnModeler.saveSVG((err, svg) => {
            if (!err && window.javaBridge && window.javaBridge.onSVGExported) {
                window.javaBridge.onSVGExported(svg);
            }
        });
    },

    /** Получить текущий XML (для асинхронных вызовов) */
    getXML: async function() {
        const result = await bpmnModeler.saveXML({ format: true });
        return result.xml;
    }
};

// Инициализация после загрузки страницы
document.addEventListener('DOMContentLoaded', createModeler);