const assert = require('node:assert/strict');
const test = require('node:test');
const {format} = require('./format-java.cjs');

test('method and field annotations each occupy their own line', () => {
    const source = 'class Example {\n    @Inject private Object field;\n    @Override public String name() { return "Name"; }\n}\n';
    const expected = 'class Example {\n    @Inject\n    private Object field;\n    @Override\n    public String name() {\n        return "Name";\n    }\n}\n';
    assert.equal(format(source), expected);
    assert.equal(format(expected), expected);
});

test('stacked annotations and annotation arguments stay separate from declarations', () => {
    const source = '@Getter @SuppressWarnings(value = {"first", "second"}) public class Example {\n}\n';
    assert.equal(format(source), '@Getter\n@SuppressWarnings(value = {"first", "second"})\npublic class Example {\n}\n');
});

test('nested annotation arguments do not split the containing annotation', () => {
    const source = '@Outer(value = @Inner("text")) public class Example {\n}\n';
    assert.equal(format(source), '@Outer(value = @Inner("text"))\npublic class Example {\n}\n');
});

test('annotation-like strings and comments are preserved', () => {
    const source = 'class Example {\n    String text = "@Override public";\n    // @Inject private Object field;\n}\n';
    assert.equal(format(source), source);
});

test('annotation type declarations and parameter annotations stay valid', () => {
    const source = '@interface Example {\n    String value();\n}\nclass Use {\n    void take(@Example("value") String value) {\n    }\n}\n';
    assert.equal(format(source), source);
});
