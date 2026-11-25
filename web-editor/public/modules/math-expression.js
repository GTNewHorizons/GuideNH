const invalidExpression = () => NaN;
const maxSourceLength = 4096;
const maxTokens = 512;
const maxDepth = 64;
const cache = new Map();

function factorial(value) {
  if (!Number.isFinite(value) || value < 0 || value > 170) return NaN;
  if (!Number.isInteger(value)) return Math.exp((value - .5) * Math.log(value) - value + .5 * Math.log(2 * Math.PI));
  let result = 1;
  for (let index = 2; index <= value; index += 1) result *= index;
  return result;
}

const mathFunctions = new Map([
  ...["sin", "cos", "tan", "asin", "acos", "atan", "sinh", "cosh", "tanh", "sqrt", "cbrt", "abs", "sign", "exp", "floor", "ceil", "round", "log", "log2", "log10"].map(name => [name, { evaluate: Math[name], minArgs: 1, maxArgs: 1 }]),
  ["ln", { evaluate: Math.log, minArgs: 1, maxArgs: 1 }],
  ["atan2", { evaluate: Math.atan2, minArgs: 2, maxArgs: 2 }],
  ["pow", { evaluate: Math.pow, minArgs: 2, maxArgs: 2 }],
  ["mod", { evaluate: (left, right) => left % right, minArgs: 2, maxArgs: 2 }],
  ["fact", { evaluate: factorial, minArgs: 1, maxArgs: 1 }],
  ...["min", "max", "hypot"].map(name => [name, { evaluate: Math[name], minArgs: 1, maxArgs: 32 }]),
]);

export function createMathFunctions() {
  return new Map([...mathFunctions].map(([name, descriptor]) => [name, { ...descriptor }]));
}

const constants = new Map([["pi", Math.PI], ["tau", 2 * Math.PI], ["phi", (1 + Math.sqrt(5)) / 2], ["e", Math.E]]);

export function compileMathExpression(expression, functions = mathFunctions) {
  const source = String(expression || "0").replace(/^\s*[xy]\s*=\s*/, "")
    .replace(/\bMath\.(PI|E)\b/g, (_, name) => name.toLowerCase()).trim();
  if (source.length > maxSourceLength) return invalidExpression;
  const useCache = functions === mathFunctions;
  if (useCache && cache.has(source)) return cache.get(source);
  let result;
  try {
    const parser = new MathExpressionParser(source, functions);
    result = parser.additive(0);
    result.evaluationCost = parser.tokens;
    if (parser.token !== "end") throw new Error("Unexpected expression token");
  } catch {
    result = invalidExpression;
  }
  if (useCache) {
    if (cache.size >= 128) cache.delete(cache.keys().next().value);
    cache.set(source, result);
  }
  return result;
}

class MathExpressionParser {
  constructor(source, functions) {
    this.source = source;
    this.functions = functions;
    this.position = 0;
    this.tokens = 0;
    this.next();
  }

  next() {
    if (++this.tokens > maxTokens) throw new Error("Expression exceeds its token limit");
    while (/\s/.test(this.source[this.position] || "") && this.position < this.source.length) this.position++;
    if (this.position >= this.source.length) { this.token = "end"; return; }
    const tail = this.source.slice(this.position);
    const numeric = /^(?:\d+(?:\.\d*)?|\.\d+)(?:[eE][+-]?\d+)?/.exec(tail);
    if (numeric) {
      this.token = "number";
      this.value = Number(numeric[0]);
      this.position += numeric[0].length;
      return;
    }
    const identifier = /^[A-Za-z_][A-Za-z_0-9]*/.exec(tail);
    if (identifier) {
      this.token = "identifier";
      this.value = identifier[0];
      this.position += identifier[0].length;
      return;
    }
    const character = this.source[this.position++];
    if (character === "π" || character === "τ") {
      this.token = "identifier";
      this.value = character === "π" ? "pi" : "tau";
    } else if (character === "*" && this.source[this.position] === "*") {
      this.position++;
      this.token = "^";
    } else if ("+-*/%^(),!|√∛²³".includes(character)) {
      this.token = character;
    } else {
      throw new Error("Unsupported expression character");
    }
  }

  additive(depth) {
    let left = this.multiplicative(depth);
    while (this.token === "+" || this.token === "-") {
      const operation = this.token;
      this.next();
      const previous = left;
      const right = this.multiplicative(depth);
      left = operation === "+" ? x => previous(x) + right(x) : x => previous(x) - right(x);
    }
    return left;
  }

  multiplicative(depth) {
    let left = this.unary(depth);
    while (["*", "/", "%", "number", "identifier", "(", "√", "∛"].includes(this.token)) {
      const operation = this.token;
      if (["*", "/", "%"].includes(operation)) this.next();
      const previous = left;
      const right = this.unary(depth);
      left = operation === "/" ? x => previous(x) / right(x)
        : operation === "%" ? x => previous(x) % right(x) : x => previous(x) * right(x);
    }
    return left;
  }

  unary(depth) {
    if (depth > maxDepth) throw new Error("Expression exceeds its nesting limit");
    if (["+", "-", "√", "∛"].includes(this.token)) {
      const operation = this.token;
      this.next();
      const child = this.unary(depth + 1);
      return operation === "-" ? x => -child(x) : operation === "√" ? x => Math.sqrt(child(x))
        : operation === "∛" ? x => Math.cbrt(child(x)) : child;
    }
    let left = this.primary(depth);
    while (this.token === "!") { this.next(); const child = left; left = x => factorial(child(x)); }
    if (this.token === "^") {
      this.next();
      const child = left;
      const right = this.unary(depth + 1);
      left = x => child(x) ** right(x);
    } else if (this.token === "²" || this.token === "³") {
      const power = this.token === "²" ? 2 : 3;
      this.next();
      const child = left;
      left = x => child(x) ** power;
    }
    return left;
  }

  primary(depth) {
    if (this.token === "number") { const value = this.value; this.next(); return () => value; }
    if (this.token === "(" || this.token === "|") {
      const close = this.token === "(" ? ")" : "|";
      this.next();
      const child = this.additive(depth + 1);
      if (this.token !== close) throw new Error("Unclosed expression");
      this.next();
      return close === "|" ? x => Math.abs(child(x)) : child;
    }
    if (this.token !== "identifier") throw new Error("Expected a numeric expression");
    const name = this.value;
    this.next();
    if (this.token !== "(") {
      if (constants.has(name.toLowerCase())) { const value = constants.get(name.toLowerCase()); return () => value; }
      if (name.length === 1) return x => x;
      throw new Error("Unknown mathematical identifier");
    }
    const descriptor = this.functions.get(name);
    if (!descriptor) throw new Error("Unknown mathematical function");
    this.next();
    const argumentsList = [];
    do {
      if (argumentsList.length >= descriptor.maxArgs) throw new Error("Too many function arguments");
      argumentsList.push(this.additive(depth + 1));
      if (this.token !== ",") break;
      this.next();
    } while (true);
    if (this.token !== ")" || argumentsList.length < descriptor.minArgs) throw new Error("Invalid function arguments");
    this.next();
    const evaluate = descriptor.evaluate;
    if (argumentsList.length === 1) return x => evaluate(argumentsList[0](x));
    if (argumentsList.length === 2) return x => evaluate(argumentsList[0](x), argumentsList[1](x));
    const values = new Array(argumentsList.length);
    return x => {
      for (let index = 0; index < values.length; index++) values[index] = argumentsList[index](x);
      return evaluate(...values);
    };
  }
}
