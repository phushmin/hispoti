// The lines it knows, by text; an older Hispoti without the hook never calls it.
var KNOWN = { '夢ならば': 'yume naraba', '사랑해': 'saranghae' };

hispoti.provide('lyrics-romanize', function (input) {
  if (hispoti.features.indexOf('lyrics.romanize') < 0) throw new Error('lyrics.romanize is not a feature of this Hispoti');
  var lines = [];
  input.lines.forEach(function (l) {
    if (KNOWN[l.text]) lines.push({ index: l.index, romanization: KNOWN[l.text] });
  });
  return lines.length ? { lines: lines } : null;
});
