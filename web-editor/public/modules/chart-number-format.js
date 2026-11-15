export function createChartValueFormatter(format = "", unit = "") {
  const template = String(format || "");
  const suffix = String(unit || "");
  const fallback = (value) => (Math.abs(value - Math.round(value)) < 1e-6 ? String(Math.round(value)) : value.toFixed(2)) + suffix;
  if (template.length > 256) return fallback;
  const match = /^((?:[^%]|%%)*)%(?:1\$)?([+ 0-]*)(\d+)?(?:\.(\d+))?([feEgG])((?:[^%]|%%)*)$/.exec(template);
  if (!match) {
    if (template && /^(?:[^%]|%%)*$/.test(template)) return () => template.replaceAll("%%", "%") + suffix;
    return fallback;
  }
  const [, prefix, flags, widthText, precisionText, kind, ending] = match;
  const precision = Number(precisionText ?? 6);
  const width = Number(widthText || 0);
  if (precision > 20 || width > 128 || new Set(flags).size !== flags.length
      || flags.includes("+") && flags.includes(" ") || flags.includes("-") && flags.includes("0")
      || (flags.includes("-") || flags.includes("0")) && !width) return fallback;
  const before = prefix.replaceAll("%%", "%");
  const after = ending.replaceAll("%%", "%") + suffix;
  return (value) => {
    let label;
    if (kind === "f") label = value.toFixed(precision);
    else if (kind.toLowerCase() === "e") label = value.toExponential(precision);
    else {
      const digits = Math.max(1, precision);
      const scientific = value.toExponential(digits - 1);
      const exponent = Number(scientific.split("e")[1]);
      label = exponent < -4 || exponent >= digits ? scientific : value.toFixed(Math.max(0, digits - exponent - 1));
    }
    label = label.replace(/e([+-])(\d)$/, (_match, sign, digit) => `e${sign}0${digit}`);
    if (kind === kind.toUpperCase()) label = label.toUpperCase();
    if (Object.is(value, -0)) label = "-" + label;
    else if (value >= 0) label = (flags.includes("+") ? "+" : flags.includes(" ") ? " " : "") + label;
    if (flags.includes("-")) label = label.padEnd(width, " ");
    else if (flags.includes("0")) {
      const sign = /^[+ -]/.test(label) ? label[0] : "";
      label = sign + label.slice(sign.length).padStart(Math.max(0, width - sign.length), "0");
    } else label = label.padStart(width, " ");
    return before + label + after;
  };
}
