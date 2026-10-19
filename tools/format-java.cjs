const fs = require('fs');
const path = require('path');
const cp = require('child_process');
const root = path.resolve(__dirname, '..');

function files(directory) {
    return fs.readdirSync(directory, {withFileTypes: true}).flatMap(entry => entry.isDirectory() ? files(path.join(directory, entry.name)) : entry.name.endsWith('.java') ? [path.join(directory, entry.name)] : []);
}

function tokens(source) {
    const result = [];
    const pattern = /\/\*[\s\S]*?\*\/|\/\/[^\r\n]*|"(?:\\[\s\S]|[^"\\])*"|'(?:\\[\s\S]|[^'\\])*'|[A-Za-z_$][\w$]*|\d+(?:[\w.]*\w)?|[^\s]/g;
    let match;
    while ((match = pattern.exec(source))) {
        const start = match.index;
        const line = source.lastIndexOf('\n', start - 1) + 1;
        const indentation = source.slice(line, start).match(/^[ \t]*/)[0].replace(/\t/g, '    ').length;
        result.push({value: match[0], start, end: pattern.lastIndex, line, indentation, comment: match[0].startsWith('//') || match[0].startsWith('/*')});
    }
    return result;
}

function format(source) {
    const parts = tokens(source);
    const stack = [];
    let parentheses = 0;
    const parenStack = [];
    let depth = 0;
    let line = -1;
    let baseDepth = 0;
    let lineIndent = 0;
    for (let index = 0; index < parts.length; index++) {
        const token = parts[index];
        if (token.line !== line) {
            line = token.line;
            baseDepth = depth - (token.value === '}' && stack.length && stack[stack.length - 1].block ? 1 : 0);
            const enclosing = [...stack].reverse().find(item => item.block);
            const continuation = parentheses > (enclosing ? enclosing.parentheses : 0) || ['.', '?', ':', '+', '&&', '||'].includes(token.value);
            const array = stack.length && !stack[stack.length - 1].block ? stack[stack.length - 1] : null;
            lineIndent = token.value === '}' && stack.length ? stack[stack.length - 1].indent : array ? array.indent + 4 : continuation ? Math.max(token.indentation, enclosing ? enclosing.indent + 4 : 0) : enclosing ? enclosing.indent + (enclosing.isSwitch && !['case', 'default'].includes(token.value) ? 8 : 4) : 0;
        }
        token.lineIndent = lineIndent;
        token.parent = [...stack].reverse().find(item => item.block);
        token.parenBefore = parentheses;
        if (token.value === '(') {
            parenStack.push({indent: lineIndent + 4 * (depth - baseDepth), header: parts[index - 1] ? parts[index - 1].value : ''});
            parentheses++;
        }
        if (token.value === ')') {
            const opening = parenStack.pop();
            token.statementIndent = opening.indent;
            token.header = opening.header;
            parentheses--;
        }
        if (token.value === '{') {
            const previous = parts[index - 1];
            const value = previous ? previous.value : '';
            const array = value === '=' || value === ']' || (stack.length && !stack[stack.length - 1].block && (value === '{' || value === ','));
            token.block = !array;
            token.indent = previous && previous.value === ')' ? previous.statementIndent : lineIndent + 4 * (depth - baseDepth);
            token.isSwitch = previous && previous.header === 'switch';
            token.parentheses = parentheses;
            stack.push(token);
            if (token.block) { depth++; }
        } else if (token.value === '}') {
            const opening = stack.pop();
            if (!opening) { throw new Error('Unmatched closing brace'); }
            token.closes = opening;
            if (opening.block) { depth--; }
            token.parent = [...stack].reverse().find(item => item.block);
        }
        token.splitStatement = token.value === ';' && token.parent && parentheses === token.parent.parentheses;
    }
    if (stack.length) { throw new Error('Unmatched opening brace'); }
    let output = parts.length ? source.slice(0, parts[0].start) + parts[0].value : source;
    for (let index = 1; index < parts.length; index++) {
        const previous = parts[index - 1];
        const token = parts[index];
        let gap = source.slice(previous.end, token.start).replace(/\t/g, '    ');
        if (gap.includes('\n')) { gap = gap.replace(/[ \t]*$/, ' '.repeat(Math.max(0, token.lineIndent))); }
        const newline = indentation => '\n' + ' '.repeat(Math.max(0, indentation));
        if (previous.block) {
            gap = newline(token.closes === previous ? previous.indent : previous.indent + 4);
        } else if (token.closes && token.closes.block) {
            gap = newline(token.closes.indent);
        } else if (previous.closes && previous.closes.block && !token.comment) {
            if (['else', 'catch', 'finally'].includes(token.value)) { gap = ' '; }
            else if (![';', ',', ')', ']', '.', ':'].includes(token.value) && !gap.includes('\n')) {
                gap = newline(Math.max(token.lineIndent, token.parent ? token.parent.indent + 4 : 0));
            }
        } else if (previous.splitStatement && token.line === previous.line && !token.comment) {
            gap = newline(Math.max(token.lineIndent, previous.parent.indent + 4));
        } else if (token.block && gap.includes('\n') && !previous.comment && [')', 'else', 'try', 'finally', 'do', 'static'].includes(previous.value)) {
            gap = ' ';
        } else if (gap.includes('\n') && !previous.comment && !token.comment && token.parenBefore > (token.parent ? token.parent.parentheses : 0)) {
            gap = ' ';
        }
        output += gap + token.value;
    }
    if (parts.length) { output += source.slice(parts[parts.length - 1].end); }
    const signature = value => tokens(value).map(token => token.value).join('\u0000');
    if (signature(source) !== signature(output)) { throw new Error('Formatting changed Java tokens'); }
    return output.replace(/\r\n/g, '\n');
}

function creationDates(source, relative) {
    return source.replace(/(\/\*\*[\s\S]*?\* @since )\d{2}\/\d{2}\/\d{4}([\s\S]*?\*\/\s*(?:@[\w.]+(?:\([^\n]*\))?\s*)*(?:(?:public|protected|private|static|final|abstract|strictfp)\s+)*(?:class|interface|enum)\s+(\w+))/g, (match, prefix, suffix, name) => {
        const history = cp.execFileSync('git', ['log', '--follow', '--format=%ad', '--date=format:%m/%d/%Y', '-G', '(class|interface|enum)[[:space:]]+' + name + '([^[:alnum:]_]|$)', '--', relative], {cwd: root, encoding: 'utf8'}).trim().split(/\r?\n/).filter(Boolean);
        if (!history.length) { throw new Error('No creation history for ' + relative + ': ' + name); }
        return prefix + history[history.length - 1] + suffix;
    });
}

let changed = 0;
const updates = [];
for (const file of files(path.join(root, 'src'))) {
    const original = fs.readFileSync(file, 'utf8');
    const relative = path.relative(root, file).replace(/\\/g, '/');
    const updated = process.argv.includes('--dates') ? creationDates(original, relative) : format(original);
    if (!process.argv.includes('--dates') && format(updated) !== updated) { throw new Error('Formatter is not idempotent: ' + relative); }
    if (updated !== original) {
        changed++;
        updates.push({file, updated});
        process.stdout.write(relative + '\n');
    }
}
if (!process.argv.includes('--check')) { for (const update of updates) { fs.writeFileSync(update.file, update.updated); } }
process.stdout.write(changed + ' Java files ' + (process.argv.includes('--check') ? 'need changes' : 'updated') + '\n');
if (changed && process.argv.includes('--check')) { process.exitCode = 1; }
