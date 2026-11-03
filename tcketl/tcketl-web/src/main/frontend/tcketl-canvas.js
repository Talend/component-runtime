/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Pipeline canvas (SVG, no dependency). The server owns the model: this element only draws the graph it is given
 * through setGraph(json) and reports user intents as 'canvas-event' events (detail = JSON string):
 *   {type:'select', kind:'node'|'link'|'none', ...}  {type:'move', id, x, y}  {type:'drop', connector, x, y}
 *   {type:'link', fromNode, fromFlow, toNode, toFlow}  {type:'delete'}
 * Graph: {nodes:[{id,label,x,y,role,state,icon,inputs:[{flow,label,declared}],outputs:[...]}],
 *         links:[{fromNode,fromFlow,toNode,toFlow}], selection:null|{kind,...}}
 */
const SVG = 'http://www.w3.org/2000/svg';
const WIDTH = 180;
const HEAD = 46;
const ROW = 22;
const GRID = 20;
const MIN_ZOOM = 0.25;
const MAX_ZOOM = 2;
const MIME = 'application/x-tcketl-connector';
const ROLE_COLORS = {INPUT: '#2e7d32', PROCESSOR: '#1565c0', OUTPUT: '#ef6c00', STANDALONE: '#6a1b9a'};

// Wraps a label on at most two lines, preferably after a space or a separator; the second line is ellipsized.
function wrapLabel(text, max) {
    if (text.length <= max) {
        return [text];
    }
    let cut = -1;
    for (let i = Math.min(max, text.length - 1); i > max / 2; i--) {
        if (/[\s_\-.\/]/.test(text[i - 1]) || /[\s_\-.\/]/.test(text[i])) {
            cut = text[i] === ' ' ? i : i;
            break;
        }
    }
    if (cut < 0) {
        cut = max;
    }
    const first = text.substring(0, cut).trimEnd();
    const rest = text.substring(cut).trimStart();
    return [first, rest.length > max ? rest.substring(0, max - 1) + '\u2026' : rest];
}

function svg(name, attributes, parent) {
    const element = document.createElementNS(SVG, name);
    Object.entries(attributes || {}).forEach(([key, value]) => element.setAttribute(key, value));
    if (parent) {
        parent.appendChild(element);
    }
    return element;
}

class TcketlCanvas extends HTMLElement {

    constructor() {
        super();
        this.graph = {nodes: [], links: [], selection: null};
        this.view = {x: 40, y: 40, k: 1};
        this.positions = {};
        this.drag = null;
        this.pending = null;
        this.attachShadow({mode: 'open'});
        this.shadowRoot.innerHTML = `<style>
            :host { display: block; position: relative; overflow: hidden; outline: none; min-height: 200px;
                    background-color: #fafafa; background-image: radial-gradient(#c8c8c8 1px, transparent 1px); }
            svg { width: 100%; height: 100%; display: block; cursor: grab; }
            .node rect.body { fill: #fff; stroke: #9e9e9e; stroke-width: 1; }
            .node.selected rect.body { stroke: #1976d2; stroke-width: 2.5; }
            .node.pending rect.body, .node.unresolved rect.body { stroke-dasharray: 5 3; }
            .node.unresolved rect.body { stroke: #c62828; }
            .node text { font: 12px sans-serif; fill: #212121; pointer-events: none; }
            .node text.port { font-size: 11px; fill: #424242; stroke: none; paint-order: normal;
                    text-rendering: geometricPrecision; }
            .node .head { cursor: move; }
            svg.linking, svg.linking * { cursor: crosshair !important; }
            .port { fill: #fff; stroke: #455a64; stroke-width: 2; cursor: crosshair; }
            .port.reject { stroke: #c62828; }
            .port.undeclared { stroke-dasharray: 2 2; }
            .port.target { fill: #66bb6a; }
            .link { fill: none; stroke: #607d8b; stroke-width: 2; }
            .link.reject { stroke: #c62828; stroke-dasharray: 6 3; }
            .link.selected { stroke: #1976d2; stroke-width: 3.5; }
            .hit { fill: none; stroke: transparent; stroke-width: 12; cursor: pointer; }
            .rubber { fill: none; stroke: #1976d2; stroke-width: 2; stroke-dasharray: 4 3; pointer-events: none; }
        </style><svg><g class="viewport"><g class="links"></g><g class="nodes"></g></g></svg>`;
        this.svg = this.shadowRoot.querySelector('svg');
        this.viewport = this.shadowRoot.querySelector('.viewport');
        this.linkLayer = this.shadowRoot.querySelector('.links');
        this.nodeLayer = this.shadowRoot.querySelector('.nodes');
    }

    connectedCallback() {
        this.tabIndex = 0;
        this.svg.addEventListener('pointerdown', e => this.onDown(e));
        this.svg.addEventListener('pointermove', e => this.onMove(e));
        this.svg.addEventListener('pointerup', e => this.onUp(e));
        this.svg.addEventListener('pointercancel', () => this.cancelDrag());
        this.svg.addEventListener('wheel', e => this.onWheel(e), {passive: false});
        this.addEventListener('keydown', e => {
            if (e.key === 'Delete' || e.key === 'Backspace') {
                this.emit({type: 'delete'});
            } else if (e.key === 'Escape') {
                this.cancelDrag();
            }
        });
        this.addEventListener('dragover', e => {
            if (e.dataTransfer && Array.from(e.dataTransfer.types).includes(MIME)) {
                e.preventDefault();
                e.dataTransfer.dropEffect = 'copy';
            }
        });
        this.addEventListener('drop', e => {
            const connector = e.dataTransfer && e.dataTransfer.getData(MIME);
            if (connector) {
                e.preventDefault();
                const at = this.toWorld(e.clientX, e.clientY);
                this.emit({type: 'drop', connector, x: at.x, y: at.y});
            }
        });
        this.applyView();
        this.render();
    }

    editLabel(id) {
        const node = this.graph.nodes.find(n => n.id === id);
        if (!node) {
            return;
        }
        this.closeEditor();
        const input = document.createElement('input');
        input.value = node.label;
        input.setAttribute('style', `position:absolute;box-sizing:border-box;z-index:2;font:bold 12px sans-serif;`
            + `left:${this.view.x + node.x * this.view.k}px;top:${this.view.y + node.y * this.view.k}px;`
            + `width:${WIDTH * this.view.k}px;height:${HEAD * this.view.k}px;padding:0 8px;`
            + 'border:2px solid #1976d2;border-radius:6px;outline:none;');
        let done = false;
        const finish = commit => {
            if (done) {
                return;
            }
            done = true;
            const label = input.value.trim();
            this.editor = null;
            input.remove();
            if (commit && label && label !== node.label) {
                this.emit({type: 'rename', id, label});
            }
            this.focus();
        };
        input.addEventListener('keydown', e => {
            e.stopPropagation();
            if (e.key === 'Enter') {
                finish(true);
            } else if (e.key === 'Escape') {
                finish(false);
            }
        });
        input.addEventListener('pointerdown', e => e.stopPropagation());
        input.addEventListener('blur', () => finish(true));
        this.editor = input;
        this.shadowRoot.appendChild(input);
        input.focus();
        input.select();
    }

    closeEditor() {
        if (this.editor) {
            this.editor.remove();
            this.editor = null;
        }
    }

    emit(detail) {
        this.dispatchEvent(new CustomEvent('canvas-event', {detail: JSON.stringify(detail)}));
    }

    setGraph(json) {
        const graph = JSON.parse(json);
        if (this.drag && this.drag.kind !== 'pan') {
            this.pending = graph;
            return;
        }
        this.graph = graph;
        this.render();
    }

    zoomBy(factor) {
        const rect = this.getBoundingClientRect();
        this.zoomAt(rect.width / 2, rect.height / 2, factor);
    }

    resetView() {
        this.view = {x: 40, y: 40, k: 1};
        this.applyView();
    }

    zoomAt(px, py, factor) {
        const k = Math.min(MAX_ZOOM, Math.max(MIN_ZOOM, this.view.k * factor));
        const ratio = k / this.view.k;
        this.view = {x: px - (px - this.view.x) * ratio, y: py - (py - this.view.y) * ratio, k};
        this.applyView();
    }

    applyView() {
        const {x, y, k} = this.view;
        this.viewport.setAttribute('transform', `translate(${x} ${y}) scale(${k})`);
        this.style.backgroundSize = `${GRID * k}px ${GRID * k}px`;
        this.style.backgroundPosition = `${x}px ${y}px`;
    }

    toWorld(clientX, clientY) {
        const rect = this.getBoundingClientRect();
        return {x: (clientX - rect.left - this.view.x) / this.view.k, y: (clientY - rect.top - this.view.y) / this.view.k};
    }

    height(node) {
        return HEAD + Math.max(node.inputs.length, node.outputs.length, 1) * ROW + 8;
    }

    portPoint(nodeId, flow, output) {
        const node = this.graph.nodes.find(n => n.id === nodeId);
        const position = this.positions[nodeId];
        if (!node || !position) {
            return null;
        }
        const ports = output ? node.outputs : node.inputs;
        const index = Math.max(0, ports.findIndex(p => p.flow === flow));
        return {x: position.x + (output ? WIDTH : 0), y: position.y + HEAD + ROW * index + ROW / 2};
    }

    isSelectedNode(node) {
        const s = this.graph.selection;
        return !!s && s.kind === 'node' && s.id === node.id;
    }

    isSelectedLink(link) {
        const s = this.graph.selection;
        return !!s && s.kind === 'link' && s.fromNode === link.fromNode && s.fromFlow === link.fromFlow
            && s.toNode === link.toNode && s.toFlow === link.toFlow;
    }

    render() {
        this.nodeLayer.replaceChildren();
        this.positions = {};
        this.graph.nodes.forEach(node => {
            this.positions[node.id] = {x: node.x, y: node.y};
            this.drawNode(node);
        });
        this.drawLinks();
    }

    drawNode(node) {
        const height = this.height(node);
        const classes = ['node', node.state || 'resolved'];
        if (this.isSelectedNode(node)) {
            classes.push('selected');
        }
        const g = svg('g', {class: classes.join(' '), transform: `translate(${node.x} ${node.y})`}, this.nodeLayer);
        g.dataset.node = node.id;
        svg('rect', {class: 'body', width: WIDTH, height, rx: 6}, g);
        svg('rect', {class: 'head', width: WIDTH, height: HEAD, rx: 6, fill: ROLE_COLORS[node.role] || '#546e7a'}, g);
        let labelX = 10;
        if (node.icon) {
            svg('circle', {cx: 29, cy: HEAD / 2, r: 21, fill: '#fff', stroke: 'rgba(0,0,0,0.25)'}, g);
            svg('image', {href: node.icon, x: 9, y: HEAD / 2 - 20, width: 40, height: 40}, g);
            labelX = 58;
        }
        const lines = wrapLabel(node.label, Math.floor((WIDTH - labelX - 8) / 6.6));
        const label = svg('text', {x: labelX, y: lines.length > 1 ? HEAD / 2 - 3 : HEAD / 2 + 4,
            style: 'fill:#fff;font-weight:bold'}, g);
        lines.forEach((line, i) => {
            const span = svg('tspan', {x: labelX, dy: i === 0 ? 0 : 14}, label);
            span.textContent = line;
        });
        svg('title', {}, label).textContent = node.label;
        node.inputs.forEach((port, i) => this.drawPort(g, node, port, i, false));
        node.outputs.forEach((port, i) => this.drawPort(g, node, port, i, true));
    }

    drawPort(g, node, port, index, output) {
        const cy = HEAD + ROW * index + ROW / 2;
        const classes = ['port'];
        if (port.flow === 'REJECT') {
            classes.push('reject');
        }
        if (!port.declared) {
            classes.push('undeclared');
        }
        const circle = svg('circle', {class: classes.join(' '), cx: output ? WIDTH : 0, cy, r: 6}, g);
        circle.dataset.node = node.id;
        circle.dataset.flow = port.flow;
        circle.dataset.dir = output ? 'out' : 'in';
        svg('title', {}, circle).textContent = port.flow;
        const text = svg('text', {class: 'port', x: output ? WIDTH - 10 : 10, y: cy + 3,
            'text-anchor': output ? 'end' : 'start'}, g);
        text.textContent = port.label;
    }

    drawLinks() {
        this.linkLayer.replaceChildren();
        this.graph.links.forEach(link => {
            const from = this.portPoint(link.fromNode, link.fromFlow, true);
            const to = this.portPoint(link.toNode, link.toFlow, false);
            if (!from || !to) {
                return;
            }
            const d = this.curve(from, to);
            const classes = ['link'];
            if (link.fromFlow === 'REJECT') {
                classes.push('reject');
            }
            if (this.isSelectedLink(link)) {
                classes.push('selected');
            }
            svg('path', {class: classes.join(' '), d}, this.linkLayer);
            const hit = svg('path', {class: 'hit', d}, this.linkLayer);
            hit.dataset.link = JSON.stringify(link);
        });
    }

    curve(from, to) {
        const reach = Math.max(40, Math.abs(to.x - from.x) / 2);
        return `M ${from.x} ${from.y} C ${from.x + reach} ${from.y} ${to.x - reach} ${to.y} ${to.x} ${to.y}`;
    }

    onDown(e) {
        if (e.button !== 0) {
            return;
        }
        this.focus();
        const target = e.target;
        const port = target.closest && target.closest('.port');
        const nodeElement = target.closest && target.closest('.node');
        const hit = target.closest && target.closest('.hit');
        this.svg.setPointerCapture(e.pointerId);
        if (port && port.dataset.dir === 'out') {
            this.drag = {kind: 'link', node: port.dataset.node, flow: port.dataset.flow,
                rubber: svg('path', {class: 'rubber'}, this.viewport)};
            this.svg.classList.add('linking');
        } else if (nodeElement) {
            const id = nodeElement.dataset.node;
            const at = this.toWorld(e.clientX, e.clientY);
            const start = this.positions[id];
            this.drag = {kind: 'move', id, element: nodeElement, dx: at.x - start.x, dy: at.y - start.y, moved: false};
            this.emit({type: 'select', kind: 'node', id});
        } else if (hit) {
            const link = JSON.parse(hit.dataset.link);
            this.emit(Object.assign({type: 'select', kind: 'link'}, link));
        } else {
            this.drag = {kind: 'pan', x: e.clientX, y: e.clientY, view: Object.assign({}, this.view)};
            if (this.graph.selection) {
                this.emit({type: 'select', kind: 'none'});
            }
        }
    }

    onMove(e) {
        const drag = this.drag;
        if (!drag) {
            return;
        }
        if (drag.kind === 'pan') {
            this.view = {x: drag.view.x + e.clientX - drag.x, y: drag.view.y + e.clientY - drag.y, k: drag.view.k};
            this.applyView();
        } else if (drag.kind === 'move') {
            const at = this.toWorld(e.clientX, e.clientY);
            const x = Math.max(GRID, at.x - drag.dx);
            const y = Math.max(GRID, at.y - drag.dy);
            drag.moved = true;
            this.positions[drag.id] = {x, y};
            drag.element.setAttribute('transform', `translate(${x} ${y})`);
            this.drawLinks();
        } else if (drag.kind === 'link') {
            const from = this.portPoint(drag.node, drag.flow, true);
            const to = this.toWorld(e.clientX, e.clientY);
            drag.rubber.setAttribute('d', this.curve(from, to));
            this.shadowRoot.querySelectorAll('.port.target').forEach(p => p.classList.remove('target'));
            const over = this.inputPortAt(e);
            if (over) {
                over.classList.add('target');
            }
        }
    }

    inputPortAt(e) {
        const element = this.shadowRoot.elementFromPoint(e.clientX, e.clientY);
        const port = element && element.closest ? element.closest('.port') : null;
        return port && port.dataset.dir === 'in' ? port : null;
    }

    onUp(e) {
        const drag = this.drag;
        if (!drag) {
            return;
        }
        this.drag = null;
        this.svg.classList.remove('linking');
        if (drag.kind === 'move' && drag.moved) {
            const p = this.positions[drag.id];
            this.emit({type: 'move', id: drag.id, x: Math.round(p.x / GRID) * GRID, y: Math.round(p.y / GRID) * GRID});
        } else if (drag.kind === 'move') {
            const now = Date.now();
            const again = this.lastTap && this.lastTap.id === drag.id && now - this.lastTap.at < 450;
            this.lastTap = again ? null : {id: drag.id, at: now};
            if (again) {
                this.flush();
                this.editLabel(drag.id);
                return;
            }
        } else if (drag.kind === 'link') {
            const target = this.inputPortAt(e);
            drag.rubber.remove();
            this.shadowRoot.querySelectorAll('.port.target').forEach(p => p.classList.remove('target'));
            if (target) {
                this.emit({type: 'link', fromNode: drag.node, fromFlow: drag.flow, toNode: target.dataset.node,
                    toFlow: target.dataset.flow});
            }
        }
        this.flush();
    }

    cancelDrag() {
        if (this.drag && this.drag.kind === 'link') {
            this.drag.rubber.remove();
        }
        this.drag = null;
        this.svg.classList.remove('linking');
        this.flush();
    }

    flush() {
        if (this.pending) {
            this.graph = this.pending;
            this.pending = null;
        }
        this.render();
    }

    onWheel(e) {
        e.preventDefault();
        const rect = this.getBoundingClientRect();
        this.zoomAt(e.clientX - rect.left, e.clientY - rect.top, e.deltaY < 0 ? 1.1 : 1 / 1.1);
    }
}

customElements.define('tcketl-canvas', TcketlCanvas);
