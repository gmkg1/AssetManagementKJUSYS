
const fs = require('fs');

function rgbaToHex(r, g, b, a) {
    const toHex = (n) => {
        const hex = Math.round(n * 255).toString(16);
        return hex.length === 1 ? '0' + hex : hex;
    };
    const hex = `#${toHex(r)}${toHex(g)}${toHex(b)}`;
    return a !== 1 ? `${hex}${toHex(a)}` : hex;
}

function processNode(node, indent = 0) {
    const spaces = ' '.repeat(indent);
    let output = `${spaces}${node.name} (${node.type})
`;

    // Layout
    if (node.layoutMode) {
        output += `${spaces}  Layout: ${node.layoutMode} | Gap: ${node.itemSpacing} | Pad: ${node.paddingTop} ${node.paddingRight} ${node.paddingBottom} ${node.paddingLeft}
`;
        output += `${spaces}  Align: ${node.primaryAxisAlignItems} / ${node.counterAxisAlignItems}
`;
    }
    if (node.layoutSizingHorizontal) output += `${spaces}  W: ${node.layoutSizingHorizontal} (${node.absoluteBoundingBox ? Math.round(node.absoluteBoundingBox.width) : '-'})`;
    if (node.layoutSizingVertical) output += `${spaces}  H: ${node.layoutSizingVertical} (${node.absoluteBoundingBox ? Math.round(node.absoluteBoundingBox.height) : '-'})`;
    output += '\n';

    // Fills (Background/Text Color)
    if (node.fills && node.fills.length > 0) {
        node.fills.forEach(fill => {
            if (fill.type === 'SOLID' && fill.visible !== false) {
                output += `${spaces}  Fill: ${rgbaToHex(fill.color.r, fill.color.g, fill.color.b, fill.color.a)}
`;
            } else if (fill.type === 'GRADIENT_LINEAR') {
                 output += `${spaces}  Fill: GRADIENT
`;
            }
        });
    }

    // Text Styles
    if (node.type === 'TEXT') {
        const s = node.style;
        output += `${spaces}  Text: "${node.characters}"
`;
        output += `${spaces}  Font: ${s.fontFamily} ${s.fontWeight} | Size: ${s.fontSize} | LH: ${s.lineHeightPx ? Math.round(s.lineHeightPx) : s.lineHeightUnit} | LS: ${s.letterSpacing}
`;
    }

    // Border/Stroke
    if (node.strokes && node.strokes.length > 0) {
        node.strokes.forEach(stroke => {
            if (stroke.type === 'SOLID' && stroke.visible !== false) {
                output += `${spaces}  Border: ${node.strokeWeight}px ${rgbaToHex(stroke.color.r, stroke.color.g, stroke.color.b, stroke.color.a)}
`;
            }
        });
    }

    // Effects (Shadows)
    if (node.effects) {
        node.effects.forEach(effect => {
             if (effect.visible !== false) {
                 output += `${spaces}  Effect: ${effect.type}
`;
             }
        });
    }
    
    // Radius
    if (node.cornerRadius) {
        output += `${spaces}  Radius: ${node.cornerRadius}
`;
    }

    console.log(output);

    if (node.children) {
        node.children.forEach(child => processNode(child, indent + 2));
    }
}

try {
    const rawData = fs.readFileSync('figma_node_data.json');
    const data = JSON.parse(rawData);
    // The root might vary, let's find the main frame.
    const rootKey = Object.keys(data.nodes)[0];
    const root = data.nodes[rootKey].document;
    
    processNode(root);

} catch (e) {
    console.error("Error:", e);
}
